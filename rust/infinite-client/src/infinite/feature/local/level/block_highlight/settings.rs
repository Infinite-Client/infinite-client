use crate::infinite::property::BlockAndColor;
use crate::utils::atomic::AtomicF32;
use atomic_enum::atomic_enum;
use parking_lot::RwLock;
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use xross_core::XrossClass;

#[derive(XrossClass)]
#[xross(unclonable)]
#[xross_package("features.local.level.highlight.block")]
pub struct Settings {
    pub blocks_to_highlight: RwLock<Vec<BlockAndColor>>,
    pub scan_range: AtomicI32,
    pub render_range: AtomicI32,
    pub render_style: AtomicRenderStyle,
    pub max_draw_count: AtomicI32,
    pub line_width: AtomicF32,
    pub view_focus: AtomicViewFocus,
    pub animation: AtomicAnimation,
    pub max_y: AtomicI32,
    pub check_surroundings: AtomicBool,
    pub sky_light_threshold: AtomicI32,
    pub player_exclusion_radius: AtomicI32,
}
impl Default for Settings {
    fn default() -> Self {
        let blocks_to_highlight: RwLock<Vec<BlockAndColor>> = Default::default();
        let scan_range = 5.into();
        let render_range = 10.into();
        let render_style = RenderStyle::Lines.into();
        let max_draw_count = 1000.into();
        let line_width = 1.0.into();
        let view_focus = ViewFocus::Balanced.into();
        let animation = Animation::Pulse.into();
        let max_y = 64.into();
        let check_surroundings = true.into();
        let sky_light_threshold = 10.into();
        let player_exclusion_radius = 10.into();
        Self {
            blocks_to_highlight,
            scan_range,
            render_range,
            render_style,
            max_draw_count,
            line_width,
            view_focus,
            animation,
            max_y,
            check_surroundings,
            sky_light_threshold,
            player_exclusion_radius,
        }
    }
}
impl Settings {
    pub fn apply_b4_setting(&self, idx: u8, num: u32) {
        match idx {
            // 0: blocks_to_highlight は Vec なので別のメソッド（前述のもの）で扱うのが一般的
            1 => self.scan_range.store(num as i32, Ordering::Relaxed),
            2 => self.render_range.store(num as i32, Ordering::Relaxed),
            3 => self
                .render_style
                .store(RenderStyle::from_u32(num), Ordering::Relaxed),
            4 => self.max_draw_count.store(num as i32, Ordering::Relaxed),
            5 => self
                .line_width
                .store(f32::from_bits(num), Ordering::Relaxed),
            6 => self
                .view_focus
                .store(ViewFocus::from_u32(num), Ordering::Relaxed),
            7 => self
                .animation
                .store(Animation::from_u32(num), Ordering::Relaxed),
            8 => self.max_y.store(num as i32, Ordering::Relaxed),
            9 => self.check_surroundings.store(num != 0, Ordering::Relaxed),
            10 => self
                .sky_light_threshold
                .store(num as i32, Ordering::Relaxed),
            11 => self
                .player_exclusion_radius
                .store(num as i32, Ordering::Relaxed),
            _ => { /* 未定義のインデックス */ }
        }
    }
}
#[atomic_enum]
#[derive(PartialEq, Default)]
pub enum RenderStyle {
    #[default]
    Lines = 0,
    Faces = 1,
    Both = 2,
}
impl RenderStyle {
    pub fn from_u32(n: u32) -> Self {
        match n {
            0 => RenderStyle::Lines,
            1 => RenderStyle::Faces,
            2 => RenderStyle::Both,
            _ => RenderStyle::default(),
        }
    }
}

#[atomic_enum]
#[derive(PartialEq, Default)]
pub enum ViewFocus {
    None = 0,
    #[default]
    Balanced = 1,
    Strict = 2,
}
impl ViewFocus {
    pub fn from_u32(n: u32) -> Self {
        match n {
            0 => ViewFocus::None,
            1 => ViewFocus::Balanced,
            2 => ViewFocus::Strict,
            _ => ViewFocus::default(),
        }
    }
}

#[atomic_enum]
#[derive(PartialEq, Default)]
pub enum Animation {
    None = 0,
    #[default]
    Pulse = 1,
    FadeIn = 2,
}

impl Animation {
    pub fn from_u32(n: u32) -> Self {
        match n {
            0 => Animation::None,
            1 => Animation::Pulse,
            2 => Animation::FadeIn,
            _ => Animation::default(),
        }
    }
}
