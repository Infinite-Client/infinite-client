// src/main/rust/Cargo.toml
// [lib]
// crate-type = ["cdylib"]
// name = "infinite_client" (ハイフンはアンダースコアに変換されます)

#[unsafe(no_mangle)]
pub extern "C" fn calculate_distance(x: f32, y: f32, z: f32) -> f32 {
    (x.powi(2) + y.powi(2) + z.powi(2)).sqrt()
}
