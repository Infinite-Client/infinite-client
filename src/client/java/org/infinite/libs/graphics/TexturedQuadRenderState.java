package org.infinite.libs.graphics;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

@Environment(EnvType.CLIENT)
public record TexturedQuadRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float x1,
    float y1,
    float x2,
    float y2,
    float u1,
    float u2,
    float v1,
    float v2,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds)
    implements GuiElementRenderState {
  public TexturedQuadRenderState(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2f pose,
      float x1,
      float y1,
      float x2,
      float y2,
      float u1,
      float u2,
      float v1,
      float v2,
      int color,
      @Nullable ScreenRectangle scissorArea) {
    this(
        pipeline,
        textureSetup,
        pose,
        x1,
        y1,
        x2,
        y2,
        u1,
        u2,
        v1,
        v2,
        color,
        scissorArea,
        createBounds(x1, y1, x2, y2, pose, scissorArea));
  }

  public void buildVertices(VertexConsumer vertices) {
    vertices
        .addVertexWith2DPose(this.pose(), this.x1(), this.y1())
        .setUv(this.u1(), this.v1())
        .setColor(this.color());
    vertices
        .addVertexWith2DPose(this.pose(), this.x1(), this.y2())
        .setUv(this.u1(), this.v2())
        .setColor(this.color());
    vertices
        .addVertexWith2DPose(this.pose(), this.x2(), this.y2())
        .setUv(this.u2(), this.v2())
        .setColor(this.color());
    vertices
        .addVertexWith2DPose(this.pose(), this.x2(), this.y1())
        .setUv(this.u2(), this.v1())
        .setColor(this.color());
  }

  @Nullable
  private static ScreenRectangle createBounds(
      float x1,
      float y1,
      float x2,
      float y2,
      Matrix3x2f pose,
      @Nullable ScreenRectangle scissorArea) {
    ScreenRectangle screenRect =
        (new ScreenRectangle((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1)))
            .transformMaxBounds(pose);
    return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
  }
}
