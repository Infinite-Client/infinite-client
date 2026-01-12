package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.block.model.BlockModelPart
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.infinite.InfiniteClient

class BlockRenderer(private val gui: GuiGraphics) {
    private val random = RandomSource.create()

    fun block(block: Block, x: Float, y: Float, size: Float) {
        val mc = Minecraft.getInstance()
        val itemStack = ItemStack(block.asItem())
        val pose = gui.pose()

        if (!itemStack.isEmpty) {
            pose.pushMatrix()
            pose.translate(x, y)
            pose.scale(size / 16f, size / 16f)
            gui.renderItem(itemStack, 0, 0)
            pose.popMatrix()
        } else {
            val model = mc.blockRenderer.getBlockModel(block.defaultBlockState())
            val parts = mutableListOf<BlockModelPart>()
            model.collectParts(random, parts)

            val sprite = if (parts.isNotEmpty()) {
                parts.first().getQuads(Direction.UP).firstOrNull()?.sprite()
                    ?: parts.first().particleIcon()
            } else {
                model.particleIcon()
            }
            val color = if (block == Blocks.WATER) {
                val aqua = 210f
                InfiniteClient.theme.colorScheme.color(aqua, 1f, 0.5f, 1f)
            } else {
                InfiniteClient.theme.colorScheme.whiteColor
            }
            val contents = sprite.contents()
            val spriteW = contents.width().toFloat()
            val spriteH = contents.height().toFloat()

            // --- 重要：アトラスサイズの逆算 ---
            // sprite.u0 = x / atlasWidth なので、atlasWidth = x / sprite.u0
            // ただし x=0 の場合に備え、u1 と u0 の差（スプライトの幅の比率）から計算するのが最も安全です。
            val uRange = sprite.u1 - sprite.u0
            val vRange = sprite.v1 - sprite.v0

            val totalAtlasW = spriteW / uRange
            val totalAtlasH = spriteH / vRange

            pose.pushMatrix()
            pose.translate(x, y)

            // スケーリング：指定サイズをスプライトのピクセル数で割る
            pose.scale(size / spriteW, size / spriteH)

            // TextureRenderer の仕様に合わせ、ピクセル単位で blit を実行
            gui.blit(
                RenderPipelines.GUI_TEXTURED,
                sprite.atlasLocation(),
                0, 0,
                sprite.u0 * totalAtlasW, // これでアトラス内の正しいピクセル開始位置になる
                sprite.v0 * totalAtlasH,
                spriteW.toInt(), // 切り出す幅 (16など)
                spriteH.toInt(), // 切り出す高さ
                spriteW.toInt(),
                spriteH.toInt(),
                totalAtlasW.toInt(), // アトラス全体のピクセル幅 (1024など)
                totalAtlasH.toInt(), // アトラス全体のピクセル高さ
                color,
            )

            pose.popMatrix()
        }
    }
}
