package org.infinite.libs.graphics.graphics2d.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ColoredRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x0,
    float y0,
    float x1,
    float y1,
    int colTopLeft, // 左上の色
    int colBottomLeft, // 左下の色
    int colBottomRight, // 右下の色
    int colTopRight, // 右上の色
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds)
    implements GuiElementRenderState {

  public ColoredRectangleRenderState(
      RenderPipeline renderPipeline,
      TextureSetup textureSetup,
      Matrix3x2fc matrix3x2fc,
      float x0,
      float y0,
      float x1,
      float y1,
      int colTopLeft,
      int colBottomLeft,
      int colBottomRight,
      int colTopRight,
      @Nullable ScreenRectangle screenRectangle) {
    this(
        renderPipeline,
        textureSetup,
        matrix3x2fc,
        x0,
        y0,
        x1,
        y1,
        colTopLeft,
        colBottomLeft,
        colBottomRight,
        colTopRight,
        screenRectangle,
        getBounds(x0, y0, x1, y1, matrix3x2fc, screenRectangle));
  }

  @Override
  public void buildVertices(VertexConsumer vertexConsumer) {
    // 頂点1: 左上 (x0, y0)
    vertexConsumer
        .addVertexWith2DPose(this.pose(), this.x0(), this.y0())
        .setColor(this.colTopLeft());
    // 頂点2: 左下 (x0, y1)
    vertexConsumer
        .addVertexWith2DPose(this.pose(), this.x0(), this.y1())
        .setColor(this.colBottomLeft());
    // 頂点3: 右下 (x1, y1)
    vertexConsumer
        .addVertexWith2DPose(this.pose(), this.x1(), this.y1())
        .setColor(this.colBottomRight());
    // 頂点4: 右上 (x1, y0)
    vertexConsumer
        .addVertexWith2DPose(this.pose(), this.x1(), this.y0())
        .setColor(this.colTopRight());
  }

  private static @Nullable ScreenRectangle getBounds(
      float x0,
      float y0,
      float x1,
      float y1,
      Matrix3x2fc matrix3x2fc,
      @Nullable ScreenRectangle screenRectangle) {
    int ix0 = (int) Math.floor(x0);
    int iy0 = (int) Math.floor(y0);
    int iw = (int) Math.ceil(x1 - x0);
    int ih = (int) Math.ceil(y1 - y0);

    ScreenRectangle screenRectangle2 =
        new ScreenRectangle(ix0, iy0, iw, ih).transformMaxBounds(matrix3x2fc);
    return screenRectangle != null
        ? screenRectangle.intersection(screenRectangle2)
        : screenRectangle2;
  }
}
