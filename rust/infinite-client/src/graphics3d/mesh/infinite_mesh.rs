use xross_core::{XrossClass, xross_methods};

#[derive(XrossClass, Default)]
pub struct InfiniteMesh {
    line_buffer: Vec<f32>,
    quad_buffer: Vec<f32>,
}

#[xross_methods]
impl InfiniteMesh {
    #[xross_new(panicable)]
    pub fn new() -> Self {
        Self::default()
    }

    #[xross_method(critical)]
    pub fn get_line_buffer_ptr(&self) -> *const f32 {
        self.line_buffer.as_ptr()
    }

    #[xross_method(critical)]
    pub fn get_line_buffer_size(&self) -> usize {
        self.line_buffer.len()
    }

    #[xross_method(critical)]
    pub fn get_quad_buffer_ptr(&self) -> *const f32 {
        self.quad_buffer.as_ptr()
    }

    #[xross_method(critical)]
    pub fn get_quad_buffer_size(&self) -> usize {
        self.quad_buffer.len()
    }
}
