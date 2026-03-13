use parking_lot::RwLock;
use std::collections::VecDeque;

type OptionalHandle = Option<Box<dyn MinecraftGpu3dHandle>>;

pub trait MinecraftGpu3dHandle: Send + Sync {
    // マルチスレッドで扱うならSend+Syncが必要
    fn render(&self);
}

pub struct MGpu3dHandleManager {
    handles: RwLock<Vec<OptionalHandle>>,
    free_ids: RwLock<VecDeque<usize>>,
}
impl Default for MGpu3dHandleManager {
    fn default() -> Self {
        Self {
            handles: RwLock::new(Vec::new()),
            free_ids: RwLock::new(VecDeque::new()),
        }
    }
}

impl MGpu3dHandleManager {
    pub const INVALID_ID: usize = usize::MAX;

    pub fn new() -> Self {
        Self {
            handles: RwLock::new(Vec::new()),
            free_ids: RwLock::new(VecDeque::new()),
        }
    }

    pub fn add_handle(&self, handle: Box<dyn MinecraftGpu3dHandle>) -> usize {
        // 空きスロットがあるか確認
        if let Some(id) = self.free_ids.write().pop_front() {
            self.handles.write()[id] = Some(handle);
            id
        } else {
            let mut handles = self.handles.write();
            let id = handles.len();
            handles.push(Some(handle));
            id
        }
    }

    pub fn remove_handle(&self, id: usize) -> Option<Box<dyn MinecraftGpu3dHandle>> {
        let mut handles = self.handles.write();
        if id >= handles.len() {
            return None;
        }

        let handle = handles[id].take();
        if handle.is_some() {
            // 削除に成功したら空きIDリストに追加
            self.free_ids.write().push_back(id);
        }
        handle
    }

    pub fn render_all(&self) {
        let handles = self.handles.read();
        for handle in handles.iter().flatten() {
            handle.render();
        }
    }
}
