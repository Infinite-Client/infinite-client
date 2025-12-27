package org.infinite.libs.graphics.graphics2d

import net.minecraft.client.gui.GuiGraphics
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand
import org.infinite.libs.graphics.graphics2d.system.QuadRenderer
import org.infinite.libs.graphics.graphics2d.system.RectRenderer
import org.infinite.libs.graphics.graphics2d.system.TextRenderer
import org.infinite.libs.graphics.graphics2d.system.TriangleRenderer

class RenderSystem2D(
    private val gui: GuiGraphics, // 外部からアクセスするため val に変更
) {
    private val rectRenderer: RectRenderer = RectRenderer(gui)
    private val quadRenderer: QuadRenderer = QuadRenderer(gui)
    private val triangleRenderer: TriangleRenderer = TriangleRenderer(gui)
    private val textRenderer: TextRenderer = TextRenderer(gui)

    fun render(commands: List<RenderCommand>) {
        commands.forEach { executeCommand(it) }
    }

    private fun executeCommand(command: RenderCommand) {
        when (command) {
            // --- Transform (変換行列) ---
            is RenderCommand.SetTransform -> {
                // 現在のPoseStackを一度初期状態(Identity)に戻し、新しい行列を適用する
                // これにより Canvas API の setTransform(m) と同等の挙動になります
                val pose = gui.pose()
                pose.clear()
                // Matrix3x2f を Matrix4f に変換して適用
                val m = command.matrix
                pose.invert(m)
            }

            // --- Clipping (クリッピング) ---
            is RenderCommand.EnableScissor -> {
                // Minecraft標準のScissorを適用
                // x, y, width, height -> x, y, x2, y2 に変換して渡す
                gui.enableScissor(
                    command.x,
                    command.y,
                    command.x + command.width,
                    command.y + command.height,
                )
            }

            is RenderCommand.DisableScissor -> {
                gui.disableScissor()
            }

            // --- 既存の描画ロジック ---
            is RenderCommand.FillRect -> {
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
                        command.x0,
                        command.y0,
                        command.x1,
                        command.y1,
                        command.x2,
                        command.y2,
                        command.x3,
                        command.y3,
                        command.col0,
                        command.col1,
                        command.col2,
                        command.col3,
                    )
                }
            }

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
                        command.x0,
                        command.y0,
                        command.x1,
                        command.y1,
                        command.x2,
                        command.y2,
                        command.col0,
                        command.col1,
                        command.col2,
                    )
                }
            }

            is RenderCommand.Text -> {
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

            is RenderCommand.TextCentered -> {
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

    private fun allEqual(vararg colors: Int): Boolean {
        if (colors.size <= 1) return true
        val first = colors[0]
        return colors.all { it == first }
    }
}
