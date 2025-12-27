package org.infinite.libs.infinite

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.infinite.InfiniteClient
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.libs.gui.screen.InfiniteScreen
import org.infinite.utils.toLowerSnakeCase
import org.lwjgl.glfw.GLFW

object InfiniteKeyBind {
    private var menuKeyBinding: KeyMapping? = null

    data class ToggleKeyBindingHandler(
        val keyBinding: KeyMapping,
        val feature: ConfigurableFeature,
    )

    var translationKeyList: MutableList<String> = mutableListOf()

    // 初期化時にリストを空にする必要はないので、valでもOKですが、MutableListであることは維持します
    private val toggleKeyBindings: MutableList<ToggleKeyBindingHandler> = mutableListOf()
    private val actionKeyBindings: MutableList<Pair<ConfigurableFeature, List<ConfigurableFeature.ActionKeybind>>> =
        mutableListOf()

    fun registerKeybindings() {
        val keyBindingCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("gameplay", "infinite"))
        menuKeyBinding =
            KeyBindingHelper.registerKeyBinding(
                KeyMapping(
                    "key.infinite-client.open_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    keyBindingCategory,
                ),
            )

        for (category in InfiniteClient.featureCategories) {
            for (feature in category.features) {
                val configurableFeature = feature.instance
                val translationKey =
                    "key.infinite-client.toggle.${category.name.toLowerSnakeCase()}.${feature.name.toLowerSnakeCase()}"
                translationKeyList += translationKey
                toggleKeyBindings +=
                    ToggleKeyBindingHandler(
                        KeyBindingHelper.registerKeyBinding(
                            KeyMapping(
                                translationKey,
                                InputConstants.Type.KEYSYM,
                                configurableFeature.toggleKeyBind.value,
                                keyBindingCategory,
                            ),
                        ),
                        configurableFeature,
                    )
                actionKeyBindings +=
                    configurableFeature to
                    configurableFeature.registerKeybinds(
                        category.name,
                        feature.name,
                        keyBindingCategory,
                    )
                translationKeyList += configurableFeature.registeredTranslations()
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (menuKeyBinding!!.consumeClick()) {
                client.setScreen(InfiniteScreen(Component.literal("")))
            }
            for (toggleKeyBind in toggleKeyBindings) {
                while (toggleKeyBind.keyBinding.consumeClick()) {
                    // 修正 2: enabled.valueをトグル（否定を代入）します
                    if (toggleKeyBind.feature.isEnabled()) {
                        toggleKeyBind.feature.disable()
                    } else {
                        toggleKeyBind.feature.enable()
                    }
                }
            }
            for ((feature, actionKeyBindList) in actionKeyBindings) {
                if (feature.isEnabled()) {
                    for (actionKeyBind in actionKeyBindList) {
                        while (actionKeyBind.keyBinding.consumeClick()) {
                            actionKeyBind.action()
                        }
                    }
                }
            }
        }
    }

    fun checkTranslations(): List<String> {
        val result = mutableListOf<String>()
        for (key in translationKeyList) {
            if (Component.translatable(key).string == key) {
                result.add(key)
            }
        }
        return result
    }
}
