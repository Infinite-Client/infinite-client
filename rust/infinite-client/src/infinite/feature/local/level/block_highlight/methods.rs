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

        for i in 0..4096 {
            let block_id = section.data[i];
            if let Some(color) = color_map.get(&block_id) {
                let x = (i & 0xF) as i32;
                let z = ((i >> 4) & 0xF) as i32;
                let y = ((i >> 8) & 0xF) as i32;

                let abs_pos = IVec3::new(
                    (section.pos.x << 4) + x,
                    (section.pos.y << 4) + y,
                    (section.pos.z << 4) + z,
                );
                found_blocks.insert(abs_pos, color.into_raw());
            }
        }

        let mut pos_writer = self.block_positions.write();
        let current_entry = pos_writer.get(&section.pos);

        // --- ここで変更判定とメッシュ生成 ---
        if current_entry != Some(&found_blocks) {
            if found_blocks.is_empty() {
                pos_writer.remove(&section.pos);
                self.mesh_cache.write().remove(&section.pos);
                self.section_first_seen.write().remove(&section.pos);
            } else {
                // 1. 新しいブロックデータを保存
                pos_writer.insert(section.pos, found_blocks.clone());

                // 2. その場でメッシュを生成
                // found_blocks をそのまま generator に渡す
                let mesh = self.generate_mesh_from_map(&found_blocks);

                // 3. メッシュキャッシュを更新
                self.mesh_cache.write().insert(section.pos, mesh);

                // 4. アニメーション開始時刻のセット
                self.section_first_seen
                    .write()
                    .entry(section.pos)
                    .or_insert_with(Instant::now);
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

        let mut total_drawn: i32 = 0; // 型を明示
        let max_count = settings.max_draw_count.load(Ordering::Relaxed);
        let line_width = settings.line_width.load(Ordering::Relaxed);

        for (sp, _) in render_list {
            if total_drawn > max_count {
                break;
            }

            let mesh_cache = self.mesh_cache.read();
            let Some(mesh) = mesh_cache.get(&sp) else {
                continue;
            };
            if mesh.is_empty() {
                continue;
            }

            let alpha = self.calculate_animation_alpha(sp, now);

            // --- Quad (Faces) 描画 ---
            if style != RenderStyle::Lines {
                for q in mesh.quads.chunks_exact(28) {
                    let v1 = DVec3::new(q[0] as f64, q[1] as f64, q[2] as f64);
                    let v2 = DVec3::new(q[7] as f64, q[8] as f64, q[9] as f64);
                    let v3 = DVec3::new(q[14] as f64, q[15] as f64, q[16] as f64);
                    let v4 = DVec3::new(q[21] as f64, q[22] as f64, q[23] as f64);

                    // q[3] (f32) -> u32 (bits) -> Color -> 修正Color -> u32
                    let color = Self::apply_alpha_to_color_bits(q[3].to_bits(), alpha);
                    mgpu3d.quad_fill(v1, v2, v3, v4, color, true);
                }
            }

            // --- Line 描画 ---
            if style != RenderStyle::Faces {
                for l in mesh.lines.chunks_exact(8) {
                    let start = DVec3::new(l[0] as f64, l[1] as f64, l[2] as f64);
                    let end = DVec3::new(l[4] as f64, l[5] as f64, l[6] as f64);

                    let color = Self::apply_alpha_to_color_bits(l[3].to_bits(), alpha);
                    mgpu3d.line(start, end, color, line_width, true);
                }
            }

            // usize を i32 にキャストして加算
            if let Some(blocks) = self.block_positions.read().get(&sp) {
                total_drawn += blocks.len() as i32;
            }
        }
    }
}

impl BlockHighlightFeature {
    /// u32(bits)の色情報にアルファ倍率を適用して u32 で返す
    fn apply_alpha_to_color_bits(color_bits: u32, alpha_multiplier: f32) -> u32 {
        let mut color = Color::from(color_bits);
        color.a = ((color.a as f32) * alpha_multiplier).round() as u8;
        color.into() // Color -> u32 (From実装を利用)
    }
}
