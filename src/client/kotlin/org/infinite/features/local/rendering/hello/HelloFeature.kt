package org.infinite.features.local.rendering.hello

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.log.LogSystem
import kotlin.math.cos
import kotlin.math.sin

class HelloFeature : LocalFeature() {
    override fun onConnected() {
        LogSystem.log("Graphics2D Test Feature Connected!")
    }

    override fun onStartUiRendering(graphics2D: Graphics2D): Graphics2D {
        // --- 1. 基本的な矩形と塗りつぶし (RectRenderer) ---
        graphics2D.zIndex = 10
        graphics2D.fillStyle = 0x80FF0000.toInt() // 半透明の赤
        graphics2D.fillRect(10f, 10f, 100f, 50f)

        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 2.0)
        graphics2D.strokeRect(10f, 10f, 100f, 50f)

        // --- 2. グラデーション三角形 (TriangleRenderer) ---
        // zIndex を変えて重なりを確認
        graphics2D.zIndex = 20
        graphics2D.fillTriangle(
            150f, 20f,  // 頂点0 (上)
            120f, 80f,  // 頂点1 (左下)
            180f, 80f,  // 頂点2 (右下)
            0xFFFF0000.toInt(), // 赤
            0xFF00FF00.toInt(), // 緑
            0xFF0000FF.toInt()  // 青
        )

        // --- 3. グラデーション四角形 (QuadRenderer) ---
        // 少し歪んだ形状でテスト
        graphics2D.zIndex = 15
        graphics2D.fillQuad(
            200f, 20f,  // 左上
            220f, 80f,  // 左下
            300f, 90f,  // 右下
            280f, 10f,  // 右上
            0xFF00FFFF.toInt(), // シアン
            0xFFFF00FF.toInt(), // マゼンタ
            0xFFFFFF00.toInt(), // イエロー
            0xFFFFFFFF.toInt()  // 白
        )

        // --- 4. 動的なアニメーションテスト ---
        // realDelta を使用して滑らかに動く三角形を描画
        val time = System.currentTimeMillis() / 1000.0
        val centerX = 200.0
        val centerY = 150.0
        val radius = 40.0

        // 回転する三角形の頂点計算
        val x0 = centerX + radius * cos(time)
        val y0 = centerY + radius * sin(time)
        val x1 = centerX + radius * cos(time + 2.094) // 120度
        val y1 = centerY + radius * sin(time + 2.094)
        val x2 = centerX + radius * cos(time + 4.188) // 240度
        val y2 = centerY + radius * sin(time + 4.188)

        graphics2D.zIndex = 30
        graphics2D.fillStyle = 0xFFFFA500.toInt() // オレンジ
        graphics2D.fillTriangle(x0, y0, x1, y1, x2, y2)

        return graphics2D
    }
}