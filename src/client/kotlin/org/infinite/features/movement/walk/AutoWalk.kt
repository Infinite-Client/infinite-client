package org.infinite.features.movement.walk

import net.minecraft.client.KeyMapping // KeyBinding のインポートを追加
import net.minecraft.client.Minecraft // MinecraftClient のインポートを追加
import org.infinite.feature.ConfigurableFeature
import org.infinite.libs.client.control.ControllerInterface
import org.infinite.settings.FeatureSetting

class AutoWalk : ConfigurableFeature(initialEnabled = false) {
    // 八方向と停止を定義
    enum class Way {
        Forward,
        Back,
        Left,
        Right,
        ForwardLeft,
        ForwardRight,
        BackLeft,
        BackRight,
    }

    private val waySetting = FeatureSetting.EnumSetting("MovementWay", Way.Forward, Way.entries)
    override val settings: List<FeatureSetting<*>> = listOf(waySetting)

    // 機能が無効化されたときに全ての移動キーを離すロジックを追加
    override fun onDisabled() {
        val options = Minecraft.getInstance().options ?: return
        ControllerInterface.release(options.keyUp)
        ControllerInterface.release(options.keyDown)
        ControllerInterface.release(options.keyLeft)
        ControllerInterface.release(options.keyRight)
    }

    override fun onTick() {
        val keysToPress: List<KeyMapping> =
            when (waySetting.value) {
                Way.Forward -> listOf(options.keyUp)

                Way.Back -> listOf(options.keyDown)

                Way.Left -> listOf(options.keyLeft)

                Way.Right -> listOf(options.keyRight)

                // 斜め方向は2つのキーを同時に押す
                Way.ForwardLeft -> listOf(options.keyUp, options.keyLeft)

                Way.ForwardRight -> listOf(options.keyUp, options.keyRight)

                Way.BackLeft -> listOf(options.keyDown, options.keyLeft)

                Way.BackRight -> listOf(options.keyDown, options.keyRight)
            }

        // 決定したキーをControllerInterface経由で押す
        keysToPress.forEach { key ->
            ControllerInterface.press(key)
        }
    }
}
