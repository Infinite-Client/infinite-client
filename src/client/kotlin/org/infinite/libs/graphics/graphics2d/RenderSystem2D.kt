package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics2d.system.ItemRenderer
import org.infinite.libs.graphics.graphics2d.system.QuadRenderer
import org.infinite.libs.graphics.graphics2d.system.RectRenderer
import org.infinite.libs.graphics.graphics2d.system.TextRenderer
import org.infinite.libs.graphics.graphics2d.system.TextureRenderer
import org.infinite.libs.graphics.graphics2d.system.TriangleRenderer

class RenderSystem2D(
    private val gui: GuiGraphics,
) {
    private val rectRenderer = RectRenderer(gui)
    private val quadRenderer = QuadRenderer(gui)
    private val triangleRenderer = TriangleRenderer(gui)
    private val textRenderer = TextRenderer(gui)
    private val textureRenderer = TextureRenderer(gui) // 追加
    private val itemRenderer = ItemRenderer(gui) // 追加

    fun render(commands: List<RenderCommand2D>) {
        commands.forEach { executeCommand(it) }
    }

    private fun executeCommand(command: RenderCommand2D) {
        when (command) {
            is RenderCommand2D.PushTransform -> {
                gui.pose().pushMatrix()
            }

            is RenderCommand2D.PopTransform -> {
                gui.pose().popMatrix()
            }

            is RenderCommand2D.Translate -> {
                gui.pose().translate(command.x, command.y)
            }

            is RenderCommand2D.Rotate -> {
                // JOMLのMatrix3x2f等で扱う回転（Z軸回転）を適用
                gui.pose().rotate(command.angle)
            }

            is RenderCommand2D.Scale -> {
                gui.pose().scale(command.x, command.y)
            }

            is RenderCommand2D.Transform -> {
//                gui.pose().transform(command.m00, command.m10, command.m01, command.m11, command.m02, command.m12)
            }

            is RenderCommand2D.ResetTransform -> {
                gui.pose().identity()
            }

            is RenderCommand2D.SetTransform -> {
                gui.pose().mul(command.matrix)
            }

            is RenderCommand2D.EnableScissor -> {
                gui.enableScissor(command.x, command.y, command.x + command.width, command.y + command.height)
            }

            is RenderCommand2D.DisableScissor -> gui.disableScissor()

            // --- 移譲 (Delegation) ---
            is RenderCommand2D.DrawTexture -> textureRenderer.drawTexture(command)
            is RenderCommand2D.DrawItem -> itemRenderer.drawItem(command)

            is RenderCommand2D.FillRect -> {
                if (allEqual(command.col0, command.col1, command.col2, command.col3)) {
                    rectRenderer.fillRect(command.x, command.y, command.width, command.height, command.col0)
                } else {
                    rectRenderer.fillRect(
                        command.x,
                        command.y,
                        command.width,
                        command.height,
                        command.col0,
                        command.col1,
                        command.col2,
                        command.col3,
                    )
                }
            }

            is RenderCommand2D.FillQuad -> {
                if (allEqual(command.col0, command.col1, command.col2, command.col3)) {
                    quadRenderer.fillQuad(
                        command.x0, command.y0, command.x1, command.y1,
                        command.x2, command.y2, command.x3, command.y3, command.col0,
                    )
                } else {
                    quadRenderer.fillQuad(
                        command.x0, command.y0, command.x1, command.y1,
                        command.x2, command.y2, command.x3, command.y3,
                        command.col0, command.col1, command.col2, command.col3,
                    )
                }
            }

            is RenderCommand2D.FillTriangle -> {
                if (allEqual(command.col0, command.col1, command.col2)) {
                    triangleRenderer.fillTriangle(
                        command.x0,
                        command.y0,
                        command.x1,
                        command.y1,
                        command.x2,
                        command.y2,
                        command.col0,
                    )
                } else {
                    triangleRenderer.fillTriangle(
                        command.x0, command.y0, command.x1, command.y1,
                        command.x2, command.y2,
                        command.col0, command.col1, command.col2,
                    )
                }
            }

            is RenderCommand2D.Text -> {
                textRenderer.text(
                    command.font,
                    command.text,
                    command.x,
                    command.y,
                    command.color,
                    command.size,
                    command.shadow,
                )
            }

            is RenderCommand2D.TextCentered -> {
                textRenderer.textCentered(
                    command.font,
                    command.text,
                    command.x,
                    command.y,
                    command.color,
                    command.size,
                    command.shadow,
                )
            }
        }
    }

    private fun allEqual(vararg colors: Int): Boolean = colors.size <= 1 || colors.all { it == colors[0] }
}
