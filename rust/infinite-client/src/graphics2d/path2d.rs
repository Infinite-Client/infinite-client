use lyon::lyon_tessellation::{FillVertex, GeometryBuilderError, VertexId};
use lyon::math::point;
use lyon::path::{FillRule, LineCap, LineJoin, Path};
use lyon::tessellation::{FillGeometryBuilder, FillOptions, FillTessellator, GeometryBuilder};
use std::f64::consts::PI;
use xross_core::{XrossClass, xross_methods};

#[derive(XrossClass, Default, Clone)]
#[xross(clonable)]
pub struct Path2D {
    segments: Vec<SegmentData>,
    // #[xross_field] を削除。メソッド経由でのみアクセスさせる。
    pub pen: Pen,
    buffer: Vec<f32>,
}

#[derive(Clone)]
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

#[derive(XrossClass, Clone, Copy, Default, Debug)]
pub struct PointData {
    #[xross_field]
    pub x: f64,
    #[xross_field]
    pub y: f64,
    #[xross_field]
    pub color: Color,
    #[xross_field]
    pub width: f64,
    #[xross_field]
    pub line_cap: XrossLineCap,
    #[xross_field]
    pub line_join: XrossLineJoin,
}

#[derive(Copy, Clone, Debug, PartialEq, XrossClass)]
pub struct Color {
    #[xross_field]
    pub r: f32,
    #[xross_field]
    pub g: f32,
    #[xross_field]
    pub b: f32,
    #[xross_field]
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

#[derive(Copy, Clone, Debug, PartialEq, XrossClass)]
pub enum XrossLineCap {
    Butt,
    Square,
    Round,
}

impl From<LineCap> for XrossLineCap {
    fn from(cap: LineCap) -> Self {
        match cap {
            LineCap::Butt => Self::Butt,
            LineCap::Square => Self::Square,
            LineCap::Round => Self::Round,
        }
    }
}

impl From<XrossLineCap> for LineCap {
    fn from(cap: XrossLineCap) -> Self {
        match cap {
            XrossLineCap::Butt => Self::Butt,
            XrossLineCap::Square => Self::Square,
            XrossLineCap::Round => Self::Round,
        }
    }
}

impl Default for XrossLineCap {
    fn default() -> Self {
        Self::Butt
    }
}

#[derive(Copy, Clone, Debug, PartialEq, XrossClass)]
pub enum XrossLineJoin {
    Miter,
    MiterClip,
    Round,
    Bevel,
}

impl From<LineJoin> for XrossLineJoin {
    fn from(join: LineJoin) -> Self {
        match join {
            LineJoin::Miter => Self::Miter,
            LineJoin::MiterClip => Self::MiterClip,
            LineJoin::Round => Self::Round,
            LineJoin::Bevel => Self::Bevel,
        }
    }
}

impl From<XrossLineJoin> for LineJoin {
    fn from(join: XrossLineJoin) -> Self {
        match join {
            XrossLineJoin::Miter => Self::Miter,
            XrossLineJoin::MiterClip => Self::MiterClip,
            XrossLineJoin::Round => Self::Round,
            XrossLineJoin::Bevel => Self::Bevel,
        }
    }
}

impl Default for XrossLineJoin {
    fn default() -> Self {
        Self::Miter
    }
}

#[derive(XrossClass, Default, Clone, Copy, Debug)]
pub struct Pen {
    #[xross_field]
    pub color: Color,
    #[xross_field]
    pub width: f64,
    #[xross_field]
    pub line_cap: XrossLineCap,
    #[xross_field]
    pub line_join: XrossLineJoin,
    #[xross_field]
    pub is_gradient_enabled: bool,
}

impl Color {
    pub fn from_raw(raw: i32) -> Self {
        let u = raw as u32;
        Self {
            a: ((u >> 24) & 0xFF) as f32 / 255.0,
            r: ((u >> 16) & 0xFF) as f32 / 255.0,
            g: ((u >> 8) & 0xFF) as f32 / 255.0,
            b: (u & 0xFF) as f32 / 255.0,
        }
    }

    pub fn to_raw(&self) -> i32 {
        let a = (self.a.clamp(0.0, 1.0) * 255.0) as u32;
        let r = (self.r.clamp(0.0, 1.0) * 255.0) as u32;
        let g = (self.g.clamp(0.0, 1.0) * 255.0) as u32;
        let b = (self.b.clamp(0.0, 1.0) * 255.0) as u32;

        ((a << 24) | (r << 16) | (g << 8) | b) as i32
    }

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

#[derive(XrossClass)]
pub enum XrossFillRule {
    EvenOdd,
    NonZero,
}

#[xross_methods]
impl Path2D {
    #[xross_new]
    pub fn new() -> Self {
        Self::default()
    }

