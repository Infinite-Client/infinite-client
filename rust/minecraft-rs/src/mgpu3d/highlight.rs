use crate::mgpu3d::MinecraftGpu3DChild;
use crate::mgpu3d::command::Color4;
use glam::{DVec3, IVec3};
use std::collections::HashMap;
use std::sync::{Mutex, OnceLock};

#[derive(Clone, Copy)]
pub enum RenderStyle {
    Lines,
    Faces,
    Both,
}

struct HighlightStore {
    /// ソース名（"block", "cave" 等）ごとのブロックデータ
    sources: Mutex<HashMap<String, HashMap<IVec3, Color4>>>,
    style: Mutex<RenderStyle>,
    line_width: Mutex<f32>,
}

impl HighlightStore {
    fn global() -> &'static Self {
        static INSTANCE: OnceLock<HighlightStore> = OnceLock::new();
        INSTANCE.get_or_init(|| Self {
            sources: Mutex::new(HashMap::new()),
            style: Mutex::new(RenderStyle::Lines),
            line_width: Mutex::new(1.5),
        })
    }
}

pub fn render_highlights(g: &mut MinecraftGpu3DChild) {
    let store = HighlightStore::global();
    let sources = store.sources.lock().unwrap();
    let style = *store.style.lock().unwrap();
    let width = *store.line_width.lock().unwrap();

    for map in sources.values() {
        for (pos, color) in map.iter() {
            let min = DVec3::new(pos.x as f64, pos.y as f64, pos.z as f64);
            let max = min + DVec3::ONE;

            match style {
                RenderStyle::Lines => draw_box_lines(g, min, max, *color, width),
                RenderStyle::Faces => draw_box_faces(g, min, max, *color),
                RenderStyle::Both => {
                    draw_box_faces(g, min, max, *color);
                    draw_box_lines(g, min, max, *color, width);
                }
            }
        }
    }
}

fn draw_box_lines(g: &mut MinecraftGpu3DChild, min: DVec3, max: DVec3, color: Color4, width: f32) {
    let x0 = min.x;
    let y0 = min.y;
    let z0 = min.z;
    let x1 = max.x;
    let y1 = max.y;
    let z1 = max.z;

    let v = [
        DVec3::new(x0, y0, z0),
        DVec3::new(x0, y0, z1),
        DVec3::new(x0, y1, z0),
        DVec3::new(x0, y1, z1),
        DVec3::new(x1, y0, z0),
        DVec3::new(x1, y0, z1),
        DVec3::new(x1, y1, z0),
        DVec3::new(x1, y1, z1),
    ];

    let edges = [
        (0, 1),
        (1, 3),
        (3, 2),
        (2, 0),
        (4, 5),
        (5, 7),
        (7, 6),
        (6, 4),
        (0, 4),
        (1, 5),
        (2, 6),
        (3, 7),
    ];

    for (start, end) in edges {
        g.line(v[start], v[end], color, width);
    }
}

fn draw_box_faces(g: &mut MinecraftGpu3DChild, min: DVec3, max: DVec3, color: Color4) {
    let x0 = min.x;
    let y0 = min.y;
    let z0 = min.z;
    let x1 = max.x;
    let y1 = max.y;
    let z1 = max.z;

    let v = [
        DVec3::new(x0, y0, z0),
        DVec3::new(x0, y0, z1),
        DVec3::new(x0, y1, z0),
        DVec3::new(x0, y1, z1),
        DVec3::new(x1, y0, z0),
        DVec3::new(x1, y0, z1),
        DVec3::new(x1, y1, z0),
        DVec3::new(x1, y1, z1),
    ];

    // 6 faces
    g.quad(v[0], v[1], v[3], v[2], color); // West
    g.quad(v[4], v[6], v[7], v[5], color); // East
    g.quad(v[0], v[4], v[5], v[1], color); // Down
    g.quad(v[2], v[3], v[7], v[6], color); // Up
    g.quad(v[0], v[2], v[6], v[4], color); // North
    g.quad(v[1], v[5], v[7], v[3], color); // South
}

// --- FFI API ---

#[xross_core::xross_function(package = "mgpu3d.highlight", critical(heap_access))]
pub fn update_highlight_blocks(source: String, positions: &[i32], colors: &[i32]) {
    println!("Rust update_highlight_blocks: source={}, pos_count={}", source, positions.len() / 3);
    let store = HighlightStore::global();
    let mut sources = store.sources.lock().unwrap();
    let map = sources.entry(source).or_insert_with(HashMap::new);
    map.clear();

    for i in 0..(positions.len() / 3) {
        let pos = IVec3::new(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]);
        let c_raw = colors[i] as u32;
        let color = Color4 {
            a: (c_raw >> 24) as u8,
            r: (c_raw >> 16) as u8,
            g: (c_raw >> 8) as u8,
            b: c_raw as u8,
        };
        map.insert(pos, color);
    }
}

#[xross_core::xross_function(package = "mgpu3d.highlight")]
pub fn set_highlight_style(style_idx: i32, line_width: f32) {
    let store = HighlightStore::global();
    if let Ok(mut s) = store.style.lock() {
        *s = match style_idx {
            1 => RenderStyle::Faces,
            2 => RenderStyle::Both,
            _ => RenderStyle::Lines,
        };
    }
    if let Ok(mut w) = store.line_width.lock() {
        *w = line_width;
    }
}
