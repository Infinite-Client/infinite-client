use crate::infinite::INFINITE_CLIENT;

pub struct BlockHighlightFeature {}
impl Default for BlockHighlightFeature {
    fn default() -> Self {
        Self::new()
    }
}
impl BlockHighlightFeature {
    pub fn new() -> Self {
        Self {}
    }
    fn instance() -> &'static Self {
        &INFINITE_CLIENT
            .features
            .local
            .level_features
            .block_highlight
    }
}