    #[xross_method]
    pub fn begin(&mut self) {
        self.segments.clear();
    }

    #[xross_method]
    pub fn set_pen(
        &mut self,
        width: f64,
        color_raw: i32,
        cap: XrossLineCap,
        join: XrossLineJoin,
        enable_gradient: bool,
    ) {
        self.pen.width = width;
        self.pen.color = Color::from_raw(color_raw);
        self.pen.line_cap = cap;
        self.pen.line_join = join;
        self.pen.is_gradient_enabled = enable_gradient;
    }

    #[xross_method]
    pub fn move_to(&mut self, x: f64, y: f64) {
        self.segments.push(SegmentData::default());
        let point = self.point(x, y);
        let current_segment = self.segments.last_mut().unwrap();
        current_segment.points.push(point);
    }

    fn point(&self, x: f64, y: f64) -> PointData {
        PointData {
            x,
            y,
            color: self.pen.color,
            line_cap: self.pen.line_cap,
            line_join: self.pen.line_join,
            width: self.pen.width,
        }
    }

    #[xross_method]
    pub fn line_to(&mut self, x: f64, y: f64) {
        let needs_new_segment = self.segments.last().map(|s| s.is_closed).unwrap_or(false);

        if needs_new_segment {
            let last_start_point = self
                .segments
                .last()
                .and_then(|s| s.points.first())
                .map(|p| (p.x, p.y));

            if let Some((sx, sy)) = last_start_point {
                self.move_to(sx, sy);
            } else {
                self.move_to(x, y);
            }
        }

        let point = self.point(x, y);
        self.push_point(point);
    }

    #[xross_method]
    pub fn close_path(&mut self) {
        if let Some(current_segment) = self.segments.last_mut() {
            if !current_segment.points.is_empty() {
                current_segment.is_closed = true;
            }
        }
    }

    fn push_point(&mut self, point: PointData) {
        if let Some(current_segment) = self.segments.last_mut() {
            current_segment.points.push(point);
        } else {
            let mut segment = SegmentData::default();
            segment.points.push(point);
            self.segments.push(segment);
        }
    }

    fn last_point(&self) -> Option<&PointData> {
        self.segments.last()?.points.last()
    }

    fn push_interpolated_point(
        &mut self,
        x: f64,
        y: f64,
        t: f32,
        start_color: Color,
        start_width: f64,
    ) {
        let mut p = self.point(x, y);
        if self.pen.is_gradient_enabled {
            p.color = start_color.mix(self.pen.color, t);
            p.width = start_width + (self.pen.width - start_width) * (t as f64);
        } else {
            p.width = self.pen.width;
        }
        self.push_point(p);
    }

    #[xross_method]
    pub fn bezier_curve_to(&mut self, cp1x: f64, cp1y: f64, cp2x: f64, cp2y: f64, x: f64, y: f64) {
        let last = self.last_point();
        let start_pos = last.map(|p| (p.x, p.y)).unwrap_or((0.0, 0.0));
        let start_color = last.map(|p| p.color).unwrap_or(self.pen.color);
        let start_width = last.map(|p| p.width).unwrap_or(self.pen.width);

        let steps = 20;
        for i in 1..=steps {
            let t = i as f64 / steps as f64;
            let inv_t = 1.0 - t;

            let px = inv_t.powi(3) * start_pos.0
                + 3.0 * inv_t.powi(2) * t * cp1x
                + 3.0 * inv_t * t.powi(2) * cp2x
                + t.powi(3) * x;
            let py = inv_t.powi(3) * start_pos.1
                + 3.0 * inv_t.powi(2) * t * cp1y
                + 3.0 * inv_t * t.powi(2) * cp2y
                + t.powi(3) * y;

            self.push_interpolated_point(px, py, t as f32, start_color, start_width);
        }
    }

    #[xross_method]
    pub fn quadratic_curve_to(&mut self, cpx: f64, cpy: f64, x: f64, y: f64) {
        let last = self.last_point();
        let start = last.map(|p| (p.x, p.y)).unwrap_or((0.0, 0.0));
        let start_color = last.map(|p| p.color).unwrap_or(self.pen.color);
        let start_width = last.map(|p| p.width).unwrap_or(self.pen.width);

        let steps = 15;
        for i in 1..=steps {
            let t = i as f64 / steps as f64;
            let inv_t = 1.0 - t;

            let px = inv_t.powi(2) * start.0 + 2.0 * inv_t * t * cpx + t.powi(2) * x;
            let py = inv_t.powi(2) * start.1 + 2.0 * inv_t * t * cpy + t.powi(2) * y;

            self.push_interpolated_point(px, py, t as f32, start_color, start_width);
        }
    }

