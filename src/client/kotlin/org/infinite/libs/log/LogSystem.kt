package org.infinite.libs.log

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import org.infinite.InfiniteClient
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.ui.theme.ColorScheme
import org.infinite.libs.ui.theme.Theme
import java.util.concurrent.ConcurrentLinkedQueue

object LogSystem : MinecraftInterface() {
    private val messageQueue = ConcurrentLinkedQueue<Component>()
    private const val MESSAGES_PER_TICK = 5
    private val theme: Theme
        get() = InfiniteClient.theme
    private val colorScheme: ColorScheme
        get() = theme.colorScheme

    // ログメソッド
    fun log(text: String) = enqueue(text, "", colorScheme.foregroundColor)

    fun info(text: String) = enqueue(text, " - Info ", colorScheme.cyanColor)

    fun warn(text: String) = enqueue(text, " - Warn ", colorScheme.yellowColor)

    fun error(text: String) = enqueue(text, " - Error", colorScheme.redColor)
    fun debug(text: String) = enqueue(text, " - Debug", colorScheme.greenColor)
    fun log(text: Component) {
        val message = createPrefixedMessage("", colorScheme.foregroundColor).append(text)
        processLog(message)
    }

    private fun enqueue(
        text: String,
        prefixType: String,
        color: Int,
    ) {
        val style = Style.EMPTY.withColor(TextColor.fromRgb(color))
        val message =
            createPrefixedMessage(prefixType, color)
                .append(Component.literal(text).withStyle(style))
        processLog(message)
    }

    private fun processLog(component: Component) {
        // 標準出力（スレッドセーフ）
        println("[Infinite] ${component.string}")
        // チャットキューに追加
        messageQueue.add(component)
    }

    private fun createPrefixedMessage(
        prefixType: String,
        textColor: Int,
    ): MutableComponent {
        val style = Style.EMPTY.withColor(TextColor.fromRgb(textColor))
        return Component
            .literal("[")
            .withStyle(ChatFormatting.BOLD)
            .append(rainbowText("Infinite Client").withStyle(ChatFormatting.BOLD))
            .append(Component.literal(prefixType).withStyle(style))
            .append(Component.literal("]: ").withStyle(ChatFormatting.RESET))
    }

    /**
     * レインボーテキスト生成ロジック
     */
    fun rainbowText(text: String): MutableComponent {
        val colors =
            intArrayOf(
                0xFFFF0000.toInt(), // 赤
                0xFFFFFF00.toInt(), // 黄
                0xFF00FF00.toInt(), // 緑
                0xFF00FFFF.toInt(), // 水
                0xFF0000FF.toInt(), // 青
                0xFFFF00FF.toInt(), // 紫
            )

        val totalLength = text.length
        val result = Component.empty()

        for (i in text.indices) {
            // 色の補間計算
            val progress = if (totalLength > 1) i.toFloat() / (totalLength - 1).toFloat() else 0f
            val colorIndex = (progress * (colors.size - 1)).toInt()
            val startColor = colors[colorIndex]
            val endColor = if (colorIndex < colors.size - 1) colors[colorIndex + 1] else colors[colorIndex]
            val segmentProgress = (progress * (colors.size - 1)) - colorIndex

            val r =
                (((startColor shr 16 and 0xFF) * (1 - segmentProgress)) + ((endColor shr 16 and 0xFF) * segmentProgress)).toInt()
            val g =
                (((startColor shr 8 and 0xFF) * (1 - segmentProgress)) + ((endColor shr 8 and 0xFF) * segmentProgress)).toInt()
            val b = (((startColor and 0xFF) * (1 - segmentProgress)) + ((endColor and 0xFF) * segmentProgress)).toInt()

            val interpolatedColor = (r shl 16) or (g shl 8) or b

            result.append(
                Component.literal(text[i].toString()).withStyle { style ->
                    style.withColor(TextColor.fromRgb(interpolatedColor))
                },
            )
        }
        return result
    }

    /**
     * 初期化：InfiniteClient から呼び出す
     */
    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player
            if (player == null) {
                if (messageQueue.size > 50) messageQueue.clear()
                return@register
            }

            repeat(MESSAGES_PER_TICK) {
                val message = messageQueue.poll() ?: return@repeat
                player.displayClientMessage(message, false)
            }
        }
    }
}
