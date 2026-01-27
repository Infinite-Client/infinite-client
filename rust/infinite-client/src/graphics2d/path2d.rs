pub mod ffi;

use lyon::lyon_tessellation::{FillVertex, GeometryBuilderError, VertexId};
use lyon::math::point;
use lyon::path::{FillRule, LineCap, LineJoin, Path};
use lyon::tessellation::{FillGeometryBuilder, FillOptions, FillTessellator, GeometryBuilder};
use std::f64::consts::PI;

#[derive(Default)]
pub struct Path2D {
    segments: Vec<SegmentData>,
    pub pen: Pen,
    buffer: Vec<f32>,
}
pub struct SegmentData {
    points: Vec<PointData>,
    is_closed: bool,
}
impl Default for SegmentData {
    fn default() -> Self {
        Self {
            points: Vec::new(),
            is_closed: false,
        }
    }
}
pub struct PointData {
    pub x: f64,
    pub y: f64,
    pub color: Color,
    pub width: f64,
    pub line_cap: LineCap,
    pub line_join: LineJoin,
}
impl Default for PointData {
    fn default() -> Self {
        Self {
            x: 0.0,
            y: 0.0,
            color: Color::default(),
            width: 0.0,
            line_cap: LineCap::Butt,
            line_join: LineJoin::Miter,
        }
    }
}
#[derive(Copy, Clone, Debug, PartialEq)]
pub struct Color {
    pub r: f32,
    pub g: f32,
    pub b: f32,
    pub a: f32,
}

impl Default for Color {
    fn default() -> Self {
        Self {
            r: 0.0,
            g: 0.0,
            b: 0.0,
            a: 0.0,
        }
    }
}

impl Color {
    /// 0xAARRGGBB 形式の i32 から Color を作成
    pub fn from_raw(raw: i32) -> Self {
        let u = raw as u32;
        Self {
            a: ((u >> 24) & 0xFF) as f32 / 255.0,
            r: ((u >> 16) & 0xFF) as f32 / 255.0,
            g: ((u >> 8) & 0xFF) as f32 / 255.0,
            b: (u & 0xFF) as f32 / 255.0,
        }
    }

    /// Color を 0xAARRGGBB 形式の i32 に変換
    pub fn to_raw(&self) -> i32 {
        let a = (self.a.clamp(0.0, 1.0) * 255.0) as u32;
        let r = (self.r.clamp(0.0, 1.0) * 255.0) as u32;
        let g = (self.g.clamp(0.0, 1.0) * 255.0) as u32;
        let b = (self.b.clamp(0.0, 1.0) * 255.0) as u32;

        ((a << 24) | (r << 16) | (g << 8) | b) as i32
    }

    /// 二つの色を比率(t)で混ぜる (線形補間: lerp)
    /// t=0.0 で self, t=1.0 で other になる
    pub fn mix(&self, other: Self, t: f32) -> Self {
        let t = t.clamp(0.0, 1.0);
        Self {
            r: self.r + (other.r - self.r) * t,
            g: self.g + (other.g - self.g) * t,
            b: self.b + (other.b - self.b) * t,
            a: self.a + (other.a - self.a) * t,
        }
    }
}

pub struct Pen {
    pub color: Color,
    pub width: f64,
    pub line_cap: LineCap,
    pub line_join: LineJoin,
    pub is_gradient_enabled: bool,
}

impl Default for Pen {
    fn default() -> Self {
        Self {
            color: Color::default(),
            width: 0.0,
            line_cap: LineCap::Butt,
            line_join: LineJoin::Miter,
            is_gradient_enabled: false,
        }
    }
}

