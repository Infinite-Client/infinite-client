use super::BlockHighlightFeature;
use minecraft_rs::glam::IVec3;
use minecraft_rs::mgpu3d::{GpuHandler, MinecraftGpu3D};
use rustc_hash::FxHashMap;
use std::sync::atomic::Ordering;
use std::time::Instant;

pub struct SectionData {
    pub data: [u32; 4096], // 16 * 16 * 16
    pub pos: IVec3,        // x, y (section_index), z
}

pub trait BlockHighlightMethods: GpuHandler {
    /// 1セクション分のデータを解析し、必要なブロックのみを抽出してキャッシュする
    fn push_section_data(&self, section: &SectionData);
    /// プレイヤー位置に基づいたクリーンアップやアニメーション更新を行う
    fn on_tick(&self, player_pos: IVec3, min_y: i32, max_y: i32);
}

impl BlockHighlightMethods for BlockHighlightFeature {
    fn push_section_data(&self, section: &SectionData) {
        // 1. まずは読み取りロックだけで、ハイライト対象のブロックが含まれているか高速スキャン
        let color_map = self.color_cache.read();

        // 色がついているブロックを一時的に集めるためのバッファ
        // ほとんどのセクションは空なので、ここではまだ重い処理はしない
        let mut found_blocks: FxHashMap<IVec3, i32> = FxHashMap::default();

        for i in 0..4096 {
            let block_id = section.data[i];

            if let Some(color) = color_map.get(&block_id) {
                // 相対座標の算出 (Minecraftの一般的なパレットレイアウト: YZX or YXZ)
                // 一般的には i = (y << 8) | (z << 4) | x
                let x = (i & 0xF) as i32;
                let z = ((i >> 4) & 0xF) as i32;
                let y = ((i >> 8) & 0xF) as i32;

                // 絶対座標を計算
                let abs_pos = IVec3::new(
                    (section.pos.x << 4) + x,
                    (section.pos.y << 4) + y,
                    (section.pos.z << 4) + z,
                );

                found_blocks.insert(abs_pos, color.into_raw());
            }
        }

        // 2. 書き込み判定
        let mut pos_writer = self.block_positions.write();

        // 現在のデータと比較して、変更がある場合のみキャッシュを更新
        let current_entry = pos_writer.get(&section.pos);

        if current_entry != Some(&found_blocks) {
            if found_blocks.is_empty() {
                pos_writer.remove(&section.pos);
                self.mesh_cache.write().remove(&section.pos);
                self.section_first_seen.write().remove(&section.pos);
            } else {
                pos_writer.insert(section.pos, found_blocks);

                // データが変わったのでメッシュキャッシュを破棄（次のrenderで再生成）
                self.mesh_cache.write().remove(&section.pos);

                // 既に記録されていなければ、フェードインアニメーションの開始時刻をセット
                let mut seen_writer = self.section_first_seen.write();
                seen_writer.entry(section.pos).or_insert_with(Instant::now);
            }
        }
    }

    fn on_tick(&self, player_pos: IVec3, _min_y: i32, _max_y: i32) {
        // 1. クリーニング判定 (一定間隔、または距離)
        // プレイヤーから離れすぎたキャッシュを削除してメモリを解放する
        let scan_range = self.settings.scan_range.load(Ordering::Relaxed);
        let player_chunk = IVec3::new(player_pos.x >> 4, 0, player_pos.z >> 4);

        // scan_range + アルファ (余裕分) を超えたデータを消す
        let threshold = scan_range + 2;

        // 書き込みロックを取得して一括クリーニング
        let mut pos_writer = self.block_positions.write();
        let mut mesh_writer = self.mesh_cache.write();
        let mut seen_writer = self.section_first_seen.write();

        // プレイヤーから遠いセクションを特定して削除
        pos_writer.retain(|sp, _| {
            let dist_x = (sp.x - player_chunk.x).abs();
            let dist_z = (sp.z - player_chunk.z).abs();

            let keep = dist_x <= threshold && dist_z <= threshold;

            if !keep {
                mesh_writer.remove(sp);
                seen_writer.remove(sp);
            }
            keep
        });
    }
}

/// 登録用の薄いラッパー。実体は常に instance() を参照する
pub struct BlockHighlightHandlerWrapper(&'static BlockHighlightFeature);
impl From<&'static BlockHighlightFeature> for BlockHighlightHandlerWrapper {
    fn from(feature: &'static BlockHighlightFeature) -> Self {
        Self(feature)
    }
}
impl GpuHandler for BlockHighlightHandlerWrapper {
    fn render<'a>(&self, mgpu3d: &mut MinecraftGpu3D<'a>) {
        self.0.render(mgpu3d);
    }
}

impl GpuHandler for BlockHighlightFeature {
    fn render<'a>(&self, _mgpu3d: &mut MinecraftGpu3D<'a>) {}
}
