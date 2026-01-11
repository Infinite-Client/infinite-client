package org.infinite.libs.core.features.property.list

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.property.ListProperty
import org.lwjgl.glfw.GLFW

class StringListProperty(default: List<String>) : ListProperty<String>(default) {

    override fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: String?,
        onComplete: (String?) -> Unit,
    ): AbstractWidget {
        val editBox = object : EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("")) {
            override fun keyPressed(keyEvent: KeyEvent): Boolean {
                val keyCode = keyEvent.key
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    if (this.value.isNotBlank()) onComplete(this.value)
                    return true
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    onComplete(null)
                    return true
                }
                return super.keyPressed(keyEvent)
            }
        }.apply {
            // ここで初期値をセットする
            this.value = initialValue ?: ""
            this.isFocused = true
            this.cursorPosition = this.value.length
        }
        return editBox
    }
}
