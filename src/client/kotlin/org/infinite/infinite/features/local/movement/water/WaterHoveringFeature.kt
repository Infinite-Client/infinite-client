package org.infinite.infinite.features.local.movement.water

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.minecraft.input.InputSystem

class WaterHoveringFeature : LocalFeature() {
    enum class Method {
        Jump,
        DeltaMovement,
    }

    private val method by property(EnumSelectionProperty(Method.Jump))
    override val featureType: FeatureLevel
        get() = if (method.value == Method.Jump) FeatureLevel.Utils else FeatureLevel.Extend

    override fun onStartTick() {
        val player = player ?: return
        if (player.isInWater && !player.isSwimming) {
            val waterFriction = 0.8
            val waterGravity = 0.020
            when (method.value) {
                Method.Jump -> {
                    val input = input ?: return
                    if (!input.keyPresses.shift) {
                        val deltaMovement = -waterGravity * waterFriction
                        if (player.deltaMovement.y < deltaMovement) {
                            InputSystem.press(options.keyJump)
                        }
                    }
                }

                Method.DeltaMovement -> {
                    val d = player.deltaMovement
                    player.setDeltaMovement(d.x, d.y + waterGravity, d.z)
                }
            }
        }
    }
}
