pub mod level;
use level::LevelFeatureCategory;
#[derive(Default)]
pub struct LocalFeatureHolder {
    pub level_features: LevelFeatureCategory,
}
