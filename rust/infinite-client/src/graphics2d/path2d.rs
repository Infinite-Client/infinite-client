use std::f32::consts::PI;

pub struct Path2D {
    points: Vec<f32>,        // [x, y, color, width, mode]
    render_buffer: Vec<f32>, // [type, x0, y0, c0, ...]
}

impl Path2D {
    pub fn new() -> Self {
        Self {
            points: Vec::with_capacity(256),
            render_buffer: Vec::with_capacity(1024),
        }
    }

    fn push_tri(&mut self, p1: (f32, f32, i32), p2: (f32, f32, i32), p3: (f32, f32, i32)) {
        self.render_buffer.push(3.0); // Type
        // まず座標を一気に送る
        self.render_buffer.push(p1.0);
        self.render_buffer.push(p1.1);
        self.render_buffer.push(p2.0);
        self.render_buffer.push(p2.1);
        self.render_buffer.push(p3.0);
        self.render_buffer.push(p3.1);
        // 次に色を一気に送る
        self.render_buffer.push(f32::from_bits(p1.2 as u32));
        self.render_buffer.push(f32::from_bits(p2.2 as u32));
        self.render_buffer.push(f32::from_bits(p3.2 as u32));
    }

    fn push_quad(
        &mut self,
        p1: (f32, f32, i32),
        p2: (f32, f32, i32),
        p3: (f32, f32, i32),
        p4: (f32, f32, i32),
    ) {
        self.render_buffer.push(4.0); // Type
        // 座標 (x0, y0, x1, y1, x2, y2, x3, y3)
        self.render_buffer.push(p1.0);
        self.render_buffer.push(p1.1);
        self.render_buffer.push(p2.0);
        self.render_buffer.push(p2.1);
        self.render_buffer.push(p3.0);
        self.render_buffer.push(p3.1);
        self.render_buffer.push(p4.0);
        self.render_buffer.push(p4.1);
        // 色 (c0, c1, c2, c3)
        self.render_buffer.push(f32::from_bits(p1.2 as u32));
        self.render_buffer.push(f32::from_bits(p2.2 as u32));
        self.render_buffer.push(f32::from_bits(p3.2 as u32));
        self.render_buffer.push(f32::from_bits(p4.2 as u32));
    }
    // ユーティリティ: 最後の点と品質スケールを取得
    fn get_last_pos(&self) -> (f32, f32) {
        if self.points.is_empty() {
            return (0.0, 0.0);
        }
        let len = self.points.len();
        (self.points[len - 5], self.points[len - 4])
    }

    pub fn arc(
        &mut self,
        cx: f32,
        cy: f32,
        r: f32,
        start_a: f32,
        end_a: f32,
        ccw: bool,
        color: i32,
        width: f32,
    ) {
        let mut diff = end_a - start_a;
        if !ccw {
            while diff <= 0.0 {
                diff += 2.0 * PI;
            }
        } else {
            while diff >= 0.0 {
                diff -= 2.0 * PI;
            }
        }

        // 分割数は角度と半径からRust側で動的に決定
        let res = ((diff.abs() * r) / 1.5).max(8.0).min(64.0) as i32;

        for i in 0..=res {
            let angle = start_a + (diff * i as f32 / res as f32);
            let x = cx + angle.cos() * r;
            let y = cy + angle.sin() * r;
            // mode 1 = lineTo
            self.add_point_internal(x, y, color, width, 1);
        }
    }

