package org.infinite.infinite.features.rendering.sensory

import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import org.infinite.infinite.features.rendering.sensory.esp.ContainerEsp
import org.infinite.infinite.features.rendering.sensory.esp.ItemEsp
import org.infinite.infinite.features.rendering.sensory.esp.MobEsp
import org.infinite.infinite.features.rendering.sensory.esp.PlayerEsp
import org.infinite.infinite.features.rendering.sensory.esp.PortalEsp
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.libs.graphics.Graphics3D
import org.infinite.libs.world.WorldManager
import org.infinite.settings.FeatureSetting
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class ExtraSensory : ConfigurableFeature(initialEnabled = false) {
    enum class Method {
        HitBox,
        OutLine,
    }

    override val level: FeatureLevel = FeatureLevel.Cheat
    val method =
        FeatureSetting.EnumSetting<Method>(
            "Method",
            Method.HitBox,
            Method.entries,
        )
    private val playerEsp =
        FeatureSetting.BooleanSetting("PlayerEsp", true)
    private val mobEsp =
        FeatureSetting.BooleanSetting("MobEsp", true)

    private val itemEsp =
        FeatureSetting.BooleanSetting("ItemEsp", true)

    private val portalEsp =
        FeatureSetting.BooleanSetting("PortalEsp", true)

    private val containerEsp =
        FeatureSetting.BooleanSetting(
            "ContainerEsp",
            true,
        )

    override val settings: List<FeatureSetting<*>> =
        listOf(
            method,
            playerEsp,
            mobEsp,
            itemEsp,
            portalEsp,
            containerEsp,
        )

    override fun render3d(graphics3D: Graphics3D) {
        if (portalEsp.value) {
            PortalEsp.render(graphics3D, method.value)
        }
        if (playerEsp.value) {
            PlayerEsp.render(graphics3D, method.value)
        }
        if (mobEsp.value) {
            MobEsp.render(graphics3D, method.value)
        }
        if (itemEsp.value) {
            ItemEsp.render(graphics3D, method.value)
        }
        if (containerEsp.value) {
            ContainerEsp.render(graphics3D, method.value)
        }
    }

    override fun handleChunk(worldChunk: WorldManager.Chunk) {
        PortalEsp.handleChunk(worldChunk)
        ContainerEsp.handleChunk(worldChunk)
    }

    override fun onDisabled() {
        super.onDisabled()
        PortalEsp.clear()
        ContainerEsp.clear()
    }

    override fun onTick() {
        PortalEsp.tick()
        ContainerEsp.tick()
    }

    fun handleIsGlowing(
        entity: Entity,
        cir: CallbackInfoReturnable<Boolean>,
    ) {
        if (method.value == Method.OutLine) {
            when (entity) {
                is Player -> {
                    if (playerEsp.value) {
                        cir.returnValue = true
                    }
                }

                is Mob -> {
                    if (mobEsp.value) {
                        cir.returnValue = true
                    }
                }

                is ItemEntity -> {
                    if (itemEsp.value) {
                        cir.returnValue = true
                    }
                }
            }
        }
    }

    fun <T, S> handleRenderState(
        entity: T,
        state: S,
        tickProgress: Float,
        ci: CallbackInfo,
    ) {
        when (entity) {
            is ItemEntity -> {
                if (state is ItemEntityRenderState) {
                    ItemEsp.handleRenderState(entity, state, tickProgress, ci)
                }
            }

            is Player -> {
                if (state is AvatarRenderState) {
                    PlayerEsp.handleRenderState(entity, state, tickProgress, ci)
                }
            }

            is Mob -> {
                if (state is LivingEntityRenderState) {
                    MobEsp.handleRenderState(entity, state, tickProgress, ci)
                }
            }
        }
    }
}
