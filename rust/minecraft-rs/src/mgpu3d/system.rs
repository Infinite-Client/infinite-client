use crate::mgpu3d::MinecraftGpu3D;
use crate::mgpu3d::handler::{GpuHandler, HandlerManager};
use glam::{DMat4, DVec3};
use parking_lot::RwLock;
use std::sync::LazyLock;

#[derive(Default)]
pub struct MinecraftGpu3dSystem {
    pub matrixes: RwLock<MinecraftMatrixes>,
    pub handler_manager: RwLock<HandlerManager>,
}
pub struct MinecraftMatrixes {
    pub projection: DMat4,
    pub model_view: DMat4,
    pub camera_position: DVec3,
    pub window_width: u32,
    pub window_height: u32,
}

impl MinecraftMatrixes {
    fn convert_inner(&self, pos: DVec3) -> DVec3 {
        let view_pos = self.model_view.project_point3(pos);
        self.projection.project_point3(view_pos)
    }
    pub fn convert<V: Into<DVec3>>(&self, pos: V) -> DVec3 {
        self.convert_inner(pos.into())
    }
}
impl Default for MinecraftMatrixes {
    fn default() -> Self {
        Self {
            projection: DMat4::NAN,
            model_view: DMat4::NAN,
            camera_position: DVec3::ZERO,
            window_width: 0,
            window_height: 0,
        }
    }
}
static MINECRAFT_GPU_3D_SYSTEM: LazyLock<MinecraftGpu3dSystem> =
    LazyLock::new(MinecraftGpu3dSystem::default);

impl MinecraftGpu3dSystem {
    pub fn add_handler(handler: Box<dyn GpuHandler>) -> usize {
        MINECRAFT_GPU_3D_SYSTEM
            .handler_manager
            .write()
            .add_handler(handler)
    }
    pub fn del_handler(id: usize) -> bool {
        MINECRAFT_GPU_3D_SYSTEM
            .handler_manager
            .write()
            .remove_handler(id)
    }
    pub fn update(
        cam_x: f64,
        cam_y: f64,
        cam_z: f64,
        window_width: u32,
        window_height: u32,
        projection_buffer: &[f64],
        model_buffer: &[f64],
    ) {
        let mut matrixes = MINECRAFT_GPU_3D_SYSTEM.matrixes.write();

        // デフォルトの空配列
        let empty = [0f64; 16];

        // スライスを固定長配列に変換を試みて、失敗（長さ不足）ならemptyを使う
        let proj_arr: [f64; 16] = projection_buffer.try_into().unwrap_or(empty);

        let model_arr: [f64; 16] = model_buffer.try_into().unwrap_or(empty);

        matrixes.camera_position = DVec3::new(cam_x, cam_y, cam_z);
        matrixes.projection = DMat4::from_cols_array(&proj_arr);
        matrixes.model_view = DMat4::from_cols_array(&model_arr);
        matrixes.window_width = window_width;
        matrixes.window_height = window_height;
    }
    pub fn process() -> Vec<u8> {
        let instance = &MINECRAFT_GPU_3D_SYSTEM;
        let matrixes = instance.matrixes.read();
        let manager = instance.handler_manager.read();
        let mgpu3d = MinecraftGpu3D::new(&matrixes);
        manager.render_all(mgpu3d)
    }
}
