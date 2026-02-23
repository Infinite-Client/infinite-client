use crate::infinite::INFINITE_CLIENT;
use minecraft_rs::color::Color;
use minecraft_rs::glam::IVec3;
use parking_lot::RwLock;
use rustc_hash::FxHashMap;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Instant;
use xross_core::{XrossClass, xross_methods};

mod methods;
mod settings;

use crate::graphics3d::mesh::{BlockMesh, BlockMeshGenerator};
use crate::infinite::property::BlockAndColor;
use methods::BlockHighlightHandlerWrapper;
use methods::{BlockHighlightMethods, SectionData};
use minecraft_rs::mgpu3d::MinecraftGpu3dSystem;
use settings::Animation;
use settings::RenderStyle;
use settings::Settings;
use settings::SettingsSetter;
use settings::ViewFocus;

#[derive(XrossClass)]
#[xross_package("features.local.level.highlight")]
pub struct BlockHighlightFeature {
    pub settings: Settings,
    // 1. スキャンデータ: SectionPos -> (BlockPos -> Color)
    // SectionPos や BlockPos は既存の共通構造体があればそれを利用
    pub block_positions: RwLock<FxHashMap<IVec3, FxHashMap<IVec3, i32>>>,
    // 2. メッシュキャッシュ: 描画用に加工したデータ
    // BlockMesh は Rust 側で定義した頂点データのリスト
    pub mesh_cache: RwLock<FxHashMap<IVec3, BlockMesh>>,
    // 3. アニメーション用: セクションが最初に見つかった時刻 (FadeIn用)
    pub section_first_seen: RwLock<FxHashMap<IVec3, Instant>>,
    // 4. スキャン進捗管理 (Atomicで不変参照のまま更新可能にする)
    pub current_scan_index: AtomicUsize,
    // 5. 設定変更検知用
    pub last_settings_hash: AtomicUsize,
    // 6. カラー検索の高速化 (String引くより高速)
    pub color_cache: RwLock<FxHashMap<u32, Color>>,
    pub render_handler_id: AtomicUsize,
}
impl Default for BlockHighlightFeature {
    fn default() -> Self {
        Self {
            settings: Settings::default(),
            block_positions: RwLock::new(FxHashMap::default()),
            mesh_cache: RwLock::new(FxHashMap::default()),
            section_first_seen: RwLock::new(FxHashMap::default()),
            current_scan_index: AtomicUsize::new(0),
            last_settings_hash: AtomicUsize::new(0),
            color_cache: RwLock::new(FxHashMap::default()),
            // ここで明確に INVALID_HANDLER_ID を指定
            render_handler_id: AtomicUsize::new(MinecraftGpu3dSystem::INVALID_HANDLER_ID),
        }
    }
}
// ロジックの実体はすべてこちらに集約する
impl SettingsSetter for BlockHighlightFeature {
    fn update_highlight_list(buff: &[u64]) {
        let instance = Self::instance();
        // 1. 設定値を更新 (Vec<BlockAndColor> に変換)
        let new_items: Vec<BlockAndColor> = buff.iter().map(|&b| BlockAndColor::from(b)).collect();
        // 2. color_cache (FxHashMap<u32, i32>) を即座に更新
        {
            let mut color_writer = instance.color_cache.write();
            color_writer.clear();
            // 変換済みのデータから直接 HashMap を作る
            for item in &new_items {
                // Color 構造体から i32 (ARGB等) を取り出す
                // ※ .as_argb() や .0 など、Colorの実装に合わせて調整してください
                color_writer.insert(item.id, item.color);
            }
        }
        // 3. 設定を保存
        {
            let mut writer = instance.settings.blocks_to_highlight.write();
            *writer = new_items;
        }
        // 4. 描画キャッシュを破棄
        instance.clear_render_cache();
    }
    fn set_scan_range(val: i32) {
        Self::instance().settings.set_scan_range(val);
    }
    fn set_render_range(val: i32) {
        Self::instance().settings.set_render_range(val);
    }

    fn set_max_draw_count(val: i32) {
        Self::instance().settings.set_max_draw_count(val);
    }

    fn set_max_y(val: i32) {
        Self::instance().settings.set_max_y(val);
    }

