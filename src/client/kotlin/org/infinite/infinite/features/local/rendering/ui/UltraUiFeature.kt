package org.infinite.infinite.features.local.rendering.ui

import org.infinite.infinite.features.local.rendering.ui.crosshair.CrosshairRenderer
import org.infinite.infinite.features.local.rendering.ui.hotbar.HotbarRenderer
import org.infinite.infinite.features.local.rendering.ui.left.LeftBoxRenderer
import org.infinite.infinite.features.local.rendering.ui.right.RightBoxRenderer
import org.infinite.infinite.features.local.rendering.ui.topbox.TopBoxRenderer
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

    // UltraUiFeature.kt 内の companion object を修正
    companion object {
        fun Graphics2D.renderUltraBar(
            x: Float,
            y: Float,
            baseWidth: Float,
            baseHeight: Float,
            progress: Float,
            heightProgress: Float,
            color: Int, // 開始色 (左側)
            colorEnd: Int = color, // 終了色 (右側) - デフォルトは開始色と同じ
            isRightToLeft: Boolean = false,
            isUpsideDown: Boolean = false,
        ) {
            if (baseWidth.absoluteValue < 1 || baseHeight.absoluteValue < 2) return

            val drawWidth = baseWidth * progress.coerceIn(0f, 1f)
            val drawHeight = baseHeight * heightProgress.coerceIn(0f, 1f)
            if (drawWidth <= 0f || drawHeight <= 0f) return

            val sx = if (isRightToLeft) floor(x - drawWidth).toInt() else floor(x).toInt()
            val sy = if (isUpsideDown) floor(y - drawHeight).toInt() else floor(y).toInt()

            this.enableScissor(sx, sy, ceil(drawWidth).toInt(), ceil(drawHeight).toInt())

            fun getX(offset: Float): Float = if (isRightToLeft) x - offset else x + offset
            fun getY(offset: Float): Float = if (isUpsideDown) y - offset else y + offset

            // 頂点ごとの色を計算 (左側はcolor, 右側はcolorEnd)
            // fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, col0, col1, col2, col3)
            // col0: 左下, col1: 左上, col2: 右上, col3: 右下 (標準的なQuad頂点順序)

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
            // 1つ目のクアッド (左端から中央斜め部分まで)
            this.fillQuad(
                x0, yBot, // 左下
                x0, yTop, // 左上
                x1, yTop, // 右上(上)
                x2, yMid, // 右下(中)
                color, color, colorMid0, colorMid1,
            )

            // 2つ目のクアッド (中央斜め部分から右端まで)
            this.fillQuad(
                x0, yBot, // 左下
                x2, yMid, // 左上(中)
                x3, yMid, // 右上(中)
                x4, yBot, // 右下
                color, colorMid1, colorEnding, colorEnd, // グラデーション適用
            )

            this.disableScissor()
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

            // 色の合成とアルファ適用
            val sColor = startColor.mix(mixColor, 0.5f).alpha((127.5 * alpha).toInt())
            val eColor = endColor.mix(mixColor, 0.5f).alpha((127.5 * alpha).toInt())
            val sMain = startColor.alpha((255 * alpha).toInt())
            val eMain = endColor.alpha((255 * alpha).toInt())
            val pColor = max(target, current)
            val pMain = min(target, current)
            // 背面レイヤー
            renderUltraBar(x, y, width, height, pColor, 1f, sColor, eColor, isRightToLeft)
            // 前面レイヤー
            renderUltraBar(x, y, width, height, pMain, 1f, sMain, eMain, isRightToLeft)
        }
    }

    val scaledWidth: Int
        get() = minecraft.window.guiScaledWidth
    val hotbarWidth = 182
    private val totalMargin: Int
        get() = scaledWidth - hotbarWidth
    val sideMargin: Int get() = totalMargin / 2
}
