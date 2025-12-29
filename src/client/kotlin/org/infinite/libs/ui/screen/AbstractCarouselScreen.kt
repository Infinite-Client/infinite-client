package org.infinite.libs.ui.screen

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.graphics.graphics2d.RenderSystem2D
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.ui.widgets.AbstractCarouselWidget
import org.lwjgl.glfw.GLFW
import kotlin.math.*

abstract class AbstractCarouselScreen<T>(title: Component) : Screen(title) {

    private var _pageIndex: Int = 0
    protected abstract val dataSource: List<T>
    protected val pageSize get() = dataSource.size

    private var animatedIndex: Float = 0f
    private val lerpFactor = 0.5f

    protected open val radius: Float get() = categoryWidgets.size * 100f
    protected val categoryWidgets = mutableListOf<AbstractCarouselWidget<T>>()

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
        categoryWidgets.clear()

        dataSource.withIndex().forEach { (index, data) ->
            val widget = createWidget(index, data)
            categoryWidgets.add(widget)
            this.addRenderableWidget(widget)
        }
    }

    data class WidgetFrameData(val x: Float, val y: Float, val z: Float, val scale: Float)

    private val focusZ = 4000f
    private fun calculateWidgetFrame(index: Int): WidgetFrameData {
        if (pageSize == 0) return WidgetFrameData(0f, 0f, 0f, 1f)

        val angle = 2 * PI.toFloat() * (index - animatedIndex) / pageSize
        val centerZ = focusZ + radius
        val worldX = sin(angle) * radius
        val worldZ = centerZ - cos(angle) * radius
        val scale = focusZ / worldZ
        val screenX = worldX / scale

        return WidgetFrameData(screenX, 0f, worldZ, scale)
    }

    class WidgetGraphics2D(
        deltaTracker: DeltaTracker,
        data: WidgetFrameData,
        screenWidth: Float,
        screenHeight: Float,
        widgetWidth: Float,
        widgetHeight: Float,
    ) : Graphics2D(deltaTracker) {
        override val width: Int = widgetWidth.roundToInt()
        override val height: Int = widgetHeight.roundToInt()

        init {
            this.save()
            this.translate(screenWidth / 2f, screenHeight / 2f)
            this.translate(data.x, data.y)
            this.scale(data.scale, data.scale)
            this.translate(-widgetWidth / 2f, -widgetHeight / 2f)
        }

        override fun commands(): List<RenderCommand2D> {
            this.restore()
            return super.commands()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // アニメーション更新
        val target = pageIndex.toFloat()
        var diff = target - animatedIndex
        if (diff > pageSize / 2f) diff -= pageSize
        if (diff < -pageSize / 2f) diff += pageSize

        if (abs(diff) < 0.001f) {
            animatedIndex = target
        } else {
            animatedIndex += diff * lerpFactor
        }

        val renderSystem2D = RenderSystem2D(guiGraphics)
        val sw = minecraft!!.window.guiScaledWidth.toFloat()
        val sh = minecraft!!.window.guiScaledHeight.toFloat()

        // 描画サイズの計算（必要に応じてオーバーライド可能にする）
        val w = (sw * 0.5f).coerceAtLeast(512f).coerceAtMost(sw * 0.9f)
        val h = sh * 0.8f

        val bundles = categoryWidgets.map { widget ->
            val frame = calculateWidgetFrame(widget.thisIndex)
            val g2d = WidgetGraphics2D(minecraft!!.deltaTracker, frame, sw, sh, w, h)
            val resultG2d = widget.renderCustom(g2d)
            frame.z to resultG2d.commands()
        }

        val sortedCommands = bundles
            .sortedByDescending { it.first }
            .flatMap { it.second }

        renderSystem2D.render(sortedCommands)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        when (keyEvent.key) {
            GLFW.GLFW_KEY_RIGHT -> pageIndex++
            GLFW.GLFW_KEY_LEFT -> pageIndex--
            else -> return super.keyPressed(keyEvent)
        }
        return true
    }
}
