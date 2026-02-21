pub mod command;
pub mod highlight;

use crate::mgpu3d::command::{
    Color4, LineCommand, MinecraftGpu3dCommand, QuadCommand, QuadGradientCommand, TriangleCommand,
    TriangleGradientCommand,
};
use glam::{DMat4, DVec3};
use std::collections::HashMap;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, OnceLock, RwLock};
use xross_core::xross_function;

/// 描画ハンドラの型定義
type DrawHandler = Arc<dyn Fn(&mut MinecraftGpu3DChild) + Send + Sync + 'static>;

#[derive(Clone, Copy)]
pub struct RenderState {
    pub model_view: DMat4,
    pub projection: DMat4,
    pub camera_pos: DVec3,
}

impl Default for RenderState {
    fn default() -> Self {
        Self {
            model_view: DMat4::IDENTITY,
            projection: DMat4::IDENTITY,
            camera_pos: DVec3::ZERO,
        }
    }
}

struct MinecraftGpu3D {
    commands: Mutex<Vec<MinecraftGpu3dCommand>>,
    buffer_len: AtomicUsize,
    handlers: Mutex<HashMap<usize, DrawHandler>>,
    next_handler_id: AtomicUsize,
    render_state: RwLock<RenderState>,
}

/// スレッドごとに作成される描画コンテキスト
pub struct MinecraftGpu3DChild {
    buffer: Vec<MinecraftGpu3dCommand>,
    buffer_len: usize,
    matrix_stack: Vec<DMat4>,
    base_state: RenderState,
}

impl Default for MinecraftGpu3D {
    fn default() -> Self {
        let mut handlers = HashMap::new();
        // ハイライト描画ハンドラをID 0として事前登録
        handlers.insert(0, Arc::new(|g: &mut MinecraftGpu3DChild| {
            highlight::render_highlights(g);
        }) as DrawHandler);

        Self {
            commands: Mutex::new(Vec::with_capacity(1024)),
            buffer_len: AtomicUsize::new(0),
            handlers: Mutex::new(handlers),
            next_handler_id: AtomicUsize::new(1),
            render_state: RwLock::new(RenderState::default()),
        }
    }
}

static MINECRAFT_GPU_3D: OnceLock<MinecraftGpu3D> = OnceLock::new();

impl MinecraftGpu3D {
    fn global() -> &'static MinecraftGpu3D {
        MINECRAFT_GPU_3D.get_or_init(MinecraftGpu3D::default)
    }

    fn extend(&self, new_commands: Vec<MinecraftGpu3dCommand>, len: usize) {
        if new_commands.is_empty() {
            return;
        }
        if let Ok(mut cmds) = self.commands.lock() {
            cmds.extend(new_commands);
            self.buffer_len.fetch_add(len, Ordering::SeqCst);
        }
    }

    fn take_all(&self) -> (Vec<MinecraftGpu3dCommand>, usize) {
        if let Ok(mut cmds) = self.commands.lock() {
            let len = self.buffer_len.swap(0, Ordering::SeqCst);
            (std::mem::take(&mut *cmds), len)
        } else {
            (Vec::new(), 0)
        }
    }
}

impl MinecraftGpu3DChild {
    fn new(state: RenderState) -> Self {
        Self {
            buffer: Vec::with_capacity(128),
            buffer_len: 0,
            matrix_stack: vec![DMat4::IDENTITY],
            base_state: state,
        }
    }

    fn current_model(&self) -> DMat4 {
        *self.matrix_stack.last().unwrap_or(&DMat4::IDENTITY)
    }

    /// 座標変換: 何もしない (ワールド座標をそのまま返すテスト)
    fn transform(&self, pos: DVec3) -> DVec3 {
        pos
    }

    pub fn push_matrix(&mut self) {
        let current = self.current_model();
        self.matrix_stack.push(current);
    }

    pub fn pop_matrix(&mut self) {
        if self.matrix_stack.len() > 1 {
            self.matrix_stack.pop();
        }
    }

    pub fn translate(&mut self, x: f64, y: f64, z: f64) {
        if let Some(top) = self.matrix_stack.last_mut() {
            *top = *top * DMat4::from_translation(DVec3::new(x, y, z));
        }
    }

    fn push_command(&mut self, mut command: MinecraftGpu3dCommand) {
        match &mut command {
            MinecraftGpu3dCommand::Line(cmd) => {
                cmd.start = self.transform(cmd.start);
                cmd.end = self.transform(cmd.end);
            }
            MinecraftGpu3dCommand::Triangle(cmd) => {
                for v in &mut cmd.vertices {
                    *v = self.transform(*v);
                }
            }
            MinecraftGpu3dCommand::TriangleGradient(cmd) => {
                for (v, _) in &mut cmd.vertices {
                    *v = self.transform(*v);
                }
            }
            MinecraftGpu3dCommand::Quad(cmd) => {
                for v in &mut cmd.vertices {
                    *v = self.transform(*v);
                }
            }
            MinecraftGpu3dCommand::QuadGradient(cmd) => {
                for (v, _) in &mut cmd.vertices {
                    *v = self.transform(*v);
                }
            }
        }
        self.buffer_len += command.buffer_length();
        self.buffer.push(command);
    }

