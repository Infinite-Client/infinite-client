package org.infinite.libs.ui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.RenderSystem2D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.ui.widgets.AbstractCarouselWidget
import org.lwjgl.glfw.GLFW
import kotlin.math.*

abstract class AbstractCarouselScreen<T>(title: Component, private val parent: Screen? = null) : Screen(title) {
    override fun onClose() {
        minecraft.screen = parent
    }

    val currentWidget: AbstractCarouselWidget<T>
        get() = carouselWidgets[pageIndex]
    private var _pageIndex: Int = 0
    protected abstract val dataSource: List<T>
    val pageSize get() = dataSource.size

    private val screenWidth: Float
        get() = minecraft.window.guiScaledWidth.toFloat()
    private val screenHeight: Float
        get() = minecraft.window.guiScaledHeight.toFloat()
    val widgetHeight: Float
        get() = screenHeight * 0.8f
    private var animatedIndex: Float = 0f
    protected open val radius: Float
        get() {
            if (pageSize == 0) return 100f
            val baseRadius = screenWidth * 0.4f // 画面幅の40%を基本半径にする
            // 要素数が多い(例えば8個以上)場合に、重なりを防ぐため少し半径を大きくする
            val countFactor = (pageSize / 8f).coerceAtLeast(1.0f)
            return baseRadius * countFactor
        }

    private val focusZ: Float
        get() = screenWidth * 0.8f // 画面幅の80%程度の距離にカメラを置く
    val widgetWidth: Float
        get() {
            // 画面が狭いときは画面幅の90%、広いときは最大値(例: 600f)までに制限
            return (screenWidth * 0.9f).coerceAtMost(600f).coerceAtLeast(screenWidth * 0.4f)
        }
    protected open val lerpFactor = 0.35f
    protected val carouselWidgets = mutableListOf<AbstractCarouselWidget<T>>()

    var pageIndex: Int
        get() = _pageIndex
        set(value) {
            _pageIndex = if (pageSize == 0) 0 else (value % pageSize + pageSize) % pageSize
        }

    /**
     * 具象クラスで、DataSourceに基づいたWidgetのインスタンスを作成して返してください。
     */
    abstract fun createWidget(index: Int, data: T): AbstractCarouselWidget<T>

    override fun init() {
        super.init()
        carouselWidgets.clear()

        dataSource.withIndex().forEach { (index, data) ->
            val widget = createWidget(index, data)
            carouselWidgets.add(widget)
            this.addRenderableWidget(widget)
        }
    }

    data class WidgetFrameData(
        val x: Float,
        val y: Float,
        val z: Float,
        val scale: Float,
        val widgetWidth: Float,
        val widgetHeight: Float,
    )

    private fun calculateWidgetFrame(index: Int): WidgetFrameData {
        if (pageSize == 0) return WidgetFrameData(0f, 0f, 0f, 1f, 0f, 0f)

        val angle = 2 * PI.toFloat() * (index - animatedIndex) / pageSize
        val centerZ = focusZ + radius
        val worldX = sin(angle) * radius
        val worldZ = centerZ - cos(angle) * radius
        val scale = focusZ / worldZ
        val screenX = worldX / scale

        return WidgetFrameData(screenX, 0f, worldZ, scale, widgetWidth, widgetHeight)
    }

