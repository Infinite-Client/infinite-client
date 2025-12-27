package org.infinite.ultimate.features.local.rendering.hello

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.structs.TextStyle
import org.infinite.libs.log.LogSystem
import org.lwjgl.glfw.GLFW
import kotlin.math.cos
import kotlin.math.sin

class HelloFeature : LocalFeature() {
    override val defaultToggleKey: Int
        get() = GLFW.GLFW_KEY_F
    val hello by property(IntProperty(1, 1, 100))

    init {
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
// テスト用の線幅設定
        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 5.0f)

        // 1. グラデーション三角形の枠線
        graphics2D.strokeTriangle(
            50f, 150f,
            20f, 200f,
            80f, 200f,
            0xFFFF0000.toInt(), // 赤
            0xFF00FF00.toInt(), // 緑
            0xFF0000FF.toInt(), // 青
        )

        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 2.0f)
        graphics2D.strokeRect(200f, 20f, 80f, 70f) // 塗りつぶしなしの枠のみ

        // --- 3. 変形・移動させた strokeQuad ---
        // 座標を右側にずらし(x + 150)、形をより歪ませています
        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 25.0f)
        graphics2D.strokeQuad(
            350f, 20f, // 左上 (少し右へ)
            330f, 100f, // 左下 (より下へ)
            480f, 120f, // 右下 (大きく右下へ)
            450f, 10f, // 右上 (鋭角に)
            0xFF00FFFF.toInt(), // シアン
            0xFFFF00FF.toInt(), // マゼンタ
            0xFFFFFF00.toInt(), // イエロー
            0xFFFFFFFF.toInt(), // 白
        )
        graphics2D.strokeRect(
            200f,
            200f,
            80f,
            70f,
            0xFF000000.toInt(),
            0xFF0000FF.toInt(),
            0xFF00FFFF.toInt(),
            0xFFFFFFFF.toInt(),
        )

        // --- 5. 新しい機能のテスト ---

        // translateとarcを使った描画
        graphics2D.save() // 現在の変換状態を保存
        graphics2D.translate(50f, 300f) // 原点を移動
        graphics2D.strokeStyle = StrokeStyle(0xFF00FF00.toInt(), 3.0f) // 緑色の線
        graphics2D.beginPath()
        graphics2D.arc(0f, 0f, 30f, 0f, (Math.PI * 1.5).toFloat(), false) // 半径30の円弧
        graphics2D.strokePath()
        graphics2D.restore() // 変換状態を元に戻す

        // arcToを使った描画
        graphics2D.save()
        graphics2D.translate(150f, 300f)
        graphics2D.strokeStyle = StrokeStyle(0xFF0000FF.toInt(), 3.0f) // 青色の線
        graphics2D.beginPath()
        graphics2D.moveTo(0f, 0f)
        graphics2D.arcTo(50f, 0f, 50f, 50f, 20f) // 制御点(50,0),(50,50), 半径20の円弧
        graphics2D.lineTo(50f, 50f)
        graphics2D.strokePath()
        graphics2D.restore()

        // bezierCurveToを使った描画
        graphics2D.save()
        graphics2D.translate(250f, 300f)
        graphics2D.strokeStyle = StrokeStyle(0xFFFF00FF.toInt(), 3.0f) // マゼンタの線
        graphics2D.beginPath()
        graphics2D.moveTo(0f, 0f)
        graphics2D.bezierCurveTo(20f, -50f, 80f, 50f, 100f, 0f) // 制御点2つ、終点1つ
        graphics2D.strokePath()
        graphics2D.restore()

        // transformを使った図形の変形
        graphics2D.save()
        graphics2D.translate(400f, 300f) // 平行移動
        graphics2D.transform(1.5f, 0.2f, 0.0f, 1.0f, 0.0f, 0.0f) // スケールとスキュー
        graphics2D.fillStyle = 0x80FFFF00.toInt() // 半透明の黄色
        graphics2D.fillRect(0f, 0f, 50f, 50f)
        graphics2D.restore()

        // --- 6. Graphics2Dプロパティと単色fillQuad, closePathのテスト ---

        // gamedelta, realdelta, width, heightの表示
        // これらはテキスト描画機能がないため、ここでは擬似的に座標で表現する
        // 例: gamedeltaの値をx座標に反映
        val deltaDisplayX = 10f + graphics2D.gameDelta * 10 // gamedeltaに応じてx座標を変化
        graphics2D.fillStyle = 0xFF00FFFF.toInt() // シアン
        graphics2D.fillRect(deltaDisplayX, 400f, 10f, 10f) // gamedelta表示用

        // realdeltaをy座標に反映
        val realDeltaDisplayY = 400f + graphics2D.realDelta * 10 // realdeltaに応じてy座標を変化
        graphics2D.fillStyle = 0xFFFF00FF.toInt() // マゼンタ
        graphics2D.fillRect(20f, realDeltaDisplayY, 10f, 10f) // realdelta表示用

        // widthとheightの表示 (画面の端に線を描くなど)
        graphics2D.strokeStyle = StrokeStyle(0xFFCCCCCC.toInt(), 1.0f) // 明るいグレー
        graphics2D.beginPath()
        graphics2D.moveTo(0f, 0f)
        graphics2D.lineTo(graphics2D.width.toFloat(), 0f)
        graphics2D.lineTo(graphics2D.width.toFloat(), graphics2D.height.toFloat())
        graphics2D.lineTo(0f, graphics2D.height.toFloat())
        graphics2D.closePath() // 画面の端を閉じる
        graphics2D.strokePath()

        // fillQuad(単色版)の描画
        graphics2D.fillStyle = 0x80808080.toInt() // 半透明のグレー
        graphics2D.fillQuad(50f, 450f, 100f, 450f, 100f, 500f, 50f, 500f) // 正方形

        // --- 7. グラデーションパスのテスト (Path APIの新機能) ---
        graphics2D.save()
        graphics2D.translate(100f, 500f) // パスの開始位置を調整
        graphics2D.enablePathGradient = true // Enable gradient for this path

        graphics2D.beginPath()
        graphics2D.moveTo(0f, 0f)

        // 最初のセグメント: 赤、太さ2.0f
        graphics2D.strokeStyle = StrokeStyle(0xFFFF0000.toInt(), 2.0f)
        graphics2D.lineTo(50f, 0f)

        // 2番目のセグメント: 緑、太さ5.0f
        graphics2D.strokeStyle = StrokeStyle(0xFF00FF00.toInt(), 5.0f)
        graphics2D.lineTo(75f, 50f)

        // 3番目のセグメント: 青、太さ8.0f
        graphics2D.strokeStyle = StrokeStyle(0xFF0000FF.toInt(), 8.0f)
        graphics2D.lineTo(25f, 100f)

        // 4番目のセグメント: 黄色、太さ3.0f
        graphics2D.strokeStyle = StrokeStyle(0xFFFFFF00.toInt(), 3.0f)
        graphics2D.lineTo(0f, 50f)

        // パスを閉じる: マゼンタ、太さ6.0f
        graphics2D.strokeStyle = StrokeStyle(0xFFFF00FF.toInt(), 6.0f)
        graphics2D.closePath()

        graphics2D.strokePath()
        graphics2D.restore()
        graphics2D.textStyle = TextStyle(false, 80f)
        graphics2D.fillStyle = 0xFFFFFFFF.toInt()
        graphics2D.textCentered("Hello, World", graphics2D.width / 2f, graphics2D.height / 2f)
        return graphics2D
    }
}
