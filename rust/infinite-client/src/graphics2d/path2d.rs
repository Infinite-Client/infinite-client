use std::ptr;

/// Rust側での構造体定義。
/// フィールドを持たせても、Java側からは不透明なポインタとして扱われます。
pub struct Path2D {
    // 例として内部状態を持たせています
    pub commands: Vec<String>,
}

impl Path2D {
    fn new() -> Self {
        Self {
            commands: Vec::new(),
        }
    }
}

// --- FFI functions ---

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_new() -> *mut Path2D {
    // Boxに入れてヒープに固定し、その生ポインタを返す
    // Java側はこれを MemorySegment (Address) として受け取る
    Box::into_raw(Box::new(Path2D::new()))
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_clone(ptr: *mut Path2D) -> *mut Path2D {
    if ptr.is_null() {
        return ptr::null_mut();
    }
    // ポインタから一時的に参照を作成し、Cloneして再度Box化する
    let old_struct = &*ptr;
    let new_struct = Path2D {
        commands: old_struct.commands.clone(),
    };
    Box::into_raw(Box::new(new_struct))
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_drop(ptr: *mut Path2D) {
    if !ptr.is_null() {
        // Box::from_raw で所有権を取り戻し、スコープを抜けることでメモリが解放される
        let _ = Box::from_raw(ptr);
    }
}
