use crate::utils::color::Color;

#[derive(Debug, Clone)]
pub struct BlockAndColor {
    pub id: u32,
    pub color: Color,
}
impl From<(u32, u32)> for BlockAndColor {
    fn from((id, color): (u32, u32)) -> Self {
        Self::new(id, color.into())
    }
}
impl From<u64> for BlockAndColor {
    fn from(combined: u64) -> Self {
        let color_val = (combined >> 32) as u32;
        let id = (combined & 0xFFFFFFFF) as u32;
        Self::new(id, color_val.into())
    }
}

impl BlockAndColor {
    pub fn new(id: u32, color: Color) -> Self {
        Self { id, color }
    }
}
