package org.infinite.infinite.features.local.rendering.hello

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.BooleanProperty
import org.infinite.libs.core.features.property.StringProperty
import org.infinite.libs.core.features.property.list.ItemListProperty
import org.infinite.libs.core.features.property.number.DoubleProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.number.LongProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.structs.StrokeStyle
import org.infinite.libs.graphics.graphics2d.structs.TextStyle
import org.infinite.libs.log.LogSystem
import org.lwjgl.glfw.GLFW
import kotlin.math.cos
import kotlin.math.sin

class HelloFeature : LocalFeature() {
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_F

    @Suppress("Unused")
    val booleanProperty by property(BooleanProperty(false))

    @Suppress("Unused")
    val intProperty by property(IntProperty(5, 0, 10))

    @Suppress("Unused")
    val longProperty by property(LongProperty(5L, 0L, 10L, "L"))

    @Suppress("Unused")
    val floatProperty by property(FloatProperty(5f, 0f, 10f, "f"))

    @Suppress("Unused")
    val doubleProperty by property(DoubleProperty(5.0, 5.0, 10.0, "d"))

    @Suppress("Unused")
    val stringProperty by property(StringProperty("Hello World"))

    @Suppress("Unused")
    enum class TestEnum {
        A,
        B,
        C,
        D,
    }

    @Suppress("Unused")
    val selectionProperty by property(EnumSelectionProperty<TestEnum>(TestEnum.A))

    @Suppress("Unused")
    val itemListProperty by property(ItemListProperty(listOf()))

    init {
        enable()
    }

    override fun onConnected() {
        LogSystem.log("HelloFeature Connected!")
    }

    override fun onEndUiRendering(graphics2D: Graphics2D) {
        // --- 1. 基本図形 ---
        graphics2D.fillStyle = 0x80FF0000.toInt()
        graphics2D.fillRect(10f, 10f, 100f, 50f)
        graphics2D.strokeStyle = StrokeStyle(0xFFFFFFFF.toInt(), 2.0f)
        graphics2D.strokeRect(10f, 10f, 100f, 50f)

        // --- 2. グラデーション三角形 ---
        graphics2D.fillTriangle(
            150f, 20f, 120f, 80f, 180f, 80f,
            0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
        )
        graphics2D.strokeStyle = StrokeStyle(0xFF000000.toInt(), 1.0f)
        graphics2D.strokeTriangle(150f, 20f, 120f, 80f, 180f, 80f)

        // --- 3. 動的な回転三角形 ---
        val time = (System.currentTimeMillis() % 10000) / 1000.0
        val centerX = 200.0
        val centerY = 150.0
        val radius = 40.0

        val x0 = (centerX + radius * cos(time)).toFloat()
        val y0 = (centerY + radius * sin(time)).toFloat()
        val x1 = (centerX + radius * cos(time + 2.094)).toFloat()
        val y1 = (centerY + radius * sin(time + 2.094)).toFloat()
        val x2 = (centerX + radius * cos(time + 4.188)).toFloat()
        val y2 = (centerY + radius * sin(time + 4.188)).toFloat()

        graphics2D.fillStyle = 0xFFFFA500.toInt()
        graphics2D.fillTriangle(x0, y0, x1, y1, x2, y2)

        // --- 4. 座標変換のテスト (安全な save/restore) ---

        // Arc テスト
        graphics2D.push()
        try {
            graphics2D.translate(50f, 300f)
            graphics2D.strokeStyle = StrokeStyle(0xFF00FF00.toInt(), 3.0f)
            graphics2D.beginPath()
            graphics2D.arc(0f, 0f, 30f, 0f, (Math.PI * 1.5).toFloat(), false)
            graphics2D.strokePath()
        } finally {
            graphics2D.pop()
        }

        // Bezier テスト
        graphics2D.push()
        try {
            graphics2D.translate(250f, 300f)
            graphics2D.strokeStyle = StrokeStyle(0xFFFF00FF.toInt(), 3.0f)
            graphics2D.beginPath()
            graphics2D.moveTo(0f, 0f)
            graphics2D.bezierCurveTo(20f, -50f, 80f, 50f, 100f, 0f)
            graphics2D.strokePath()
        } finally {
            graphics2D.pop()
        }

        // Transform (行列演算) テスト
        graphics2D.push()
        try {
            graphics2D.translate(400f, 300f)
            graphics2D.transform(1.2f, 0.2f, 0.1f)
            graphics2D.fillStyle = 0x80FFFF00.toInt()
            graphics2D.fillRect(0f, 0f, 50f, 50f)
        } finally {
            graphics2D.pop()
        }

        // --- 5. アイテム描画テスト ---
        val player = player ?: return
        val stack = if (!player.mainHandItem.isEmpty) {
            player.mainHandItem
        } else {
            net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)
        }

        graphics2D.push()
        try {
            graphics2D.translate(100f, 400f)
            graphics2D.rotateDegrees((time * 45).toFloat()) // 時間で回転
            graphics2D.itemCentered(stack, 0f, 0f, 32f)
        } finally {
            graphics2D.pop()
        }

        // --- 6. Scissor (クリッピング) テスト ---
        graphics2D.push()
        try {
            val sX = 50
            val sY = 150
            val sW = 100
            val sH = 100
            // ガイド枠
            graphics2D.fillStyle = 0x20FFFFFF
            graphics2D.fillRect(sX.toFloat(), sY.toFloat(), sW.toFloat(), sH.toFloat())

            graphics2D.enableScissor(sX, sY, sW, sH)

            // クリップ内を動く矩形
            val moveX = sX + (time.toFloat() * 50 % 150) - 25f
            graphics2D.fillStyle = 0xFFFF0000.toInt()
            graphics2D.fillRect(moveX, sY + 20f, 30f, 30f)

            graphics2D.disableScissor()
        } finally {
            graphics2D.pop()
        }

        // --- 7. テキスト描画 ---
        graphics2D.textStyle = TextStyle(shadow = true, size = 20f)
        graphics2D.fillStyle = 0xFFFFFFFF.toInt()
        graphics2D.text("Hello World", 10f, graphics2D.height - 20f)

        // --- 8. ワールド座標の投影 ---
        val pos = player.getPosition(graphics2D.gameDelta)
        graphics2D.projectWorldToScreen(pos)?.let { (screenX, screenY) ->
            graphics2D.push()
            graphics2D.fillStyle = 0xFF00FF00.toInt()
            graphics2D.textCentered("YOU", screenX.toFloat(), screenY.toFloat() - 10f)
            graphics2D.pop()
        }
        graphics2D.push()
        try {
            graphics2D.translate(50f, 500f)
            graphics2D.enablePathGradient = true
            graphics2D.beginPath()
            graphics2D.moveTo(0f, 0f)
            graphics2D.strokeStyle = StrokeStyle(0xFFFF0000.toInt(), 2f)
            graphics2D.lineTo(50f, 0f)
            graphics2D.strokeStyle = StrokeStyle(0xFF00FF00.toInt(), 5f)
            graphics2D.lineTo(75f, 50f)
            graphics2D.strokeStyle = StrokeStyle(0xFF0000FF.toInt(), 8f)
            graphics2D.lineTo(25f, 100f)
            graphics2D.closePath()
            graphics2D.strokePath()
        } finally {
            graphics2D.pop()
        }
    }
}
