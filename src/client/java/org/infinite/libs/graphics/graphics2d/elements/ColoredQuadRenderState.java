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
public record ColoredQuadRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0, float y0,
        float x1, float y1,
        float x2, float y2,
        float x3, float y3,
        int col0, int col1, int col2, int col3,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds)
        implements GuiElementRenderState {

    public ColoredQuadRenderState(
            RenderPipeline renderPipeline,
            TextureSetup textureSetup,
            Matrix3x2fc matrix3x2fc,
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            int col0, int col1, int col2, int col3,
            @Nullable ScreenRectangle scissorArea) {
        this(
                renderPipeline,
                textureSetup,
                matrix3x2fc,
                x0, y0,
                x1, y1,
                x2, y2,
                x3, y3,
                col0, col1, col2, col3,
                scissorArea,
                getBounds(x0, y0, x1, y1, x2, y2, x3, y3, matrix3x2fc, scissorArea));
    }

    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0, this.y0).setColor(this.col0);
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1, this.y1).setColor(this.col1);
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x2, this.y2).setColor(this.col2);
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x3, this.y3).setColor(this.col3);
    }

    private static @Nullable ScreenRectangle getBounds(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            Matrix3x2fc matrix3x2fc,
            @Nullable ScreenRectangle scissorArea) {

        // 4つの頂点から最小・最大座標を求める (Axis-Aligned Bounding Box)
        float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
        float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
        float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
        float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));

        int ix = (int) Math.floor(minX);
        int iy = (int) Math.floor(minY);
        int iw = (int) Math.ceil(maxX) - ix;
        int ih = (int) Math.ceil(maxY) - iy;

        ScreenRectangle quadBounds = new ScreenRectangle(ix, iy, iw, ih).transformMaxBounds(matrix3x2fc);

        return scissorArea != null
                ? scissorArea.intersection(quadBounds)
                : quadBounds;
    }
}

