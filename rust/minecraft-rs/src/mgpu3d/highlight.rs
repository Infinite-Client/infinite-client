use crate::mgpu3d::MinecraftGpu3DChild;

// 古いハイライトロジックは infinite-client 側に移行されました。
// このファイルは互換性または将来的なユーティリティのために残されますが、
// デフォルトの描画ハンドラからは除外されます。

pub fn render_highlights(_g: &mut MinecraftGpu3DChild) {
    // No-op: Moved to BlockHighlightFeature in infinite-client
}

#[xross_core::xross_function(package = "mgpu3d.highlight", critical(heap_access))]
pub fn update_highlight_blocks(_source: String, _positions: &[i32], _colors: &[i32]) {
    // No-op
}

#[xross_core::xross_function(package = "mgpu3d.highlight")]
pub fn set_highlight_style(_style_idx: i32, _line_width: f32) {
    // No-op
}
