use glam::{DVec3, DVec2};

/// テクスチャ付き頂点の構造体
#[derive(Debug, Clone, Copy)]
pub struct TexturedVertex {
    pub position: DVec3,
    pub uv: DVec2,
    pub color: u32,
}

/// 識別子（MinecraftのIdentifierに相当）
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Identifier(pub String);

/// 3D空間での描画命令をカプセル化するデータ構造
#[derive(Debug, Clone)]
pub enum Command3D {
    Line {
        from: DVec3,
        to: DVec3,
        color: u32,
        size: f32,
        depth_test: bool,
    },
    Triangle {
        a: DVec3,
        b: DVec3,
        c: DVec3,
        color: u32,
        depth_test: bool,
    },
    TriangleFill {
        a: DVec3,
        b: DVec3,
        c: DVec3,
        color: u32,
        depth_test: bool,
    },
    TriangleFillGradient {
        a: DVec3,
        b: DVec3,
        c: DVec3,
        color_a: u32,
        color_b: u32,
        color_c: u32,
        depth_test: bool,
    },
    Quad {
        a: DVec3,
        b: DVec3,
        c: DVec3,
        d: DVec3,
        color: u32,
        depth_test: bool,
    },
    QuadFill {
        a: DVec3,
        b: DVec3,
        c: DVec3,
        d: DVec3,
        color: u32,
        depth_test: bool,
    },
    QuadFillGradient {
        a: DVec3,
        b: DVec3,
        c: DVec3,
        d: DVec3,
        color_a: u32,
        color_b: u32,
        color_c: u32,
        color_d: u32,
        depth_test: bool,
    },
    TriangleTextured {
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        texture: Identifier,
        depth_test: bool,
    },
    QuadTextured {
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        d: TexturedVertex,
        texture: Identifier,
        depth_test: bool,
    },
}

impl Command3D {
    /// 命令の種類を識別するシグネチャ
    pub fn signature(&self) -> u8 {
        match self {
            Self::Line { .. } => 0,
            Self::Triangle { .. } => 1,
            Self::TriangleFill { .. } => 2,
            Self::TriangleFillGradient { .. } => 3,
            Self::Quad { .. } => 4,
            Self::QuadFill { .. } => 5,
            Self::QuadFillGradient { .. } => 6,
            Self::TriangleTextured { .. } => 7,
            Self::QuadTextured { .. } => 8,
        }
    }

    /// バッファに書き込まれるデータの合計バイトサイズを計算
    pub fn size(&self) -> usize {
        let signature_size = 1; // u8
        let bool_size = 1;      // bool (u8)
        let color_size = 4;     // u32
        let f32_size = 4;
        let dvec3_size = 24;    // f64 * 3
        let dvec2_size = 16;    // f64 * 2
        let textured_vertex_size = dvec3_size + dvec2_size + color_size;

        signature_size + match self {
            Self::Line { .. } => (dvec3_size * 2) + color_size + f32_size + bool_size,
            Self::Triangle { .. } | Self::TriangleFill { .. } => (dvec3_size * 3) + color_size + bool_size,
            Self::TriangleFillGradient { .. } => (dvec3_size * 3) + (color_size * 3) + bool_size,
            Self::Quad { .. } | Self::QuadFill { .. } => (dvec3_size * 4) + color_size + bool_size,
            Self::QuadFillGradient { .. } => (dvec3_size * 4) + (color_size * 4) + bool_size,
            Self::TriangleTextured { texture, .. } => (textured_vertex_size * 3) + bool_size + texture.0.len() + 4,
            Self::QuadTextured { texture, .. } => (textured_vertex_size * 4) + bool_size + texture.0.len() + 4,
        }
    }

    /// データをバイナリ形式のバッファとして生成
    pub fn buffer(&self) -> Vec<u8> {
        let mut buf = Vec::with_capacity(self.size());

        // 1. Signature
        buf.push(self.signature());

        // 2. Data
        match self {
            Self::Line { from, to, color, size, depth_test } => {
                append_dvec3(&mut buf, from);
                append_dvec3(&mut buf, to);
                buf.extend_from_slice(&color.to_le_bytes());
                buf.extend_from_slice(&size.to_le_bytes());
                buf.push(*depth_test as u8);
            }
            Self::Triangle { a, b, c, color, depth_test } |
            Self::TriangleFill { a, b, c, color, depth_test } => {
                append_dvec3(&mut buf, a);
                append_dvec3(&mut buf, b);
                append_dvec3(&mut buf, c);
                buf.extend_from_slice(&color.to_le_bytes());
                buf.push(*depth_test as u8);
            }
            Self::TriangleFillGradient { a, b, c, color_a, color_b, color_c, depth_test } => {
                append_dvec3(&mut buf, a);
                append_dvec3(&mut buf, b);
                append_dvec3(&mut buf, c);
                buf.extend_from_slice(&color_a.to_le_bytes());
                buf.extend_from_slice(&color_b.to_le_bytes());
                buf.extend_from_slice(&color_c.to_le_bytes());
                buf.push(*depth_test as u8);
            }
            Self::Quad { a, b, c, d, color, depth_test } |
            Self::QuadFill { a, b, c, d, color, depth_test } => {
                append_dvec3(&mut buf, a);
                append_dvec3(&mut buf, b);
                append_dvec3(&mut buf, c);
                append_dvec3(&mut buf, d);
                buf.extend_from_slice(&color.to_le_bytes());
                buf.push(*depth_test as u8);
            }
            Self::QuadFillGradient { a, b, c, d, color_a, color_b, color_c, color_d, depth_test } => {
                append_dvec3(&mut buf, a);
                append_dvec3(&mut buf, b);
                append_dvec3(&mut buf, c);
                append_dvec3(&mut buf, d);
                buf.extend_from_slice(&color_a.to_le_bytes());
                buf.extend_from_slice(&color_b.to_le_bytes());
                buf.extend_from_slice(&color_c.to_le_bytes());
                buf.extend_from_slice(&color_d.to_le_bytes());
                buf.push(*depth_test as u8);
            }
            Self::TriangleTextured { a, b, c, texture, depth_test } => {
                append_textured_vertex(&mut buf, a);
                append_textured_vertex(&mut buf, b);
                append_textured_vertex(&mut buf, c);
                append_string(&mut buf, &texture.0);
                buf.push(*depth_test as u8);
            }
            Self::QuadTextured { a, b, c, d, texture, depth_test } => {
                append_textured_vertex(&mut buf, a);
                append_textured_vertex(&mut buf, b);
                append_textured_vertex(&mut buf, c);
                append_textured_vertex(&mut buf, d);
                append_string(&mut buf, &texture.0);
                buf.push(*depth_test as u8);
            }
        }
        buf
    }
}

// --- 内部補助関数 ---

fn append_dvec3(buf: &mut Vec<u8>, v: &DVec3) {
    buf.extend_from_slice(&v.x.to_le_bytes());
    buf.extend_from_slice(&v.y.to_le_bytes());
    buf.extend_from_slice(&v.z.to_le_bytes());
}

fn append_textured_vertex(buf: &mut Vec<u8>, v: &TexturedVertex) {
    append_dvec3(buf, &v.position);
    buf.extend_from_slice(&v.uv.x.to_le_bytes());
    buf.extend_from_slice(&v.uv.y.to_le_bytes());
    buf.extend_from_slice(&v.color.to_le_bytes());
}

fn append_string(buf: &mut Vec<u8>, s: &str) {
    let bytes = s.as_bytes();
    buf.extend_from_slice(&(bytes.len() as u32).to_le_bytes());
    buf.extend_from_slice(bytes);
}
