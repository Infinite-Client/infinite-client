use super::types::{Axis, AxisDirection, BlockPos, Direction, Line, Quad};
use glam::DVec3;
use rayon::prelude::*;
use rustc_hash::{FxHashMap, FxHashSet};
use std::collections::HashSet;
use xross_core::{XrossClass, xross_methods};

#[derive(XrossClass, Default)]
pub struct BlockMeshGenerator {
    blocks: FxHashMap<u64, i32>,
    lines: Vec<Line>,
    quads: Vec<Quad>,
}

type Ung = HashSet<((i32, i32, i32), (i32, i32, i32))>;

#[xross_methods]
impl BlockMeshGenerator {
    #[xross_new(panicable)]
    pub fn new() -> Self {
        Self::default()
    }

    #[xross_method(critical)]
    pub fn clear(&mut self) {
        self.blocks.clear();
        self.lines.clear();
        self.quads.clear();
    }

    pub fn get_quads(&self) -> Vec<Quad> {
        self.quads.clone()
    }

    pub fn get_lines(&self) -> Vec<Line> {
        self.lines.clone()
    }

    #[xross_method(critical)]
    pub fn add_block(&mut self, x: i32, y: i32, z: i32, color: i32) {
        let pos = BlockPos::new(x, y, z);
        self.blocks.insert(pos.pack(), color);
    }

    #[xross_method(panicable)]
    #[allow(clippy::too_many_arguments)]
    pub fn scan_section_caves(
        &mut self,
        start_x: i32,
        start_y: i32,
        start_z: i32,
        ids: &[i32],
        sky_lights: &[i32],
        target_id: i32,
        color: i32,
        max_sky_light: i32,
    ) {
        if ids.len() < 4096 || sky_lights.len() < 4096 {
            return;
        }

        for y in 0..16 {
            for z in 0..16 {
                for x in 0..16 {
                    let idx = (y << 8) | (z << 4) | x;
                    let id = ids[idx as usize];
                    let sky = sky_lights[idx as usize];

                    if id == target_id && sky <= max_sky_light {
                        self.add_block(start_x + x, start_y + y, start_z + z, color);
                    }
                }
            }
        }
    }

    #[xross_method(panicable)]
    pub fn generate(&mut self) {
        self.lines.clear();
        self.quads.clear();

        if self.blocks.is_empty() {
            return;
        }

        let directions = Direction::all();
        let face_results: Vec<(Vec<Quad>, Vec<Line>)> = directions
            .par_iter()
            .map(|&dir| {
                let mut quads = Vec::new();
                let mut face_positions: FxHashMap<i32, FxHashMap<i32, FxHashSet<(i32, i32)>>> =
                    FxHashMap::default();

                let (nx, ny, nz) = {
                    let (dx, dy, dz) = dir.step();
                    (dx as f32, dy as f32, dz as f32)
                };

                for (&packed_pos, &color) in &self.blocks {
                    let pos = BlockPos::unpack(packed_pos);
                    let neighbor = pos.relative(dir);
                    if self.blocks.get(&neighbor.pack()) != Some(&color) {
                        let (plane, u, v) = self.get_coords(pos, dir);
                        face_positions
                            .entry(color)
                            .or_default()
                            .entry(plane)
                            .or_default()
                            .insert((u, v));
                    }
                }

                for (color, planes) in face_positions {
                    for (plane, mut cells) in planes {
                        self.greedy_mesh_2d(
                            &mut quads,
                            plane,
                            color,
                            &mut cells,
                            dir,
                            (nx, ny, nz),
                        );
                    }
                }

                (quads, Vec::new())
            })
            .collect();

        for (quads, _) in face_results {
            self.quads.extend(quads);
        }

        let mut raw_lines = Vec::new();
        let mut unique_lines = HashSet::new();

        for (&packed_pos, &color) in &self.blocks {
            let pos = BlockPos::unpack(packed_pos);
            self.process_edges_for_pos(&mut raw_lines, &mut unique_lines, pos, color);
        }

        self.lines = self.combine_lines(raw_lines);
    }
}

impl BlockMeshGenerator {
    #[inline(always)]
    fn get_coords(&self, pos: BlockPos, dir: Direction) -> (i32, i32, i32) {
        match dir.axis() {
            Axis::X => (pos.x, pos.z, pos.y),
            Axis::Y => (pos.y, pos.x, pos.z),
            Axis::Z => (pos.z, pos.x, pos.y),
        }
    }

