package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.world.item.ItemDisplayContext
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D
import org.infinite.libs.interfaces.MinecraftInterface

class ItemRenderer(private val gui: GuiGraphics) : MinecraftInterface() {

// ItemRenderer.kt

    fun drawItem(cmd: RenderCommand2D.RenderItem) {
        val stack = cmd.stack
        if (stack.isEmpty) return
        val x = cmd.x
        val y = cmd.y
        val level = level ?: return
        val keyedItemRenderState = TrackingItemStackRenderState()
        this.minecraft.itemModelResolver.updateForTopItem(
            keyedItemRenderState,
            stack,
            ItemDisplayContext.FIXED,
            level,
            player,
            0,
        )
        val pose = gui.pose()
        try {
            // アイテム本体のみを submit する
            pose.pushMatrix()
            pose.translate(x, y)
            pose.scale(cmd.scale, cmd.scale)
            gui.renderItem(stack, 0, 0)
            pose.popMatrix()
        } catch (throwable: Throwable) {
            throw throwable
        }
    }
}
