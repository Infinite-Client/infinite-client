use crate::mgpu3d::MinecraftGpu3D;
use rayon::prelude::*;

pub trait GpuHandler: Send + Sync {
    fn render<'a>(&self, mgpu3d: &mut MinecraftGpu3D<'a>);
}
#[derive(Default)]
pub struct HandlerManager {
    handlers: Vec<Option<Box<dyn GpuHandler>>>,
    free_ids: Vec<usize>,
}

impl HandlerManager {
    /// ハンドラーを追加し、割り当てられたIDを返す
    pub fn add_handler(&mut self, handler: Box<dyn GpuHandler>) -> usize {
        if let Some(id) = self.free_ids.pop() {
            // 再利用可能なIDがある場合
            self.handlers[id] = Some(handler);
            id
        } else {
            // 新規に末尾へ追加
            let id = self.handlers.len();
            self.handlers.push(Some(handler));
            id
        }
    }
    /// 指定したIDのハンドラーを削除する
    pub fn remove_handler(&mut self, id: usize) -> bool {
        if id < self.handlers.len() && self.handlers[id].is_some() {
            self.handlers[id] = None;
            self.free_ids.push(id);
            true
        } else {
            false
        }
    }
    pub fn render_all(&self, mgpu3d: MinecraftGpu3D) -> Vec<u8> {
        // 1. 有効なハンドラーの参照を収集（インデックス順を維持）
        let active_handlers: Vec<&Box<dyn GpuHandler>> = self.handlers.iter().flatten().collect();

        if active_handlers.is_empty() {
            return Vec::new();
        }

        // 2. 並列レンダリング (コマンド生成)
        // par_iter() は元の Vec の順序を保持したまま結果を collect します
        let commands_list: Vec<Vec<u8>> = active_handlers
            .into_par_iter()
            .map(|handler| {
                let mut local_gpu = mgpu3d.clone();
                handler.render(&mut local_gpu);
                local_gpu.build()
            })
            .collect();

        // 3. 結合 (一気にメモリを確保してコピー)
        let total_size: usize = commands_list.iter().map(|c| c.len()).sum();
        let mut result = Vec::with_capacity(total_size);

        for cmd in commands_list {
            result.extend(cmd);
        }
        result
    }
}
