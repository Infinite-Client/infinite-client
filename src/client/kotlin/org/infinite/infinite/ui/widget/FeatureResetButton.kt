package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import org.infinite.utils.alpha
import org.infinite.utils.mix

class FeatureResetButton(x: Int, y: Int, width: Int, height: Int, feature: Feature) :
    Button(
        x,
        y,
        width,
        height,
        Component.empty(),
        { button ->
            button as FeatureResetButton
            val soundManager = Minecraft.getInstance().soundManager
            playButtonClickSound(soundManager)
            feature.reset()
        },
        DEFAULT_NARRATION,
    ) {

    override fun renderContents(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }

    fun render(graphics2D: Graphics2D) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        val radius = (height * 0.35f).coerceAtLeast(4f)
        val baseColor = colorScheme.surfaceColor.mix(colorScheme.backgroundColor, 0.6f)
        val hoverMix = if (isHovered) 0.12f else 0.0f
        graphics2D.fillStyle = baseColor.mix(colorScheme.accentColor, hoverMix).alpha(200)
        graphics2D.fillRoundedRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), radius)
    }
}