    fn greedy_mesh_2d(
        &self,
        quads: &mut Vec<Quad>,
        plane: i32,
        color: i32,
        cells: &mut FxHashSet<(i32, i32)>,
        dir: Direction,
        normal: (f32, f32, f32),
    ) {
        while let Some(&start) = cells.iter().next() {
            let mut width = 1;
            while cells.contains(&(start.0 + width, start.1)) {
                width += 1;
            }

            let mut height = 1;
            loop {
                let mut all_row = true;
                for w in 0..width {
                    if !cells.contains(&(start.0 + w, start.1 + height)) {
                        all_row = false;
                        break;
                    }
                }
                if all_row {
                    height += 1;
                } else {
                    break;
                }
            }

            quads.push(self.build_quad(plane, start.0, start.1, width, height, color, dir, normal));

            for w in 0..width {
                for h in 0..height {
                    cells.remove(&(start.0 + w, start.1 + h));
                }
            }
        }
    }

    #[allow(clippy::too_many_arguments)]
    fn build_quad(
        &self,
        p: i32,
        u: i32,
        v: i32,
        w: i32,
        h: i32,
        color: i32,
        dir: Direction,
        normal: (f32, f32, f32),
    ) -> Quad {
        let offset = if dir.axis_direction() == AxisDirection::Positive {
            1.0
        } else {
            0.0
        };
        let plane = p as f64 + offset;

        let (v1, v2, v3, v4) = match dir.axis() {
            Axis::X => {
                if dir.axis_direction() == AxisDirection::Positive {
                    (
                        DVec3::new(plane, v as f64, u as f64),
                        DVec3::new(plane, (v + h) as f64, u as f64),
                        DVec3::new(plane, (v + h) as f64, (u + w) as f64),
                        DVec3::new(plane, v as f64, (u + w) as f64),
                    )
                } else {
                    (
                        DVec3::new(plane, v as f64, u as f64),
                        DVec3::new(plane, v as f64, (u + w) as f64),
                        DVec3::new(plane, (v + h) as f64, (u + w) as f64),
                        DVec3::new(plane, (v + h) as f64, u as f64),
                    )
                }
            }
            Axis::Y => {
                if dir.axis_direction() == AxisDirection::Positive {
                    (
                        DVec3::new(u as f64, plane, v as f64),
                        DVec3::new(u as f64, plane, (v + h) as f64),
                        DVec3::new((u + w) as f64, plane, (v + h) as f64),
                        DVec3::new((u + w) as f64, plane, v as f64),
                    )
                } else {
                    (
                        DVec3::new(u as f64, plane, v as f64),
                        DVec3::new((u + w) as f64, plane, v as f64),
                        DVec3::new((u + w) as f64, plane, (v + h) as f64),
                        DVec3::new(u as f64, plane, (v + h) as f64),
                    )
                }
            }
            Axis::Z => {
                if dir.axis_direction() == AxisDirection::Positive {
                    (
                        DVec3::new(u as f64, v as f64, plane),
                        DVec3::new((u + w) as f64, v as f64, plane),
                        DVec3::new((u + w) as f64, (v + h) as f64, plane),
                        DVec3::new(u as f64, (v + h) as f64, plane),
                    )
                } else {
                    (
                        DVec3::new(u as f64, v as f64, plane),
                        DVec3::new(u as f64, (v + h) as f64, plane),
                        DVec3::new((u + w) as f64, (v + h) as f64, plane),
                        DVec3::new((u + w) as f64, v as f64, plane),
                    )
                }
            }
        };

        Quad {
            v1,
            v2,
            v3,
            v4,
            color,
            normal,
        }
    }

