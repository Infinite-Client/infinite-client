package org.infinite.libs.graphics.graphics2d.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ColoredTriangleRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0, float y0,
        float x1, float y1,
        float x2, float y2,
        int col0, int col1, int col2,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds)
        implements GuiElementRenderState {

    public ColoredTriangleRenderState(
            RenderPipeline renderPipeline,
            TextureSetup textureSetup,
            Matrix3x2fc matrix3x2fc,
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            int col0, int col1, int col2,
            @Nullable ScreenRectangle scissorArea) {
        this(
                renderPipeline,
                textureSetup,
                matrix3x2fc,
                x0, y0,
                x1, y1,
                x2, y2,
                col0, col1, col2,
                scissorArea,
                getBounds(x0, y0, x1, y1, x2, y2, matrix3x2fc, scissorArea));
    }

    /**
     * 1->2->3->1 の順で4つの頂点を構築します。
     */
    public void buildVertices(VertexConsumer vertexConsumer) {
        // 頂点 1
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0, this.y0).setColor(this.col0);
        // 頂点 2
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1, this.y1).setColor(this.col1);
        // 頂点 3
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x2, this.y2).setColor(this.col2);
        // 頂点 1 (4つ目の頂点として最初の位置に戻る)
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0, this.y0).setColor(this.col0);
    }

    private static @Nullable ScreenRectangle getBounds(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            Matrix3x2fc matrix3x2fc,
            @Nullable ScreenRectangle scissorArea) {

        // 3つの頂点から最小・最大座標を求める
        float minX = Math.min(x0, Math.min(x1, x2));
        float minY = Math.min(y0, Math.min(y1, y2));
        float maxX = Math.max(x0, Math.max(x1, x2));
        float maxY = Math.max(y0, Math.max(y1, y2));

        int ix = (int) Math.floor(minX);
        int iy = (int) Math.floor(minY);
        int iw = (int) Math.ceil(maxX) - ix;
        int ih = (int) Math.ceil(maxY) - iy;

        ScreenRectangle triBounds = new ScreenRectangle(ix, iy, iw, ih).transformMaxBounds(matrix3x2fc);

        return scissorArea != null
                ? scissorArea.intersection(triBounds)
                : triBounds;
    }
}
