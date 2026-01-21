package org.infinite.infinite.features.local.movement

import org.infinite.infinite.features.local.movement.bettersneak.BetterSneakFeature
import org.infinite.infinite.features.local.movement.fastdescent.FastDescentFeature
import org.infinite.infinite.features.local.movement.fly.SuperFlyFeature
import org.infinite.infinite.features.local.movement.quickmove.QuickMoveFeature
import org.infinite.infinite.features.local.movement.water.WaterHoveringFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalMovementCategory : LocalCategory() {
    val betterSneakFeature by feature(BetterSneakFeature())
    val fastDescentFeature by feature(FastDescentFeature())
    val superFlyFeature by feature(SuperFlyFeature())
    val quickMoveFeature by feature(QuickMoveFeature())
    val waterHoveringFeature by feature(WaterHoveringFeature())
}
