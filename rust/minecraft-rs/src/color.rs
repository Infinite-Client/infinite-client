#[derive(Default, Clone, Copy)]
pub struct Color4 {
    pub a: u8,
    pub r: u8,
    pub g: u8,
    pub b: u8,
}

impl From<Color4> for u32 {
    fn from(color: Color4) -> u32 {
        ((color.a as u32) << 24)
            | ((color.r as u32) << 16)
            | ((color.g as u32) << 8)
            | (color.b as u32)
    }
}
impl From<Color4> for i32 {
    fn from(color: Color4) -> i32 {
        let unsigned: u32 = color.into();
        i32::from_ne_bytes(unsigned.to_ne_bytes())
    }
}

impl Color4 {
    pub fn new(a: u8, r: u8, g: u8, b: u8) -> Self {
        Color4 { a, r, g, b }
    }

    /// u32からColor4を復元するヘルパー（デバッグ等に便利）
    pub fn from_u32(val: u32) -> Self {
        Self {
            a: ((val >> 24) & 0xFF) as u8,
            r: ((val >> 16) & 0xFF) as u8,
            g: ((val >> 8) & 0xFF) as u8,
            b: (val & 0xFF) as u8,
        }
    }
}
