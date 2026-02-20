use xross_core::XrossClass;

/// Minecraft coordinates (X: 26bit, Y: 12bit, Z: 26bit) packed into u64.
/// This matches BlockPos::asLong() in modern Minecraft versions.
#[derive(XrossClass, Clone, Copy, PartialEq, Eq, Hash, Default, Debug)]
pub struct BlockPos {
    #[xross_field]
    pub x: i32,
    #[xross_field]
    pub y: i32,
    #[xross_field]
    pub z: i32,
}

impl BlockPos {
    #[inline(always)]
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        Self { x, y, z }
    }

    #[inline(always)]
    pub fn pack(&self) -> u64 {
        ((self.x as u64 & 0x3FFFFFF) << 38)
            | ((self.z as u64 & 0x3FFFFFF) << 12)
            | (self.y as u64 & 0xFFF)
    }

    #[inline(always)]
    pub fn unpack(val: u64) -> Self {
        let x = (val >> 38) as i64;
        let z = (val << 26 >> 38) as i64;
        let y = (val << 52 >> 52) as i64;

        // Sign extend from 26bit for X, Z and 12bit for Y
        let x = if x >= 0x2000000 { x - 0x4000000 } else { x } as i32;
        let z = if z >= 0x2000000 { z - 0x4000000 } else { z } as i32;
        let y = if y >= 0x800 { y - 0x1000 } else { y } as i32;

        Self::new(x, y, z)
    }

    pub fn relative(&self, dir: Direction) -> Self {
        let (dx, dy, dz) = dir.step();
        Self::new(self.x + dx, self.y + dy, self.z + dz)
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub enum Direction {
    Down,
    Up,
    North,
    South,
    West,
    East,
}

impl Direction {
    pub fn step(&self) -> (i32, i32, i32) {
        match self {
            Direction::Down => (0, -1, 0),
            Direction::Up => (0, 1, 0),
            Direction::North => (0, 0, -1),
            Direction::South => (0, 0, 1),
            Direction::West => (-1, 0, 0),
            Direction::East => (1, 0, 0),
        }
    }

    pub fn axis(&self) -> Axis {
        match self {
            Direction::Down | Direction::Up => Axis::Y,
            Direction::North | Direction::South => Axis::Z,
            Direction::West | Direction::East => Axis::X,
        }
    }

    pub fn axis_direction(&self) -> AxisDirection {
        match self {
            Direction::Up | Direction::South | Direction::East => AxisDirection::Positive,
            Direction::Down | Direction::North | Direction::West => AxisDirection::Negative,
        }
    }

    pub fn all() -> [Direction; 6] {
        [
            Direction::Down,
            Direction::Up,
            Direction::North,
            Direction::South,
            Direction::West,
            Direction::East,
        ]
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Axis {
    X,
    Y,
    Z,
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum AxisDirection {
    Positive,
    Negative,
}
