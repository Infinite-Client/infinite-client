package org.infinite.libs.core.features.property.list

import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.ui.widgets.SuggestInputWidget
import kotlin.jvm.optionals.getOrNull

class ItemListProperty(default: List<String>) : StringListProperty(default) {
    override fun renderElement(graphics2D: Graphics2D, item: String, x: Int, y: Int, width: Int, height: Int) {
        graphics2D.textStyle.font = "infinite_regular"
        graphics2D.textStyle.size = height - 4f
        graphics2D.textStyle.shadow = true
        graphics2D.fillStyle = InfiniteClient.theme.colorScheme.foregroundColor
        graphics2D.text(item, x + 20, y + (height - 8) / 2)
        val identifier = Identifier.tryParse(item) ?: return
        val item = BuiltInRegistries.ITEM.get(identifier).getOrNull() ?: return
        val itemStack = ItemStack(item)
        val itemSize = height - 2f
        graphics2D.itemCentered(itemStack, x + height / 2f, y + height / 2f, itemSize)
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
        suggestions = { BuiltInRegistries.ITEM.keySet().map { it.toString() } },
        onComplete = onComplete,
    )
}