impl Path2D {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn begin(&mut self) {
        self.segments.clear();
    }
    pub fn move_to(&mut self, x: f64, y: f64) {
        self.segments.push(SegmentData::default());
        let point = self.point(x, y);
        let current_segment = self.segments.last_mut().unwrap();
        current_segment.points.push(point);
    }
    fn point(&self, x: f64, y: f64) -> PointData {
        let mut point = PointData::default();
        point.x = x;
        point.y = y;
        point.color = self.pen.color;
        point.line_cap = self.pen.line_cap;
        point.line_join = self.pen.line_join;
        point.width = self.pen.width;
        point
    }
    pub fn line_to(&mut self, x: f64, y: f64) {
        // 現在のセグメントが存在し、かつ既に閉じられているかチェック
        let needs_new_segment = self.segments.last().map(|s| s.is_closed).unwrap_or(false);

        if needs_new_segment {
            // 閉じられた後の line_to は、前の終点（＝閉じられたパスの始点）を
            // 起点とする新しいセグメントとして扱うのが一般的
            let last_start_point = self
                .segments
                .last()
                .and_then(|s| s.points.first())
                .map(|p| (p.x, p.y));

            if let Some((sx, sy)) = last_start_point {
                self.move_to(sx, sy);
            } else {
                self.move_to(x, y); // 安全策：始点がなければ今ここを始点にする
            }
        }

        let point = self.point(x, y);
        self.push_point(point);
    }

    pub fn close(&mut self) {
        if let Some(current_segment) = self.segments.last_mut() {
            // 既に点がある場合のみ閉じるフラグを立てる
            if !current_segment.points.is_empty() {
                current_segment.is_closed = true;
            }
        }
    }

    fn push_point(&mut self, point: PointData) {
        if let Some(current_segment) = self.segments.last_mut() {
            // ここで is_closed をチェックしても良いが、
            // move_to / line_to 側で制御するほうが責務が明確
            current_segment.points.push(point);
        } else {
            // セグメントがない場合は自動的に作成
            let mut segment = SegmentData::default();
            segment.points.push(point);
            self.segments.push(segment);
        }
    }

    // ユーティリティ: 現在の最新の点を取得する
    fn last_point(&self) -> Option<&PointData> {
        self.segments.last()?.points.last()
    }

    // ユーティリティ: 点を追加し、必要に応じて色を補間する
    fn push_interpolated_point(&mut self, x: f64, y: f64, t: f32, start_color: Color) {
        let mut p = self.point(x, y);
        if self.pen.is_gradient_enabled {
            // 始点の色(start_color)から現在のペンの色(self.pen.color)へ補間
            p.color = start_color.mix(self.pen.color, t);
        }
        self.push_point(p);
    }

    // --- 新規実装関数 ---

    /// 3次ベジェ曲線
    pub fn bezier_curve_to(&mut self, cp1x: f64, cp1y: f64, cp2x: f64, cp2y: f64, x: f64, y: f64) {
        let start = self.last_point().map(|p| (p.x, p.y)).unwrap_or((0.0, 0.0));
        let start_color = self.last_point().map(|p| p.color).unwrap_or(self.pen.color);

        let steps = 20; // 分割数（必要に応じて精度を調整）
        for i in 1..=steps {
            let t = i as f64 / steps as f64;
            let inv_t = 1.0 - t;

            // Bernstein多項式による計算
            let px = inv_t.powi(3) * start.0
                + 3.0 * inv_t.powi(2) * t * cp1x
                + 3.0 * inv_t * t.powi(2) * cp2x
                + t.powi(3) * x;
            let py = inv_t.powi(3) * start.1
                + 3.0 * inv_t.powi(2) * t * cp1y
                + 3.0 * inv_t * t.powi(2) * cp2y
                + t.powi(3) * y;

            self.push_interpolated_point(px, py, t as f32, start_color);
        }
    }

    /// 2次ベジェ曲線
    pub fn quadratic_curve_to(&mut self, cpx: f64, cpy: f64, x: f64, y: f64) {
        let start = self.last_point().map(|p| (p.x, p.y)).unwrap_or((0.0, 0.0));
        let start_color = self.last_point().map(|p| p.color).unwrap_or(self.pen.color);

        let steps = 15;
        for i in 1..=steps {
            let t = i as f64 / steps as f64;
            let inv_t = 1.0 - t;

            let px = inv_t.powi(2) * start.0 + 2.0 * inv_t * t * cpx + t.powi(2) * x;
            let py = inv_t.powi(2) * start.1 + 2.0 * inv_t * t * cpy + t.powi(2) * y;

            self.push_interpolated_point(px, py, t as f32, start_color);
        }
    }

