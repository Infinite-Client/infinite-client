use std::sync::LazyLock;
use xross_core::{XrossClass, xross_methods};
pub mod feature;
mod property;

use feature::FeatureBundler;
#[derive(XrossClass, Default)]
pub struct InfiniteClient {
    pub features: FeatureBundler,
}
#[xross_methods]
impl InfiniteClient {
    #[xross_method]
    pub fn on_initialized() {
        let _ = INFINITE_CLIENT;
        println!("Native Infinite Client has initialized.");
    }
}
static INFINITE_CLIENT: LazyLock<InfiniteClient> = LazyLock::new(InfiniteClient::default);
