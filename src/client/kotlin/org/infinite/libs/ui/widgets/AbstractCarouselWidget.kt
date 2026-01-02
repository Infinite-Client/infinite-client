package org.infinite.libs.ui.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.libs.ui.screen.AbstractCarouselScreen

abstract class AbstractCarouselWidget<T>(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val data: T,
    val parent: AbstractCarouselScreen<T>,
    val thisIndex: Int,
    title: Component,
) : AbstractContainerWidget(x, y, width, height, title) {
    var widgetFrameData: AbstractCarouselScreen.WidgetFrameData? = null
    protected val children = mutableListOf<Renderable>()
    override fun children(): List<GuiEventListener> = children.filterIsInstance<GuiEventListener>()

    /**
     * 描画時の行列変換
     */
    private fun GuiGraphics.carouselTransform() {
        val frame = widgetFrameData ?: return
        val minecraft = Minecraft.getInstance()
        val screenWidth = minecraft.window.guiScaledWidth
        val screenHeight = minecraft.window.guiScaledHeight

        val pose = this.pose()
        pose.pushMatrix()

        // 1. 画面中央へ移動
        pose.translate(screenWidth / 2f, screenHeight / 2f)
        // 2. カルーセルの配置座標へ移動
        pose.translate(frame.x, frame.y)
        // 3. スケーリング
        pose.scale(frame.scale, frame.scale)
        // 4. ウィジェットの中心を描画の基準点にするためのオフセット
        pose.translate(-frame.widgetWidth / 2f, -frame.widgetHeight / 2f)
    }

    /**
     * マウス座標をスクリーン座標から「ウィジェット内の相対座標」へ変換する
     * 描画時と逆のステップを、逆順で、逆の操作として適用します。
     */
    private fun Pair<Double, Double>.carouselInverseTransform(): Pair<Double, Double> {
        val frame = widgetFrameData ?: return this
        val minecraft = Minecraft.getInstance()
        val screenWidth = minecraft.window.guiScaledWidth.toDouble()
        val screenHeight = minecraft.window.guiScaledHeight.toDouble()

        var curX = this.first
        var curY = this.second

        // --- 描画時の逆順で処理 ---

        // 1. 画面中央移動の逆
        curX -= screenWidth / 2.0
        curY -= screenHeight / 2.0

        // 2. フレーム移動の逆
        curX -= frame.x.toDouble()
        curY -= frame.y.toDouble()

        // 3. スケーリングの逆
        if (frame.scale != 0f) {
            curX /= frame.scale.toDouble()
            curY /= frame.scale.toDouble()
        }

        // 4. 中心オフセットの逆 (描画時がマイナスなのでプラスに戻す)
        curX += frame.widgetWidth / 2.0
        curY += frame.widgetHeight / 2.0

        return curX to curY
    }

    private fun MouseButtonEvent.carouselTransform(): MouseButtonEvent {
        val (tx, ty) = (this.x to this.y).carouselInverseTransform()
        return MouseButtonEvent(tx, ty, this.buttonInfo)
    }

    // --- 各種イベントのオーバーライド ---

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        // マウス座標をウィジェットのローカル座標系に変換してから判定
        val (localX, localY) = (mouseX to mouseY).carouselInverseTransform()

        // AbstractWidgetは本来(this.x, this.y)を基準にするが、
        // 変換後の座標は(0, 0)から(width, height)の範囲にあるはずなので
        // ウィジェット自身の座標オフセットを加味して判定
        return active && visible &&
            localX >= this.x && localX < (this.x + this.width) &&
            localY >= this.y && localY < (this.y + this.height)
    }

    public final override fun renderWidget(guiGraphics: GuiGraphics, x: Int, y: Int, delta: Float) {
        guiGraphics.carouselTransform()

        // 内部ウィジェットの描画（既に座標系がズレているので(x,y)は0,0基準で渡すのが一般的）
        for (child in children) {
            child.render(guiGraphics, x, y, delta)
        }

        guiGraphics.pose().popMatrix()
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        // 現在のウィジェットが最前面(current)でない場合はイベントを無視するなどの制御
        if (parent.currentWidget != this) return false

        // 変換後のイベントを渡す
        val transformedEvent = mouseButtonEvent.carouselTransform()
        return super.mouseClicked(transformedEvent, bl)
    }

    // 他のイベントも同様に .carouselTransform() を適用
    override fun mouseMoved(d: Double, e: Double) {
        val (tx, ty) = (d to e).carouselInverseTransform()
        super.mouseMoved(tx, ty)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        val transformedEvent = mouseButtonEvent.carouselTransform()
        val (tx, ty) = (d to e).carouselInverseTransform()
        return super.mouseDragged(transformedEvent, tx, ty)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        return super.mouseReleased(mouseButtonEvent.carouselTransform())
    }

    // --- 既存の abstract / メソッド ---
    abstract fun render(graphics2D: AbstractCarouselScreen.WidgetGraphics2D): AbstractCarouselScreen.WidgetGraphics2D
    abstract fun onSelected(data: T)

    override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
        parent.pageIndex = thisIndex
        onSelected(data)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        if (verticalAmount > 0) parent.pageIndex-- else if (verticalAmount < 0) parent.pageIndex++
        return true
    }

    override fun contentHeight(): Int = this.height
    override fun scrollRate(): Double = 10.0
    protected fun <R : Renderable> addInnerWidget(widget: R): R {
        children.add(widget)
        return widget
    }
}
