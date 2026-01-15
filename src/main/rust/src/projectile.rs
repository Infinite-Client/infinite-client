use std::f32::consts::PI;

#[repr(C)]
pub struct AdvancedAnalysisResult {
    pub low_pitch: f32,
    pub high_pitch: f32,
    pub yaw: f32,
    pub travel_ticks: i32,
    pub target_pos_x: f32,
    pub target_pos_y: f32,
    pub target_pos_z: f32,
    pub max_range_dist: f32,
}

// 基礎的な1ステップのシミュレーション
fn simulate_step(v: &mut f32, vy: &mut f32, drag: f32, grav: f32, px: &mut f32, py: &mut f32) {
    *px += *v;
    *py += *vy;
    *v *= drag;
    *vy = (*vy * drag) - grav;
}

// 指定したピッチでの到達距離と時間を計算
fn simulate_for_dist(
    v: f32,
    pitch: f32,
    dy: f32,
    max_step: i32,
    drag: f32,
    grav: f32,
) -> (f32, i32) {
    let rad = pitch * (PI / 180.0);
    let (mut px, mut py) = (0.0, 0.0);
    let (mut vx, mut vy) = (v * rad.cos(), -rad.sin() * v);
    for t in 1..=max_step {
        simulate_step(&mut vx, &mut vy, drag, grav, &mut px, &mut py);
        if vy < 0.0 && py <= dy {
            return (px, t);
        }
    }
    (px, max_step)
}

#[unsafe(no_mangle)]
pub extern "C" fn rust_analyze_advanced(
    power: f32,
    start_x: f32,
    start_y: f32,
    start_z: f32,
    target_x: f32,
    target_y: f32,
    target_z: f32,
    v_x: f32,
    v_y: f32,
    v_z: f32,
    drag: f32,
    grav: f32,
    target_grav: f32,
    precision: i32,
    max_step: i32,
    iterations: i32,
) -> AdvancedAnalysisResult {
    let mut predicted_x = target_x;
    let mut predicted_y = target_y;
    let mut predicted_z = target_z;
    let mut last_ticks = 10;

    let mut low_p = 0.0;
    let mut high_p = 0.0;
    let mut max_d = 0.0;

    // 初期 Yaw の計算（静止ターゲット対応）
    let mut final_yaw = (-(target_x - start_x)).atan2(target_z - start_z) * (180.0 / PI);

    for _ in 0..iterations {
        let t = last_ticks as f32;
        predicted_x = target_x + v_x * t;
        predicted_y = target_y + (v_y * t) - (0.5 * target_grav * t * t);
        predicted_z = target_z + v_z * t;

        let dx = predicted_x - start_x;
        let dy = predicted_y - start_y;
        let dz = predicted_z - start_z;
        let h_dist = (dx * dx + dz * dz).sqrt();

        final_yaw = (-dx).atan2(dz) * (180.0 / PI);
        // 最大射程の確認 (三分割探索)
        let mut l = -90.0;
        let mut r = 45.0;
        for _ in 0..8 {
            let m1 = l + (r - l) / 3.0;
            let m2 = r - (r - l) / 3.0;
            if simulate_for_dist(power, m1, dy, max_step, drag, grav).0
                > simulate_for_dist(power, m2, dy, max_step, drag, grav).0
            {
                r = m2;
            } else {
                l = m1;
            }
        }
        let max_range_p = (l + r) * 0.5;
        max_d = simulate_for_dist(power, max_range_p, dy, max_step, drag, grav).0;

        // 低角と高角を解く
        let (lp, lt) = solve_single_pitch(
            power,
            h_dist,
            dy,
            max_range_p,
            45.0,
            precision,
            max_step,
            drag,
            grav,
        );
        let (hp, _ht) = solve_single_pitch(
            power,
            h_dist,
            dy,
            -90.0,
            max_range_p,
            precision,
            max_step,
            drag,
            grav,
        );

        low_p = lp;
        high_p = hp;

        // 収束判定のためのticks更新 (低角を基準にするのが一般的)
        if (last_ticks - lt).abs() < 1 {
            last_ticks = lt;
            break;
        }
        last_ticks = lt;
    }

    AdvancedAnalysisResult {
        low_pitch: low_p,
        high_pitch: high_p,
        yaw: final_yaw,
        travel_ticks: last_ticks,
        target_pos_x: predicted_x,
        target_pos_y: predicted_y,
        target_pos_z: predicted_z,
        max_range_dist: max_d,
    }
}

// 内部用ピッチ解法 (前回のものを流用)
fn solve_single_pitch(
    power: f32,
    dx: f32,
    dy: f32,
    mut low: f32,
    mut high: f32,
    precision: i32,
    max_step: i32,
    drag: f32,
    grav: f32,
) -> (f32, i32) {
    let mut last_t = 0;
    for _ in 0..precision {
        let mid = (low + high) * 0.5;
        let (y, t) = simulate_core(power, mid, dx, max_step, drag, grav);
        last_t = t;
        if y < dy {
            high = mid;
        } else {
            low = mid;
        }
    }
    ((low + high) * 0.5, last_t)
}

fn simulate_core(
    v: f32,
    pitch: f32,
    target_x: f32,
    max_step: i32,
    drag: f32,
    grav: f32,
) -> (f32, i32) {
    let rad = pitch * (PI / 180.0);
    let (mut px, mut py) = (0.0, 0.0);
    let (mut vx, mut vy) = (v * rad.cos(), -rad.sin() * v);
    for t in 1..=max_step {
        simulate_step(&mut vx, &mut vy, drag, grav, &mut px, &mut py);
        if px >= target_x {
            return (py, t);
        }
    }
    (py, max_step)
}
