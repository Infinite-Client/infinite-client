use glam::DVec3;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Color4 {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl Color4 {
    pub const fn rgba(r: u8, g: u8, b: u8, a: u8) -> Self {
        Self { r, g, b, a }
    }

    pub const fn rgb(r: u8, g: u8, b: u8) -> Self {
        Self { r, g, b, a: 255 }
    }

    pub fn to_argb_u32(&self) -> u32 {
        ((self.a as u32) << 24) | ((self.r as u32) << 16) | ((self.g as u32) << 8) | (self.b as u32)
    }

    pub fn write_to_buffer(&self, buffer: &mut [u8], offset: &mut usize) {
        buffer[*offset] = self.a;
        buffer[*offset + 1] = self.r;
        buffer[*offset + 2] = self.g;
        buffer[*offset + 3] = self.b;
        *offset += 4;
    }
}

#[derive(Debug, Clone)]
pub struct LineCommand {
    pub start: DVec3,
    pub end: DVec3,
    pub color: Color4,
    pub width: f32,
}

#[derive(Debug, Clone)]
pub struct TriangleCommand {
    pub vertices: [DVec3; 3],
    pub color: Color4,
}

#[derive(Debug, Clone)]
pub struct TriangleGradientCommand {
    pub vertices: [(DVec3, Color4); 3],
}

#[derive(Debug, Clone)]
pub struct QuadCommand {
    pub vertices: [DVec3; 4],
    pub color: Color4,
}

#[derive(Debug, Clone)]
pub struct QuadGradientCommand {
    pub vertices: [(DVec3, Color4); 4],
}

#[derive(Debug, Clone)]
pub enum MinecraftGpu3dCommand {
    Line(LineCommand),
    Triangle(TriangleCommand),
    TriangleGradient(TriangleGradientCommand),
    Quad(QuadCommand),
    QuadGradient(QuadGradientCommand),
}

impl MinecraftGpu3dCommand {
    pub fn sig(&self) -> u8 {
        match &self {
            Self::Line(_) => 0,
            Self::Triangle(_) => 1,
            Self::TriangleGradient(_) => 2,
            Self::Quad(_) => 3,
            Self::QuadGradient(_) => 4,
        }
    }

    pub fn buffer_length(&self) -> usize {
        const SIG_SIZE: usize = 1;
        const VEC3_SIZE: usize = 24; // f64 * 3
        const COLOR4_SIZE: usize = 4;
        const F32_SIZE: usize = 4;

        match self {
            Self::Line(_) => SIG_SIZE + (VEC3_SIZE * 2) + COLOR4_SIZE + F32_SIZE,
            Self::Triangle(_) => SIG_SIZE + (VEC3_SIZE * 3) + COLOR4_SIZE,
            Self::TriangleGradient(_) => SIG_SIZE + (VEC3_SIZE + COLOR4_SIZE) * 3,
            Self::Quad(_) => SIG_SIZE + (VEC3_SIZE * 4) + COLOR4_SIZE,
            Self::QuadGradient(_) => SIG_SIZE + (VEC3_SIZE + COLOR4_SIZE) * 4,
        }
    }

    pub fn write_to_buffer(&self, buffer: &mut [u8], offset: &mut usize) {
        buffer[*offset] = self.sig();
        *offset += 1;

        match self {
            Self::Line(cmd) => {
                write_dvec3(cmd.start, buffer, offset);
                write_dvec3(cmd.end, buffer, offset);
                cmd.color.write_to_buffer(buffer, offset);
                buffer[*offset..*offset + 4].copy_from_slice(&cmd.width.to_be_bytes());
                *offset += 4;
            }
            Self::Triangle(cmd) => {
                for v in &cmd.vertices {
                    write_dvec3(*v, buffer, offset);
                }
                cmd.color.write_to_buffer(buffer, offset);
            }
            Self::TriangleGradient(cmd) => {
                for (v, c) in &cmd.vertices {
                    write_dvec3(*v, buffer, offset);
                    c.write_to_buffer(buffer, offset);
                }
            }
            Self::Quad(cmd) => {
                for v in &cmd.vertices {
                    write_dvec3(*v, buffer, offset);
                }
                cmd.color.write_to_buffer(buffer, offset);
            }
            Self::QuadGradient(cmd) => {
                for (v, c) in &cmd.vertices {
                    write_dvec3(*v, buffer, offset);
                    c.write_to_buffer(buffer, offset);
                }
            }
        }
    }
}

fn write_dvec3(v: DVec3, buffer: &mut [u8], offset: &mut usize) {
    buffer[*offset..*offset + 8].copy_from_slice(&v.x.to_be_bytes());
    *offset += 8;
    buffer[*offset..*offset + 8].copy_from_slice(&v.y.to_be_bytes());
    *offset += 8;
    buffer[*offset..*offset + 8].copy_from_slice(&v.z.to_be_bytes());
    *offset += 8;
}
