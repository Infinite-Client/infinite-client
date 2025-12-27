package org.infinite

import net.minecraft.network.chat.Component

object Translation {
    fun t(key: String): String = Component.translatable(key).string
}
