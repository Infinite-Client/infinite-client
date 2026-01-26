use std::f32::consts::PI;
#[repr(C)]
#[derive(Clone, Copy)]
pub enum LineJoin {
    Miter,
    Round,
    Bevel,
}
#[repr(C)]
#[derive(Clone, Copy)]
pub enum LineCap {
    Butt,
    Round,
    Square,
}

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

    fn add_point_internal(&mut self, x: f32, y: f32, color: i32, width: f32, mode: i32) {
        // 状態リセット直後の最初の点は、何が何でも moveTo (0.0) にする
        let actual_mode = if self.points.is_empty() {
            0.0
        } else {
            mode as f32
        };

        self.points
            .extend_from_slice(&[x, y, f32::from_bits(color as u32), width, actual_mode]);
    }

    pub fn tessellate_stroke(&mut self, cap: LineCap, join: LineJoin, enable_gradient: bool) {
        self.render_buffer.clear();

        let num_points = self.points.len() / 5;
        if num_points < 2 {
            return;
        }

        // 1. 各サブパスの開始インデックスを特定する
        let mut subpath_starts = Vec::new();
        for i in 0..num_points {
            // mode (5番目の要素) が 0.0 (moveTo) の位置を記録
            if self.points[i * 5 + 4] == 0.0 {
                subpath_starts.push(i);
            }
        }

        // 最初の点が moveTo でない場合（通常はないはずですが念のため）のケア
        if subpath_starts.is_empty() || subpath_starts[0] != 0 {
            subpath_starts.insert(0, 0);
        }

        // 2. 特定した範囲ごとに処理を実行
        for i in 0..subpath_starts.len() {
            let start = subpath_starts[i];
            let end = if i + 1 < subpath_starts.len() {
                subpath_starts[i + 1]
            } else {
                num_points
            };

            // この時点では self.points への不変参照は残っていないので
            // self.process_stroke_subpath (&mut self) を安全に呼べる
            self.process_stroke_range(start, end, cap, join, enable_gradient);
        }
    }
    fn process_stroke_range(
        &mut self,
        start: usize,
        end: usize,
        cap: LineCap,
        join: LineJoin,
        grad: bool,
    ) {
        let len = end - start;
        if len < 2 {
            return;
        }

        // --- 重要: 色の基準となる「パス全体の末尾」をサブパス内で特定 ---
        let last_point_idx = (end - 1) * 5;
        let last_color_f = self.points[last_point_idx + 2];

        let mut total_len = 0.0;
        if grad {
            for i in start..end - 1 {
                total_len += self.dist_by_idx(i * 5, (i + 1) * 5);
            }
        }
        // 長さが0の場合のゼロ除算防止
        let total_len = if total_len < 1e-4 { 1.0 } else { total_len };

        let mut current_dist = 0.0;
        for i in start..end - 1 {
            let p1_idx = i * 5;
            let p2_idx = (i + 1) * 5;

            let x1 = self.points[p1_idx];
            let y1 = self.points[p1_idx + 1];
            let c1_f = self.points[p1_idx + 2]; // ポイント自体の色（lerpの始点用）
            let w1 = self.points[p1_idx + 3];

            let x2 = self.points[p2_idx];
            let y2 = self.points[p2_idx + 1];
            let w2 = self.points[p2_idx + 3];

            let d = self.dist_by_idx(p1_idx, p2_idx);
            let (nx, ny) = self.normal_at(x1, y1, x2, y2, d);

            // --- 色の計算 ---
            // パスの開始点の色 (self.points[start*5 + 2]) から
            // パスの終了点の色 (last_color_f) へ、現在の距離で補間
            let start_color_f = self.points[start * 5 + 2];
            let c1 = if grad {
                self.lerp_color_bits(start_color_f, last_color_f, current_dist / total_len)
            } else {
                c1_f.to_bits() as i32
            };

            current_dist += d;

            let c2 = if grad {
                self.lerp_color_bits(start_color_f, last_color_f, current_dist / total_len)
            } else {
                c1_f.to_bits() as i32
            }; // 単色の場合は始点の色を維持
            // 本体の Quad 描画
            let hw1 = w1 / 2.0;
            let hw2 = w2 / 2.0;
            self.push_quad(
                (x1 - nx * hw1, y1 - ny * hw1, c1),
                (x2 - nx * hw2, y2 - ny * hw2, c2),
                (x2 + nx * hw2, y2 + ny * hw2, c2),
                (x1 + nx * hw1, y1 + ny * hw1, c1),
            );

            // Cap & Join ロジックへ続く (同様にインデックスから座標を取得して呼び出し)

            if i == start {
                self.draw_cap(x1, y1, -nx, -ny, hw1, c1, cap);
            }
            if i == end - 2 {
                self.draw_cap(x2, y2, nx, ny, hw2, c2, cap);
            }

            if i < end - 2 {
                let p3_idx = (i + 2) * 5;
                let x3 = self.points[p3_idx];
                let y3 = self.points[p3_idx + 1];
                let d2 = ((x3 - x2).powi(2) + (y3 - y2).powi(2)).sqrt().max(1e-4);
                let nx2 = -(y3 - y2) / d2;
                let ny2 = (x3 - x2) / d2;
                self.draw_join(x2, y2, nx, ny, nx2, ny2, hw2, c2, join);
            }
        }
    }

    // 補助関数
    fn dist_by_idx(&self, i1: usize, i2: usize) -> f32 {
        let dx = self.points[i2] - self.points[i1];
        let dy = self.points[i2 + 1] - self.points[i1 + 1];
        (dx * dx + dy * dy).sqrt()
    }
    fn draw_cap(&mut self, x: f32, y: f32, dx: f32, dy: f32, r: f32, c: i32, cap: LineCap) {
        let (nx, ny) = (-dy, dx); // 進行方向の垂線
        match cap {
            LineCap::Square => {
                self.push_quad(
                    (x + nx * r, y + ny * r, c),
                    (x + nx * r + dx * r, y + ny * r + dy * r, c),
                    (x - nx * r + dx * r, y - ny * r + dy * r, c),
                    (x - nx * r, y - ny * r, c),
                );
            }
            LineCap::Round => {
                let steps = 8;
                for i in 0..steps {
                    let a1 = (i as f32 / steps as f32) * PI;
                    let a2 = ((i + 1) as f32 / steps as f32) * PI;
                    // 回転行列で方向ベクトル dx, dy を基準に扇形を生成
                    let v1x = nx * a1.cos() - dx * a1.sin();
                    let v1y = ny * a1.cos() - dy * a1.sin();
                    let v2x = nx * a2.cos() - dx * a2.sin();
                    let v2y = ny * a2.cos() - dy * a2.sin();
                    self.push_tri(
                        (x, y, c),
                        (x + v1x * r, y + v1y * r, c),
                        (x + v2x * r, y + v2y * r, c),
                    );
                }
            }
            LineCap::Butt => {}
        }
    }

    fn draw_join(
        &mut self,
        x: f32,
        y: f32,
        n1x: f32,
        n1y: f32,
        n2x: f32,
        n2y: f32,
        r: f32,
        c: i32,
        join: LineJoin,
    ) {
        let cross = n1x * n2y - n1y * n2x;
        let is_outer_side = cross > 0.0; // 曲がり角の外側を判定

        match join {
            LineJoin::Bevel => {
                if is_outer_side {
                    self.push_tri(
                        (x, y, c),
                        (x + n1x * r, y + n1y * r, c),
                        (x + n2x * r, y + n2y * r, c),
                    );
                } else {
                    self.push_tri(
                        (x, y, c),
                        (x - n1x * r, y - n1y * r, c),
                        (x - n2x * r, y - n2y * r, c),
                    );
                }
            }
            LineJoin::Miter => {
                let mx = n1x + n2x;
                let my = n1y + n2y;
                let m_len_sq = mx * mx + my * my;
                if m_len_sq > 0.001 {
                    let miter_dist = 2.0 / m_len_sq;
                    if miter_dist <= 10.0 {
                        // Miter Limit = 10.0
                        let ox = mx * miter_dist * r;
                        let oy = my * miter_dist * r;
                        if is_outer_side {
                            self.push_quad(
                                (x, y, c),
                                (x + n1x * r, y + n1y * r, c),
                                (x + ox, y + oy, c),
                                (x + n2x * r, y + n2y * r, c),
                            );
                        } else {
                            self.push_quad(
                                (x, y, c),
                                (x - n1x * r, y - n1y * r, c),
                                (x - ox, y - oy, c),
                                (x - n2x * r, y - n2y * r, c),
                            );
                        }
                        return;
                    }
                }
                self.draw_join(x, y, n1x, n1y, n2x, n2y, r, c, LineJoin::Bevel);
            }
            LineJoin::Round => {
                let steps = 8;
                let start_ang = n1y.atan2(n1x);
                let mut diff = n2y.atan2(n2x) - start_ang;
                while diff > PI {
                    diff -= 2.0 * PI;
                }
                while diff < -PI {
                    diff += 2.0 * PI;
                }
                for i in 0..steps {
                    let a1 = start_ang + diff * (i as f32 / steps as f32);
                    let a2 = start_ang + diff * ((i + 1) as f32 / steps as f32);
                    self.push_tri(
                        (x, y, c),
                        (x + a1.cos() * r, y + a1.sin() * r, c),
                        (x + a2.cos() * r, y + a2.sin() * r, c),
                    );
                }
            }
        }
    }

    fn lerp_color_bits(&self, c1_f: f32, c2_f: f32, t: f32) -> i32 {
        let c1 = c1_f.to_bits();
        let c2 = c2_f.to_bits();
        let t = t.clamp(0.0, 1.0);
        let a = self.lerp_p(c1 >> 24, c2 >> 24, t);
        let r = self.lerp_p(c1 >> 16, c2 >> 16, t);
        let g = self.lerp_p(c1 >> 8, c2 >> 8, t);
        let b = self.lerp_p(c1, c2, t);
        ((a << 24) | (r << 16) | (g << 8) | b) as i32
    }

    fn lerp_p(&self, v1: u32, v2: u32, t: f32) -> u32 {
        let v1 = (v1 & 0xFF) as f32;
        let v2 = (v2 & 0xFF) as f32;
        (v1 + (v2 - v1) * t) as u32
    }
    pub fn move_to(&mut self, x: f32, y: f32, color: i32, width: f32) {
        self.add_point_internal(x, y, color, width, 0);
    }

    pub fn close_path(&mut self) {
        if let Some((x, y, c, w)) = self.get_first_point_of_current_subpath() {
            self.add_point_internal(x, y, c, w, 2);
        }
    }

    fn get_first_point_of_current_subpath(&self) -> Option<(f32, f32, i32, f32)> {
        // 最後に現れた mode=0 (moveTo) のポイントを探す
        for chunk in self.points.chunks_exact(5).rev() {
            if chunk[4] == 0.0 {
                return Some((chunk[0], chunk[1], chunk[2].to_bits() as i32, chunk[3]));
            }
        }
        None
    }
    fn normal_at(&self, x1: f32, y1: f32, x2: f32, y2: f32, len: f32) -> (f32, f32) {
        if len < 1e-4 {
            return (0.0, 0.0); // 距離が短すぎる場合は描画しない
        }
        (-(y2 - y1) / len, (x2 - x1) / len)
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
    // Path2D.rs 内
    fn get_last_pos(&self) -> (f32, f32) {
        if self.points.is_empty() {
            return (200.0, 200.0);
        }
        let len = self.points.len();
        // ここで points[len-1] (mode) が 2.0 (close) などの場合、
        // 次の arc の開始時に座標がリセットされていないか確認が必要
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

        let res = ((diff.abs() * r) / 1.5).max(8.0).min(64.0) as i32;

        for i in 0..=res {
            let angle = start_a + (diff * i as f32 / res as f32);
            let x = cx + angle.cos() * r;
            let y = cy + angle.sin() * r;

            // ここで add_point_internal を呼べば、自動的に最初の点が moveTo になる
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

#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_tessellate_stroke(
    ptr: *mut Path2D,
    cap: i32,
    join: i32,
    enable_gradient: bool,
) {
    unsafe {
        (*ptr).tessellate_stroke(
            match cap {
                0 => LineCap::Butt,
                1 => LineCap::Round,
                2 => LineCap::Square,
                _ => LineCap::Butt,
            },
            match join {
                0 => LineJoin::Miter,
                1 => LineJoin::Round,
                2 => LineJoin::Bevel,
                _ => LineJoin::Miter,
            },
            enable_gradient,
        );
    }
}

// path2d.rs に追加
#[unsafe(no_mangle)]
pub unsafe extern "C" fn graphics2d_path2d_close_path(ptr: *mut Path2D, color: i32, width: f32) {
    let path = unsafe { &mut *ptr };
    if let Some((x, y, _, _)) = path.get_first_point_of_current_subpath() {
        // 元の色の代わりに StrokeStyle の色を使えるように引数で受け取る
        path.add_point_internal(x, y, color, width, 2);
    }
}
