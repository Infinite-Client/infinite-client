package org.infinite.infinite.features.local.rendering.ui

import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.entity.HumanoidArm
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.crosshair.CrosshairRenderer
import org.infinite.infinite.features.local.rendering.ui.hotbar.HotbarRenderer
import org.infinite.infinite.features.local.rendering.ui.left.LeftBoxRenderer
import org.infinite.infinite.features.local.rendering.ui.right.RightBoxRenderer
import org.infinite.infinite.features.local.rendering.ui.topbox.TopBoxRenderer
import org.infinite.infinite.ui.screen.UltraHudEditorScreen
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.alpha
import org.infinite.utils.mix
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class UltraUiFeature : LocalFeature() {
    enum class HudElement {
        Hotbar,
        TopBox,
        LeftBox,
        RightBox,
        Crosshair,
    }

    data class HudRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    ) {
        fun contains(px: Double, py: Double): Boolean = px >= x && px <= x + width && py >= y && py <= y + height
    }

    val crosshairRenderer = CrosshairRenderer()
    val hotbarRenderer = HotbarRenderer()
    val topBoxRenderer = TopBoxRenderer()
    val leftBoxRenderer = LeftBoxRenderer()
    val rightBoxRenderer = RightBoxRenderer()
    val hotbarUi by property(BooleanProperty(true))
    val topBoxUi by property(BooleanProperty(true))
    val leftBoxUi by property(BooleanProperty(true))
    val rightBoxUi by property(BooleanProperty(true))
    val crosshairUi by property(BooleanProperty(true))
    val barHeight by property(IntProperty(24, 8, 32))
    val padding by property(IntProperty(4, 0, 8))
    val alpha by property(FloatProperty(0.8f, 0f, 1f))
    val hotbarOffsetX by property(IntProperty(0, -400, 400))
    val hotbarOffsetY by property(IntProperty(0, -240, 240))
    val topBoxOffsetX by property(IntProperty(0, -400, 400))
    val topBoxOffsetY by property(IntProperty(0, -240, 240))
    val leftBoxOffsetX by property(IntProperty(0, -400, 400))
    val leftBoxOffsetY by property(IntProperty(0, -240, 240))
    val rightBoxOffsetX by property(IntProperty(0, -400, 400))
    val rightBoxOffsetY by property(IntProperty(0, -240, 240))
    val crosshairOffsetX by property(IntProperty(0, -400, 400))
    val crosshairOffsetY by property(IntProperty(0, -240, 240))

    override fun onStartUiRendering(graphics2D: Graphics2D) {
        if (hotbarUi.value) {
            hotbarRenderer.render(graphics2D)
        }
        if (topBoxUi.value) {
            topBoxRenderer.render(graphics2D)
        }
        if (leftBoxUi.value) {
            leftBoxRenderer.render(graphics2D)
        }
        if (rightBoxUi.value) {
            rightBoxRenderer.render(graphics2D)
        }
        if (crosshairUi.value) {
            crosshairRenderer.render(graphics2D)
        }
    }

    fun openEditor(parent: Screen? = null) {
        minecraft.setScreen(UltraHudEditorScreen(parent))
    }

    fun resetOffsets() {
        hotbarOffsetX.reset()
        hotbarOffsetY.reset()
        topBoxOffsetX.reset()
        topBoxOffsetY.reset()
        leftBoxOffsetX.reset()
        leftBoxOffsetY.reset()
        rightBoxOffsetX.reset()
        rightBoxOffsetY.reset()
        crosshairOffsetX.reset()
        crosshairOffsetY.reset()
    }

    fun offsetText(element: HudElement): String = when (element) {
        HudElement.Hotbar -> "x=${hotbarOffsetX.value} y=${hotbarOffsetY.value}"
        HudElement.TopBox -> "x=${topBoxOffsetX.value} y=${topBoxOffsetY.value}"
        HudElement.LeftBox -> "x=${leftBoxOffsetX.value} y=${leftBoxOffsetY.value}"
        HudElement.RightBox -> "x=${rightBoxOffsetX.value} y=${rightBoxOffsetY.value}"
        HudElement.Crosshair -> "x=${crosshairOffsetX.value} y=${crosshairOffsetY.value}"
    }

    fun elementRects(graphics2D: Graphics2D): Map<HudElement, HudRect> = buildMap {
        if (hotbarUi.value) put(HudElement.Hotbar, hotbarRect(graphics2D))
        if (topBoxUi.value) put(HudElement.TopBox, topBoxRect(graphics2D))
        if (leftBoxUi.value) put(HudElement.LeftBox, leftBoxRect(graphics2D))
        if (rightBoxUi.value) put(HudElement.RightBox, rightBoxRect(graphics2D))
        if (crosshairUi.value) put(HudElement.Crosshair, crosshairRect(graphics2D))
    }

    fun applyDrag(element: HudElement, dx: Int, dy: Int) {
        when (element) {
            HudElement.Hotbar -> {
                hotbarOffsetX.value += dx
                hotbarOffsetY.value += dy
            }

            HudElement.TopBox -> {
                topBoxOffsetX.value += dx
                topBoxOffsetY.value += dy
            }

            HudElement.LeftBox -> {
                leftBoxOffsetX.value += dx
                leftBoxOffsetY.value += dy
            }

            HudElement.RightBox -> {
                rightBoxOffsetX.value += dx
                rightBoxOffsetY.value += dy
            }

            HudElement.Crosshair -> {
                crosshairOffsetX.value += dx
                crosshairOffsetY.value += dy
            }
        }
    }

    fun effectiveUiScale(graphics2D: Graphics2D): Float {
        val widthScale = graphics2D.width / 420f
        val heightScale = graphics2D.height / 260f
        return min(1f, max(0.7f, min(widthScale, heightScale)))
    }

    fun hotbarOrigin(graphics2D: Graphics2D): Pair<Float, Float> {
        val scale = effectiveUiScale(graphics2D)
        val x = (graphics2D.width - hotbarWidth * scale) / 2f + hotbarOffsetX.value
        val y = graphics2D.height - 22f * scale + hotbarOffsetY.value
        return x to y
    }

    fun topBoxOrigin(graphics2D: Graphics2D): Pair<Float, Float> {
        val scale = effectiveUiScale(graphics2D)
        val barHeight = 4f * scale
        val padding = padding.value.toFloat() * scale
        val x = (graphics2D.width - hotbarWidth * scale) / 2f + topBoxOffsetX.value
        val yOffset = if (containerUtilPreviewVisible()) 64f * scale else 0f
        val y = graphics2D.height - 20f * scale - padding - barHeight - 4f * scale - yOffset + topBoxOffsetY.value
        return x to y
    }

    fun leftBoxOrigin(graphics2D: Graphics2D): Pair<Float, Float> =
        leftBoxOffsetX.value.toFloat() to graphics2D.height.toFloat() - barHeight.value * effectiveUiScale(graphics2D) + leftBoxOffsetY.value

    fun rightBoxOrigin(graphics2D: Graphics2D): Pair<Float, Float> {
        val scale = effectiveUiScale(graphics2D)
        val width = sideMargin.toFloat() * rightWidthFactor() * scale
        val x = graphics2D.width.toFloat() - width + rightBoxOffsetX.value
        val y = graphics2D.height.toFloat() - barHeight.value * scale + rightBoxOffsetY.value
        return x to y
    }

    fun crosshairCenter(graphics2D: Graphics2D): Pair<Float, Float> =
        graphics2D.width / 2f + crosshairOffsetX.value to graphics2D.height / 2f + crosshairOffsetY.value

    fun leftWidthFactor(): Float {
        val player = player ?: return 1f
        val isOffhandOnLeft = player.mainArm == HumanoidArm.RIGHT && !player.offhandItem.isEmpty
        return if (isOffhandOnLeft) 0.85f else 1f
    }

    fun rightWidthFactor(): Float {
        val player = player ?: return 1f
        val isOffhandOnRight = player.mainArm == HumanoidArm.LEFT && !player.offhandItem.isEmpty
        return if (isOffhandOnRight) 0.85f else 1f
    }

    private fun hotbarRect(graphics2D: Graphics2D): HudRect {
        val scale = effectiveUiScale(graphics2D)
        val (x, y) = hotbarOrigin(graphics2D)
        val player = player
        val offhandExtra = if (player != null && !player.offhandItem.isEmpty) 26f * scale else 0f
        return HudRect(x - offhandExtra, y, hotbarWidth.toFloat() * scale + offhandExtra * 2, 22f * scale)
    }

    private fun topBoxRect(graphics2D: Graphics2D): HudRect {
        val scale = effectiveUiScale(graphics2D)
        val (x, y) = topBoxOrigin(graphics2D)
        val height = if (containerUtilPreviewVisible()) 64f * scale else 16f * scale
        return HudRect(x, y - (height - 4f * scale), hotbarWidth.toFloat() * scale, height)
    }

    private fun leftBoxRect(graphics2D: Graphics2D): HudRect {
        val scale = effectiveUiScale(graphics2D)
        val width = sideMargin.toFloat() * leftWidthFactor() * scale
        val (x, y) = leftBoxOrigin(graphics2D)
        return HudRect(x, y, width, barHeight.value.toFloat() * scale)
    }

    private fun rightBoxRect(graphics2D: Graphics2D): HudRect {
        val scale = effectiveUiScale(graphics2D)
        val width = sideMargin.toFloat() * rightWidthFactor() * scale
        val (x, y) = rightBoxOrigin(graphics2D)
        return HudRect(x, y, width, barHeight.value.toFloat() * scale)
    }

    private fun crosshairRect(graphics2D: Graphics2D): HudRect {
        val scale = effectiveUiScale(graphics2D)
        val (cx, cy) = crosshairCenter(graphics2D)
        return HudRect(cx - 14f * scale, cy - 14f * scale, 28f * scale, 28f * scale)
    }

    private fun containerUtilPreviewVisible(): Boolean {
        val containerUtil = InfiniteClient.localFeatures.inventory.containerUtilFeature
        return containerUtil.isEnabled() && containerUtil.hotbarRotate.value
    }

    companion object {
        fun Graphics2D.renderUltraBar(
            x: Float,
            y: Float,
            baseWidth: Float,
            baseHeight: Float,
            progress: Float,
            heightProgress: Float,
            color: Int,
            colorEnd: Int = color,
            isRightToLeft: Boolean = false,
            isUpsideDown: Boolean = false,
        ) {
            if (baseWidth.absoluteValue < 1 || baseHeight.absoluteValue < 2) return

            val drawWidth = baseWidth * progress.coerceIn(0f, 1f)
            val drawHeight = baseHeight * heightProgress.coerceIn(0f, 1f)
            if (drawWidth <= 0f || drawHeight <= 0f) return

            val sx = if (isRightToLeft) floor(x - drawWidth).toInt() else floor(x).toInt()
            val sy = if (isUpsideDown) floor(y - drawHeight).toInt() else floor(y).toInt()

            enableScissor(sx, sy, ceil(drawWidth).toInt(), ceil(drawHeight).toInt())

            fun getX(offset: Float): Float = if (isRightToLeft) x - offset else x + offset
            fun getY(offset: Float): Float = if (isUpsideDown) y - offset else y + offset

            val yTop = getY(0f)
            val yMid = getY(baseHeight * 0.5f)
            val yBot = getY(baseHeight)

            val x0 = getX(0f)
            val x1 = getX(baseWidth * 0.45f)
            val x2 = getX(baseWidth * 0.55f)
            val x3 = getX(baseWidth * 0.9f)
            val x4 = getX(baseWidth)
            val colorMid0 = color.mix(colorEnd, 0.45f)
            val colorMid1 = color.mix(colorEnd, 0.55f)
            val colorEnding = color.mix(colorEnd, 0.9f)

            fillQuad(
                x0, yBot,
                x0, yTop,
                x1, yTop,
                x2, yMid,
                color, color, colorMid0, colorMid1,
            )

            fillQuad(
                x0, yBot,
                x2, yMid,
                x3, yMid,
                x4, yBot,
                color, colorMid1, colorEnding, colorEnd,
            )

            disableScissor()
        }

        fun Graphics2D.renderLayeredBar(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            current: Float,
            target: Float,
            startColor: Int,
            endColor: Int,
            alpha: Float,
            isRightToLeft: Boolean,
            whiteColor: Int,
            blackColor: Int,
        ) {
            val isInc = target > current
            val mixColor = if (isInc) whiteColor else blackColor

            val sColor = startColor.mix(mixColor, 0.5f).alpha((127.5 * alpha).toInt())
            val eColor = endColor.mix(mixColor, 0.5f).alpha((127.5 * alpha).toInt())
            val sMain = startColor.alpha((255 * alpha).toInt())
            val eMain = endColor.alpha((255 * alpha).toInt())
            val pColor = max(target, current)
            val pMain = min(target, current)
            renderUltraBar(x, y, width, height, pColor, 1f, sColor, eColor, isRightToLeft)
            renderUltraBar(x, y, width, height, pMain, 1f, sMain, eMain, isRightToLeft)
        }
    }

    val scaledWidth: Int
        get() = minecraft.window.guiScaledWidth
    val hotbarWidth = 182
    private val totalMargin: Int
        get() = scaledWidth - hotbarWidth
    val sideMargin: Int
        get() = totalMargin / 2
}
