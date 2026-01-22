package org.infinite.infinite.features.local.rendering.ui.left

import net.minecraft.world.entity.ai.attributes.Attributes
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderUltraBar
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface

class LeftBoxRenderer :
    MinecraftInterface(),
    IUiRenderer {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    override fun render(graphics2D: Graphics2D) {
        val player = player ?: return
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val alpha = ultraUiFeature.alpha.value
        val padding = ultraUiFeature.padding.value.toFloat()
        var height = ultraUiFeature.barHeight.value.toFloat()
        var x = padding
        var y = (graphics2D.height - height - padding * 2)
        var width = ultraUiFeature.sideMargin - padding * 2f

        graphics2D.renderUltraBar(x, y, width, height, 1f, colorScheme.backgroundColor)
        x += padding
        width -= padding
        y += padding
        height -= padding * 2f
        val health = player.health / player.maxHealth
        graphics2D.renderUltraBar(x, y, width, height, health, colorScheme.color(health / 12f, 1f, 0.5f, alpha))
        height /= 2f
        y += height / 2f
        val armor = player.armorValue / 20f
        graphics2D.renderUltraBar(x, y, width, height, armor, colorScheme.blueColor)
        val toughnessInstance = player.attributes.getInstance(Attributes.ARMOR_TOUGHNESS)
        val toughnessValue = toughnessInstance?.value?.toFloat() ?: 0f
        val toughness = toughnessValue / 12f
        graphics2D.renderUltraBar(x, y, width, height, toughness, colorScheme.cyanColor)
    }
}
