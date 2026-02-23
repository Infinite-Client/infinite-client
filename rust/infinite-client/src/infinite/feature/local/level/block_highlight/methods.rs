use super::BlockHighlightFeature;
use super::settings::RenderStyle;
use super::settings::ViewFocus;
use minecraft_rs::color::Color;
use minecraft_rs::glam::{DVec3, IVec3};
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
        let color_map = self.color_cache.read();
        let mut found_blocks: FxHashMap<IVec3, i32> = FxHashMap::default();

        // 1. スキャンとフィルタリング
        for i in 0..4096 {
            let block_id = section.data[i];

            // color_map に存在し、かつ Alpha が 0 でない場合のみ抽出
            if let Some(color) = color_map.get(&block_id)
                && color.a > 0
            {
                let x = (i & 0xF) as i32;
                let z = ((i >> 4) & 0xF) as i32;
                let y = ((i >> 8) & 0xF) as i32;

                let abs_pos = IVec3::new(
                    (section.pos.x << 4) + x,
                    (section.pos.y << 4) + y,
                    (section.pos.z << 4) + z,
                );
                // メッシュ生成時に透明度計算をやり直すため、
                // ここでは元の色（Alpha込）をそのまま入れる
                found_blocks.insert(abs_pos, color.into_raw());
            }
        }

        // 2. キャッシュの更新判定
        let mut pos_writer = self.block_positions.write();
        let mut mesh_writer = self.mesh_cache.write();
        let mut seen_writer = self.section_first_seen.write();

        if found_blocks.is_empty() {
            // 有効なブロックが一つもなければ、当該セクションのキャッシュをすべて破棄
            if pos_writer.remove(&section.pos).is_some() {
                mesh_writer.remove(&section.pos);
                seen_writer.remove(&section.pos);
            }
            return;
        }

        // 前回のデータと比較して変更がある場合のみ、重い処理（メッシュ生成）を行う
        let is_changed = match pos_writer.get(&section.pos) {
            Some(old) => old != &found_blocks,
            None => true,
        };

        if is_changed {
            // メッシュ生成の前にデータを確定させる
            pos_writer.insert(section.pos, found_blocks.clone());

            // メッシュ生成（Alpha > 0 のブロックのみが含まれるため、透明な線は生成されない）
            let mesh = self.generate_mesh_from_map(&found_blocks);

            if mesh.is_empty() {
                mesh_writer.remove(&section.pos);
            } else {
                mesh_writer.insert(section.pos, mesh);
                // 新規発見時のみアニメーション時刻を記録
                seen_writer.entry(section.pos).or_insert_with(Instant::now);
            }
        }
    }
    fn on_tick(&self, player_pos: IVec3, _min_y: i32, _max_y: i32) {
        // 削除の閾値は scan_range ではなく、render_range を基準にするのが安全です
        let render_range = self.settings.render_range.load(Ordering::Relaxed);
        // セクション単位の距離に変換 (1セクション=16ブロック)
        let threshold = (render_range / 16) + 2;

        // プレイヤーの現在セクション座標 (X, Y, Z すべて取得)
        let px = player_pos.x >> 4;
        let py = player_pos.y >> 4;
        let pz = player_pos.z >> 4;

        let mut pos_writer = self.block_positions.write();
        let mut mesh_writer = self.mesh_cache.write();
        let mut seen_writer = self.section_first_seen.write();

        // 削除対象のキーを一旦収集（借用の競合を避けるため）
        pos_writer.retain(|sp, _| {
            let dist_x = (sp.x - px).abs();
            let dist_y = (sp.y - py).abs(); // Y軸の差も計算に含める
            let dist_z = (sp.z - pz).abs();

            // X, Y, Z すべてにおいて範囲内にあるものだけ残す
            let keep = dist_x <= threshold && dist_y <= threshold && dist_z <= threshold;

            if !keep {
                // 範囲外なら関連するキャッシュをすべて削除
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
    fn render<'a>(&self, mgpu3d: &mut MinecraftGpu3D<'a>) {
        let matrixes = mgpu3d.matrixes();
        let camera_pos = matrixes.camera_position;

        let look_vec = DVec3::new(
            -matrixes.model_view.z_axis.x,
            -matrixes.model_view.z_axis.y,
            -matrixes.model_view.z_axis.z,
        );

        let settings = &self.settings;
        let render_range_sq = settings.render_range.load(Ordering::Relaxed).pow(2) as f64;
        let style = settings.render_style.load(Ordering::Relaxed);
        let view_focus = settings.view_focus.load(Ordering::Relaxed);

        let now = Instant::now();
        let mut render_list = Vec::new();

        {
            let positions = self.block_positions.read();
            for (&sp, blocks) in positions.iter() {
                if blocks.is_empty() {
                    continue;
                }

                let section_center = DVec3::new(
                    (sp.x << 4) as f64 + 8.0,
                    (sp.y << 4) as f64 + 8.0,
                    (sp.z << 4) as f64 + 8.0,
                );

                let diff = section_center - camera_pos;
                let dist_sq = diff.length_squared();
                if dist_sq > render_range_sq * 4.0 {
                    continue;
                }

                let dot = if dist_sq > 0.001 {
                    look_vec.dot(diff.normalize())
                } else {
                    1.0
                };

                let score = match view_focus {
                    ViewFocus::Strict => {
                        if dot < 0.2 {
                            -1.0
                        } else {
                            dot / (dist_sq + 1.0)
                        }
                    }
                    ViewFocus::Balanced => (dot + 1.5) / (dist_sq + 1.0),
                    ViewFocus::None => 1.0 / (dist_sq + 1.0),
                };

                if score >= 0.0 {
                    render_list.push((sp, score));
                }
            }
        }

        render_list.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        let mut total_drawn: usize = 0; // 型を明示
        let max_count = settings.max_draw_count.load(Ordering::Relaxed);
        let line_width = settings.line_width.load(Ordering::Relaxed);

        {
            let block_positions = self.block_positions.read();
            let mesh_cache = self.mesh_cache.read();

            for (sp, _) in render_list {
                if total_drawn > max_count as usize {
                    break;
                }
                // --- 1. キャッシュの存在確認 (on_tick で消された直後の対策) ---
                // ここで read lock を取得し、確実に存在する場合のみ続行
                let Some(mesh) = mesh_cache.get(&sp) else {
                    continue;
                };

                if mesh.is_empty() {
                    continue;
                }

                // --- 2. 距離によるハードカット (透明メッシュ防止) ---
                // settings.render_range を超えているものは描画命令すら出さない
                let dist_sq = {
                    let camera_pos = mgpu3d.matrixes().camera_position;
                    let section_center = DVec3::new(
                        (sp.x << 4) as f64 + 8.0,
                        (sp.y << 4) as f64 + 8.0,
                        (sp.z << 4) as f64 + 8.0,
                    );
                    (section_center - camera_pos).length_squared()
                };

                if dist_sq > (render_range_sq * 1.2) {
                    // 遊びを持たせてカット
                    continue;
                }

                // --- 3. アニメーション/アルファの最終チェック ---
                let alpha = self.calculate_animation_alpha(sp, now);
                if alpha <= 0.005 {
                    // ほぼ透明ならスキップ
                    continue;
                }
                // --- Quad (Faces) 描画 ---
                if style != RenderStyle::Lines {
                    for q in mesh.quads.chunks_exact(28) {
                        let v1 = DVec3::new(q[0] as f64, q[1] as f64, q[2] as f64);
                        let v2 = DVec3::new(q[7] as f64, q[8] as f64, q[9] as f64);
                        let v3 = DVec3::new(q[14] as f64, q[15] as f64, q[16] as f64);
                        let v4 = DVec3::new(q[21] as f64, q[22] as f64, q[23] as f64);
                        let color = Color::from(q[3]).alpha(alpha);
                        if color.a == 0 {
                            continue;
                        }
                        mgpu3d.quad_fill(v1, v2, v3, v4, color.into(), false);
                    }
                }

                // --- Line 描画 ---
                if style != RenderStyle::Faces {
                    for l in mesh.lines.chunks_exact(8) {
                        let start = DVec3::new(l[0] as f64, l[1] as f64, l[2] as f64);
                        let end = DVec3::new(l[4] as f64, l[5] as f64, l[6] as f64);
                        let color = Color::from(l[3]).alpha(alpha);
                        if color.a == 0 {
                            continue;
                        }
                        mgpu3d.line(start, end, color.into(), line_width, false);
                    }
                }
                // usize を i32 にキャストして加算
                if let Some(blocks) = block_positions.get(&sp) {
                    total_drawn += blocks.len();
                }
            }
        }
    }
}
