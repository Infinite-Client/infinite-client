package org.infinite.libs.interfaces

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer

open class MinecraftInterface {
    protected val client: Minecraft
        get() = Minecraft.getInstance()
    protected val player: LocalPlayer?
        get() = client.player
    protected val level: ClientLevel?
        get() = client.level
}
