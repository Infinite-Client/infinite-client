use crate::graphics3d::mesh::BlockMeshGenerator;

#[derive(Clone, Default)]
pub struct BlockMesh {
    pub quads: Vec<f32>,
    pub lines: Vec<f32>,
}

impl BlockMesh {
    pub fn new(quads: Vec<f32>, lines: Vec<f32>) -> Self {
        Self { quads, lines }
    }

    pub fn is_empty(&self) -> bool {
        self.quads.is_empty() && self.lines.is_empty()
    }

    pub fn from_generator(generator: &BlockMeshGenerator) -> Self {
        let quads = unsafe {
            std::slice::from_raw_parts(
                generator.get_quad_buffer_ptr(),
                generator.get_quad_buffer_size(),
            )
            .to_vec()
        };
        let lines = unsafe {
            std::slice::from_raw_parts(
                generator.get_line_buffer_ptr(),
                generator.get_line_buffer_size(),
            )
            .to_vec()
        };
        Self::new(quads, lines)
    }
}