    pub fn line(&mut self, start: DVec3, end: DVec3, color: Color4, width: f32) {
        self.push_command(MinecraftGpu3dCommand::Line(LineCommand {
            start,
            end,
            color,
            width,
        }));
    }

    pub fn triangle(&mut self, a: DVec3, b: DVec3, c: DVec3, color: Color4) {
        self.push_command(MinecraftGpu3dCommand::Triangle(TriangleCommand {
            vertices: [a, b, c],
            color,
        }));
    }

    pub fn triangle_gradient(&mut self, vertices: [(DVec3, Color4); 3]) {
        self.push_command(MinecraftGpu3dCommand::TriangleGradient(
            TriangleGradientCommand { vertices },
        ));
    }

    pub fn quad(&mut self, a: DVec3, b: DVec3, c: DVec3, d: DVec3, color: Color4) {
        self.push_command(MinecraftGpu3dCommand::Quad(QuadCommand {
            vertices: [a, b, c, d],
            color,
        }));
    }

    pub fn quad_gradient(&mut self, vertices: [(DVec3, Color4); 4]) {
        self.push_command(MinecraftGpu3dCommand::QuadGradient(QuadGradientCommand {
            vertices,
        }));
    }
}

impl Drop for MinecraftGpu3DChild {
    fn drop(&mut self) {
        if !self.buffer.is_empty() {
            let len = self.buffer_len;
            MinecraftGpu3D::global().extend(std::mem::take(&mut self.buffer), len);
        }
    }
}

// --- Public API ---

pub fn register_handler<F>(handler: F) -> usize
where
    F: Fn(&mut MinecraftGpu3DChild) + Send + Sync + 'static,
{
    let global = MinecraftGpu3D::global();
    let id = global.next_handler_id.fetch_add(1, Ordering::SeqCst);
    if let Ok(mut handlers) = global.handlers.lock() {
        handlers.insert(id, Arc::new(handler));
    }
    id
}

#[xross_function(package = "com.mgpu3d")]
pub fn remove_handler(id: usize) {
    let global = MinecraftGpu3D::global();
    if let Ok(mut handlers) = global.handlers.lock() {
        handlers.remove(&id);
    }
}

/// critical(heap_access) を使用して、Javaのヒープ配列を直接受け取れるようにします
#[xross_function(package = "com.mgpu3d", critical(heap_access))]
pub fn update_matrices(
    model_view: &[f64],
    projection: &[f64],
    cam_x: f64,
    cam_y: f64,
    cam_z: f64,
) {
    let global = MinecraftGpu3D::global();
    if let Ok(mut state) = global.render_state.write() {
        if model_view.len() >= 16 {
            state.model_view = DMat4::from_cols_slice(&model_view[0..16]);
        }
        if projection.len() >= 16 {
            state.projection = DMat4::from_cols_slice(&projection[0..16]);
        }
        state.camera_pos = DVec3::new(cam_x, cam_y, cam_z);
    }
}

static POOL: OnceLock<threadpool::ThreadPool> = OnceLock::new();

#[xross_function(package = "com.mgpu3d")]
pub fn process_handlers() {
    let global = MinecraftGpu3D::global();
    let state = if let Ok(s) = global.render_state.read() {
        *s
    } else {
        return;
    };

    let handlers = if let Ok(h) = global.handlers.lock() {
        let h_list = h.values().cloned().collect::<Vec<_>>();
        if !h_list.is_empty() {
            println!("Rust process_handlers: {} handlers executing", h_list.len());
        }
        h_list
    } else {
        return;
    };

    if handlers.is_empty() {
        return;
    }

    let pool = POOL.get_or_init(|| threadpool::ThreadPool::new(num_cpus::get()));
    let (tx, rx) = std::sync::mpsc::channel();
    let handler_count = handlers.len();

    for h in handlers {
        let tx = tx.clone();
        pool.execute(move || {
            let mut child = MinecraftGpu3DChild::new(state);
            h(&mut child);
            drop(child);
            let _ = tx.send(());
        });
    }

    for _ in 0..handler_count {
        let _ = rx.recv();
    }
}

#[xross_function(package = "com.mgpu3d")]
pub fn flush() -> Vec<u8> {
    let (commands, total_len) = MinecraftGpu3D::global().take_all();
    if total_len == 0 {
        return Vec::new();
    }
    println!("Rust Flush: flushing {} commands, {} bytes", commands.len(), total_len);
    let mut buffer = vec![0u8; total_len];
    let mut offset = 0;
    for command in commands {
        command.write_to_buffer(&mut buffer, &mut offset);
    }
    buffer
}
