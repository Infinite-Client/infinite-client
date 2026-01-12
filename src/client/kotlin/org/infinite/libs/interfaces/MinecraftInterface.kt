package org.infinite.libs.interfaces

import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.player.ClientInput
import net.minecraft.client.player.LocalPlayer

open class MinecraftInterface {
    protected val minecraft: Minecraft
        get() = Minecraft.getInstance()
    protected val player: LocalPlayer?
        get() = minecraft.player
    protected val input: ClientInput?
        get() = player?.input
    protected val level: ClientLevel?
        get() = minecraft.level
    protected val options: Options
        get() = minecraft.options
    protected val connection: ClientPacketListener?
        get() = minecraft.connection
}
