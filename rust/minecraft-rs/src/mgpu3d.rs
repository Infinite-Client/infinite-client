mod command;
mod handler;
mod system;

use crate::mgpu3d::system::{MinecraftGpu3dSystem, MinecraftMatrixes};
use command::{Command3D, Identifier, TexturedVertex};
use glam::{DMat4, DVec3};
use std::collections::VecDeque;
use xross_core::xross_function;

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
        // ここに独自の変換ロジックを実装可能
        // 現状は Kotlin 版に合わせモデル行列を適用
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

    pub fn triangle_fill(&mut self, a: DVec3, b: DVec3, c: DVec3, color: u32, depth_test: bool) {
        self.add_command(Command3D::TriangleFill {
            a: self.transform(a),
            b: self.transform(b),
            c: self.transform(c),
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

    // --- 内部処理・ビルド ---

    fn add_command(&mut self, command: Command3D) {
        self.size += command.size();
        self.buffer.push(command);
    }

    pub fn capture_frame(self) -> Vec<u8> {
        self.build()
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
    position_matrix_buffer: &[f64], // 16要素のf64
    model_matrix_buffer: &[f64],    // 16要素のf64
) -> Vec<u8> {
    MinecraftGpu3dSystem::update(
        cam_x,
        cam_y,
        cam_z,
        position_matrix_buffer,
        model_matrix_buffer,
    );
    MinecraftGpu3dSystem::process()
}
