pub mod global;
pub mod local;
use global::GlobalFeatureHolder;
use local::LocalFeatureHolder;
#[derive(Default)]
pub struct FeatureBundler {
    pub global: GlobalFeatureHolder,
    pub local: LocalFeatureHolder,
}
