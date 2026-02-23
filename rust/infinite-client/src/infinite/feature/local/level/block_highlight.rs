use crate::infinite::INFINITE_CLIENT;
use xross_core::{XrossClass, xross_methods};

mod settings;
use crate::infinite::property::BlockAndColor;
use settings::Animation;
use settings::RenderStyle;
use settings::Settings;
use settings::SettingsSetter;
use settings::ViewFocus;

#[derive(XrossClass, Default)]
#[xross_package("features.local.level.highlight")]
pub struct BlockHighlightFeature {
    settings: Settings,
}

// ロジックの実体はすべてこちらに集約する
impl SettingsSetter for BlockHighlightFeature {
    fn update_highlight_list(buff: &[u64]) {
        let instance = Self::instance();
        let mut writer = instance.settings.blocks_to_highlight.write();
        writer.clear();
        writer.extend(buff.iter().map(|&b| BlockAndColor::from(b)));
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
        Self::instance()
            .settings
            .set_render_style(RenderStyle::from_u32(ordinal));
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
}
