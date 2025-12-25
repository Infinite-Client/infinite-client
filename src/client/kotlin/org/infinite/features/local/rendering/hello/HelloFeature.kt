package org.infinite.features.local.rendering.hello

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.log.LogSystem
import kotlin.math.cos
import kotlin.math.sin

class HelloFeature : LocalFeature() {
    init {
        property("Hello", IntProperty(1, 1, 100))
        enable()
    }

    override fun onConnected() {
        LogSystem.log("Graphics2D Test Feature Connected!")
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        graphics2D.fillStyle = 0x80FF0000.toInt() // 半透明の赤
        graphics2D.fillRect(10f, 10f, 100f, 50f)

        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 2.0f)
        graphics2D.strokeRect(10f, 10f, 100f, 50f)
        // 三角形の枠線を追加
        // --- 2. グラデーション三角形 (TriangleRenderer) ---
        graphics2D.fillTriangle(
            150f, 20f, // 頂点0 (上)
            120f, 80f, // 頂点1 (左下)
            180f, 80f, // 頂点2 (右下)
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
        )
        graphics2D.strokeStyle = StrokeStyle(0xFF000000.toInt(), 10.0f)
        graphics2D.strokeTriangle(150f, 20f, 120f, 80f, 180f, 80f)

        graphics2D.fillQuad(
            200f, 20f, // 左上
            220f, 80f, // 左下
            300f, 90f, // 右下
            280f, 10f, // 右上
            0xFF00FFFF.toInt(),
            0xFFFF00FF.toInt(),
            0xFFFFFF00.toInt(),
            0xFFFFFFFF.toInt(),
        )
        // 四角形の枠線を追加
        graphics2D.strokeQuad(200f, 20f, 220f, 80f, 300f, 90f, 280f, 10f)

        // --- 4. 動的なアニメーションテスト ---
        // floatで計算するために f を明示
        val time = (System.currentTimeMillis() % 10000) / 1000.0 // 精度維持のためDoubleで計算開始
        val centerX = 200.0
        val centerY = 150.0
        val radius = 40.0

        // 回転する三角形の頂点計算（描画時にtoFloat()）
        val x0 = (centerX + radius * cos(time)).toFloat()
        val y0 = (centerY + radius * sin(time)).toFloat()
        val x1 = (centerX + radius * cos(time + 2.094)).toFloat()
        val y1 = (centerY + radius * sin(time + 2.094)).toFloat()
        val x2 = (centerX + radius * cos(time + 4.188)).toFloat()
        val y2 = (centerY + radius * sin(time + 4.188)).toFloat()

        // 塗りつぶし
        graphics2D.fillStyle = 0xFFFFA500.toInt() // オレンジ
        graphics2D.fillTriangle(x0, y0, x1, y1, x2, y2)

        // 枠線の追加
        graphics2D.strokeStyle = StrokeStyle(0xFF000000.toInt(), 10.0f) // 黒い枠線
        graphics2D.strokeTriangle(x0, y0, x1, y1, x2, y2)
        return graphics2D
    }
}
