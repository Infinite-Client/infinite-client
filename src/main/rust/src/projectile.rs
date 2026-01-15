use std::f32::consts::PI;

#[repr(C)]
pub struct TrajectoryResult {
    pub pitch: f32,
    pub travel_ticks: i32,
    pub hit_y: f32,
    pub hit_x: f32,
    pub success: i32, // Booleanの代わりにi32を使用 (1 or 0)
}

fn simulate_core(v: f32, pitch_deg: f32, target_x: f32, max_step: i32, drag: f32, gravity: f32) -> (f32, i32) {
    let rad = pitch_deg * (PI / 180.0);
    let mut p_x = 0.0;
    let mut p_y = 0.0;
    let mut vel_x = v * rad.cos();
    let mut vel_y = -rad.sin() * v;

    for tick in 1..=max_step {
        p_x += vel_x;
        p_y += vel_y;
        vel_x *= drag;
        vel_y = (vel_y * drag) - gravity;

        if p_x >= target_x { return (p_y, tick); }
        if vel_y < 0.0 && p_y < -100.0 { break; }
    }
    (p_y, max_step)
}
#[unsafe(no_mangle)]
pub extern "C" fn rust_solve_pitch(
    power: f32, dx: f32, dy: f32,
    min_p: f32, max_p: f32,
    precision: i32, max_step: i32,
    drag: f32, gravity: f32
) -> TrajectoryResult {
    let mut low = min_p;
    let mut high = max_p;
    let mut last_y = 0.0;
    let mut last_t = 0;

    for _ in 0..precision {
        let mid = (low + high) * 0.5;
        let (y, t) = simulate_core(power, mid, dx, max_step, drag, gravity);
        last_y = y;
        last_t = t;
        if y < dy { high = mid; } else { low = mid; }
    }

    TrajectoryResult {
        pitch: (low + high) * 0.5,
        travel_ticks: last_t,
        hit_y: last_y,
        hit_x: dx,
        success: if (last_y - dy).abs() < 1.0 { 1 } else { 0 },
    }
}