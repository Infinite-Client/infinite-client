package org.infinite.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import org.jetbrains.annotations.Nullable
import java.util.UUID

/**
 * Creates a client-side "fake player" entity that mimics the real player's appearance and inventory.
 * This is typically used for client-side modding features like visual testing or movement tricks.
 */
class FakePlayerEntity : RemotePlayer {
    // MinecraftClientのインスタンスはシングルトンであるため、遅延初期化プロパティとして取得
    private val client: Minecraft = Minecraft.getInstance()
    private val player: LocalPlayer =
        client.player ?: throw IllegalStateException("Client player must be present.")
    private val world: ClientLevel = client.level ?: throw IllegalStateException("Client world must be present.")

    // PlayerListEntryは必要に応じて遅延で初期化
    private var playerListEntry: PlayerInfo? = null

    constructor() : super(
        Minecraft.getInstance().level!!,
        Minecraft.getInstance().player?.gameProfile
            ?: throw IllegalStateException("Client player profile must be present."),
    ) {
        // UUIDをランダムに設定し、本物のプレイヤーと区別
        uuid = UUID.randomUUID()

        // 位置と回転（視線）を本物のプレイヤーからコピー
        copyPosition(player)

        // インベントリと体の向きをコピー
        copyInventory()
        copyRotation()

        // ワールドにスポーンさせる
        spawn()
    }

    override fun getPlayerInfo(): @Nullable PlayerInfo? {
        if (playerListEntry == null) {
            // 本物のプレイヤーのプロフィールIDでエントリを取得しようと試みる
            playerListEntry = client.connection?.getPlayerInfo(gameProfile.id)
        }
        return playerListEntry
    }

    override fun doPush(entity: Entity) {
        // 本物のプレイヤーを押し出さないように、エンティティの押し合い処理を無効化
    }

    private fun copyInventory() {
        inventory.replaceWith(player.inventory)
    }

    private fun copyRotation() {
        yHeadRot = player.yHeadRot
        yBodyRot = player.yBodyRot
    }

    private fun spawn() {
        // ワールドにエンティティとして追加
        world.addEntity(this)
    }

    /**
     * ワールドから偽プレイヤーを削除する
     */
    fun despawn() {
        discard() // エンティティを削除する
    }

    /**
     * 本物のプレイヤーの位置を、この偽プレイヤーの現在位置にリセットする
     */
    fun resetPlayerPosition() {
        player.snapTo(x, y, z, yRot, xRot)
    }
}
