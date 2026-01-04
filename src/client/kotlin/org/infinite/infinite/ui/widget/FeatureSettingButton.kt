package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FeatureSettingButton(x: Int, y: Int, width: Int, height: Int, feature: Feature) :
    Button(
        x,
        y,
        width,
        height,
        Component.literal("Setting"),
        { feature.reset() },
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

    private fun Graphics2D.renderSettingIcon(x: Int, y: Int, width: Int, height: Int) =
        this.renderSettingIcon(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

    private fun Graphics2D.renderSettingIcon(x: Float, y: Float, width: Float, height: Float) {
        val colorScheme = InfiniteClient.theme.colorScheme
        val rX = width / 2f
        val rY = height / 2f
        val centerX = x + rX
        val centerY = y + rY
        val rMin = 0.3f
        val rBase = 0.5f
        val rLevel = 1.5f
        val rX0 = rMin * rX
        val rY0 = rMin * rY
        val rX1 = rBase * rX
        val rY1 = rBase * rY
        val rX2 = rLevel * rX1
        val rY2 = rLevel * rY1
        val sep = 6
        val sepF = sep.toFloat()
        val intervalMs = 10000.0
        val t = System.currentTimeMillis() % intervalMs / intervalMs
        for (i in 0 until sep) {
            val d0 = i / sepF + t
            val s0 = sin(d0 * PI * 2).toFloat()
            val c0 = cos(d0 * PI * 2).toFloat()
            val d1 = i / sepF + 0.5f / sepF + t
            val s1 = sin(d1 * PI * 2).toFloat()
            val c1 = cos(d1 * PI * 2).toFloat()
            val d2 = (i + 1) / sepF + t
            val s2 = sin(d2 * PI * 2).toFloat()
            val c2 = cos(d2 * PI * 2).toFloat()
            this.fillStyle = colorScheme.foregroundColor
            this.fillTriangle(
                centerX,
                centerY,
                centerX + rX1 * s0,
                centerY + rY1 * c0,
                centerX + rX1 * s1,
                centerY + rY1 * c1,
            )
            this.fillTriangle(
                centerX,
                centerY,
                centerX + rX2 * s1,
                centerY + rY2 * c1,
                centerX + rX2 * s2,
                centerY + rY2 * c2,
            )
        }
        for (ih in 0 until sep / 2) {
            val i = 2 * ih
            val d0 = i / sepF + t
            val d1 = (i + 1) / sepF + t
            val d2 = (i + 2) / sepF + t
            val x0 = centerX + rX0 * sin(d0 * PI * 2).toFloat()
            val y0 = centerY + rY0 * cos(d0 * PI * 2).toFloat()
            val x1 = centerX + rX0 * sin(d1 * PI * 2).toFloat()
            val y1 = centerY + rY0 * cos(d1 * PI * 2).toFloat()
            val x2 = centerX + rX0 * sin(d2 * PI * 2).toFloat()
            val y2 = centerY + rY0 * cos(d2 * PI * 2).toFloat()
            this.fillStyle = colorScheme.cyanColor
            this.fillQuad(centerX, centerY, x0, y0, x1, y1, x2, y2)
        }
    }

    fun render(
        graphics2D: Graphics2D,
    ) {
        val theme = InfiniteClient.theme
        val colorScheme = theme.colorScheme
        theme.renderBackGround(this.x, this.y, this.width, this.height, graphics2D, 0.8f)
        graphics2D.strokeStyle.width = 2f
        graphics2D.strokeStyle.color = colorScheme.redColor
        graphics2D.renderSettingIcon(this.x, this.y, this.width, this.height)
    }
}
