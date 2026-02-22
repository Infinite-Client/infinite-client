use crate::infinite::infinite_client;

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
        &infinite_client()
            .features
            .local
            .level_features
            .block_highlight
    }
}
