use std::sync::OnceLock;
use xross_core::{XrossClass, xross_methods};

#[derive(XrossClass, Default)]
pub struct InfiniteClient;
#[xross_methods]
impl InfiniteClient {
    #[xross_method]
    pub fn on_initialized() {
        let instance = infinite_client();
        println!("Infinite Client:");
    }
}
static INFINITE_CLIENT: OnceLock<InfiniteClient> = OnceLock::new();
fn infinite_client() -> &'static InfiniteClient {
    INFINITE_CLIENT.get_or_init(InfiniteClient::default)
}