    #[xross_method]
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

        let last = self.last_point();
        let start_width = last.map(|p| p.width).unwrap_or(self.pen.width);
        let steps = (diff.abs() / (PI / 18.0)).ceil().max(10.0) as i32;
        let start_color = last.map(|p| p.color).unwrap_or(self.pen.color);

        for i in 0..=steps {
            let t = i as f64 / steps as f64;
            let angle = start_angle + diff * t;
            let px = x + radius * angle.cos();
            let py = y + radius * angle.sin();
            self.push_point_in_sequence(i, px, py, t as f32, start_color, start_width);
        }
    }

    fn push_point_in_sequence(
        &mut self,
        i: i32,
        x: f64,
        y: f64,
        t: f32,
        start_color: Color,
        start_width: f64,
    ) {
        if i == 0 {
            if self.segments.is_empty() {
                self.move_to(x, y);
            } else {
                self.line_to(x, y);
            }
        } else {
            self.push_interpolated_point(x, y, t, start_color, start_width);
        }
    }

    #[xross_method]
    pub fn arc_to(&mut self, x1: f64, y1: f64, x2: f64, y2: f64, radius: f64) {
        let start = self.last_point().map(|p| (p.x, p.y)).unwrap_or((x1, y1));

        let v1 = (start.0 - x1, start.1 - y1);
        let v2 = (x2 - x1, y2 - y1);
        let len1 = (v1.0.powi(2) + v1.1.powi(2)).sqrt();
        let len2 = (v2.0.powi(2) + v2.1.powi(2)).sqrt();

        if len1 < 1e-6 || len2 < 1e-6 {
            self.line_to(x1, y1);
            return;
        }

        let cos_theta = (v1.0 * v2.0 + v1.1 * v2.1) / (len1 * len2);
        let theta = cos_theta.acos();
        let dist = radius / (theta / 2.0).tan();

        let p_start = (x1 + v1.0 / len1 * dist, y1 + v1.1 / len1 * dist);
        let p_end = (x1 + v2.0 / len2 * dist, y1 + v2.1 / len2 * dist);

        let v_bisect = (v1.0 / len1 + v2.0 / len2, v1.1 / len1 + v2.1 / len2);
        let len_b = (v_bisect.0.powi(2) + v_bisect.1.powi(2)).sqrt();
        let center_dist = radius / (theta / 2.0).sin();
        let center = (
            x1 + v_bisect.0 / len_b * center_dist,
            y1 + v_bisect.1 / len_b * center_dist,
        );

        let start_angle = (p_start.1 - center.1).atan2(p_start.0 - center.0);
        let end_angle = (p_end.1 - center.1).atan2(p_end.0 - center.0);

        self.line_to(p_start.0, p_start.1);

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

    #[xross_method]
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
        let last = self.last_point();
        let start_width = last.map(|p| p.width).unwrap_or(self.pen.width);

        for i in 0..=steps {
            let t = i as f64 / steps as f64;
            let angle = start_angle + diff * t;

            let lx = radius_x * angle.cos();
            let ly = radius_y * angle.sin();

            let px = x + lx * cos_rot - ly * sin_rot;
            let py = y + lx * sin_rot + ly * cos_rot;

            self.push_point_in_sequence(i, px, py, t as f32, start_color, start_width);
        }
    }

    #[xross_method]
    pub fn tessellate_fill(&mut self, rule: XrossFillRule) {
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
            vertices: Vec::new(),
            current_pen_color: self.pen.color.to_raw(),
        };
        let options = FillOptions::default().with_fill_rule(match rule {
            XrossFillRule::EvenOdd => FillRule::EvenOdd,
            XrossFillRule::NonZero => FillRule::NonZero,
        });

        let _ = tessellator.tessellate_path(&path, &options, &mut output);
    }

