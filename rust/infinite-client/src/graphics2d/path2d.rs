pub mod ffi;

use lyon::path::{LineCap, LineJoin};

#[derive(Default)]
pub struct Path2D {
    segments: Vec<SegmentData>,
    pub pen: Pen,
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
}

impl Default for Pen {
    fn default() -> Self {
        Self {
            color: Color::default(),
            width: 0.0,
            line_cap: LineCap::Butt,
            line_join: LineJoin::Miter,
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
}
