use xross_core::{XrossClass, xross_methods};

#[derive(XrossClass, Default, Clone)]
#[xross(clonable)]
pub struct InfiniteMesh {
    lines: Vec<f32>, // [x1, y1, z1, x2, y2, z2, color_bits] -> 7 elements
    quads: Vec<f32>, // [x1, y1, z1, ..., x4, y4, z4, color_bits] -> 13 elements
}

#[xross_methods]
impl InfiniteMesh {
    #[xross_new(panicable)]
    pub fn new() -> Self {
        Self::default()
    }

    #[xross_method(critical)]
    pub fn clear(&mut self) {
        self.lines.clear();
        self.quads.clear();
    }

    #[xross_method(critical)]
    pub fn add_line(&mut self, x1: f32, y1: f32, z1: f32, x2: f32, y2: f32, z2: f32, color: i32) {
        let c = f32::from_bits(color as u32);
        self.lines.extend_from_slice(&[x1, y1, z1, x2, y2, z2, c]);
    }

    #[xross_method(critical)]
    pub fn add_quad(&mut self, x1: f32, y1: f32, z1: f32, x2: f32, y2: f32, z2: f32, x3: f32, y3: f32, z3: f32, x4: f32, y4: f32, z4: f32, color: i32) {
        let c = f32::from_bits(color as u32);
        self.quads.extend_from_slice(&[x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, c]);
    }

    #[xross_method(critical)]
    pub fn add_box(&mut self, x: f32, y: f32, z: f32, w: f32, h: f32, d: f32, color: i32, lines: bool) {
        let x2 = x + w;
        let y2 = y + h;
        let z2 = z + d;
        if lines {
            // 12 lines
            self.add_line(x, y, z, x2, y, z, color);
            self.add_line(x2, y, z, x2, y2, z, color);
            self.add_line(x2, y2, z, x, y2, z, color);
            self.add_line(x, y2, z, x, y, z, color);

            self.add_line(x, y, z2, x2, y, z2, color);
            self.add_line(x2, y, z2, x2, y2, z2, color);
            self.add_line(x2, y2, z2, x, y2, z2, color);
            self.add_line(x, y2, z2, x, y, z2, color);

            self.add_line(x, y, z, x, y, z2, color);
            self.add_line(x2, y, z, x2, y, z2, color);
            self.add_line(x2, y2, z, x2, y2, z2, color);
            self.add_line(x, y2, z, x, y2, z2, color);
        } else {
            // 6 quads
            // Front
            self.add_quad(x, y, z, x2, y, z, x2, y2, z, x, y2, z, color);
            // Back
            self.add_quad(x, y, z2, x, y2, z2, x2, y2, z2, x2, y, z2, color);
            // Top
            self.add_quad(x, y2, z, x2, y2, z, x2, y2, z2, x, y2, z2, color);
            // Bottom
            self.add_quad(x, y, z, x, y, z2, x2, y, z2, x2, y, z, color);
            // Left
            self.add_quad(x, y, z, x, y2, z, x, y2, z2, x, y, z2, color);
            // Right
            self.add_quad(x2, y, z, x2, y, z2, x2, y2, z2, x2, y2, z, color);
        }
    }

    #[xross_method(critical)]
    pub fn get_line_buffer_ptr(&self) -> *const f32 { self.lines.as_ptr() }
    #[xross_method(critical)]
    pub fn get_line_buffer_size(&self) -> usize { self.lines.len() }
    #[xross_method(critical)]
    pub fn get_quad_buffer_ptr(&self) -> *const f32 { self.quads.as_ptr() }
    #[xross_method(critical)]
    pub fn get_quad_buffer_size(&self) -> usize { self.quads.len() }
}
