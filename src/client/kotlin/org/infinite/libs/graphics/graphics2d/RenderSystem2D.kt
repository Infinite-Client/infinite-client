package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.system.QuadRenderer
import org.infinite.libs.graphics.graphics2d.system.RectRenderer
import org.infinite.libs.graphics.graphics2d.system.TriangleRenderer

class RenderSystem2D(
    gui: GuiGraphics,
) {
    private val rectRenderer: RectRenderer = RectRenderer(gui)
    private val quadRenderer: QuadRenderer = QuadRenderer(gui)
    private val triangleRenderer: TriangleRenderer = TriangleRenderer(gui)

    fun render(commands: List<RenderCommand>) {
        commands.forEach { executeCommand(it) }
    }

    private fun executeCommand(command: RenderCommand) {
        when (command) {
            // --- Rectangle (矩形) ---
            is RenderCommand.FillRect -> {
                // すべての色が同じなら単色版、そうでなければ個別色版を呼ぶ（引数で判別）
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

            is RenderCommand.StrokeRect -> {
                val isSingleIn = allEqual(command.col0, command.col1, command.col2, command.col3)
                val isSingleOut =
                    allEqual(command.col0, command.col1, command.col2, command.col3)

                if (isSingleIn && isSingleOut) {
                    rectRenderer.strokeRect(
                        command.x,
                        command.y,
                        command.width,
                        command.height,
                        command.col0,
                        command.strokeWidth,
                    )
                } else {
                    rectRenderer.strokeRect(
                        command.x, command.y, command.width, command.height,
                        command.col0, command.col1, command.col2, command.col3,
                        command.strokeWidth,
                    )
                }
            }

            // --- Quad (四角形) ---
            is RenderCommand.FillQuad -> {
                if (allEqual(command.col0, command.col1, command.col2, command.col3)) {
                    quadRenderer.fillQuad(
                        command.x0,
                        command.y0,
                        command.x1,
                        command.y1,
                        command.x2,
                        command.y2,
                        command.x3,
                        command.y3,
                        command.col0,
                    )
                } else {
                    quadRenderer.fillQuad(
                        command.x0, command.y0, command.x1, command.y1, command.x2, command.y2, command.x3, command.y3,
                        command.col0, command.col1, command.col2, command.col3,
                    )
                }
            }

            is RenderCommand.StrokeQuad -> {
                val isSingleIn = allEqual(command.col0, command.col1, command.col2, command.col3)
                val isSingleOut =
                    allEqual(command.col0, command.col1, command.col2, command.col3)
                if (isSingleIn && isSingleOut) {
                    quadRenderer.strokeQuad(
                        command.x0, command.y0, command.x1, command.y1, command.x2, command.y2, command.x3, command.y3,
                        command.col0, command.strokeWidth,
                    )
                } else {
                    quadRenderer.strokeQuad(
                        command.x0, command.y0, command.x1, command.y1, command.x2, command.y2, command.x3, command.y3,
                        command.col0, command.col1, command.col2, command.col3,
                        command.strokeWidth,
                    )
                }
            }

            // --- Triangle (三角形) ---
            is RenderCommand.FillTriangle -> {
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
                        command.x0, command.y0, command.x1, command.y1, command.x2, command.y2,
                        command.col0, command.col1, command.col2,
                    )
                }
            }

            is RenderCommand.StrokeTriangle -> {
                val isSingleIn = allEqual(command.col0, command.col1, command.col2)
                val isSingleOut = allEqual(command.col0, command.col1, command.col2)

                if (isSingleIn && isSingleOut) {
                    triangleRenderer.strokeTriangle(
                        command.x0,
                        command.y0,
                        command.x1,
                        command.y1,
                        command.x2,
                        command.y2,
                        command.col0,
                        command.strokeWidth,
                    )
                } else {
                    triangleRenderer.strokeTriangle(
                        command.x0, command.y0, command.x1, command.y1, command.x2, command.y2,
                        command.col0, command.col1, command.col2,
                        command.strokeWidth,
                    )
                }
            }
        }
    }

    private fun allEqual(vararg colors: Int): Boolean {
        if (colors.size <= 1) return true
        val first = colors[0]
        for (i in 1 until colors.size) {
            if (colors[i] != first) return false
        }
        return true
    }
}
