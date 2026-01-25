package org.infinite.infinite.features.local.movement.fastdescent

import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.AABB
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.DoubleProperty

class FastDescentFeature : LocalFeature() {
    // 段差検知の感度設定
    val descentFactor by property(DoubleProperty(0.05, 0.00, 1.00))

    private var isSneakingInternal = false

    override fun onDisabled() {
        if (isSneakingInternal) {
            updateSneakState(false)
        }
    }

    override fun onStartTick() {
        val player = player ?: return
        val level = level ?: return

        // 地面にいる時のみ動作（空中では加速させない）
        if (!player.onGround()) {
            if (isSneakingInternal) updateSneakState(false)
            return
        }

        // 発見された特殊なAABB操作:
        // Yをマイナスにdeflate（拡大）し、移動方向にexpandTowardsする
        val aabb: AABB = player.boundingBox
        val adjustedBox: AABB = aabb
            .deflate(0.0, -player.maxUpStep().toDouble(), 0.0)
            .expandTowards(-descentFactor.value, 0.0, -descentFactor.value)

        // 判定が空の状態（段差の縁など）でスニークをON/OFFさせることで
        // 垂直方向の慣性を変化させている可能性が高い
        val shouldTrigger = level.noCollision(adjustedBox)
        updateSneakState(shouldTrigger)
    }

    private fun updateSneakState(state: Boolean) {
        val sneakKey: KeyMapping = options.keyShift
        if (isSneakingInternal == state) return

        sneakKey.isDown = state
        isSneakingInternal = state
    }
}