    /// 円弧 (arc)
    pub fn arc(
        &mut self,
        x: f64,
        y: f64,
        radius: f64,
        start_angle: f64,
        end_angle: f64,
        counterclockwise: bool,
    ) {
        let mut diff = end_angle - start_angle;

        // 角度の正規化
        if counterclockwise {
            if diff > 0.0 {
                diff -= 2.0 * PI;
            } else if diff < -2.0 * PI {
                diff += 2.0 * PI;
            }
        } else {
            if diff < 0.0 {
                diff += 2.0 * PI;
            } else if diff > 2.0 * PI {
                diff -= 2.0 * PI;
            }
        }

        let steps = (diff.abs() / (PI / 18.0)).ceil().max(10.0) as i32; // 約10度ごとに分割
        let start_color = self.last_point().map(|p| p.color).unwrap_or(self.pen.color);

        for i in 0..=steps {
            let t = i as f64 / steps as f64;
            let angle = start_angle + diff * t;
            let px = x + radius * angle.cos();
            let py = y + radius * angle.sin();
            self.push_point_in_sequence(i, px, py, t as f32, start_color);
        }
    }
    fn push_point_in_sequence(&mut self, i: i32, x: f64, y: f64, t: f32, start_color: Color) {
        if i == 0 {
            if self.segments.is_empty() {
                // パスが空ならここを起点にする
                self.move_to(x, y);
            } else {
                // 既存のパスがあれば、そこからこの曲線の開始点まで直線を引く
                // (Canvas API の arc や ellipse の標準仕様に準拠)
                self.line_to(x, y);
            }
        } else {
            // 2点目以降はグラデーションを考慮して追加
            self.push_interpolated_point(x, y, t, start_color);
        }
    }

    /// 角丸接続 (arcTo)
    pub fn arc_to(&mut self, x1: f64, y1: f64, x2: f64, y2: f64, radius: f64) {
        let start = self.last_point().map(|p| (p.x, p.y)).unwrap_or((x1, y1));

        // ベクトル計算
        let v1 = (start.0 - x1, start.1 - y1);
        let v2 = (x2 - x1, y2 - y1);
        let len1 = (v1.0.powi(2) + v1.1.powi(2)).sqrt();
        let len2 = (v2.0.powi(2) + v2.1.powi(2)).sqrt();

        // 直線上にある場合のフォールバック
        if len1 < 1e-6 || len2 < 1e-6 {
            self.line_to(x1, y1);
            return;
        }

        let cos_theta = (v1.0 * v2.0 + v1.1 * v2.1) / (len1 * len2);
        let theta = cos_theta.acos();
        let dist = radius / (theta / 2.0).tan();

        // 接点計算
        let p_start = (x1 + v1.0 / len1 * dist, y1 + v1.1 / len1 * dist);
        let p_end = (x1 + v2.0 / len2 * dist, y1 + v2.1 / len2 * dist);

        // 中心点
        let v_bisect = (v1.0 / len1 + v2.0 / len2, v1.1 / len1 + v2.1 / len2);
        let len_b = (v_bisect.0.powi(2) + v_bisect.1.powi(2)).sqrt();
        let center_dist = radius / (theta / 2.0).sin();
        let center = (
            x1 + v_bisect.0 / len_b * center_dist,
            y1 + v_bisect.1 / len_b * center_dist,
        );

        // 角度計算
        let start_angle = (p_start.1 - center.1).atan2(p_start.0 - center.0);
        let end_angle = (p_end.1 - center.1).atan2(p_end.0 - center.0);

        // 最初の接点まで直線を引き、そこからarcを描画
        self.line_to(p_start.0, p_start.1);

        // 外積で回転方向を判定
        let cross_product = v1.0 * v2.1 - v1.1 * v2.0;
        self.arc(
            center.0,
            center.1,
            radius,
            start_angle,
            end_angle,
            cross_product > 0.0,
        );
    }

