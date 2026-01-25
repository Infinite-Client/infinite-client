package org.infinite.infinite.features.local.movement.bettersneak

import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.AABB
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.DoubleProperty

class BetterSneakFeature : LocalFeature() {
    // --- プロパティ ---
    val safeWalk by property(BooleanProperty(true))
    val edgeDistance by property(DoubleProperty(0.10, 0.01, 0.50))

    // --- 内部状態 ---
    private var isInternalSneaking = false

    override fun onEnabled() {
        isInternalSneaking = false
    }

    override fun onDisabled() {
        if (isInternalSneaking) {
            updateSneakKeyState(false)
        }
    }

    override fun onStartTick() {
        val player = player ?: return
        val level = level ?: return

        // 機能オフ、または空中の場合はスニークを解除
        if (!safeWalk.value || !player.onGround()) {
            if (isInternalSneaking) updateSneakKeyState(false)
            return
        }

        // --- 修正版 SafeWalk ロジック ---
        val aabb: AABB = player.boundingBox

        // 1. 足元(下方向)に判定を少し伸ばす
        // 2. 水平方向(X, Z)を内側に削る。これにより「崖から身を乗り出した時」に
        //    このBoxが完全に空中へはみ出し、noCollisionがtrueになる
        val checkBox: AABB = aabb
            .expandTowards(0.0, -0.5, 0.0)
            .deflate(edgeDistance.value, 0.0, edgeDistance.value)

        // 指定したBoxの範囲内に衝突体（地面）がなければ、スニークを発動
        val shouldSneak = level.noCollision(player, checkBox)

        updateSneakKeyState(shouldSneak)
    }

    private fun updateSneakKeyState(state: Boolean) {
        val sneakKey: KeyMapping = options.keyShift
        if (isInternalSneaking == state) return

        sneakKey.isDown = state
        isInternalSneaking = state
    }
}
