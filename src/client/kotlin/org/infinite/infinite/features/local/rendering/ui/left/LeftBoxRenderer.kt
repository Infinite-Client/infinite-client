package org.infinite.infinite.features.local.rendering.ui.left

import net.minecraft.world.entity.ai.attributes.Attributes
import org.infinite.InfiniteClient
import org.infinite.infinite.features.local.rendering.ui.IUiRenderer
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature.Companion.renderUltraBar
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.utils.alpha
import org.infinite.utils.mix

class LeftBoxRenderer :
    MinecraftInterface(),
    IUiRenderer {
    private val ultraUiFeature: UltraUiFeature
        get() = InfiniteClient.localFeatures.rendering.ultraUiFeature

    private var animatedHealth = 0f
    private var animatedArmor = 0f
    private var animatedToughness = 0f

    override fun render(graphics2D: Graphics2D) {
        val player = player ?: return
        val colorScheme = InfiniteClient.theme.colorScheme
        val alpha = ultraUiFeature.alpha.value

        // --- 1. アニメーション計算 ---
        val actualHealth = (player.health / player.maxHealth).coerceIn(0f, 1f)
        val actualArmor = (player.armorValue / 20f).coerceIn(0f, 1f)
        val actualToughness = ((player.attributes.getInstance(Attributes.ARMOR_TOUGHNESS)?.value?.toFloat() ?: 0f) / 20f).coerceIn(0f, 1f)

        animatedHealth += (actualHealth - animatedHealth) * 0.1f
        animatedArmor += (actualArmor - animatedArmor) * 0.1f
        animatedToughness += (actualToughness - animatedToughness) * 0.1f

        // --- 2. 座標定数の定義 ---
        val pad = ultraUiFeature.padding.value.toFloat()
        val bH = ultraUiFeature.barHeight.value.toFloat() // 背景の高さ
        val sM = ultraUiFeature.sideMargin.toFloat()

        // すべての基準となる「下端」の座標
        val bottomY = graphics2D.height - pad
        val baseW = sM - pad * 2f

        // 背景描画：下端(bottomY)から上にbH分さかのぼった位置をyとする
        graphics2D.renderUltraBar(
            x = pad, y = bottomY - bH, baseWidth = baseW, baseHeight = bH,
            progress = 1f, heightProgress = 1f, color = colorScheme.backgroundColor,
            isRightToLeft = false, isUpsideDown = false,
        )

        // 内部コンテンツの基準下端と幅
        val iPad = pad.coerceAtLeast(1f)
        val contentBottomY = bottomY - iPad
        val cX = pad + iPad
        val cW = baseW - iPad * 2f
        val cH = bH - iPad * 2f // 利用可能な最大の高さ

        // --- 3. 共通描画関数 (下端固定レイアウト) ---
        fun drawLayer(height: Float, current: Float, target: Float, baseColor: Int, isHealth: Boolean = false) {
            val isInc = target > current
            val mixColor = if (isInc) colorScheme.whiteColor else colorScheme.blackColor

            // 下端(contentBottomY)から自身の高さを引いた位置を開始点にする
            val drawY = contentBottomY - height

            val secColor = (if (isHealth) colorScheme.color(current / 12f, 1f, 0.5f) else baseColor)
                .mix(mixColor, 0.5f).alpha((255 * alpha).toInt())

            val mainColor = (if (isHealth) colorScheme.color(target / 4f, 0.8f, 0.5f) else baseColor)
                .alpha((255 * alpha).toInt())

            // 背面
            graphics2D.renderUltraBar(
                x = cX, y = drawY, baseWidth = cW, baseHeight = height,
                progress = current, heightProgress = 1f, color = secColor,
                isRightToLeft = false, isUpsideDown = false,
            )
            // 前面
            graphics2D.renderUltraBar(
                x = cX, y = drawY, baseWidth = cW, baseHeight = height,
                progress = target, heightProgress = 1f, color = mainColor,
                isRightToLeft = false, isUpsideDown = false,
            )
        }

        // --- 4. 各バーの描画 (すべて下端contentBottomYに揃う) ---

        // HPバー: 高さ9割
        drawLayer(height = cH * 0.9f, current = animatedHealth, target = actualHealth, baseColor = 0, isHealth = true)

        // 防具バー: 高さ5割 (下端がHPと重なる)
        val subH = cH * 0.5f
        drawLayer(height = subH, current = animatedArmor, target = actualArmor, baseColor = colorScheme.blueColor)

        // タフネス: 防具バーのさらに中央付近 (これだけは少し浮かせるか、下端に揃えるか選べます)
        if (actualToughness > 0 || animatedToughness > 0) {
            val tH = subH * 0.4f
            // 防具バーのちょうど真ん中に配置したい場合は、以下のようにyを調整
            val tYOffset = (subH - tH) / 2f

            // タフネス用の特別描画（これのみ中央寄せのため共通関数を使わず微調整）
            val tDrawY = (contentBottomY - tH) - tYOffset

            val tSecColor = colorScheme.cyanColor.mix(if (actualToughness > animatedToughness) colorScheme.whiteColor else colorScheme.blackColor, 0.5f).alpha((255 * alpha).toInt())

            graphics2D.renderUltraBar(cX, tDrawY, cW, tH, animatedToughness, 1f, tSecColor, false, isUpsideDown = false)
            graphics2D.renderUltraBar(
                cX, tDrawY, cW, tH, actualToughness, 1f, colorScheme.cyanColor.alpha((255 * alpha).toInt()), false,
                isUpsideDown = false,
            )
        }
    }
}
