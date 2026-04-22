use crate::infinite::INFINITE_CLIENT;
use xross_core::XrossClass;

#[derive(XrossClass)]
#[xross_package("features.local.level.highlight")]
pub struct CaveHighlightFeature {}
impl Default for CaveHighlightFeature {
    fn default() -> Self {
        Self::new()
    }
}
impl CaveHighlightFeature {
    pub fn new() -> Self {
        Self {}
    }
    pub fn instance() -> &'static Self {
        &INFINITE_CLIENT.features.local.level_features.cave_highlight
    }
}
