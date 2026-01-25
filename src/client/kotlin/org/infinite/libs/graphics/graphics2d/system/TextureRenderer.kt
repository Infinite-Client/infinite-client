package org.infinite.libs.graphics.graphics2d.system

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import org.infinite.libs.graphics.graphics2d.structs.RenderCommand2D

class TextureRenderer(private val gui: GuiGraphics) {

    fun drawTexture(cmd: RenderCommand2D.DrawTexture) {
        val pose = gui.pose()
        val image = cmd.image!!
        val identifier = image.identifier

        pose.pushMatrix()

        // 1. 位置の決定 (Float精度のまま移動)
        pose.translate(cmd.x, cmd.y)

        // 2. スケーリングの計算
        // 画面上の表示希望サイズ / テクスチャの切り出しサイズ
        val scaleX = cmd.width / cmd.uWidth.toFloat()
        val scaleY = cmd.height / cmd.vHeight.toFloat()
        pose.scale(scaleX, scaleY)

        // 3. 描画
        // blitに渡す「表示サイズ(k, l)」と「切り出しサイズ(m, n)」を同じにする
        // これにより、blit内部での計算による誤差を防ぎ、行列側のscaleにスケーリングを任せる
        gui.blit(
            RenderPipelines.GUI_TEXTURED,
            identifier,
            0, 0, // すでにtranslateとscaleで行列が調整されているので0, 0
            cmd.u.toFloat(), // 開始U
            cmd.v.toFloat(), // 開始V
            cmd.uWidth, // k: 表示幅を切り出し幅と一致させる
            cmd.vHeight, // l: 表示高さを切り出し高さと一致させる
            cmd.uWidth, // m: 切り出し幅
            cmd.vHeight, // n: 切り出し高さ
            image.width, // o: 画像全幅
            image.height, // p: 画像全高
            cmd.color,
        )

        pose.popMatrix()
    }
}
