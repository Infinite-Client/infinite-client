#![feature(offset_of_enum)]

pub mod graphics2d;
pub mod graphics3d;
pub mod projectile;

#[global_allocator]
static ALLOC: mimalloc::MiMalloc = mimalloc::MiMalloc;
