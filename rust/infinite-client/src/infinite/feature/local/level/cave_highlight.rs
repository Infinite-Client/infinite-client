use crate::infinite::infinite_client;
use xross_core::XrossClass;

#[derive(XrossClass)]
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
    fn instance() -> &'static Self {
        &infinite_client()
            .features
            .local
            .level_features
            .cave_highlight
    }
}
