package org.infinite.libs.graphics.text

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.render.state.GuiTextRenderState
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import org.infinite.UltimateClient
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager
import org.infinite.mixin.graphics.GuiGraphicsAccessor
import org.infinite.mixin.graphics.MinecraftAccessor
import org.joml.Matrix3x2f
import java.util.function.Consumer

class ModernTextRenderer(
    private val graphics: GuiGraphics,
    private val hoveredTextEffects: GuiGraphics.HoveredTextEffects,
    private val additionalConsumer: Consumer<Style>? = null,
) : ActiveTextCollector, Consumer<Style> {
    private fun createDefaultTextParameters(f: Float): ActiveTextCollector.Parameters {
        return ActiveTextCollector.Parameters(Matrix3x2f(graphics.pose()), f, graphics.scissorStack.peek())
    }

    private var params: ActiveTextCollector.Parameters = createDefaultTextParameters(1.0f)

    private val minecraft: Minecraft = Minecraft.getInstance()

    override fun defaultParameters(): ActiveTextCollector.Parameters = this.params

    override fun defaultParameters(parameters: ActiveTextCollector.Parameters) {
        this.params = parameters
    }

    private fun originalAccept(style: Style) {
        val accessor = graphics as GuiGraphicsAccessor

        if (this.hoveredTextEffects.allowTooltip && style.hoverEvent != null) {
            accessor.setHoveredTextStyle(style)
        }

        if (this.hoveredTextEffects.allowCursorChanges && style.clickEvent != null) {
            accessor.setClickableTextStyle(style)
        }

        this.additionalConsumer?.accept(style)
    }

    override fun accept(style: Style) {
        originalAccept(style)
        val ultimateFontFeature =
            UltimateClient.globalFeatures.rendering.ultimateFontFeature
        val shouldEnable = ultimateFontFeature.isEnabled()
        if (!shouldEnable) {
            return
        }
    }

    override fun accept(
        textAlignment: TextAlignment,
        x: Int,
        y: Int,
        parameters: ActiveTextCollector.Parameters,
        formattedCharSequence: FormattedCharSequence,
    ) {
        val ultimateFontFeature =
            UltimateClient.globalFeatures.rendering.ultimateFontFeature
        val shouldEnable = ultimateFontFeature.isEnabled()
        if (!shouldEnable) {
            originalAccept(textAlignment, x, y, parameters, formattedCharSequence)
            return
        }
        val minecraft = minecraft as MinecraftAccessor
        val accessor = graphics as GuiGraphicsAccessor
        val guiRenderState = accessor.getGuiRenderState()
        val fontManager = minecraft.fontManager as? IModernFontManager ?: return

        // 1. 本来のスタイルを取得して FontSet を選ぶ
        var originalStyle = Style.EMPTY
        formattedCharSequence.accept { _, style, _ ->
            originalStyle = style
            false
        }
        val fontSet = fontManager.`ultimate$fontSetFromStyle`(originalStyle)
        val font = fromFontSet(fontSet)

        val noBoldSequence = FormattedCharSequence { visitor ->
            formattedCharSequence.accept { index, style, codepoint ->
                visitor.accept(index, style.withBold(false), codepoint)
            }
        }

        val hasEffects =
            this.hoveredTextEffects.allowCursorChanges || this.hoveredTextEffects.allowTooltip || this.additionalConsumer != null

        val leftPos = textAlignment.calculateLeft(x, font, noBoldSequence)

        val renderState = GuiTextRenderState(
            font,
            noBoldSequence, // 太字フラグを消したシーケンスを渡す
            parameters.pose(),
            leftPos,
            y,
            ARGB.white(parameters.opacity()),
            0,
            true,
            hasEffects,
            parameters.scissor(),
        )

        if (ARGB.as8BitChannel(parameters.opacity()) != 0) {
            guiRenderState.submitText(renderState)
        }
        if (hasEffects) {
            ActiveTextCollector.findElementUnderCursor(
                renderState,
                Minecraft.getInstance().mouseHandler.xpos().toFloat(),
                Minecraft.getInstance().mouseHandler.ypos().toFloat(),
                this,
            )
        }
    }

    override fun acceptScrolling(
        component: Component,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        m: Int,
        parameters: ActiveTextCollector.Parameters,
    ) {
        val textWidth = minecraft.font.width(component)
        val lineHeight = 9
        this.defaultScrollingHelper(component, x, y, width, height, m, textWidth, lineHeight, parameters)
    }

    private fun originalAccept(
        textAlignment: TextAlignment,
        i: Int,
        j: Int,
        parameters: ActiveTextCollector.Parameters,
        formattedCharSequence: FormattedCharSequence,
    ) {
        val minecraft = minecraft as MinecraftAccessor
        val graphics = graphics as GuiGraphicsAccessor
        val font = minecraft.fontManager.createFont()
        val bl =
            this.hoveredTextEffects.allowCursorChanges || this.hoveredTextEffects.allowTooltip || this.additionalConsumer != null
        val k = textAlignment.calculateLeft(i, font, formattedCharSequence)
        val guiTextRenderState = GuiTextRenderState(
            font,
            formattedCharSequence,
            parameters.pose(),
            k,
            j,
            ARGB.white(parameters.opacity()),
            0,
            true,
            bl,
            parameters.scissor(),
        )
        if (ARGB.as8BitChannel(parameters.opacity()) != 0) {
            graphics.guiRenderState.submitText(guiTextRenderState)
        }

        if (bl) {
            ActiveTextCollector.findElementUnderCursor(
                guiTextRenderState,
                Minecraft.getInstance().mouseHandler.xpos().toFloat(),
                Minecraft.getInstance().mouseHandler.ypos().toFloat(),
                this,
            )
        }
    }
}
