use std::sync::atomic::{AtomicU32, AtomicU64, Ordering};

/// f32を内部的にAtomicU32で保持するラッパー
pub struct AtomicF32(AtomicU32);

impl AtomicF32 {
    pub fn new(val: f32) -> Self {
        Self(AtomicU32::new(val.to_bits()))
    }

    pub fn load(&self, order: Ordering) -> f32 {
        f32::from_bits(self.0.load(order))
    }

    pub fn store(&self, val: f32, order: Ordering) {
        self.0.store(val.to_bits(), order);
    }
}

impl From<f32> for AtomicF32 {
    fn from(val: f32) -> Self {
        Self::new(val)
    }
}

/// f64を内部的にAtomicU64で保持するラッパー
pub struct AtomicF64(AtomicU64);

impl AtomicF64 {
    pub fn new(val: f64) -> Self {
        Self(AtomicU64::new(val.to_bits()))
    }

    pub fn load(&self, order: Ordering) -> f64 {
        f64::from_bits(self.0.load(order))
    }

    pub fn store(&self, val: f64, order: Ordering) {
        self.0.store(val.to_bits(), order);
    }
}

impl From<f64> for AtomicF64 {
    fn from(val: f64) -> Self {
        Self::new(val)
    }
}