    /// 楕円
    pub fn ellipse(
        &mut self,
        x: f64,
        y: f64,
        radius_x: f64,
        radius_y: f64,
        rotation: f64,
        start_angle: f64,
        end_angle: f64,
        counterclockwise: bool,
    ) {
        let mut diff = end_angle - start_angle;
        if counterclockwise {
            if diff > 0.0 {
                diff -= 2.0 * PI;
            }
        } else {
            if diff < 0.0 {
                diff += 2.0 * PI;
            }
        }

        let steps = 40;
        let start_color = self.last_point().map(|p| p.color).unwrap_or(self.pen.color);
        let cos_rot = rotation.cos();
        let sin_rot = rotation.sin();

        for i in 0..=steps {
            let t = i as f64 / steps as f64;
            let angle = start_angle + diff * t;

            // ローカル座標
            let lx = radius_x * angle.cos();
            let ly = radius_y * angle.sin();

            // 回転適用
            let px = x + lx * cos_rot - ly * sin_rot;
            let py = y + lx * sin_rot + ly * cos_rot;

            self.push_point_in_sequence(i, px, py, t as f32, start_color);
        }
    }
    /// 塗りつぶしのテッセレーションを実行
    pub fn tessellate_fill(&mut self, rule: FillRule) {
        self.buffer.clear();

        let mut builder = Path::builder();
        for segment in &self.segments {
            if segment.points.is_empty() {
                continue;
            }

            let first = &segment.points[0];
            builder.begin(point(first.x as f32, first.y as f32));

            for p in &segment.points[1..] {
                builder.line_to(point(p.x as f32, p.y as f32));
            }

            if segment.is_closed {
                builder.end(true);
            } else {
                builder.end(false);
            }
        }

        let path = builder.build();
        let mut tessellator = FillTessellator::new();

        let mut output = FillOutput {
            buffer: &mut self.buffer,
            vertices: Vec::new(), // ここで一時頂点リストを初期化
            current_pen_color: self.pen.color.to_raw(), // 現在のペンの色を渡す
        };
        let options = FillOptions::default().with_fill_rule(rule);

        let _ = tessellator.tessellate_path(&path, &options, &mut output);
    }

    /// FFI用: バッファポインタの取得
    pub fn get_buffer_ptr(&self) -> *const f32 {
        self.buffer.as_ptr()
    }

    /// FFI用: バッファ要素数の取得
    pub fn get_buffer_size(&self) -> usize {
        self.buffer.len()
    }

    pub fn tessellate_stroke(&mut self, cap: LineCap, join: LineJoin, enable_gradient: bool) {
        // 1. 借用競合を避けるため、一旦出力用のバッファをローカルで作成
        let mut output_buffer = Vec::new();

        // 2. 必要なデータ(segments)だけをイテレート
        for segment in &self.segments {
            let n = segment.points.len();
            if n < 2 {
                continue;
            }

            for i in 0..n - 1 {
                let p0 = &segment.points[i];
                let p1 = &segment.points[i + 1];

                let half_w0 = p0.width / 2.0;
                let half_w1 = p1.width / 2.0;

                let v = normalize(p1.x - p0.x, p1.y - p0.y);
                let n0 = (-v.1 * half_w0, v.0 * half_w0);
                let n1 = (-v.1 * half_w1, v.0 * half_w1);

                // --- Cap (始点) ---
                if i == 0 {
                    Self::static_push_cap(&mut output_buffer, p0, v, n0, cap, true);
                }

                // --- Main Segment ---
                let colors = if enable_gradient {
                    [
                        p0.color.to_raw(),
                        p0.color.to_raw(),
                        p1.color.to_raw(),
                        p1.color.to_raw(),
                    ]
                } else {
                    let c = self.pen.color.to_raw();
                    [c, c, c, c]
                };

                Self::static_push_quad(
                    &mut output_buffer,
                    (p0.x + n0.0, p0.y + n0.1),
                    (p0.x - n0.0, p0.y - n0.1),
                    (p1.x - n1.0, p1.y - n1.1),
                    (p1.x + n1.0, p1.y + n1.1),
                    colors,
                );

                // --- Join ---
                if i < n - 2 {
                    let p2 = &segment.points[i + 2];
                    let v_next = normalize(p2.x - p1.x, p2.y - p1.y);
                    Self::static_push_join(&mut output_buffer, p1, v, v_next, join);
                }

                // --- Cap (終点) ---
                if i == n - 2 {
                    Self::static_push_cap(&mut output_buffer, p1, v, n1, cap, false);
                }
            }
        }

        // 3. 最後にメインバッファに結合
        self.buffer = output_buffer;
    }

