package org.infinite.infinite.features.local.rendering.ui

import org.infinite.infinite.features.local.rendering.ui.crosshair.CrosshairRenderer
import org.infinite.infinite.features.local.rendering.ui.hotbar.HotbarRenderer
import org.infinite.infinite.features.local.rendering.ui.left.LeftBoxRenderer
import org.infinite.infinite.features.local.rendering.ui.right.RightBoxRenderer
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor

class UltraUiFeature : LocalFeature() {
    val crosshairRenderer = CrosshairRenderer()
    val hotbarRenderer = HotbarRenderer()
    val leftBoxRenderer = LeftBoxRenderer()
    val rightBoxRenderer = RightBoxRenderer()

    val hotbarUi by property(BooleanProperty(true))
    val leftBoxUi by property(BooleanProperty(true))
    val rightBoxUi by property(BooleanProperty(true))
    val crosshairUi by property(BooleanProperty(true))
    val barHeight by property(IntProperty(16, 8, 32))
    val padding by property(IntProperty(2, 0, 4))
    val alpha by property(FloatProperty(0.8f, 0f, 1f))
    override fun onStartUiRendering(graphics2D: Graphics2D) {
        if (hotbarUi.value) {
            hotbarRenderer.render(graphics2D)
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

    companion object {
        fun Graphics2D.renderUltraBar(
            x: Float,
            y: Float,
            baseWidth: Float, // 形状の基準となる幅
            baseHeight: Float, // 形状の基準となる高さ
            progress: Float, // 横方向の進捗 (0.0~1.0)
            heightProgress: Float, // 縦方向の表示割合 (0.0~1.0)
            color: Int,
            isRightToLeft: Boolean = false,
            isUpsideDown: Boolean = false,
        ) {
            if (baseWidth.absoluteValue < 1 || baseHeight.absoluteValue < 2) return

            // 1. 実際に描画するサイズを計算
            val drawWidth = baseWidth * progress.coerceIn(0f, 1f)
            val drawHeight = baseHeight * heightProgress.coerceIn(0f, 1f)
            if (drawWidth <= 0f || drawHeight <= 0f) return

            // 2. シザー設定 (表示領域の切り抜き)
            // 左右反転時は x から左へ、上下反転時は y から上へ領域を確保
            val sx = if (isRightToLeft) floor(x - drawWidth).toInt() else floor(x).toInt()
            val sy = if (isUpsideDown) floor(y - drawHeight).toInt() else floor(y).toInt()

            this.enableScissor(sx, sy, ceil(drawWidth).toInt(), ceil(drawHeight).toInt())

            // 3. 座標計算関数
            fun getX(offset: Float): Float = if (isRightToLeft) x - offset else x + offset
            fun getY(offset: Float): Float = if (isUpsideDown) y - offset else y + offset

            // 4. 頂点定義 (常に baseWidth / baseHeight を使って計算することで傾斜を固定)
            this.fillStyle = color

            // Y座標の各ライン (0.0, 0.5, 1.0 の位置)
            val yTop = getY(0f)
            val yMid = getY(baseHeight * 0.5f)
            val yBot = getY(baseHeight)

            // X座標の各ポイント (baseWidthを基準に固定)
            val x0 = getX(0f)
            val x1 = getX(baseWidth * 0.45f)
            val x2 = getX(baseWidth * 0.55f)
            val x3 = getX(baseWidth * 0.9f)
            val x4 = getX(baseWidth)

            // クアッド描画 (基準サイズで形を作り、シザーで削る)
            this.fillQuad(
                x0,
                yBot, // p0 (Bottom-Left相当)
                x0,
                yTop, // p1 (Top-Left相当)
                x1,
                yTop, // p2
                x2,
                yMid, // p3
            )
            this.fillQuad(
                x0,
                yBot, // p0
                x2,
                yMid, // p3
                x3,
                yMid, // p4
                x4,
                yBot, // p5
            )

            this.disableScissor()
        }
    }

    val scaledWidth: Int
        get() = minecraft.window.guiScaledWidth
    val hotbarWidth = 182
    private val totalMargin: Int
        get() = scaledWidth - hotbarWidth
    val sideMargin: Int get() = totalMargin / 2
}
