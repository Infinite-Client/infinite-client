package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.graphics.graphics2d.system.*

class RenderSystem2D(
    private val gui: GuiGraphics,
) {
    private val rectRenderer = RectRenderer(gui)
    private val quadRenderer = QuadRenderer(gui)
    private val triangleRenderer = TriangleRenderer(gui)
    private val textRenderer = TextRenderer(gui)
    private val textureRenderer = TextureRenderer(gui) // 追加
    private val itemRenderer = ItemRenderer(gui) // 追加
    private val blockRenderer = BlockRenderer(gui)

    fun render(commands: List<RenderCommand2D>) {
        for (i in 0 until commands.size) {
            val command = commands[i]
            executeCommand(command)
        }
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
                gui.pose().transform(command.vec)
            }

            is RenderCommand2D.ResetTransform -> {
                gui.pose().identity()
            }

            is RenderCommand2D.SetTransform -> {
                gui.pose().set(command.matrix)
            }

            is RenderCommand2D.EnableScissor -> {
                gui.enableScissor(command.x, command.y, command.x + command.width, command.y + command.height)
            }

            is RenderCommand2D.DisableScissor -> gui.disableScissor()

            // --- 移譲 (Delegation) ---
            is RenderCommand2D.DrawTexture -> textureRenderer.drawTexture(command)

            is RenderCommand2D.RenderItem -> itemRenderer.drawItem(command)

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

            is RenderCommand2D.AbstractText -> {
                when (command) {
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

                    is RenderCommand2D.TextRight -> {
                        textRenderer.textRight(
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

            is RenderCommand2D.RenderBlock -> {
                blockRenderer.block(command.block!!, command.x, command.y, command.size)
            }
        }
    }

    private fun allEqual(vararg colors: Int): Boolean = colors.size <= 1 || colors.all { it == colors[0] }
}
