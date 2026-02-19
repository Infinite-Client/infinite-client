use std::f32::consts::PI;
use xross_core::{xross_methods, XrossClass};

#[derive(XrossClass, Default, Clone, Debug)]
#[xross(clonable(panicable), drop(panicable))]
pub struct AdvancedResult {
    #[xross_field]
    pub low_pitch: f32,
    #[xross_field]
    pub high_pitch: f32,
    #[xross_field]
    pub yaw: f32,
    #[xross_field]
    pub travel_ticks: i32,
    #[xross_field]
    pub target_pos_x: f32,
    #[xross_field]
    pub target_pos_y: f32,
    #[xross_field]
    pub target_pos_z: f32,
    #[xross_field]
    pub max_range_dist: f32,
}

#[inline(always)]
fn simulate_step(vx: &mut f32, vy: &mut f32, drag: f32, grav: f32, px: &mut f32, py: &mut f32) {
    *px += *vx;
    *py += *vy;
    *vx *= drag;
    *vy = (*vy * drag) - grav;
}

fn simulate_to_x(v: f32, pitch: f32, dx: f32, max_step: i32, drag: f32, grav: f32) -> (f32, i32) {
    let rad = pitch * (PI / 180.0);
    let (mut px, mut py) = (0.0, 0.0);
    let (mut vx, mut vy) = (v * rad.cos(), -rad.sin() * v);

    for t in 1..=max_step {
        simulate_step(&mut vx, &mut vy, drag, grav, &mut px, &mut py);
        if px >= dx {
            return (py, t);
        }
    }
    (py, max_step)
}

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

#[xross_methods]
impl AdvancedResult {
    #[xross_new(panicable)]
    #[allow(clippy::too_many_arguments)]
    pub fn new(
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
    ) -> Self {
        let (mut p_x, mut p_y, mut p_z) = (t_x, t_y, t_z);
        let (mut l_p, mut h_p, mut m_d, mut l_t) = (0.0, 0.0, 0.0, 10);

        for _ in 0..iter {
            let t = l_t as f32;
            p_x = t_x + v_x * t;
            p_y = t_y + (v_y * t) - (0.5 * t_grav * t * t);
            p_z = t_z + v_z * t;

            let dx = (p_x - s_x).hypot(p_z - s_z);
            let dy = p_y - s_y;

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

            let mut la = -90.0;
            let mut ha = max_p;
            for _ in 0..prec {
                let mid = (la + ha) * 0.5;
                let (y, _) = simulate_to_x(power, mid, dx, max_s, drag, grav);
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

        Self {
            low_pitch: l_p,
            high_pitch: h_p,
            yaw: (-(p_x - s_x)).atan2(p_z - s_z) * (180.0 / PI),
            travel_ticks: l_t,
            target_pos_x: p_x,
            target_pos_y: p_y,
            target_pos_z: p_z,
            max_range_dist: m_d,
        }
    }
}
