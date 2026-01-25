package org.infinite.libs.core.features.property.list

import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.ui.widgets.SuggestInputWidget
import kotlin.jvm.optionals.getOrNull

class BlockListProperty(default: List<String>) : StringListProperty(default) {
    override fun renderElement(graphics2D: Graphics2D, item: String, x: Int, y: Int, width: Int, height: Int) {
        // テキスト描画の設定
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = height - 4f
        graphics2D.textStyle.shadow = true
        graphics2D.fillStyle = InfiniteClient.theme.colorScheme.foregroundColor
        graphics2D.text(item, x + 20, y + (height - 8) / 2)

        // ブロックIDの解析と描画
        val identifier = Identifier.tryParse(item) ?: return
        val block = BuiltInRegistries.BLOCK.get(identifier).getOrNull()?.value() ?: return
        val blockSize = height - 2f
        graphics2D.blockCentered(block, x + height / 2f, y + height / 2f, blockSize)
    }

    override fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: String?,
        onComplete: (String?) -> Unit,
    ): AbstractWidget = SuggestInputWidget(
        x,
        y,
        width,
        initialValue ?: "",
        // ブロックのレジストリキーを提案リストとして提供
        suggestions = { BuiltInRegistries.BLOCK.keySet().map { it.toString() } },
        onComplete = onComplete,
    )
}
