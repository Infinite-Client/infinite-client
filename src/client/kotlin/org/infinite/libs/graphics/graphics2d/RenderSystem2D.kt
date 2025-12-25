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
                // `allEqual` 判定を削除し、常に多色版を呼び出す
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

            // --- Quad (四角形) ---
            is RenderCommand.FillQuad -> {
                // `allEqual` 判定を削除し、常に多色版を呼び出す
                quadRenderer.fillQuad(
                    command.x0, command.y0, command.x1, command.y1, command.x2, command.y2, command.x3, command.y3,
                    command.col0, command.col1, command.col2, command.col3,
                )
            }

            // --- Triangle (三角形) ---
            is RenderCommand.FillTriangle -> {
                // `allEqual` 判定を削除し、常に多色版を呼び出す
                triangleRenderer.fillTriangle(
                    command.x0, command.y0, command.x1, command.y1, command.x2, command.y2,
                    command.col0, command.col1, command.col2,
                )
            }
        }
    }
}
