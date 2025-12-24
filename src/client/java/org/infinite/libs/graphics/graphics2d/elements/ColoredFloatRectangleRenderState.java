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
public record ColoredFloatRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x0,
    float y0,
    float x1,
    float y1,
    int col1,
    int col2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds)
    implements GuiElementRenderState {

  public ColoredFloatRectangleRenderState(
      RenderPipeline renderPipeline,
      TextureSetup textureSetup,
      Matrix3x2fc matrix3x2fc,
      float x0,
      float y0,
      float x1,
      float y1,
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
    vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.col1());
    vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setColor(this.col2());
    vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.col2());
    vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setColor(this.col1());
  }

  private static @Nullable ScreenRectangle getBounds(
      float x0,
      float y0,
      float x1,
      float y1,
      Matrix3x2fc matrix3x2fc,
      @Nullable ScreenRectangle screenRectangle) {
    // ScreenRectangleはint型を期待するため、四捨五入または切り捨て/切り上げの判断が必要
    // ここでは包含領域を確保するためMath.floor/Math.ceil的なキャストを想定
    int ix0 = (int) x0;
    int iy0 = (int) y0;
    int iw = (int) (x1 - x0);
    int ih = (int) (y1 - y0);

    ScreenRectangle screenRectangle2 =
        (new ScreenRectangle(ix0, iy0, iw, ih)).transformMaxBounds(matrix3x2fc);
    return screenRectangle != null
        ? screenRectangle.intersection(screenRectangle2)
        : screenRectangle2;
  }
}