    fn set_sky_light_threshold(val: i32) {
        Self::instance().settings.set_sky_light_threshold(val);
    }

    fn set_player_exclusion_radius(val: i32) {
        Self::instance().settings.set_player_exclusion_radius(val);
    }

    fn set_check_surroundings(enabled: bool) {
        Self::instance().settings.set_check_surroundings(enabled);
    }

    fn set_line_width_bits(bits: u32) {
        Self::instance()
            .settings
            .set_line_width(f32::from_bits(bits));
    }

    fn set_render_style(ordinal: u32) {
        let instance = Self::instance();
        let style = RenderStyle::from_u32(ordinal);
        instance.settings.set_render_style(style);

        // スタイル（線のみ/面のみ等）が変わるとメッシュの作り直しが必要
        instance.clear_render_cache();
    }

    fn set_view_focus(ordinal: u32) {
        Self::instance()
            .settings
            .set_view_focus(ViewFocus::from_u32(ordinal));
    }

    fn set_animation(ordinal: u32) {
        Self::instance()
            .settings
            .set_animation(Animation::from_u32(ordinal));
    }
}

// 外部(Kotlin)公開用の窓口
#[xross_methods]
impl BlockHighlightFeature {
    fn instance() -> &'static Self {
        &INFINITE_CLIENT
            .features
            .local
            .level_features
            .block_highlight
    }

    #[xross_method]
    pub fn update_highlight_list(buff: &[u64]) {
        <Self as SettingsSetter>::update_highlight_list(buff);
    }

    #[xross_method]
    pub fn set_scan_range(val: i32) {
        <Self as SettingsSetter>::set_scan_range(val);
    }
    #[xross_method]
    pub fn set_render_range(val: i32) {
        <Self as SettingsSetter>::set_render_range(val);
    }
    #[xross_method]
    pub fn set_max_draw_count(val: i32) {
        <Self as SettingsSetter>::set_max_draw_count(val);
    }
    #[xross_method]
    pub fn set_max_y(val: i32) {
        <Self as SettingsSetter>::set_max_y(val);
    }
    #[xross_method]
    pub fn set_sky_light_threshold(val: i32) {
        <Self as SettingsSetter>::set_sky_light_threshold(val);
    }
    #[xross_method]
    pub fn set_player_exclusion_radius(val: i32) {
        <Self as SettingsSetter>::set_player_exclusion_radius(val);
    }
    #[xross_method]
    pub fn set_check_surroundings(enabled: bool) {
        <Self as SettingsSetter>::set_check_surroundings(enabled);
    }
    #[xross_method]
    pub fn set_line_width_bits(bits: u32) {
        <Self as SettingsSetter>::set_line_width_bits(bits);
    }
    #[xross_method]
    pub fn set_render_style(ordinal: u32) {
        <Self as SettingsSetter>::set_render_style(ordinal);
    }
    #[xross_method]
    pub fn set_view_focus(ordinal: u32) {
        <Self as SettingsSetter>::set_view_focus(ordinal);
    }
    #[xross_method]
    pub fn set_animation(ordinal: u32) {
        <Self as SettingsSetter>::set_animation(ordinal);
    }
    fn clear_render_cache(&self) {
        self.mesh_cache.write().clear();
        self.block_positions.write().clear();
        self.section_first_seen.write().clear();
    }

    #[xross_method(critical)]
    pub fn on_tick(player_x: f64, player_y: f64, player_z: f64, min_y: i32, max_y: i32) {
        let instance = Self::instance();
        // 1. f64 座標を IVec3 (ブロック座標) に変換
        // floor() を使うことで、負の座標（-0.5 など）も正しく -1 に変換されます
        let player_pos = IVec3::new(
            player_x.floor() as i32,
            player_y.floor() as i32,
            player_z.floor() as i32,
        );
        // 2. 実装済みのトレイトメソッドを呼び出し
        // &self に対して BlockHighlightMethods のメソッドを呼ぶ
        <Self as BlockHighlightMethods>::on_tick(instance, player_pos, min_y, max_y);
    }
    #[xross_method(critical(heap_access))]
    pub fn push_section_data(cx: i32, cy: i32, cz: i32, data: &[u32]) {
        if data.len() != 4096 {
            return;
        }
        let instance = Self::instance();
        let mut buf = [0u32; 4096];
        buf.copy_from_slice(data);
        let section = SectionData {
            data: buf,
            pos: IVec3::new(cx, cy, cz),
        };
        {
            let mesh_length = instance.mesh_cache.read().len();
            println!("mesh_len: {}", mesh_length);
            let color_length = instance.color_cache.read().len();
            println!("color_length: {}", color_length);
            let render_handler_id = instance.render_handler_id.load(Ordering::Relaxed);
            println!("render_handler_id: {}", render_handler_id);
        }
        <Self as BlockHighlightMethods>::push_section_data(instance, &section);
    }
    #[xross_method(critical)]
    pub fn on_enabled() {
        let instance = Self::instance();

        // 二重登録防止
        if instance.render_handler_id.load(Ordering::Relaxed)
            != MinecraftGpu3dSystem::INVALID_HANDLER_ID
        {
            return;
        }

        // Static参照を包んだラッパーをBoxに入れて登録
        let id = MinecraftGpu3dSystem::add_handler(Box::new(BlockHighlightHandlerWrapper::from(
            instance,
        )));

        instance.render_handler_id.store(id, Ordering::Relaxed);
    }
    #[xross_method(critical)]
    pub fn on_disabled() {
        let instance = Self::instance();
        let id = instance
            .render_handler_id
            .swap(MinecraftGpu3dSystem::INVALID_HANDLER_ID, Ordering::Relaxed);

        if id != MinecraftGpu3dSystem::INVALID_HANDLER_ID {
            MinecraftGpu3dSystem::del_handler(id);
        }
    }
    fn generate_mesh_from_map(&self, blocks: &FxHashMap<IVec3, i32>) -> BlockMesh {
        let mut generator = BlockMeshGenerator::new();

        // FxHashMap の中身を generator に登録
        for (pos, &color) in blocks {
            generator.add_block(pos.x, pos.y, pos.z, color);
        }

        // Greedy Meshing 等の重い計算を実行
        generator.generate();

        // 出来上がったバッファを BlockMesh に変換して返す
        BlockMesh::from_generator(&generator)
    }
    /// アニメーション設定に基づき、現在のアルファ倍率を 0.0 ~ 1.0 で計算する
    pub fn calculate_animation_alpha(&self, sp: IVec3, now: Instant) -> f32 {
        let animation_type = self.settings.animation.load(Ordering::Relaxed);

        // 1. FadeIn アニメーションの計算
        let fade_multiplier = if animation_type == Animation::FadeIn {
            let seen_reader = self.section_first_seen.read();
            if let Some(&first_seen) = seen_reader.get(&sp) {
                // 0.6秒 (600ms) で完全にフェードイン
                let elapsed = now.duration_since(first_seen).as_secs_f32();
                (elapsed / 0.6).min(1.0)
            } else {
                1.0
            }
        } else {
            1.0
        };

        // 2. Pulse アニメーションの計算
        let pulse_multiplier = if animation_type == Animation::Pulse {
            // Instant から経過秒数を取得してサイン波を作る
            // Minecraft の起動時間やエポック秒の代わりに now を使用
            // Kotlin版: (sin(time * 4.0) * 0.5 + 0.5) * 0.4 + 0.6
            // ※ Rust の f32::sin はラジアン。周期を調整
            let time = self.get_animation_time(now);
            ((time * 4.0).sin() * 0.5 + 0.5) * 0.4 + 0.6
        } else {
            1.0
        };

        // 両方の効果を掛け合わせる (FadeIn 中に Pulse させることも可能)
        fade_multiplier * pulse_multiplier
    }
    /// アニメーション用の基準時間 (秒) を取得
    /// 単純に Instant::now() を使うと各セクションでズレるため、
    /// render の冒頭で取得した共通の now を使用することを推奨
    fn get_animation_time(&self, now: Instant) -> f32 {
        // プログラム起動時からの経過時間などを利用して一貫したサイン波を作る
        // Staticな基準点がない場合は、適当な大きな数からの差分でもOK
        static START_TIME: std::sync::OnceLock<Instant> = std::sync::OnceLock::new();
        let start = START_TIME.get_or_init(Instant::now);
        now.duration_since(*start).as_secs_f32()
    }
}