    // self を借用しないスタティックメソッドとして分離
    fn static_push_quad(
        buffer: &mut Vec<f32>,
        q0: (f64, f64),
        q1: (f64, f64),
        q2: (f64, f64),
        q3: (f64, f64),
        colors: [i32; 4],
    ) {
        buffer.push(4.0); // Quad type
        let pts = [q0, q1, q2, q3];
        for pt in &pts {
            buffer.push(pt.0 as f32);
            buffer.push(pt.1 as f32);
        }
        for c in &colors {
            buffer.push(f32::from_bits(*c as u32));
        }
    }

    fn static_push_join(
        buffer: &mut Vec<f32>,
        p: &PointData,
        v1: (f64, f64),
        v2: (f64, f64),
        join: LineJoin,
    ) {
        let half_w = p.width / 2.0;
        let c = p.color.to_raw();
        let color_arr = [c, c, c, c];
        let cross = v1.0 * v2.1 - v1.1 * v2.0;
        if cross.abs() < 1e-6 {
            return;
        }

        let n1 = (-v1.1 * half_w, v1.0 * half_w);
        let n2 = (-v2.1 * half_w, v2.0 * half_w);

        match join {
            LineJoin::Bevel => {
                if cross > 0.0 {
                    Self::static_push_quad(
                        buffer,
                        (p.x, p.y),
                        (p.x - n1.0, p.y - n1.1),
                        (p.x - n2.0, p.y - n2.1),
                        (p.x, p.y),
                        color_arr,
                    );
                } else {
                    Self::static_push_quad(
                        buffer,
                        (p.x, p.y),
                        (p.x + n1.0, p.y + n1.1),
                        (p.x + n2.0, p.y + n2.1),
                        (p.x, p.y),
                        color_arr,
                    );
                }
            }
            LineJoin::Miter => {
                let miter_v = normalize(v2.0 - v1.0, v2.1 - v1.1);
                let cos_theta = v1.0 * v2.0 + v1.1 * v2.1;
                let miter_len = half_w / ((1.0 + cos_theta) / 2.0).sqrt();

                if miter_len > half_w * 10.0 {
                    Self::static_push_join(buffer, p, v1, v2, LineJoin::Bevel);
                    return;
                }

                let nx = if cross > 0.0 {
                    -miter_v.0 * miter_len
                } else {
                    miter_v.0 * miter_len
                };
                let ny = if cross > 0.0 {
                    -miter_v.1 * miter_len
                } else {
                    miter_v.1 * miter_len
                };

                if cross > 0.0 {
                    Self::static_push_quad(
                        buffer,
                        (p.x, p.y),
                        (p.x - n1.0, p.y - n1.1),
                        (p.x + nx, p.y + ny),
                        (p.x - n2.0, p.y - n2.1),
                        color_arr,
                    );
                } else {
                    Self::static_push_quad(
                        buffer,
                        (p.x, p.y),
                        (p.x + n1.0, p.y + n1.1),
                        (p.x + nx, p.y + ny),
                        (p.x + n2.0, p.y + n2.1),
                        color_arr,
                    );
                }
            }
            LineJoin::Round => {
                let start_ang = if cross > 0.0 {
                    (-v1.1).atan2(-v1.0)
                } else {
                    v1.1.atan2(v1.0)
                };
                let end_ang = if cross > 0.0 {
                    (-v2.1).atan2(-v2.0)
                } else {
                    v2.1.atan2(v2.0)
                };
                let mut diff = end_ang - start_ang;
                while diff > PI {
                    diff -= 2.0 * PI;
                }
                while diff < -PI {
                    diff += 2.0 * PI;
                }
                Self::static_push_arc_fan(buffer, (p.x, p.y), start_ang, diff, half_w, color_arr)
            }
            LineJoin::MiterClip => {
                let miter_v = normalize(v2.0 - v1.0, v2.1 - v1.1);
                let cos_theta = v1.0 * v2.0 + v1.1 * v2.1;

                // マイター頂点までの距離
                let miter_len = half_w / ((1.0 + cos_theta) / 2.0).sqrt();

                // 制限距離（ここでは一般的な 10.0 倍としていますが、必要なら引数で調整）
                let limit = half_w * 10.0;

                if miter_len <= limit {
                    // 制限内なら通常の Miter と同じ
                    Self::static_push_join(buffer, p, v1, v2, LineJoin::Miter);
                } else {
                    // 制限を超える場合：先端をフラットに切り落とす
                    // 1. 本来のマイター方向の単位ベクトルを計算
                    let nx_unit = if cross > 0.0 { -miter_v.0 } else { miter_v.0 };
                    let ny_unit = if cross > 0.0 { -miter_v.1 } else { miter_v.1 };

                    // 2. 切り落とし面（Clip面）の左右の端点を計算
                    // マイターの軸に対して垂直な方向に幅を広げる
                    let clip_v = (-ny_unit, nx_unit);
                    // 切り落とし面の幅は、ジオメトリ計算上、交点付近の広がりを考慮
                    let clip_w = half_w * (1.0 - (limit / miter_len)); // 簡易的な幅補正

                    let cx = nx_unit * limit;
                    let cy = ny_unit * limit;

                    // 3. 2つの Quad を使って「台形」状の接合部を作る
                    // [中心, 前法線点, Clip左, Clip右] と [中心, Clip右, 次法線点, 中心] のイメージ
                    if cross > 0.0 {
                        // 左曲がり：右側を Clip
                        let c1 = (p.x + cx + clip_v.0 * clip_w, p.y + cy + clip_v.1 * clip_w);
                        let c2 = (p.x + cx - clip_v.0 * clip_w, p.y + cy - clip_v.1 * clip_w);

                        // Quad 1: 前の線分から Clip 面へ
                        Self::static_push_quad(
                            buffer,
                            (p.x, p.y),
                            (p.x - n1.0, p.y - n1.1),
                            c1,
                            c2,
                            color_arr,
                        );
                        // Quad 2: Clip 面から次の線分へ
                        Self::static_push_quad(
                            buffer,
                            (p.x, p.y),
                            c2,
                            (p.x - n2.0, p.y - n2.1),
                            (p.x, p.y),
                            color_arr,
                        );
                    } else {
                        // 右曲がり：左側を Clip
                        let c1 = (p.x + cx + clip_v.0 * clip_w, p.y + cy + clip_v.1 * clip_w);
                        let c2 = (p.x + cx - clip_v.0 * clip_w, p.y + cy - clip_v.1 * clip_w);

                        Self::static_push_quad(
                            buffer,
                            (p.x, p.y),
                            (p.x + n1.0, p.y + n1.1),
                            c1,
                            c2,
                            color_arr,
                        );
                        Self::static_push_quad(
                            buffer,
                            (p.x, p.y),
                            c2,
                            (p.x + n2.0, p.y + n2.1),
                            (p.x, p.y),
                            color_arr,
                        );
                    }
                }
            }
        }
    }

