use crate::mgpu3d::MinecraftGpu3D;
use crate::mgpu3d::handler::HandlerManager;
use glam::{DMat4, DVec3};
use std::sync::{OnceLock, RwLock};

#[derive(Default)]
pub struct MinecraftGpu3dSystem {
    pub matrixes: RwLock<MinecraftMatrixes>,
    pub handler_manager: RwLock<HandlerManager>,
}
pub struct MinecraftMatrixes {
    pub projection: DMat4,
    pub model_view: DMat4,
    pub camera_position: DVec3,
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
        }
    }
}

static MINECRAFT_GPU_3D_SYSTEM: OnceLock<MinecraftGpu3dSystem> = OnceLock::new();
fn minecraft_gpu_3d_system() -> &'static MinecraftGpu3dSystem {
    MINECRAFT_GPU_3D_SYSTEM.get_or_init(MinecraftGpu3dSystem::default)
}

impl MinecraftGpu3dSystem {
    pub fn update(
        cam_x: f64,
        cam_y: f64,
        cam_z: f64,
        projection_buffer: &[f64],
        model_buffer: &[f64],
    ) {
        let instance = minecraft_gpu_3d_system();
        let mut matrixes = instance.matrixes.write().unwrap();

        matrixes.camera_position = DVec3::new(cam_x, cam_y, cam_z);

        // デフォルトの空配列
        let empty = [0f64; 16];

        // スライスを固定長配列に変換を試みて、失敗（長さ不足）ならemptyを使う
        let proj_arr: [f64; 16] = projection_buffer.try_into().unwrap_or(empty);

        let model_arr: [f64; 16] = model_buffer.try_into().unwrap_or(empty);

        matrixes.projection = DMat4::from_cols_array(&proj_arr);
        matrixes.model_view = DMat4::from_cols_array(&model_arr);
    }
    pub fn process() -> Vec<u8> {
        let instance = minecraft_gpu_3d_system();
        let matrixes = instance.matrixes.read().unwrap();
        let manager = instance.handler_manager.read().unwrap();
        let mgpu3d = MinecraftGpu3D::new(&matrixes);
        manager.render_all(mgpu3d)
    }
}
