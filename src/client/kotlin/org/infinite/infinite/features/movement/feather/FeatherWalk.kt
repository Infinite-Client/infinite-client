package org.infinite.infinite.features.movement.feather

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.BuiltInRegistries
import org.infinite.feature.ConfigurableFeature
import org.infinite.settings.FeatureSetting

// FeatherWalk Featureの定義
class FeatherWalk : ConfigurableFeature(initialEnabled = false) {
    override val level: FeatureLevel = FeatureLevel.Utils

    // フィーチャーのロジックで利用する設定
    private val blockList: FeatureSetting.BlockListSetting =
        FeatureSetting.BlockListSetting(
            name = "AllowedBlocks",
            defaultValue = mutableListOf("minecraft:farmland"),
        )

    private val disableJump: FeatureSetting.BooleanSetting =
        FeatureSetting.BooleanSetting(
            name = "DisableJump",
            defaultValue = true,
        )

    private val disableSprint: FeatureSetting.BooleanSetting =
        FeatureSetting.BooleanSetting(
            name = "DisableSprint",
            defaultValue = true,
        )
    override val settings: List<FeatureSetting<*>> =
        listOf(
            blockList,
            disableJump,
            disableSprint,
        )

    // --- 仮定されるゲームクライアントへのアクセスポイント ---
    // 実際の環境に合わせて適宜変更してください。
    // 例: Minecraft client object
    private var mc = Minecraft.getInstance()

    override fun onTick() {
        var isWalkingOnFeatherBlock = false
        val level = mc.level ?: return
        // 1. プレイヤーの現在のブロック座標を取得
        // プレイヤーの足元（Y-1）ではなく、プレイヤーの中心ブロックを取得する
        val playerX =
            mc.player
                ?.x
                ?.toInt() ?: return
        val playerY =
            mc.player
                ?.y
                ?.toInt() ?: return
        val playerZ =
            mc.player
                ?.z
                ?.toInt() ?: return

        // 2. プレイヤーの中心ブロックとその周囲1ブロック（3x3x3 = 27ブロック）を確認
        // X, Y, Z軸で -1 から +1 までの範囲を反復処理
        for (xOffset in -1..1) {
            for (yOffset in -1..1) {
                for (zOffset in -1..1) {
                    // 確認するブロックの座標
                    val checkX = playerX + xOffset
                    val checkY = playerY + yOffset
                    val checkZ = playerZ + zOffset
                    val block = level.getBlockState(BlockPos(Vec3i(checkX, checkY, checkZ)))?.block ?: continue
                    val blockName =
                        BuiltInRegistries.BLOCK
                            .getKey(block)
                            .toString()

                    // 設定されたブロックリストに含まれているかチェック
                    if (blockList.value.contains(blockName)) {
                        isWalkingOnFeatherBlock = true
                        // 一致するブロックが見つかったら、すぐにループを終了
                        break
                    }
                }
                if (isWalkingOnFeatherBlock) break
            }
            if (isWalkingOnFeatherBlock) break
        }

        // 3. 設定に基づいて制御を適用
        if (isWalkingOnFeatherBlock) {
            if (disableJump.value) {
                // ジャンプ入力を抑制するロジック (例: mc.player.input.jumping = false)
                mc.options.keyJump.isDown = false
            }
            if (disableSprint.value) {
                mc.player?.isSprinting = false // Stop sprinting if hunger is too low
            }
        }
    }
}
