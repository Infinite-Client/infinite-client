pub mod projectile;

// プロジェクトのルートに関数を公開するための正しい方法
// 以前の空の関数定義を削除し、projectile.rs の関数を直接エクスポートします。
pub use crate::projectile::rust_solve_pitch;

#[unsafe(no_mangle)]
pub extern "C" fn calculate_distance(x: f32, y: f32, z: f32) -> f32 {
    (x.powi(2) + y.powi(2) + z.powi(2)).sqrt()
}