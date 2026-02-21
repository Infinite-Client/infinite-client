use crate::graphics3d::mesh::generator::BlockMeshGenerator;
use glam::DVec3;
use minecraft_rs::mgpu3d::{Color4, MinecraftGpu3DChild};
use xross_core::{XrossClass, xross_methods};

#[derive(XrossClass, Default)]
pub struct BlockHighlight {
    generator: BlockMeshGenerator,
    #[xross_field]
    pub line_width: f32,
    #[xross_field]
    pub render_style: i32, // 0: Lines, 1: Faces, 2: Both
}

#[xross_methods]
impl BlockHighlight {
    #[xross_new(panicable)]
    pub fn new() -> Self {
        Self {
            generator: BlockMeshGenerator::new(),
            line_width: 1.5,
            render_style: 0,
        }
    }

    #[xross_method(critical)]
    pub fn clear(&mut self) {
        self.generator.clear();
    }

    #[xross_method(critical)]
    pub fn add_block(&mut self, x: i32, y: i32, z: i32, color: i32) {
        self.generator.add_block(x, y, z, color);
    }

    #[xross_method(critical(heap_access))]
    pub fn add_blocks(&mut self, positions: &[i32], colors: &[i32]) {
        let n = positions.len() / 3;
        for i in 0..n {
            self.generator.add_block(
                positions[i * 3],
                positions[i * 3 + 1],
                positions[i * 3 + 2],
                colors[i],
            );
        }
    }

    #[xross_method(panicable)]
    pub fn generate(&mut self) {
        self.generator.generate();
    }

    #[xross_method(critical)]
    pub fn register_to_mgpu3d(&self) -> usize {
        let ptr = self as *const Self as usize;
        minecraft_rs::mgpu3d::register_handler(move |g| {
            let s = unsafe { &*(ptr as *const Self) };
            s.render(g);
        })
    }

    /// mgpu3d システムを利用して描画コマンドを発行します。
    pub fn render(&self, g: &mut MinecraftGpu3DChild) {
        let render_style = self.render_style;

        // Quad (Faces)
        if render_style == 1 || render_style == 2 {
            let ptr = self.generator.get_quad_buffer_ptr();
            let size = self.generator.get_quad_buffer_size();
            let slice = unsafe { std::slice::from_raw_parts(ptr, size) };

            // 1 vertex = 7 f32 (x, y, z, color(bits), nx, ny, nz)
            // 1 quad = 4 vertices = 28 f32
            for i in (0..size).step_by(28) {
                let v1 = DVec3::new(slice[i] as f64, slice[i + 1] as f64, slice[i + 2] as f64);
                let c_bits = slice[i + 3].to_bits();
                let color = Color4::rgba(
                    ((c_bits >> 16) & 0xFF) as u8,
                    ((c_bits >> 8) & 0xFF) as u8,
                    (c_bits & 0xFF) as u8,
                    ((c_bits >> 24) & 0xFF) as u8,
                );
                let v2 = DVec3::new(
                    slice[i + 7] as f64,
                    slice[i + 8] as f64,
                    slice[i + 9] as f64,
                );
                let v3 = DVec3::new(
                    slice[i + 14] as f64,
                    slice[i + 15] as f64,
                    slice[i + 16] as f64,
                );
                let v4 = DVec3::new(
                    slice[i + 21] as f64,
                    slice[i + 22] as f64,
                    slice[i + 23] as f64,
                );

                g.quad(v1, v2, v3, v4, color);
            }
        }

        // Lines
        if render_style == 0 || render_style == 2 {
            let ptr = self.generator.get_line_buffer_ptr();
            let size = self.generator.get_line_buffer_size();
            let slice = unsafe { std::slice::from_raw_parts(ptr, size) };

            // 1 vertex = 4 f32 (x, y, z, color(bits))
            // 1 line = 2 vertices = 8 f32
            for i in (0..size).step_by(8) {
                let start = DVec3::new(slice[i] as f64, slice[i + 1] as f64, slice[i + 2] as f64);
                let c_bits = slice[i + 3].to_bits();
                let color = Color4::rgba(
                    ((c_bits >> 16) & 0xFF) as u8,
                    ((c_bits >> 8) & 0xFF) as u8,
                    (c_bits & 0xFF) as u8,
                    ((c_bits >> 24) & 0xFF) as u8,
                );
                let end = DVec3::new(
                    slice[i + 4] as f64,
                    slice[i + 5] as f64,
                    slice[i + 6] as f64,
                );

                g.line(start, end, color, self.line_width);
            }
        }
    }
}
