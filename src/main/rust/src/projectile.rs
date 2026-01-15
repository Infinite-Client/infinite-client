use std::f32::consts::PI;

#[repr(C)]
pub struct AdvancedResultPtr {
    pub low_pitch: f32,
    pub high_pitch: f32,
    pub yaw: f32,
    pub travel_ticks: i32,
    pub target_pos_x: f32,
    pub target_pos_y: f32,
    pub target_pos_z: f32,
    pub max_range_dist: f32,
}

#[inline(always)]
fn simulate_step(vx: &mut f32, vy: &mut f32, drag: f32, grav: f32, px: &mut f32, py: &mut f32) {
    *px += *vx;
    *py += *vy;
    *vx *= drag;
    *vy = (*vy * drag) - grav;
}

// 特定の角度で水平距離 dx に達した時の y 座標と時間を返す
fn simulate_to_x(v: f32, pitch: f32, dx: f32, max_step: i32, drag: f32, grav: f32) -> (f32, i32) {
    let rad = pitch * (PI / 180.0);
    let (mut px, mut py) = (0.0, 0.0);
    let (mut vx, mut vy) = (v * rad.cos(), -rad.sin() * v); // Minecraft: 上が負なので -sin

    for t in 1..=max_step {
        simulate_step(&mut vx, &mut vy, drag, grav, &mut px, &mut py);
        if px >= dx {
            return (py, t);
        }
    }
    (py, max_step)
}

// 最大射程距離を計算するためのシミュレーション
fn simulate_max_dist(v: f32, pitch: f32, dy: f32, max_step: i32, drag: f32, grav: f32) -> f32 {
    let rad = pitch * (PI / 180.0);
    let (mut px, mut py) = (0.0, 0.0);
    let (mut vx, mut vy) = (v * rad.cos(), -rad.sin() * v);
    for _ in 1..=max_step {
        simulate_step(&mut vx, &mut vy, drag, grav, &mut px, &mut py);
        if vy < 0.0 && py <= dy {
            return px;
        }
    }
    px
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rust_analyze_advanced(
    power: f32,
    s_x: f32,
    s_y: f32,
    s_z: f32,
    t_x: f32,
    t_y: f32,
    t_z: f32,
    v_x: f32,
    v_y: f32,
    v_z: f32,
    drag: f32,
    grav: f32,
    t_grav: f32,
    prec: i32,
    max_s: i32,
    iter: i32,
    out_ptr: *mut AdvancedResultPtr,
) {
    let (mut p_x, mut p_y, mut p_z) = (t_x, t_y, t_z);
    let (mut l_p, mut h_p, mut m_d, mut l_t) = (0.0, 0.0, 0.0, 10);

    for _ in 0..iter {
        let t = l_t as f32;
        p_x = t_x + v_x * t;
        p_y = t_y + (v_y * t) - (0.5 * t_grav * t * t);
        p_z = t_z + v_z * t;

        let dx = (p_x - s_x).hypot(p_z - s_z);
        let dy = p_y - s_y;

        // 1. 最大射程となるピッチを三分割探索 (通常 -45度付近)
        let mut low_limit = -90.0;
        let mut high_limit = 90.0;
        for _ in 0..10 {
            let m1 = low_limit + (high_limit - low_limit) / 3.0;
            let m2 = high_limit - (high_limit - low_limit) / 3.0;
            if simulate_max_dist(power, m1, dy, max_s, drag, grav)
                > simulate_max_dist(power, m2, dy, max_s, drag, grav)
            {
                high_limit = m2;
            } else {
                low_limit = m1;
            }
        }
        let max_p = (low_limit + high_limit) * 0.5;
        m_d = simulate_max_dist(power, max_p, dy, max_s, drag, grav);

        // 2. Low Arc (max_p から 90度[下向き] の間で探索)
        let mut lp = max_p;
        let mut hp = 90.0;
        let mut last_lt = max_s;
        for _ in 0..prec {
            let mid = (lp + hp) * 0.5;
            let (y, t) = simulate_to_x(power, mid, dx, max_s, drag, grav);
            if y < dy {
                hp = mid;
            } else {
                lp = mid;
            }
            last_lt = t;
        }
        l_p = (lp + hp) * 0.5;
        l_t = last_lt;

        // 3. High Arc (-90度[真上] から max_p の間で探索)
        let mut la = -90.0;
        let mut ha = max_p;
        for _ in 0..prec {
            let mid = (la + ha) * 0.5;
            let (y, _) = simulate_to_x(power, mid, dx, max_s, drag, grav);
            // High Arc側では角度を下げると（max_pに近づけると）着弾点は高くなる
            if y > dy {
                la = mid;
            } else {
                ha = mid;
            }
        }
        h_p = (la + ha) * 0.5;

        if (l_t - last_lt).abs() < 1 {
            break;
        }
    }

    let res = unsafe { &mut *out_ptr };
    res.low_pitch = l_p;
    res.high_pitch = h_p;
    res.yaw = (-(p_x - s_x)).atan2(p_z - s_z) * (180.0 / PI);
    res.travel_ticks = l_t;
    res.target_pos_x = p_x;
    res.target_pos_y = p_y;
    res.target_pos_z = p_z;
    res.max_range_dist = m_d;
}
