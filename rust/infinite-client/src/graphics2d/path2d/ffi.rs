use super::Path2D;

/// Create new Path2D
/// # Safety
/// Please call from ffi.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_new() -> *mut Path2D {
    Box::into_raw(Box::new(Path2D::new()))
}
/// Drop Path2D
/// # Safety
/// Please check pointer.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_drop(ptr: *mut Path2D) {
    if ptr.is_null() {
        return;
    }
    let path2d = unsafe { Box::from_raw(ptr) };
    drop(path2d);
}