    #[xross_method]
    pub fn tessellate_stroke(&mut self) {
        let mut output_buffer = Vec::new();
        let cap = self.pen.line_cap.into();
        let join = self.pen.line_join.into();
        let enable_gradient = self.pen.is_gradient_enabled;

        for segment in &self.segments {
            let n = segment.points.len();
            if n < 2 {
                continue;
            }

            let is_closed = segment.is_closed;

            let mut dirs = Vec::with_capacity(n);
            for i in 0..n {
                let p0 = &segment.points[i];
                let p1 = &segment.points[(i + 1) % n];
                dirs.push(normalize(p1.x - p0.x, p1.y - p0.y));
            }

            let loop_limit = if is_closed { n } else { n - 1 };
            for i in 0..loop_limit {
                let i_curr = i % n;
                let i_next = (i + 1) % n;

                let p0 = &segment.points[i_curr];
                let p1 = &segment.points[i_next];
                let v_curr = dirs[i_curr];

                if v_curr == (0.0, 0.0) {
                    continue;
                }

                let half_w0 = p0.width / 2.0;
                let half_w1 = p1.width / 2.0;

                let n_curr = (-v_curr.1, v_curr.0);

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
                    (p0.x + n_curr.0 * half_w0, p0.y + n_curr.1 * half_w0),
                    (p0.x - n_curr.0 * half_w0, p0.y - n_curr.1 * half_w0),
                    (p1.x - n_curr.0 * half_w1, p1.y - n_curr.1 * half_w1),
                    (p1.x + n_curr.0 * half_w1, p1.y + n_curr.1 * half_w1),
                    colors,
                );

                if is_closed || i < n - 2 {
                    let v_next = dirs[i_next];
                    Self::static_push_join(&mut output_buffer, p1, v_curr, v_next, join);
                }

                if !is_closed {
                    if i == 0 {
                        Self::static_push_cap(
                            &mut output_buffer,
                            p0,
                            v_curr,
                            (n_curr.0 * half_w0, n_curr.1 * half_w0),
                            cap,
                            true,
                        );
                    }
                    if i == n - 2 {
                        Self::static_push_cap(
                            &mut output_buffer,
                            p1,
                            v_curr,
                            (n_curr.0 * half_w1, n_curr.1 * half_w1),
                            cap,
                            false,
                        );
                    }
                }
            }
        }
        self.buffer = output_buffer;
    }

    #[xross_method]
    pub fn get_buffer_ptr(&self) -> *const f32 {
        self.buffer.as_ptr()
    }

    #[xross_method]
    pub fn get_buffer_size(&self) -> usize {
        self.buffer.len()
    }
}

impl Path2D {
    fn static_push_quad(
        buffer: &mut Vec<f32>,
        q0: (f64, f64),
        q1: (f64, f64),
        q2: (f64, f64),
        q3: (f64, f64),
        colors: [i32; 4],
    ) {
        buffer.push(4.0);
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
                let miter_len = half_w / ((1.0 + cos_theta) / 2.0).sqrt();
                let limit = half_w * 10.0;

                if miter_len <= limit {
                    Self::static_push_join(buffer, p, v1, v2, LineJoin::Miter);
                } else {
                    let nx_unit = if cross > 0.0 { -miter_v.0 } else { miter_v.0 };
                    let ny_unit = if cross > 0.0 { -miter_v.1 } else { miter_v.1 };
                    let clip_v = (-ny_unit, nx_unit);
                    let clip_w = half_w * (1.0 - (limit / miter_len));
                    let cx = nx_unit * limit;
                    let cy = ny_unit * limit;

                    if cross > 0.0 {
                        let c1 = (p.x + cx + clip_v.0 * clip_w, p.y + cy + clip_v.1 * clip_w);
                        let c2 = (p.x + cx - clip_v.0 * clip_w, p.y + cy - clip_v.1 * clip_w);

                        Self::static_push_quad(
                            buffer,
                            (p.x, p.y),
                            (p.x - n1.0, p.y - n1.1),
                            c1,
                            c2,
                            color_arr,
                        );
                        Self::static_push_quad(
                            buffer,
                            (p.x, p.y),
                            c2,
                            (p.x - n2.0, p.y - n2.1),
                            (p.x, p.y),
                            color_arr,
                        );
                    } else {
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
                            (p.x - n2.0, p.y - n2.1),
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

    fn static_push_arc_fan(
        buffer: &mut Vec<f32>,
        center: (f64, f64),
        start_ang: f64,
        diff: f64,
        radius: f64,
        color_arr: [i32; 4],
    ) {
        let steps = 8;
        for j in 0..steps {
            let a0 = start_ang + diff * (j as f64 / steps as f64);
            let a1 = start_ang + diff * ((j + 1) as f64 / steps as f64);

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

struct VertexInfo {
    position: [f32; 2],
    color: i32,
}

struct FillOutput<'a> {
    buffer: &'a mut Vec<f32>,
    vertices: Vec<VertexInfo>,
    current_pen_color: i32,
}

impl<'a> GeometryBuilder for FillOutput<'a> {
    fn begin_geometry(&mut self) {
        self.vertices.clear();
    }

    fn end_geometry(&mut self) {}

    fn add_triangle(&mut self, a: VertexId, b: VertexId, c: VertexId) {
        self.buffer.push(3.0);
        let ids = [a, b, c];
        for &id in &ids {
            let v = &self.vertices[id.0 as usize];
            self.buffer.push(v.position[0]);
            self.buffer.push(v.position[1]);
        }
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
        let color = self.current_pen_color;

        self.vertices.push(VertexInfo {
            position: [pos.x, pos.y],
            color,
        });

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
