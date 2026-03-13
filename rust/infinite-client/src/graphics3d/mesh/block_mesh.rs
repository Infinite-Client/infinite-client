use crate::graphics3d::mesh::BlockMeshGenerator;
use crate::graphics3d::mesh::types::{Line, Quad};

#[derive(Clone, Default)]
pub struct BlockMesh {
    pub quads: Vec<Quad>,
    pub lines: Vec<Line>,
}

impl BlockMesh {
    pub fn new(quads: Vec<Quad>, lines: Vec<Line>) -> Self {
        Self { quads, lines }
    }

    pub fn is_empty(&self) -> bool {
        self.quads.is_empty() && self.lines.is_empty()
    }

    pub fn from_generator(generator: &BlockMeshGenerator) -> Self {
        Self::new(generator.get_quads(), generator.get_lines())
    }
}
