package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.system.RectRenderer

class RenderSystem2D(
    gui: GuiGraphics,
) {
    private val rect: RectRenderer = RectRenderer(gui)

    fun render(commands: List<RenderCommand>) {
        // リストはソート済みとのことですので、そのまま順番に描画します
        commands.forEach { command ->
            command(command)
        }
    }

    private fun command(command: RenderCommand) {
        when (command) {
            // --- DrawRect (枠線) ---
            is RenderCommand.DrawRectInt -> {
                rect.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }

            is RenderCommand.DrawRectFloat -> {
                rect.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }

            is RenderCommand.DrawRectDouble -> {
                rect.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }

            // --- FillRect (塗りつぶし) ---
            is RenderCommand.FillRectInt -> {
                rect.fillRect(command.x, command.y, command.width, command.height, command.color)
            }

            is RenderCommand.FillRectFloat -> {
                rect.fillRect(command.x, command.y, command.width, command.height, command.color)
            }

            is RenderCommand.FillRectDouble -> {
                rect.fillRect(command.x, command.y, command.width, command.height, command.color)
            }
        }
    }
}