    pub fn bezier_curve_to(
        &mut self,
        cp1: (f32, f32),
        cp2: (f32, f32),
        dest: (f32, f32),
        color: i32,
        width: f32,
    ) {
        let (p0x, p0y) = self.get_last_pos();

        // 制御点の距離から分割数を概算
        let dist = (cp1.0 - p0x).abs()
            + (cp2.0 - cp1.0).abs()
            + (dest.0 - cp2.0).abs()
            + (cp1.1 - p0y).abs()
            + (cp2.1 - cp1.1).abs()
            + (dest.1 - cp2.1).abs();
        let res = (dist / 1.5).max(8.0).min(64.0) as i32;

        for i in 1..=res {
            let t = i as f32 / res as f32;
            let it = 1.0 - t;
            // 3次ベジェ曲線の方程式
            let x = it.powi(3) * p0x
                + 3.0 * it.powi(2) * t * cp1.0
                + 3.0 * it * t.powi(2) * cp2.0
                + t.powi(3) * dest.0;
            let y = it.powi(3) * p0y
                + 3.0 * it.powi(2) * t * cp1.1
                + 3.0 * it * t.powi(2) * cp2.1
                + t.powi(3) * dest.1;
            self.add_point_internal(x, y, color, width, 1);
        }
    }
    pub fn arc_to(
        &mut self,
        x1: f32,
        y1: f32,
        x2: f32,
        y2: f32,
        radius: f32,
        color: i32,
        width: f32,
    ) {
        let (p0x, p0y) = self.get_last_pos();

        // ベクトル計算
        let dx1 = x1 - p0x;
        let dy1 = y1 - p0y;
        let dx2 = x2 - x1;
        let dy2 = y2 - y1;
        let len1 = (dx1 * dx1 + dy1 * dy1).sqrt();
        let len2 = (dx2 * dx2 + dy2 * dy2).sqrt();

        // 特殊ケース：点がつぶれている、または半径が0
        if len1 < 1e-6 || len2 < 1e-6 || radius <= 0.0 {
            self.add_point_internal(x1, y1, color, width, 1);
            return;
        }

        // 単位ベクトルと外積
        let u1x = dx1 / len1;
        let u1y = dy1 / len1;
        let u2x = dx2 / len2;
        let u2y = dy2 / len2;
        let cross = u1x * u2y - u1y * u2x;

        // 直線状の場合
        if cross.abs() < 1e-6 {
            self.add_point_internal(x1, y1, color, width, 1);
            return;
        }

        // 接線距離の計算
        let angle = (-(u1x * u2x + u1y * u2y)).clamp(-1.0, 1.0).acos();
        let tangent_dist = radius / (angle / 2.0).tan();

        // 接点 (t1, t2)
        let t1x = x1 - u1x * tangent_dist;
        let t1y = y1 - u1y * tangent_dist;
        let t2x = x1 + u2x * tangent_dist;
        let t2y = y1 + u2y * tangent_dist;

        // 円の中心 (cx, cy)
        let is_cw = cross > 0.0;
        let nx = if is_cw { -u1y } else { u1y };
        let ny = if is_cw { u1x } else { -u1x };
        let cx = t1x + nx * radius;
        let cy = t1y + ny * radius;

        // 描画
        self.add_point_internal(t1x, t1y, color, width, 1);
        let start_angle = (t1y - cy).atan2(t1x - cx);
        let end_angle = (t2y - cy).atan2(t2x - cx);

        // 既存の arc 関数を再利用して分割
        self.arc(cx, cy, radius, start_angle, end_angle, !is_cw, color, width);
    }
    fn add_point_internal(&mut self, x: f32, y: f32, color: i32, width: f32, mode: i32) {
        self.points
            .extend_from_slice(&[x, y, color as f32, width, mode as f32]);
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_new() -> *mut Path2D {
    Box::into_raw(Box::new(Path2D::new()))
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_drop(ptr: *mut Path2D) {
    if !ptr.is_null() {
        drop(unsafe { Box::from_raw(ptr) });
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_clear(ptr: *mut Path2D) {
    let path = unsafe { &mut *ptr };
    path.points.clear();
    path.render_buffer.clear();
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_add_point(
    ptr: *mut Path2D,
    x: f32,
    y: f32,
    color: i32,
    width: f32,
    mode: i32,
) {
    let path = unsafe { &mut *ptr };
    path.points
        .extend_from_slice(&[x, y, color as f32, width, mode as f32]);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_tessellate_fill(ptr: *mut Path2D, _fill_rule: i32) {
    let path = unsafe { &mut *ptr };
    path.render_buffer.clear();

    // 簡易的な耳切法 (Ear Clipping)
    let mut vertices = Vec::new();
    for chunk in path.points.chunks_exact(5) {
        vertices.push((chunk[0], chunk[1], chunk[2].to_bits() as i32));
    }

    if vertices.len() < 3 {
        return;
    }

    let mut indices: Vec<usize> = (0..vertices.len()).collect();
    while indices.len() > 2 {
        let mut ear_found = false;
        for i in 0..indices.len() {
            let prev = indices[(i + indices.len() - 1) % indices.len()];
            let curr = indices[i];
            let next = indices[(i + 1) % indices.len()];

            let a = vertices[prev];
            let b = vertices[curr];
            let c = vertices[next];
            // 凸判定
            let area = (b.0 - a.0) * (c.1 - a.1) - (b.1 - a.1) * (c.0 - a.0);
            if area > 0.0 {
                path.push_tri(a, b, c);
                indices.remove(i);
                ear_found = true;
                break;
            }
        }
        if !ear_found {
            break;
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_tessellate_stroke(ptr: *mut Path2D) {
    let path = unsafe { &mut *ptr };
    path.render_buffer.clear();

    // 先に長さを計算（5要素で1ポイント）
    let num_points = path.points.len() / 5;
    if num_points < 2 {
        return;
    }

    for i in 0..num_points - 1 {
        // インデックス計算で直接値を取得することで、pointsへの借用を最小限にする
        let idx1 = i * 5;
        let idx2 = (i + 1) * 5;

        // 不変借用はこのブロック内で完結
        let (x1, y1, c1_f, w1) = (
            path.points[idx1],
            path.points[idx1 + 1],
            path.points[idx1 + 2],
            path.points[idx1 + 3],
        );
        let (x2, y2, c2_f, w2) = (
            path.points[idx2],
            path.points[idx2 + 1],
            path.points[idx2 + 2],
            path.points[idx2 + 3],
        );

        // mode（5番目の要素）が 0 (moveTo) の場合はスキップするなどの処理が必要ならここに追加
        if path.points[idx2 + 4] == 0.0 {
            continue;
        }

        let c1 = c1_f.to_bits() as i32;
        let c2 = c2_f.to_bits() as i32;

        let dx = x2 - x1;
        let dy = y2 - y1;
        let len = (dx * dx + dy * dy).sqrt().max(1e-4);
        let nx = -dy / len;
        let ny = dx / len;

        let hw1 = w1 / 2.0;
        let hw2 = w2 / 2.0;

        // ここで path.push_quad (可変借用) を呼んでも、
        // 上記の points への不変借用は既に終わっているので安全
        path.push_quad(
            (x1 - nx * hw1, y1 - ny * hw1, c1),
            (x2 - nx * hw2, y2 - ny * hw2, c2),
            (x2 + nx * hw2, y2 + ny * hw2, c2),
            (x1 + nx * hw1, y1 + ny * hw1, c1),
        );
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_get_buffer_ptr(ptr: *mut Path2D) -> *const f32 {
    unsafe { (*ptr).render_buffer.as_ptr() }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_get_buffer_size(ptr: *mut Path2D) -> i32 {
    unsafe { (*ptr).render_buffer.len() as i32 }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_arc(
    ptr: *mut Path2D,
    cx: f32,
    cy: f32,
    r: f32,
    sa: f32,
    ea: f32,
    ccw: bool,
    color: i32,
    width: f32,
) {
    unsafe {
        (*ptr).arc(cx, cy, r, sa, ea, ccw, color, width);
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_bezier_to(
    ptr: *mut Path2D,
    cp1x: f32,
    cp1y: f32,
    cp2x: f32,
    cp2y: f32,
    x: f32,
    y: f32,
    color: i32,
    width: f32,
) {
    unsafe {
        (*ptr).bezier_curve_to((cp1x, cp1y), (cp2x, cp2y), (x, y), color, width);
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_arc_to(
    ptr: *mut Path2D,
    x1: f32,
    y1: f32,
    x2: f32,
    y2: f32,
    radius: f32,
    color: i32,
    width: f32,
) {
    unsafe {
        (*ptr).arc_to(x1, y1, x2, y2, radius, color, width);
    }
}
