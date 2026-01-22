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
            width: Float,
            height: Float,
            progress: Float,
            color: Int,
        ) {
            if (width.absoluteValue < 1 || height < 2) return

            // 実際に描画する幅（進捗率を反映）
            val currentWidth = width * progress.coerceIn(0f, 1f)
            if (currentWidth.absoluteValue <= 0f) return
            val isRightToLeft = currentWidth < 0
            val width = width.absoluteValue
            // シザー領域の設定
            // 右から左の場合、開始X座標を (基点x - currentWidth) に調整
            val scissorX = if (isRightToLeft) floor(x - currentWidth).toInt() else floor(x).toInt()
            this.enableScissor(
                scissorX,
                floor(y).toInt(),
                ceil(currentWidth).toInt(),
                ceil(height).toInt(),
            )

            this.fillStyle = color

            // 座標計算用の関数：isRightToLeftがtrueならX軸方向を反転させる
            fun getX(offset: Float): Float = if (isRightToLeft) x - offset else x + offset

            // 各頂点の定義 (0.0f 〜 width の相対座標で計算)
            // 左端基準か右端基準かで offset の符号を変える
            val p1 = getX(0f) to y
            val p2 = getX(width * 0.45f) to y
            val p3 = getX(width * 0.55f) to y + height * 0.5f
            val p4 = getX(width * 0.9f) to y + height * 0.5f
            val p5 = getX(width) to y + height
            val p0 = getX(0f) to y + height

            // クアッド描画
            // getXを通しているため、isRightToLeft時には自動的に左右が入れ替わる
            this.fillQuad(
                p0.first,
                p0.second,
                p1.first,
                p1.second,
                p2.first,
                p2.second,
                p3.first,
                p3.second,
            )
            this.fillQuad(
                p0.first,
                p0.second,
                p3.first,
                p3.second,
                p4.first,
                p4.second,
                p5.first,
                p5.second,
            )

            this.disableScissor() // シザーの解除を忘れずに
        }
    }

    val scaledWidth: Int
        get() = minecraft.window.guiScaledWidth
    val hotbarWidth = 182
    private val totalMargin: Int
        get() = scaledWidth - hotbarWidth
    val sideMargin: Int get() = totalMargin / 2
}
