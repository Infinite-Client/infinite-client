mod command;
mod system;
mod handler;

use command::Command3D;
use xross_core::xross_function;
use crate::mgpu3d::system::{MinecraftGpu3dSystem, MinecraftMatrixes};

#[derive(Clone)]
pub struct MinecraftGpu3D<'a> {
    buffer: Vec<Command3D>,
    size: usize,
    // 読み取り専用の参照。'a は matrixes が存続している期間を示す
    matrixes: &'a MinecraftMatrixes,
}

impl<'a> MinecraftGpu3D<'a> {
    pub fn new(matrixes: &'a MinecraftMatrixes) -> Self {
        Self {
            buffer: Vec::with_capacity(64),
            size: 0,
            matrixes,
        }
    }
    fn add_command(&mut self, command: Command3D) {
        // 各コマンドのシリアライズ後のサイズを加算
        self.size += command.size();
        self.buffer.push(command);
    }

    /// 保持している全てのコマンドを一つのバイナリバッファに結合する
    pub fn build(&self) -> Vec<u8> {
        let mut byte_buffer = Vec::with_capacity(self.size);

        for cmd in &self.buffer {
            // 各コマンドの Vec<u8> を全体バッファに流し込む
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
        cam_x, cam_y, cam_z,
        position_matrix_buffer, model_matrix_buffer);
    MinecraftGpu3dSystem::process()
}
