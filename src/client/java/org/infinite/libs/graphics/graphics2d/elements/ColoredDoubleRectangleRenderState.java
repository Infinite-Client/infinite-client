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
public record ColoredDoubleRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    double x0,
    double y0,
    double x1,
    double y1, // doubleに変更
    int col1,
    int col2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds)
    implements GuiElementRenderState {

  public ColoredDoubleRectangleRenderState(
      RenderPipeline renderPipeline,
      TextureSetup textureSetup,
      Matrix3x2fc matrix3x2fc,
      double x0,
      double y0,
      double x1,
      double y1,
      int col1,
      int col2,
      @Nullable ScreenRectangle screenRectangle) {
    this(
        renderPipeline,
        textureSetup,
        matrix3x2fc,
        x0,
        y0,
        x1,
        y1,
        col1,
        col2,
        screenRectangle,
        getBounds(x0, y0, x1, y1, matrix3x2fc, screenRectangle));
  }

  public void buildVertices(VertexConsumer vertexConsumer) {
    // VertexConsumerはfloatを要求するため、ここでfloatにキャスト
    vertexConsumer
        .addVertexWith2DPose(this.pose(), (float) this.x0(), (float) this.y0())
        .setColor(this.col1());
    vertexConsumer
        .addVertexWith2DPose(this.pose(), (float) this.x0(), (float) this.y1())
        .setColor(this.col2());
    vertexConsumer
        .addVertexWith2DPose(this.pose(), (float) this.x1(), (float) this.y1())
        .setColor(this.col2());
    vertexConsumer
        .addVertexWith2DPose(this.pose(), (float) this.x1(), (float) this.y0())
        .setColor(this.col1());
  }

  private static @Nullable ScreenRectangle getBounds(
      double x0,
      double y0,
      double x1,
      double y1,
      Matrix3x2fc matrix3x2fc,
      @Nullable ScreenRectangle screenRectangle) {
    int ix0 = (int) Math.floor(x0);
    int iy0 = (int) Math.floor(y0);
    int iw = (int) Math.ceil(x1 - x0);
    int ih = (int) Math.ceil(y1 - y0);

    ScreenRectangle screenRectangle2 =
        (new ScreenRectangle(ix0, iy0, iw, ih)).transformMaxBounds(matrix3x2fc);
    return screenRectangle != null
        ? screenRectangle.intersection(screenRectangle2)
        : screenRectangle2;
  }
}
