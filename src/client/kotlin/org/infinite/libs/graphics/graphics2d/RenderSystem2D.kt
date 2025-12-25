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
    private val triangleRenderer: TriangleRenderer = TriangleRenderer(gui) // 型を修正

    fun render(commands: List<RenderCommand>) {
        // zIndex順にソートされている前提で描画を実行
        commands.forEach { command ->
            command(command)
        }
    }

    private fun command(command: RenderCommand) {
        when (command) {
            // --- DrawRect (枠線) ---
            is RenderCommand.DrawRectInt -> {
                rectRenderer.strokeRect(command.x.toFloat(), command.y.toFloat(), command.width.toFloat(), command.height.toFloat(), command.color, command.strokeWidth.toFloat())
            }
            is RenderCommand.DrawRectFloat -> {
                rectRenderer.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }
            is RenderCommand.DrawRectDouble -> {
                rectRenderer.strokeRect(command.x.toFloat(), command.y.toFloat(), command.width.toFloat(), command.height.toFloat(), command.color, command.strokeWidth.toFloat())
            }

            // --- FillRect (塗りつぶし) ---
            is RenderCommand.FillRectInt -> {
                rectRenderer.fillRect(command.x.toFloat(), command.y.toFloat(), command.width.toFloat(), command.height.toFloat(), command.color)
            }
            is RenderCommand.FillRectFloat -> {
                rectRenderer.fillRect(command.x, command.y, command.width, command.height, command.color)
            }
            is RenderCommand.FillRectDouble -> {
                rectRenderer.fillRect(command.x.toFloat(), command.y.toFloat(), command.width.toFloat(), command.height.toFloat(), command.color)
            }

            // --- FillQuad (4点塗りつぶし) ---
            is RenderCommand.FillQuadFloat -> {
                quadRenderer.fillQuad(
                    command.x0, command.y0, command.x1, command.y1,
                    command.x2, command.y2, command.x3, command.y3,
                    command.col0, command.col1, command.col2, command.col3
                )
            }
            is RenderCommand.FillQuadDouble -> {
                quadRenderer.fillQuad(
                    command.x0.toFloat(), command.y0.toFloat(), command.x1.toFloat(), command.y1.toFloat(),
                    command.x2.toFloat(), command.y2.toFloat(), command.x3.toFloat(), command.y3.toFloat(),
                    command.col0, command.col1, command.col2, command.col3
                )
            }

            // --- FillTriangle (三角形塗りつぶし) ---
            is RenderCommand.FillTriangleFloat -> {
                triangleRenderer.fillTriangle(
                    command.x0, command.y0, command.x1, command.y1, command.x2, command.y2,
                    command.col0, command.col1, command.col2
                )
            }
            is RenderCommand.FillTriangleDouble -> {
                triangleRenderer.fillTriangle(
                    command.x0.toFloat(), command.y0.toFloat(), command.x1.toFloat(), command.y1.toFloat(),
                    command.x2.toFloat(), command.y2.toFloat(),
                    command.col0, command.col1, command.col2
                )
            }
        }
    }
}