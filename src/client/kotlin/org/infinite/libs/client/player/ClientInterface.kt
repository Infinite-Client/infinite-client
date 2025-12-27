package org.infinite.libs.client.player

import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.vehicle.VehicleEntity
import net.minecraft.world.phys.Vec3

open class ClientInterface {
    protected val client: Minecraft
        get() = Minecraft.getInstance()
    protected val player: LocalPlayer?
        get() = client.player
    protected val world: ClientLevel?
        get() = client.level
    protected val options: Options
        get() = client.options
    protected val interactionManager: MultiPlayerGameMode?
        get() = client.gameMode
    protected val inventory: Inventory?
        get() = client.player?.inventory
    protected val vehicle: VehicleEntity?
        get() = player?.vehicle as? VehicleEntity
    protected var velocity: Vec3?
        get() = if (player?.vehicle != null) player?.vehicle?.deltaMovement else player?.deltaMovement
        set(value) {
            val value = value ?: return
            if (player?.vehicle != null) {
                player?.deltaMovement = value
            } else {
                player?.vehicle?.deltaMovement =
                    value
            }
        }
    protected var playerPos: Vec3?
        get() {
            val x = player?.x ?: return null
            val y = player?.y ?: return null
            val z = player?.z ?: return null
            return Vec3(x, y, z)
        }
        set(value) {
            val vec = value ?: return
            player?.setPosRaw(vec.x, vec.y, vec.z)
        }
    protected val networkHandler: ClientCommonPacketListenerImpl?
        get() = client.connection
}
