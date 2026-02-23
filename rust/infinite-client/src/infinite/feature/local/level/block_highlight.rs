use crate::infinite::INFINITE_CLIENT;
use xross_core::{XrossClass, xross_methods};

mod settings;
use crate::infinite::property::BlockAndColor;
use settings::Settings;
#[derive(XrossClass, Default)]
#[xross_package("features.local.level.highlight")]
pub struct BlockHighlightFeature {
    settings: Settings,
}
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
    pub fn update_settings_highlight_list(buff: &[u64]) {
        let instance = Self::instance();
        let mut writer = instance.settings.blocks_to_highlight.write();
        writer.clear();
        writer.extend(buff.iter().map(|&b| BlockAndColor::from(b)));
    }
    #[xross_method(critical)]
    pub fn update_settings_b4(idx: u8, num: u32) {
        Self::instance().settings.apply_b4_setting(idx, num);
    }
}
