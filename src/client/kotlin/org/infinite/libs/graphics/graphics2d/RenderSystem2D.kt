package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.system.RectRenderer

class RenderSystem2D(
    private val gui: GuiGraphics,
) {
    private val rect: RectRenderer = RectRenderer(gui)

    fun render(commands: List<RenderCommand>) {
        commands.forEach { command ->
            command(command)
        }
    }

    private fun command(command: RenderCommand) {
        when (command) {
            is RenderCommand.DrawRectInt -> {
                rect.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }

            is RenderCommand.DrawRectFloat -> {
                rect.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }

            is RenderCommand.DrawRectDouble -> {
                rect.strokeRect(command.x, command.y, command.width, command.height, command.color, command.strokeWidth)
            }
        }
    }
}
