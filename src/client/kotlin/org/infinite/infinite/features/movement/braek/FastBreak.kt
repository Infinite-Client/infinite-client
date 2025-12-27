package org.infinite.infinite.features.movement.braek

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.mixin.features.movement.fastbreak.MultiPlayerGameModeAccessor
import org.infinite.settings.FeatureSetting
import org.infinite.utils.block.BlockUtils

class FastBreak : ConfigurableFeature(initialEnabled = false) {
    override val level: FeatureLevel = FeatureLevel.Utils
    val safeMode = FeatureSetting.BooleanSetting("SafeMode", true)
    val interval = FeatureSetting.IntSetting("Interval", 0, 0, 5)
    val thresholdPercentage =
        FeatureSetting.DoubleSetting(
            "ThresholdPercentage",
            20.0,
            0.0,
            100.0,
        )
    val thresholdTick =
        FeatureSetting.IntSetting("ThresholdPercentage", 5, 1, 100)
    override val settings: List<FeatureSetting<*>> = listOf(safeMode, thresholdPercentage, thresholdTick, interval)

    fun handle(manager: MultiPlayerGameModeAccessor) {
        manager.setDestroyDelay(interval.value)
    }

    fun handle(blockPos: BlockPos) {
        val interactionManager = interactionManager ?: return
        val params = BlockUtils.getBlockBreakingParams(blockPos) ?: return // 破壊パラメータが取得できない場合は中止
        val progress = interactionManager.destroyProgress
        val remainProgress = 1 - progress
        // 残りティックの計算 (前回のコードのロジックを踏襲)
        val remainTick = interactionManager.destroyStage * (1 - progress)

        val thresholdTick = thresholdTick.value.toDouble()
        val thresholdPercentage = thresholdPercentage.value / 100.0

        // 2. 強制破壊の条件チェック
        if (remainTick < thresholdTick && remainProgress < thresholdPercentage) {
            // 3. 視線補正: 強制破壊の前に、ブロックのヒット座標へ向くようにサーバーに回転パケットを送信
            //    これにより、パケット偽装がより自然に見えるようになる
            BlockUtils.faceVectorPacket(params.hitVec)
            // 4. 強制破壊パケットの送信
            networkHandler?.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    blockPos,
                    params.side, // BlockUtilsから取得した正確な破壊方向を使用
                ),
            )
        }
    }

    override fun onTick() {
        if (safeMode.value) return

        val interactionManager = interactionManager ?: return
        if (!interactionManager.isDestroying) return
        // 1. ブロック破壊に必要な情報をBlockUtilsから取得
        val blockPos = interactionManager.destroyBlockPos
        handle(blockPos)
    }
}