    fn process_edges_for_pos(
        &self,
        ls: &mut Vec<InternalLine>,
        unq: &mut Ung,
        pos: BlockPos,
        color: i32,
    ) {
        let x = pos.x;
        let y = pos.y;
        let z = pos.z;

        let mut edge_check =
            |x1: i32, y1: i32, z1: i32, x2: i32, y2: i32, z2: i32, n1: BlockPos, n2: BlockPos| {
                let c1 = self.blocks.get(&n1.pack()).copied();
                let c2 = self.blocks.get(&n2.pack()).copied();
                if c1 != Some(color) || c2 != Some(color) {
                    let s = (x1, y1, z1);
                    let e = (x2, y2, z2);
                    let pair = if s < e { (s, e) } else { (e, s) };
                    if unq.insert(pair) {
                        let edge_color = if let Some(c) = c1 {
                            if c != color {
                                interpolate(color, c)
                            } else if let Some(cc) = c2 {
                                if cc != color {
                                    interpolate(color, cc)
                                } else {
                                    color
                                }
                            } else {
                                color
                            }
                        } else if let Some(cc) = c2 {
                            if cc != color {
                                interpolate(color, cc)
                            } else {
                                color
                            }
                        } else {
                            color
                        };

                        ls.push(InternalLine {
                            start: s,
                            end: e,
                            color: edge_color,
                        });
                    }
                }
            };

        // Down/North
        edge_check(
            x,
            y,
            z,
            x + 1,
            y,
            z,
            pos.relative(Direction::Down),
            pos.relative(Direction::North),
        );
        edge_check(
            x,
            y + 1,
            z,
            x + 1,
            y + 1,
            z,
            pos.relative(Direction::Up),
            pos.relative(Direction::North),
        );
        edge_check(
            x,
            y,
            z + 1,
            x + 1,
            y,
            z + 1,
            pos.relative(Direction::Down),
            pos.relative(Direction::South),
        );
        edge_check(
            x,
            y + 1,
            z + 1,
            x + 1,
            y + 1,
            z + 1,
            pos.relative(Direction::Up),
            pos.relative(Direction::South),
        );

        edge_check(
            x,
            y,
            z,
            x,
            y + 1,
            z,
            pos.relative(Direction::West),
            pos.relative(Direction::North),
        );
        edge_check(
            x + 1,
            y,
            z,
            x + 1,
            y + 1,
            z,
            pos.relative(Direction::East),
            pos.relative(Direction::North),
        );
        edge_check(
            x,
            y,
            z + 1,
            x,
            y + 1,
            z + 1,
            pos.relative(Direction::West),
            pos.relative(Direction::South),
        );
        edge_check(
            x + 1,
            y,
            z + 1,
            x + 1,
            y + 1,
            z + 1,
            pos.relative(Direction::East),
            pos.relative(Direction::South),
        );

        edge_check(
            x,
            y,
            z,
            x,
            y,
            z + 1,
            pos.relative(Direction::West),
            pos.relative(Direction::Down),
        );
        edge_check(
            x + 1,
            y,
            z,
            x + 1,
            y,
            z + 1,
            pos.relative(Direction::East),
            pos.relative(Direction::Down),
        );
        edge_check(
            x,
            y + 1,
            z,
            x,
            y + 1,
            z + 1,
            pos.relative(Direction::West),
            pos.relative(Direction::Up),
        );
        edge_check(
            x + 1,
            y + 1,
            z,
            x + 1,
            y + 1,
            z + 1,
            pos.relative(Direction::East),
            pos.relative(Direction::Up),
        );
    }

    fn combine_lines(&self, lines: Vec<InternalLine>) -> Vec<Line> {
        let mut result = Vec::new();
        if lines.is_empty() {
            return result;
        }

        for axis_idx in 0..3 {
            let mut grouped: FxHashMap<(i32, i32, i32), Vec<InternalLine>> = FxHashMap::default();
            for l in &lines {
                let is_axis = match axis_idx {
                    0 => l.start.1 == l.end.1 && l.start.2 == l.end.2,
                    1 => l.start.0 == l.end.0 && l.start.2 == l.end.2,
                    2 => l.start.0 == l.end.0 && l.start.1 == l.end.1,
                    _ => false,
                };
                if is_axis {
                    let key = match axis_idx {
                        0 => (l.start.1, l.start.2, l.color),
                        1 => (l.start.0, l.start.2, l.color),
                        2 => (l.start.0, l.start.1, l.color),
                        _ => (0, 0, 0),
                    };
                    grouped.entry(key).or_default().push(*l);
                }
            }

            for (_, mut list) in grouped {
                list.sort_by_key(|l| match axis_idx {
                    0 => l.start.0,
                    1 => l.start.1,
                    2 => l.start.2,
                    _ => 0,
                });

                let mut cur_s = list[0].start;
                let mut cur_e = list[0].end;
                let mut cur_c = list[0].color;
                list.iter().for_each(|l| {
                    if l.start == cur_e && l.color == cur_c {
                        cur_e = l.end;
                    } else {
                        result.push(Line {
                            start: DVec3::new(cur_s.0 as f64, cur_s.1 as f64, cur_s.2 as f64),
                            end: DVec3::new(cur_e.0 as f64, cur_e.1 as f64, cur_e.2 as f64),
                            color: cur_c,
                        });
                        cur_s = l.start;
                        cur_e = l.end;
                        cur_c = l.color;
                    }
                });

                result.push(Line {
                    start: DVec3::new(cur_s.0 as f64, cur_s.1 as f64, cur_s.2 as f64),
                    end: DVec3::new(cur_e.0 as f64, cur_e.1 as f64, cur_e.2 as f64),
                    color: cur_c,
                });
            }
        }
        result
    }
}

#[derive(Clone, Copy)]
struct InternalLine {
    start: (i32, i32, i32),
    end: (i32, i32, i32),
    color: i32,
}

#[inline(always)]
fn interpolate(c1: i32, c2: i32) -> i32 {
    let a1 = (c1 >> 24) & 0xFF;
    let r1 = (c1 >> 16) & 0xFF;
    let g1 = (c1 >> 8) & 0xFF;
    let b1 = c1 & 0xFF;

    let a2 = (c2 >> 24) & 0xFF;
    let r2 = (c2 >> 16) & 0xFF;
    let g2 = (c2 >> 8) & 0xFF;
    let b2 = c2 & 0xFF;

    let a = (a1 + a2) / 2;
    let r = (r1 + r2) / 2;
    let g = (g1 + g2) / 2;
    let b = (b1 + b2) / 2;

    (a << 24) | (r << 16) | (g << 8) | b
}
