package org.infinite.libs.graphics;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

@Environment(EnvType.CLIENT)
public final class ItemRenderState extends GuiItemRenderState {

  // --- ItemRenderStateで追加・再定義するフィールド ---
  private final float floatX; // floatのX座標
  private final float floatY; // floatのY座標
  private final float size; // レンダリングサイズ (デフォルト: 16.0F)
  private final float alpha; // アルファ値 (デフォルト: 1.0F)
  // ---

  // ItemGuiElementRenderStateのフィールドを再定義 (accesswidenerを前提)
  // 親クラスのプライベートフィールドにアクセスできるため、ここで再計算した値を代入
  @Nullable private final ScreenRectangle oversizedBounds;
  @Nullable private final ScreenRectangle bounds;

  /**
   * ItemRenderStateのコンストラクタ。
   *
   * @param name レンダリング状態の名前
   * @param pose レンダリングに使用する変換行列
   * @param state アイテムのレンダリング状態 (KeyedItemRenderState)
   * @param x アイテムのX座標 (float)
   * @param y アイテムのY座標 (float)
   * @param size アイテムのサイズ (float)
   * @param alpha アイテムのアルファ値 (float, 0.0F - 1.0F)
   * @param scissor レンダリングをクリップする領域 (ScreenRect)
   */
  public ItemRenderState(
      String name,
      Matrix3x2f pose,
      TrackingItemStackRenderState state,
      float x,
      float y,
      float size,
      float alpha,
      @Nullable ScreenRectangle scissor) {
    // 親クラスのコンストラクタを呼び出す。
    // 親クラスのint x/yフィールドは、このクラスのfloatX/floatYに置き換えられることを前提とし、
    // 適切な整数値（ここでは切り捨て）を渡す。
    super(name, pose, state, Mth.floor(x), Mth.floor(y), scissor);
    this.floatX = x;
    this.floatY = y;
    this.size = size;
    this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
    // floatX, floatY, size, alpha を使用して、boundsを再計算しフィールドを更新
    this.oversizedBounds =
        this.itemStackRenderState().isOversizedInGui()
            ? this.createOversizedBounds(this.floatX, this.floatY, this.size)
            : null;
    this.bounds =
        this.calculateBounds(
            this.oversizedBounds != null
                ? this.oversizedBounds
                : new ScreenRectangle(
                    Mth.floor(this.floatX),
                    Mth.floor(this.floatY),
                    Mth.ceil(this.size),
                    Mth.ceil(this.size)));
  }

  /** サイズがデフォルト値(16.0F)で、アルファ値がデフォルト値(1.0F)のコンストラクタ。 */
  public ItemRenderState(
      String name,
      Matrix3x2f pose,
      TrackingItemStackRenderState state,
      float x,
      float y,
      @Nullable ScreenRectangle scissor) {
    this(name, pose, state, x, y, 16.0F, 1.0F, scissor);
  }

  // --- 拡張ゲッターメソッド ---

  /** アイテムのX座標をfloatで取得します。 */
  public int x() {
    return Math.round(this.floatX);
  }

  /** アイテムのY座標をfloatで取得します。 */
  public int y() {
    return Math.round(this.floatY);
  }

  /** アイテムのレンダリングサイズを取得します。 */
  public float size() {
    return this.size;
  }

  /** アイテムのアルファ値（透明度）を取得します。 */
  public float alpha() {
    return this.alpha;
  }

  @Override
  @Nullable
  public ScreenRectangle oversizedItemBounds() {
    return this.oversizedBounds;
  }

  @Override
  @Nullable
  public ScreenRectangle bounds() {
    return this.bounds;
  }

  // --- bounds計算ロジックのオーバーライド ---

  /** 親クラスのプライベートメソッドをオーバーライド/再定義 float座標とfloatサイズに対応したboundsを計算 */
  @Nullable
  private ScreenRectangle createOversizedBounds(float x, float y, float size) {
    AABB box = this.itemStackRenderState().getModelBoundingBox();

    // sizeを基準にした大きさを計算
    int i = Mth.ceil(box.getXsize() * size);
    int j = Mth.ceil(box.getYsize() * size);

    if (i <= (int) size && j <= (int) size) {
      return null;
    } else {
      float f = (float) (box.minX * size);
      float g = (float) (box.maxY * size);
      int k = Mth.floor(f);
      int l = Mth.floor(g);

      // floatのx/yに整数オフセットを加える
      int m = Mth.floor(x + (float) k + size / 2.0F);
      int n = Mth.floor(y - (float) l + size / 2.0F);

      return new ScreenRectangle(m, n, i, j);
    }
  }

  /** 親クラスのプライベートメソッドをオーバーライド/再定義 float座標とsizeを反映したScreenRectを作成 */
  @Nullable
  public ScreenRectangle calculateBounds(ScreenRectangle rect) {
    // pose() は親クラスのパブリックメソッドなのでアクセス可能
    ScreenRectangle screenRect = rect.transformMaxBounds(this.pose());

    // scissorArea() も親クラスのパブリックメソッドなのでアクセス可能
    return this.scissorArea() != null ? this.scissorArea().intersection(screenRect) : screenRect;
  }
}
