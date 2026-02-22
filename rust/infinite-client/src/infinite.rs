use std::sync::OnceLock;
use xross_core::{XrossClass, xross_methods};
pub mod feature;
use feature::FeatureBundler;
#[derive(XrossClass, Default)]
pub struct InfiniteClient {
    pub features: FeatureBundler,
}
#[xross_methods]
impl InfiniteClient {
    #[xross_method]
    pub fn on_initialized() {
        let _ = infinite_client();
        println!("Native Infinite Client has initialized.");
    }
}
static INFINITE_CLIENT: OnceLock<InfiniteClient> = OnceLock::new();
fn infinite_client() -> &'static InfiniteClient {
    INFINITE_CLIENT.get_or_init(InfiniteClient::default)
}