    class WidgetGraphics2D(
        data: WidgetFrameData,
        screenWidth: Float,
        screenHeight: Float,
    ) : Graphics2D() {
        override val width: Int = data.widgetWidth.roundToInt()
        override val height: Int = data.widgetHeight.roundToInt()

        init {
            this.push()
            this.translate(screenWidth / 2f, screenHeight / 2f)
            this.translate(data.x, data.y)
            this.scale(data.scale, data.scale)
            this.translate(-data.widgetWidth / 2f, -data.widgetHeight / 2f)
        }

        override fun commands(): List<RenderCommand2D> {
            this.pop()
            return super.commands()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // 1. アニメーション更新
        if (!isDragging) {
            // ドラッグ中でない時だけ pageIndex へ追従
            val target = pageIndex.toFloat()
            var diff = target - animatedIndex
            if (diff > pageSize / 2f) diff -= pageSize
            if (diff < -pageSize / 2f) diff += pageSize

            if (abs(diff) < 0.001f) {
                animatedIndex = target
            } else {
                animatedIndex += diff * lerpFactor
            }
        }

        val renderSystem2D = RenderSystem2D(guiGraphics)

        // 2. 各Widgetのフレーム計算と描画データの準備
        val renderData = carouselWidgets.map { widget ->
            val frame = calculateWidgetFrame(widget.thisIndex)
            widget.widgetFrameData = frame

            // 当たり判定・座標の更新
            val scaledWidth = (widgetWidth * frame.scale).toInt()
            val scaledHeight = (widgetHeight * frame.scale).toInt()
            widget.x = (screenWidth / 2f + frame.x - scaledWidth / 2f).toInt()
            widget.y = (screenHeight / 2f + frame.y - scaledHeight / 2f).toInt()
            widget.width = scaledWidth
            widget.height = scaledHeight

            // Widget固有の描画コマンド生成
            val g2d = WidgetGraphics2D(frame, screenWidth, screenHeight)
            val resultG2d = widget.render(g2d)

            // Z値, コマンド, Widget本体を保持
            RenderBundle(frame.z, resultG2d.commands(), widget)
        }

        // 3. 重なり順（Z-Order）のソート
        // Z値が大きい（＝奥にある）ものから順に描画（Painter's Algorithm）
        val sortedBundles = renderData.sortedByDescending { it.z }

        for (bundle in sortedBundles) {
            renderSystem2D.render(bundle.commands)
            bundle.widget.render(guiGraphics, mouseX, mouseY, delta)
        }
    }

    // 可読性のためのヘルパーデータクラス
    private data class RenderBundle<T>(
        val z: Float,
        val commands: List<RenderCommand2D>,
        val widget: AbstractCarouselWidget<T>,
    )

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        when (keyEvent.key) {
            GLFW.GLFW_KEY_RIGHT -> pageIndex++
            GLFW.GLFW_KEY_LEFT -> pageIndex--
            else -> return super.keyPressed(keyEvent)
        }
        return true
    }

    private val sortedWidgets
        get() = carouselWidgets.sortedBy { calculateWidgetFrame(it.thisIndex).z }

    override fun mouseMoved(x: Double, y: Double) {
        for (widget in sortedWidgets) {
            widget.mouseMoved(x, y)
        }
        super.mouseMoved(x, y)
    }

    private var isDragging = false
    private var dragVelocity = 1.0f // 慣性をつける場合に利用可能

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        for (widget in sortedWidgets) {
            if (widget.mouseClicked(mouseButtonEvent, bl)) {
                this.focused = widget
                return true
            }
        }

        // Widget以外をクリックした場合、ドラッグ開始
        isDragging = true
        return true
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        if (isDragging) {
            isDragging = false
            // ドラッグ終了時、現在のアニメーション位置に最も近いページをpageIndexに設定してスナップさせる
            pageIndex = animatedIndex.roundToInt()
        }

        for (widget in sortedWidgets) {
            if (widget.mouseReleased(mouseButtonEvent)) return true
        }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        // Widgetがドラッグを処理した場合はそちらを優先
        for (widget in sortedWidgets) {
            if (widget.mouseDragged(mouseButtonEvent, dragX, dragY)) {
                return true
            }
        }
        if (isDragging && pageSize > 0) {
            val sensitivity = dragVelocity / (screenWidth * 0.8f)
            val deltaIndex = dragX.toFloat() * sensitivity * pageSize

            // インデックスを更新（逆回転にする場合は -=）
            animatedIndex -= deltaIndex

            // 範囲内に収める（ループ処理）
            while (animatedIndex < 0) animatedIndex += pageSize
            while (animatedIndex >= pageSize) animatedIndex -= pageSize

            return true
        }
        return super.mouseDragged(mouseButtonEvent, dragX, dragY)
    }
}
