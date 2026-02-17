#![feature(offset_of_enum)]

pub mod graphics2d;
pub mod graphics3d;
pub mod projectile;

#[cfg(not(target_os = "macos"))]
#[global_allocator]
static ALLOC: mimalloc::MiMalloc = mimalloc::MiMalloc;
