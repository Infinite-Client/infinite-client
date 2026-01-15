// bindings.rs を include すると、その中にある `pub mod net;` が
// OUT_DIR 内の同名ファイルを探しに行きます。
include!(concat!(env!("OUT_DIR"), "/bindings.rs"));

use std::ffi::c_void;

#[unsafe(no_mangle)]
pub unsafe extern "C" fn register_vtable(_class_hash: u64, _table_ptr: *const c_void) -> i32 {
    0
}

// Java 側から個別に初期化する場合のエントリポイント例
#[unsafe(no_mangle)]
pub unsafe extern "C" fn init_level_vtable(_ptr: *const c_void) {
    // 例:
    // let table: net::minecraft::world::level::LevelVTable = std::ptr::read(_ptr as *const _);
    // let _ = net::minecraft::world::level::VTABLE.set(table);
}