    fn static_push_cap(
        buffer: &mut Vec<f32>,
        p: &PointData,
        v: (f64, f64),
        n: (f64, f64),
        cap: LineCap,
        is_start: bool,
    ) {
        let half_w = p.width / 2.0;
        let color_arr = [p.color.to_raw(); 4];
        let sign = if is_start { -1.0 } else { 1.0 };

        match cap {
            LineCap::Butt => {}
            LineCap::Square => {
                let ox = v.0 * half_w * sign;
                let oy = v.1 * half_w * sign;
                Self::static_push_quad(
                    buffer,
                    (p.x + n.0, p.y + n.1),
                    (p.x - n.0, p.y - n.1),
                    (p.x - n.0 + ox, p.y - n.1 + oy),
                    (p.x + n.0 + ox, p.y + n.1 + oy),
                    color_arr,
                );
            }
            LineCap::Round => {
                let base_ang = n.1.atan2(n.0);
                let diff = if is_start { PI } else { -PI };
                Self::static_push_arc_fan(
                    buffer,
                    (p.x, p.y),
                    base_ang + diff,
                    diff,
                    half_w,
                    color_arr,
                )
            }
        }
    }
    /// 中心点から円弧状に扇形のジオメトリ（複数のQuad）を生成する
    fn static_push_arc_fan(
        buffer: &mut Vec<f32>,
        center: (f64, f64),
        start_ang: f64,
        diff: f64,
        radius: f64,
        color_arr: [i32; 4],
    ) {
        let steps = 8; // 分割数は必要に応じて調整
        for j in 0..steps {
            let a0 = start_ang + diff * (j as f64 / steps as f64);
            let a1 = start_ang + diff * ((j + 1) as f64 / steps as f64);

            // KotlinのQuad形式に合わせるため、中心点を2つ、円周上の2点を2つ送る
            Self::static_push_quad(
                buffer,
                (center.0, center.1),
                (center.0 + a0.cos() * radius, center.1 + a0.sin() * radius),
                (center.0 + a1.cos() * radius, center.1 + a1.sin() * radius),
                (center.0, center.1),
                color_arr,
            );
        }
    }
}
// lyon の結果を Kotlin 用のバッファ形式に変換するアダプタ
// 頂点情報を一時的に保持する構造体
struct VertexInfo {
    position: [f32; 2],
    color: i32,
}

