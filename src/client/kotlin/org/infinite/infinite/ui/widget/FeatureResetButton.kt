package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer

class FeatureResetButton(x: Int, y: Int, width: Int, height: Int, feature: Feature) :
    Button(
        x,
        y,
        width,
        height,
        Component.literal("Reset"),
        {
            val soundManager = Minecraft.getInstance().soundManager
            playButtonClickSound(soundManager)
            feature.reset()
        },
        DEFAULT_NARRATION,
    ) {
    override fun renderContents(
        guiGraphics: GuiGraphics,
        i: Int,
        j: Int,
        f: Float,
    ) {
        val graphics2DRenderer = Graphics2DRenderer(guiGraphics)
        render(graphics2DRenderer)
        graphics2DRenderer.flush()
    }

    fun render(
        graphics2D: Graphics2D,
    ) {
        val theme = InfiniteClient.theme
        theme.renderBackGround(this.x, this.y, this.width, this.height, graphics2D, 0.8f)
    }
}
