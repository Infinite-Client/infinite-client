use super::{Color, Path2D};
use lyon::path::{FillRule, LineCap, LineJoin};

/// Path2Dインスタンスの生成
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_new() -> *mut Path2D {
    Box::into_raw(Box::new(Path2D::new()))
}

/// Path2Dインスタンスの破棄
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_drop(ptr: *mut Path2D) {
    if !ptr.is_null() {
        let _ = unsafe { Box::from_raw(ptr) };
    }
}

/// パスのリセット
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_begin(ptr: *mut Path2D) {
    let path = unsafe { &mut *ptr };
    path.begin();
}

/// ペンの基本設定
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_set_pen(
    ptr: *mut Path2D,
    width: f64,
    color_raw: i32,
    cap: i32,
    join: i32,
    enable_gradient: bool,
) {
    let path = unsafe { &mut *ptr };
    path.pen.width = width;
    path.pen.color = Color::from_raw(color_raw);
    path.pen.is_gradient_enabled = enable_gradient;

    // LyonのEnumへマッピング (Kotlin側のordinalと合わせる)
    path.pen.line_cap = match cap {
        0 => LineCap::Butt,
        1 => LineCap::Square,
        2 => LineCap::Round,
        _ => LineCap::Butt,
    };
    path.pen.line_join = match join {
        0 => LineJoin::Miter,
        1 => LineJoin::MiterClip,
        2 => LineJoin::Round,
        3 => LineJoin::Bevel,
        _ => LineJoin::Miter,
    };
}

// --- パス構築コマンド ---

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_move_to(ptr: *mut Path2D, x: f64, y: f64) {
    let path = unsafe { &mut *ptr };
    path.move_to(x, y);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_line_to(ptr: *mut Path2D, x: f64, y: f64) {
    let path = unsafe { &mut *ptr };
    path.line_to(x, y);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_bezier_curve_to(
    ptr: *mut Path2D,
    cp1x: f64,
    cp1y: f64,
    cp2x: f64,
    cp2y: f64,
    x: f64,
    y: f64,
) {
    let path = unsafe { &mut *ptr };
    path.bezier_curve_to(cp1x, cp1y, cp2x, cp2y, x, y);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_quadratic_curve_to(
    ptr: *mut Path2D,
    cpx: f64,
    cpy: f64,
    x: f64,
    y: f64,
) {
    let path = unsafe { &mut *ptr };
    path.quadratic_curve_to(cpx, cpy, x, y);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_arc(
    ptr: *mut Path2D,
    x: f64,
    y: f64,
    radius: f64,
    start_angle: f64,
    end_angle: f64,
    ccw: bool,
) {
    let path = unsafe { &mut *ptr };
    path.arc(x, y, radius, start_angle, end_angle, ccw);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_arc_to(
    ptr: *mut Path2D,
    x1: f64,
    y1: f64,
    x2: f64,
    y2: f64,
    radius: f64,
) {
    let path = unsafe { &mut *ptr };
    path.arc_to(x1, y1, x2, y2, radius);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_ellipse(
    ptr: *mut Path2D,
    x: f64,
    y: f64,
    rx: f64,
    ry: f64,
    rot: f64,
    start: f64,
    end: f64,
    ccw: bool,
) {
    let path = unsafe { &mut *ptr };
    path.ellipse(x, y, rx, ry, rot, start, end, ccw);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_close(ptr: *mut Path2D) {
    let path = unsafe { &mut *ptr };
    path.close();
}

// --- テッセレーション実行 ---

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_tessellate_fill(ptr: *mut Path2D, rule: i32) {
    let path = unsafe { &mut *ptr };
    let fill_rule = if rule == 0 {
        FillRule::EvenOdd
    } else {
        FillRule::NonZero
    };
    path.tessellate_fill(fill_rule);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_tessellate_stroke(ptr: *mut Path2D) {
    let path = unsafe { &mut *ptr };
    // Penに保持されている設定を使用してストロークを生成
    path.tessellate_stroke(
        path.pen.line_cap,
        path.pen.line_join,
        path.pen.is_gradient_enabled,
    );
}

// --- バッファアクセス ---

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_get_buffer_ptr(ptr: *const Path2D) -> *const f32 {
    let path = unsafe { &*ptr };
    path.get_buffer_ptr()
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_get_buffer_size(ptr: *const Path2D) -> usize {
    let path = unsafe { &*ptr };
    path.get_buffer_size()
}