struct FillOutput<'a> {
    buffer: &'a mut Vec<f32>,
    // テッセレーション中に生成された頂点を保持
    vertices: Vec<VertexInfo>,
    // 補間計算のために現在のパスのセグメント情報を参照（オプション）
    current_pen_color: i32,
}

impl<'a> GeometryBuilder for FillOutput<'a> {
    fn begin_geometry(&mut self) {
        self.vertices.clear();
    }

    fn end_geometry(&mut self) {
        // 必要ならここで後処理
    }

    fn add_triangle(&mut self, a: VertexId, b: VertexId, c: VertexId) {
        // 1. Type identifier (Triangle = 3.0f)
        self.buffer.push(3.0);

        let ids = [a, b, c];

        // 2. 頂点座標を順番に push (x0, y0, x1, y1, x2, y2)
        for &id in &ids {
            let v = &self.vertices[id.0 as usize];
            self.buffer.push(v.position[0]);
            self.buffer.push(v.position[1]);
        }

        // 3. 頂点色を順番に push (c0, c1, c2)
        // Kotlin 側で JAVA_INT として読み込むため、ビットパターンを保持したまま f32 として格納
        for &id in &ids {
            let v = &self.vertices[id.0 as usize];
            self.buffer.push(f32::from_bits(v.color as u32));
        }
    }
    fn abort_geometry(&mut self) {
        self.vertices.clear();
    }
}

impl<'a> FillGeometryBuilder for FillOutput<'a> {
    fn add_fill_vertex(&mut self, vertex: FillVertex) -> Result<VertexId, GeometryBuilderError> {
        let pos = vertex.position();

        // 頂点ごとの色を決定
        // Fillの場合、複雑なグラデーション（Linear/Radial）は座標から計算する必要があります。
        // ここでは一旦、現在のペンの色を使用します。
        let color = self.current_pen_color;

        self.vertices.push(VertexInfo {
            position: [pos.x, pos.y],
            color,
        });

        // 頂点のインデックスを返す
        Ok(VertexId(self.vertices.len() as u32 - 1))
    }
}

fn normalize(x: f64, y: f64) -> (f64, f64) {
    let len = (x * x + y * y).sqrt();
    if len < 1e-9 {
        (0.0, 0.0)
    } else {
        (x / len, y / len)
    }
}
