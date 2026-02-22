mod command;
mod handler;
mod system;

use command::{Command3D, Identifier, TexturedVertex};
use glam::{DMat4, DVec3, Vec4Swizzles};
use std::collections::VecDeque;
use system::MinecraftMatrixes;
use xross_core::xross_function;

pub use system::MinecraftGpu3dSystem;
pub use handler::GpuHandler;
#[derive(Clone)]
pub struct MinecraftGpu3D<'a> {
    buffer: Vec<Command3D>,
    size: usize,
    /// 読み取り専用の参照。システム全体の行列（Projection/View）
    matrixes: &'a MinecraftMatrixes,
    /// モデル行列のスタック (Kotlin の modelMatrixStack に相当)
    model_matrix_stack: VecDeque<DMat4>,
}

impl<'a> MinecraftGpu3D<'a> {
    pub fn new(matrixes: &'a MinecraftMatrixes) -> Self {
        let mut stack = VecDeque::new();
        stack.push_back(DMat4::IDENTITY); // 初期行列

        Self {
            buffer: Vec::with_capacity(64),
            size: 0,
            matrixes,
            model_matrix_stack: stack,
        }
    }

    pub fn matrixes(&self) -> &'a MinecraftMatrixes {
        self.matrixes
    }

    // --- 行列操作 (Matrix Stack) ---

    pub fn push_matrix(&mut self) {
        let current = self.model_matrix_stack.back().unwrap_or(&DMat4::IDENTITY);
        self.model_matrix_stack.push_back(*current);
    }

    pub fn pop_matrix(&mut self) {
        if self.model_matrix_stack.len() > 1 {
            self.model_matrix_stack.pop_back();
        }
    }

    /// 現在のスタックのトップにあるモデル行列で座標を変換する
    fn transform(&self, pos: DVec3) -> DVec3 {
        let model = self.model_matrix_stack.back().unwrap_or(&DMat4::IDENTITY);
        model.project_point3(pos)
    }

    fn transform_vertex(&self, mut vertex: TexturedVertex) -> TexturedVertex {
        vertex.position = self.transform(vertex.position);
        vertex
    }

    // --- 描画コマンド (Public API) ---
    pub fn line(&mut self, start: DVec3, end: DVec3, color: u32, size: f32, depth_test: bool) {
        self.add_command(Command3D::Line {
            from: self.transform(start),
            to: self.transform(end),
            color,
            size,
            depth_test,
        });
    }

    pub fn triangle(&mut self, a: DVec3, b: DVec3, c: DVec3, color: u32, depth_test: bool) {
        self.add_command(Command3D::Triangle {
            a: self.transform(a),
            b: self.transform(b),
            c: self.transform(c),
            color,
            depth_test,
        });
    }

    pub fn triangle_fill(&mut self, a: DVec3, b: DVec3, c: DVec3, color: u32, depth_test: bool) {
        self.add_command(Command3D::TriangleFill {
            a: self.transform(a),
            b: self.transform(b),
            c: self.transform(c),
            color,
            depth_test,
        });
    }

    pub fn triangle_fill_gradient(
        &mut self,
        a: (DVec3, u32),
        b: (DVec3, u32),
        c: (DVec3, u32),
        depth_test: bool,
    ) {
        self.add_command(Command3D::TriangleFillGradient {
            a: self.transform(a.0),
            b: self.transform(b.0),
            c: self.transform(c.0),
            color_a: a.1,
            color_b: b.1,
            color_c: c.1,
            depth_test,
        });
    }

    pub fn quad(&mut self, a: DVec3, b: DVec3, c: DVec3, d: DVec3, color: u32, depth_test: bool) {
        self.add_command(Command3D::Quad {
            a: self.transform(a),
            b: self.transform(b),
            c: self.transform(c),
            d: self.transform(d),
            color,
            depth_test,
        });
    }

    pub fn quad_fill(
        &mut self,
        a: DVec3,
        b: DVec3,
        c: DVec3,
        d: DVec3,
        color: u32,
        depth_test: bool,
    ) {
        self.add_command(Command3D::QuadFill {
            a: self.transform(a),
            b: self.transform(b),
            c: self.transform(c),
            d: self.transform(d),
            color,
            depth_test,
        });
    }

    pub fn quad_fill_gradient(
        &mut self,
        a: (DVec3, u32),
        b: (DVec3, u32),
        c: (DVec3, u32),
        d: (DVec3, u32),
        depth_test: bool,
    ) {
        self.add_command(Command3D::QuadFillGradient {
            a: self.transform(a.0),
            b: self.transform(b.0),
            c: self.transform(c.0),
            d: self.transform(d.0),
            color_a: a.1,
            color_b: b.1,
            color_c: c.1,
            color_d: d.1,
            depth_test,
        });
    }

    pub fn triangle_textured(
        &mut self,
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        texture: String,
        depth_test: bool,
    ) {
        self.add_command(Command3D::TriangleTextured {
            a: self.transform_vertex(a),
            b: self.transform_vertex(b),
            c: self.transform_vertex(c),
            texture: Identifier(texture),
            depth_test,
        });
    }

    pub fn quad_textured(
        &mut self,
        a: TexturedVertex,
        b: TexturedVertex,
        c: TexturedVertex,
        d: TexturedVertex,
        texture: String,
        depth_test: bool,
    ) {
        self.add_command(Command3D::QuadTextured {
            a: self.transform_vertex(a),
            b: self.transform_vertex(b),
            c: self.transform_vertex(c),
            d: self.transform_vertex(d),
            texture: Identifier(texture),
            depth_test,
        });
    }

    // --- ユーティリティ (Project / Unproject) ---

    pub fn project(&self, world_pos: DVec3) -> Option<DVec3> {
        let proj = &self.matrixes.projection;
        let view = &self.matrixes.model_view;
        let cam_pos = &self.matrixes.camera_position;

        let relative_pos = world_pos - *cam_pos;
        let mvp = *proj * *view;
        let local_pos = self.transform(relative_pos);
        let pos_v4 = mvp * local_pos.extend(1.0);

        if pos_v4.w <= 0.0 {
            return None;
        }

        let ndc = pos_v4.xyz() / pos_v4.w;
        let screen_x = (ndc.x + 1.0) / 2.0 * self.matrixes.window_width as f64;
        let screen_y = (1.0 - ndc.y) / 2.0 * self.matrixes.window_height as f64;
        let depth = (ndc.z + 1.0) / 2.0;

        Some(DVec3::new(screen_x, screen_y, depth))
    }

    pub fn unproject(&self, screen_pos: DVec3) -> DVec3 {
        let proj = &self.matrixes.projection;
        let view = &self.matrixes.model_view;
        let cam_pos = &self.matrixes.camera_position;

        let mvp_inv = (*proj * *view).inverse();
        let ndc_x = (screen_pos.x / self.matrixes.window_width as f64) * 2.0 - 1.0;
        let ndc_y = 1.0 - (screen_pos.y / self.matrixes.window_height as f64) * 2.0;
        let ndc_z = screen_pos.z * 2.0 - 1.0;

        let world_v4 = mvp_inv * DVec3::new(ndc_x, ndc_y, ndc_z).extend(1.0);
        let world_pos_relative = world_v4.xyz() / world_v4.w;

        world_pos_relative + *cam_pos
    }
    fn add_command(&mut self, command: Command3D) {
        self.size += command.size();
        self.buffer.push(command);
    }

    fn build(&self) -> Vec<u8> {
        let mut byte_buffer = Vec::with_capacity(self.size);
        for cmd in &self.buffer {
            byte_buffer.extend_from_slice(&cmd.buffer());
        }
        byte_buffer
    }
}

#[xross_function(package = "mgpu3d", critical(heap_access))]
pub fn mgpu3d_process(
    cam_x: f64,
    cam_y: f64,
    cam_z: f64,
    window_width: u32,
    window_height: u32,
    position_matrix_buffer: &[f64], // 16要素のf64
    model_matrix_buffer: &[f64],    // 16要素のf64
) -> Vec<u8> {
    MinecraftGpu3dSystem::update(
        cam_x,
        cam_y,
        cam_z,
        window_width,
        window_height,
        position_matrix_buffer,
        model_matrix_buffer,
    );
    MinecraftGpu3dSystem::process()
}
