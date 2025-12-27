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
public record RectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float x1,
    float y1,
    float x2,
    float y2,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds)
    implements GuiElementRenderState {
  /** カスタムコンストラクタ。boundsフィールドを自動的に計算します。 x1, y1は左上隅、x2, y2は右下隅を想定しています。 */
  public RectangleRenderState(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2f pose,
      float x1,
      float y1,
      float x2,
      float y2,
      int color,
      @Nullable ScreenRectangle scissorArea) {
    // フィールドを初期化する正規コンストラクタを呼び出し、boundsを計算して渡します。
    this(
        pipeline,
        textureSetup,
        pose,
        x1,
        y1,
        x2,
        y2,
        color,
        scissorArea,
        createBounds(x1, y1, x2, y2, pose, scissorArea));
  }

  public void buildVertices(VertexConsumer vertices) {
    // 長方形の4つの頂点座標を計算
    float left = Math.min(this.x1(), this.x2());
    float top = Math.min(this.y1(), this.y2());
    float right = Math.max(this.x1(), this.x2());
    float bottom = Math.max(this.y1(), this.y2());

    // 描画プリミティブとして2つの三角形（Quad）を出力します:
    // 頂点: (left, top), (left, bottom), (right, bottom), (right, top)

    // 頂点 1 (左上)
    vertices.addVertexWith2DPose(this.pose(), left, top).setColor(this.color());
    // 頂点 2 (左下)
    vertices.addVertexWith2DPose(this.pose(), left, bottom).setColor(this.color());
    // 頂点 3 (右下)
    vertices.addVertexWith2DPose(this.pose(), right, bottom).setColor(this.color());

    // 頂点 4 (右上)
    vertices.addVertexWith2DPose(this.pose(), right, top).setColor(this.color());
  }

  @Nullable
  private static ScreenRectangle createBounds(
      float x1,
      float y1,
      float x2,
      float y2,
      Matrix3x2f pose,
      @Nullable ScreenRectangle scissorArea) {
    var startX = Math.min(x1, x2);
    var startY = Math.min(y1, y2);
    var endX = Math.max(x1, x2);
    var endY = Math.max(y1, y2);

    // 変換前の座標で矩形を作成 (長方形は常に軸並行であるため、min/maxでそのままバウンディングボックスになります)
    ScreenRectangle screenRect =
        new ScreenRectangle(
            (int) Math.floor(startX),
            (int) Math.floor(startY),
            ((int) Math.ceil(endX - startX)),
            ((int) Math.ceil(endY - startY)));

    // 変換行列を適用して最小AABBを計算
    ScreenRectangle transformedRect = screenRect.transformMaxBounds(pose);

    // スクリムエリアとの交差を計算
    return scissorArea != null ? scissorArea.intersection(transformedRect) : transformedRect;
  }
}
