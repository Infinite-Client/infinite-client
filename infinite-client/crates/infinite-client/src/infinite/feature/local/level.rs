pub mod block_highlight;
pub mod cave_highlight;

use block_highlight::BlockHighlightFeature;
use cave_highlight::CaveHighlightFeature;

#[derive(Default)]
pub struct LevelFeatureCategory {
    pub cave_highlight: CaveHighlightFeature,
    pub block_highlight: BlockHighlightFeature,
}
