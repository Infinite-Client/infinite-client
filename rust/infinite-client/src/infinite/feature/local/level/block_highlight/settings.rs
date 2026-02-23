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
    pub fn set_scan_range(&self, val: i32) {
        self.scan_range.store(val, Ordering::Relaxed);
    }

    pub fn set_render_range(&self, val: i32) {
        self.render_range.store(val, Ordering::Relaxed);
    }

    pub fn set_render_style(&self, style: RenderStyle) {
        self.render_style.store(style, Ordering::Relaxed);
    }

    pub fn set_max_draw_count(&self, val: i32) {
        self.max_draw_count.store(val, Ordering::Relaxed);
    }

    pub fn set_line_width(&self, val: f32) {
        self.line_width.store(val, Ordering::Relaxed);
    }

    pub fn set_view_focus(&self, focus: ViewFocus) {
        self.view_focus.store(focus, Ordering::Relaxed);
    }

    pub fn set_animation(&self, anim: Animation) {
        self.animation.store(anim, Ordering::Relaxed);
    }

    pub fn set_max_y(&self, val: i32) {
        self.max_y.store(val, Ordering::Relaxed);
    }

    pub fn set_check_surroundings(&self, enabled: bool) {
        self.check_surroundings.store(enabled, Ordering::Relaxed);
    }

    pub fn set_sky_light_threshold(&self, val: i32) {
        self.sky_light_threshold.store(val, Ordering::Relaxed);
    }

    pub fn set_player_exclusion_radius(&self, val: i32) {
        self.player_exclusion_radius.store(val, Ordering::Relaxed);
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

pub trait SettingsSetter {
    // リスト更新
    fn update_highlight_list(buff: &[u64]);

    // 数値・基本設定 (i32)
    fn set_scan_range(val: i32);
    fn set_render_range(val: i32);
    fn set_max_draw_count(val: i32);
    fn set_max_y(val: i32);
    fn set_sky_light_threshold(val: i32);
    fn set_player_exclusion_radius(val: i32);

    // 論理値 (bool)
    fn set_check_surroundings(enabled: bool);

    // 型変換が必要なもの (u32経由)
    fn set_line_width_bits(bits: u32);
    fn set_render_style(ordinal: u32);
    fn set_view_focus(ordinal: u32);
    fn set_animation(ordinal: u32);
}
