package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.system.QuadRenderer
import org.infinite.libs.graphics.graphics2d.system.RectRenderer
import org.infinite.libs.graphics.graphics2d.system.TriangleRenderer
import org.infinite.libs.log.LogSystem

class RenderSystem2D(
    gui: GuiGraphics,
) {
    private val rectRenderer: RectRenderer = RectRenderer(gui)
    private val quadRenderer: QuadRenderer = QuadRenderer(gui)
    private val triangleRenderer: TriangleRenderer = TriangleRenderer(gui)

    /**
     * Zインデックス順にソートされたコマンドリストを描画します。
     */
    fun render(commands: List<RenderCommand>) {
        commands.forEach { command ->
            executeCommand(command)
        }
    }

    private fun executeCommand(command: RenderCommand) {
        LogSystem.log("${command::class.java.simpleName}")
        when (command) {
            // --- Rectangle (矩形) ---
            is RenderCommand.StrokeRect -> {
                rectRenderer.strokeRect(
                    command.x,
                    command.y,
                    command.width,
                    command.height,
                    command.color,
                    command.strokeWidth,
                )
            }

            is RenderCommand.FillRect -> {
                rectRenderer.fillRect(
                    command.x,
                    command.y,
                    command.width,
                    command.height,
                    command.color,
                )
            }

            // --- Quad (四角形) ---
            is RenderCommand.FillQuad -> {
                quadRenderer.fillQuad(
                    command.x0, command.y0,
                    command.x1, command.y1,
                    command.x2, command.y2,
                    command.x3, command.y3,
                    command.col0, command.col1, command.col2, command.col3,
                )
            }

            is RenderCommand.StrokeQuad -> {
                quadRenderer.strokeQuad(
                    command.x0, command.y0,
                    command.x1, command.y1,
                    command.x2, command.y2,
                    command.x3, command.y3,
                    command.color,
                    command.strokeWidth,
                )
            }

            // --- Triangle (三角形) ---
            is RenderCommand.FillTriangle -> {
                triangleRenderer.fillTriangle(
                    command.x0, command.y0,
                    command.x1, command.y1,
                    command.x2, command.y2,
                    command.col0, command.col1, command.col2,
                )
            }

            is RenderCommand.StrokeTriangle -> {
                triangleRenderer.strokeTriangle(
                    command.x0,
                    command.y0,
                    command.x1,
                    command.y1,
                    command.x2,
                    command.y2,
                    command.color,
                    command.strokeWidth,
                )
            }
        }
    }
}
