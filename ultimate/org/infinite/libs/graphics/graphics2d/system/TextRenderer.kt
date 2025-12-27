package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.state.GuiTextRenderState
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager
import org.infinite.libs.graphics.text.fromFontSet
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.mixin.graphics.MinecraftAccessor
import org.joml.Matrix3x2f

class TextRenderer(private val guiGraphics: GuiGraphics) : MinecraftInterface() {
    private fun font(name: String): Font {
        val client = client as MinecraftAccessor
        val fontManager = client.fontManager as IModernFontManager
        val fontSet = fontManager.`ultimate$fontSetFromIdentifier`(name)
        return fromFontSet(fontSet)
    }

    fun text(font: String, text: String, x: Float, y: Float, color: Int, size: Float = 8.0f, shadow: Boolean = false) {
        val poseStack = guiGraphics.pose()
        val font = font(font)
        poseStack.pushMatrix()

        poseStack.translate(x, y)
        val fontSize = size / client.font.lineHeight
        poseStack.scale(fontSize, fontSize)

        // 描画（座標は0, 0でOK）
        val formattedCharSequence = Language.getInstance().getVisualOrder(FormattedText.of(text))
        guiGraphics.guiRenderState.submitText(
            GuiTextRenderState(
                font,
                formattedCharSequence,
                Matrix3x2f(guiGraphics.pose()),
                0,
                0,
                color,
                0,
                shadow,
                false,
                guiGraphics.scissorStack.peek(),
            ),
        )
        poseStack.popMatrix()
    }

    fun textCentered(
        font: String,
        text: String,
        x: Float,
        y: Float,
        color: Int,
        size: Float = 8.0f,
        shadow: Boolean = false,
    ) {
        val fontStr = font
        val font = font(fontStr)
        val scale = size / client.font.lineHeight
        val width = font.width(text) * scale
        text(fontStr, text, x - width / 2, y - size / 2, color, size, shadow)
    }
}
