package org.infinite.infinite.features.local.movement

import org.infinite.infinite.features.local.movement.fly.SuperFly
import org.infinite.infinite.features.local.movement.quickmove.QuickMove
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalMovementCategory : LocalCategory() {
    val superFlyFeature by feature(SuperFly())
    val quickMoveFeature by feature(QuickMove())
}
