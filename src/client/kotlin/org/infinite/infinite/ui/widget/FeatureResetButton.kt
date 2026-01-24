package org.infinite.infinite.ui.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.bundle.Graphics2DRenderer
import kotlin.math.PI
import kotlin.math.sin

class FeatureResetButton(x: Int, y: Int, width: Int, height: Int, feature: Feature) :
    Button(
        x,
        y,
        width,
        height,
        Component.empty(), // Component.literal("Reset") から empty に変更（アイコンのみのため）
        { button ->
            val self = button as FeatureResetButton
            val soundManager = Minecraft.getInstance().soundManager
            playButtonClickSound(soundManager)

            // --- 追加: クリック時にアニメーション開始時刻を記録 ---
            self.clickAnimStartTime = System.currentTimeMillis()

            feature.reset()
        },
        DEFAULT_NARRATION,
    ) {

    // --- 追加: アニメーション管理用プロパティ ---
    private var clickAnimStartTime: Long = -1L
    private val clickAnimDuration = 500.0 // 0.5秒で1回転

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

    private fun Graphics2D.renderResetIcon(x: Float, y: Float, width: Float, height: Float) {
        // 1. 通常の回転（ゆっくり）
        val duration = 4000.0 // 4秒で1サイクル（少し遅くして追加回転を際立たせる）
        val t = (System.currentTimeMillis() % duration) / duration
        var extraAngle = 0.0

        // 2. クリック時の追加回転（素早く1回転）
        if (clickAnimStartTime != -1L) {
            val elapsed = System.currentTimeMillis() - clickAnimStartTime
            val progress = (elapsed / clickAnimDuration).coerceAtMost(1.0)

            // イージング（滑らかに動かしたい場合は sin 等を使用）
            val easedProgress = sin(progress * PI / 2.0)
            extraAngle = 2.0 * PI * easedProgress

            if (progress >= 1.0) {
                clickAnimStartTime = -1L // アニメーション終了
            }
        }

        val colorScheme = InfiniteClient.theme.colorScheme
        val color = if (isHovered) colorScheme.accentColor else colorScheme.foregroundColor
        val centerX = x + width / 2f
        val centerY = y + height / 2f

        // 合計の回転角
        val totalAngle = (2.0 * PI * t) + extraAngle

        val rX = width / 3f
        val rY = height / 3f
        val r = (rX + rY) / 2f

        this.push()
        this.rotateAt(totalAngle.toFloat(), centerX, centerY)

        // アイコン描画
        this.fillStyle = color
        // 三角形（矢印の頭）
        this.fillTriangle(centerX, centerY - r - (rY / 2), centerX, centerY - r + (rY / 2), centerX + rX / 1.5f, centerY - r)

        // 円弧
        this.beginPath()
        this.strokeStyle.width = 1.5f
        this.strokeStyle.color = color
        this.arc(centerX, centerY, r, 0f, (PI * 1.7).toFloat()) // 少し隙間を開けるとリセットアイコンらしくなります
        this.strokePath()

        this.pop()
    }

    fun render(graphics2D: Graphics2D) {
        graphics2D.renderResetIcon(this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat())
    }
}
