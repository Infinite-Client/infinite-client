mod handle_manager;
use handle_manager::MGpu3dHandleManager;
pub use handle_manager::MinecraftGpu3dHandle;
use std::{
    ffi::c_void,
    ptr::null_mut,
    sync::{
        LazyLock,
        atomic::{self, AtomicPtr, AtomicU32},
    },
};
use xross_core::{XrossClass, xross_methods};

use super::child::MinecraftGpu3dChild;
#[derive(XrossClass)]
pub struct MGpu3dSystem {
    buffer_address: AtomicPtr<std::ffi::c_void>,
    width: AtomicU32,
    height: AtomicU32,
    device: wgpu::Device,
    queue: wgpu::Queue,
    handle_manager: MGpu3dHandleManager,
}
impl Default for MGpu3dSystem {
    fn default() -> Self {
        // wgpuの初期化フローを同期的に実行
        let (device, queue) = pollster::block_on(async {
            let instance = wgpu::Instance::default();

            let adapter = instance
                .request_adapter(&wgpu::RequestAdapterOptions {
                    power_preference: wgpu::PowerPreference::HighPerformance,
                    force_fallback_adapter: false,
                    compatible_surface: None, // 外部バッファ書き込みならNoneでOK
                })
                .await
                .expect("Failed to find an appropriate wgpu adapter");

            adapter
                .request_device(&wgpu::DeviceDescriptor {
                    label: Some("MinecraftGpu3d Device"),
                    required_features: wgpu::Features::empty(),
                    experimental_features: wgpu::ExperimentalFeatures::disabled(),
                    trace: wgpu::Trace::default(),
                    required_limits: wgpu::Limits::default(),
                    memory_hints: wgpu::MemoryHints::default(),
                })
                .await
                .expect("Failed to create wgpu device")
        });

        Self {
            buffer_address: AtomicPtr::new(null_mut::<c_void>()),
            width: AtomicU32::new(0),
            height: AtomicU32::new(0),
            device,
            queue,
            handle_manager: MGpu3dHandleManager::new(),
        }
    }
}

static M_GPU_3D_SYSTEM: LazyLock<MGpu3dSystem> = LazyLock::new(MGpu3dSystem::default);

#[xross_methods]
impl MGpu3dSystem {
    fn this() -> &'static Self {
        &M_GPU_3D_SYSTEM
    }
    #[xross_raw_method{
        args = (buf: *mut std::ffi::c_void,width:u32,height:u32);
        import = |buf,width,height|{(buf,width,height)};
        export = ||{};
    }]
    pub fn init(buf: *mut c_void, width: u32, height: u32) {
        let buf = buf as usize;
        let _ = std::thread::spawn(move || {
            let buf = buf as *mut c_void;
            Self::this().on_init(buf, width, height);
        });
    }
    fn on_init(&self, buf: *mut c_void, width: u32, height: u32) {
        self.buffer_address.store(buf, atomic::Ordering::Relaxed);
        self.width.store(width, atomic::Ordering::Relaxed);
        self.height.store(height, atomic::Ordering::Relaxed);
    }
    #[xross_method(critical)]
    pub fn resize_window(width: u32, height: u32) {
        Self::this().on_resize_window(width, height);
    }
    fn on_resize_window(&self, width: u32, height: u32) {
        self.width.store(width, atomic::Ordering::Relaxed);
        self.height.store(height, atomic::Ordering::Relaxed);
    }
    #[xross_method(critical)]
    pub fn start() {
        Self::this().on_start();
    }
    fn on_start(&self) {}
    #[xross_method(critical)]
    pub fn stop() {
        Self::this().on_stop();
    }
    fn on_stop(&self) {}
    pub fn add_handle(handle: Box<dyn MinecraftGpu3dHandle>) -> usize {
        Self::this().on_add_handle(handle)
    }
    fn on_add_handle(&self, handle: Box<dyn MinecraftGpu3dHandle>) -> usize {
        self.handle_manager.add_handle(handle)
    }
    pub(crate) fn child(&self) -> MinecraftGpu3dChild {
        MinecraftGpu3dChild::new(
            self.width.load(atomic::Ordering::Relaxed),
            self.height.load(atomic::Ordering::Relaxed),
        )
    }
}
