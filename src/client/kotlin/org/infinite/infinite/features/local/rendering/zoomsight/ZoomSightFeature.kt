package org.infinite.infinite.features.local.rendering.zoomsight

import org.infinite.InfiniteClient
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.graphics.Graphics2D
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min

class ZoomSightFeature : LocalFeature() {
    override val featureType = FeatureType.Extend
    override val defaultToggleKey = GLFW.GLFW_KEY_V

    val smoothZoom by property(BooleanProperty(true))
    val zoomLevel by property(FloatProperty(4.0f, 1.0f, 50.0f))
    val zoomStep by property(FloatProperty(1.1f, 1.01f, 2.0f))
    val sensitivityReduction by property(FloatProperty(1.0f, 0.0f, 1.0f))
    val autoReleaseOnMove by property(BooleanProperty(true))
    val maxZoom by property(FloatProperty(50.0f, 10.0f, 256f))
    var currentZoom: Float = 1.0f

    init {
        defineAction("zoom_in", GLFW.GLFW_KEY_UP) {
            currentZoom = min(maxZoom.value, currentZoom * zoomStep.value)
        }
        defineAction("zoom_out", GLFW.GLFW_KEY_DOWN) {
            currentZoom = max(1.0f, currentZoom / zoomStep.value)
        }
    }

    override fun onEnabled() {
        currentZoom = zoomLevel.value
    }

    override fun onDisabled() {
        currentZoom = 1.0f
    }

    override fun onEndTick() {
        minecraft.player ?: return

        if (autoReleaseOnMove.value) {
            val input = input ?: return
            val keyPresses = input.keyPresses
            if (!keyPresses.shift) {
                if (keyPresses.forward || keyPresses.backward || keyPresses.left || keyPresses.right || keyPresses.jump) {
                    disable()
                    return
                }
            }
        }

        // 1.0倍以下になったら自動解除
        if (currentZoom <= 1.0f) {
            disable()
        }
    }

    override fun onEndUiRendering(graphics2D: Graphics2D): Graphics2D {
        val centerX = graphics2D.width / 2f
        val centerY = graphics2D.height / 2f

        // テーマカラーの適用
        graphics2D.strokeStyle.width = 2.0f
        graphics2D.strokeStyle.color = InfiniteClient.theme.colorScheme.accentColor

        val bracketSize = 20f // 角の線の長さ
        val offset = 50f // 中心からの距離

        // --- 四隅のブラケット描画 ---

        // 左上
        drawBracket(graphics2D, centerX - offset, centerY - offset, bracketSize, bracketSize)
        // 右上
        drawBracket(graphics2D, centerX + offset, centerY - offset, -bracketSize, bracketSize)
        // 左下
        drawBracket(graphics2D, centerX - offset, centerY + offset, bracketSize, -bracketSize)
        // 右下
        drawBracket(graphics2D, centerX + offset, centerY + offset, -bracketSize, -bracketSize)

        // --- 倍率テキスト ---
        val zoomText = String.format("%.1fx", currentZoom)
        graphics2D.textStyle.size = 12f
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.fillStyle = InfiniteClient.theme.colorScheme.foregroundColor
        graphics2D.textCentered(zoomText, centerX, centerY + offset + 20f)

        return graphics2D
    }

    /**
     * 指定座標からL字の角を描画する補助関数
     */
    private fun drawBracket(g: Graphics2D, x: Float, y: Float, w: Float, h: Float) {
        g.beginPath()
        g.moveTo(x, y + h)
        g.lineTo(x, y)
        g.lineTo(x + w, y)
        g.strokePath()
    }
}
