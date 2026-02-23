use xross_core::XrossClass;

#[derive(Copy, Clone, Debug, PartialEq, XrossClass)]
#[xross_package("color")]
pub struct Color {
    #[xross_field]
    pub a: u8,
    #[xross_field]
    pub r: u8,
    #[xross_field]
    pub g: u8,
    #[xross_field]
    pub b: u8,
}
impl Default for Color {
    fn default() -> Self {
        Self::new(0, 0, 0, 0)
    }
}
impl From<Color> for u32 {
    fn from(color: Color) -> u32 {
        ((color.a as u32) << 24)
            | ((color.r as u32) << 16)
            | ((color.g as u32) << 8)
            | (color.b as u32)
    }
}
impl From<Color> for i32 {
    fn from(color: Color) -> i32 {
        let unsigned: u32 = color.into();
        i32::from_ne_bytes(unsigned.to_ne_bytes())
    }
}
impl From<i32> for Color {
    fn from(int: i32) -> Self {
        let bytes = u32::from_ne_bytes(int.to_ne_bytes());
        bytes.into()
    }
}
impl From<u32> for Color {
    fn from(bytes: u32) -> Self {
        let a = (bytes >> 24) as u8; // 最上位8ビット
        let r = (bytes >> 16) as u8; // 次の8ビット
        let g = (bytes >> 8) as u8; // 次の8ビット
        let b = (bytes & 0xFF) as u8; // 最下位8ビット
        Self { a, r, g, b }
    }
}
impl Color {
    pub fn new(a: u8, r: u8, g: u8, b: u8) -> Self {
        Color { a, r, g, b }
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
    pub fn from_raw(raw: i32) -> Self {
        raw.into()
    }
    pub fn into_raw(self) -> i32 {
        self.into()
    }
    pub fn mix(&self, other: Self, t: f32) -> Self {
        let t = t.clamp(0.0, 1.0);
        // 各チャンネルごとに補間計算を行うクロージャ
        let lerp = |start: u8, end: u8, t: f32| -> u8 {
            let s = start as f32;
            let e = end as f32;
            // 線形補間の公式: s + (e - s) * t
            let result = s + (e - s) * t;
            result.round() as u8
        };
        Self {
            r: lerp(self.r, other.r, t),
            g: lerp(self.g, other.g, t),
            b: lerp(self.b, other.b, t),
            a: lerp(self.a, other.a, t),
        }
    }
}
