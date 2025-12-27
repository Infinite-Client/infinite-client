package org.infinite.infinite.features.rendering.detailinfo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.EnderChestBlockEntity
import net.minecraft.world.level.block.entity.FurnaceBlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import net.minecraft.world.level.block.entity.SmokerBlockEntity
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.infinite.feature.ConfigurableFeature
import org.infinite.libs.graphics.Graphics2D
import org.infinite.settings.FeatureSetting
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.sqrt

// InventoryData, InventoryType, FurnaceData, BrewingDataの定義は変更なし
data class InventoryData(
    val type: InventoryType,
    val items: List<ItemStack>,
)

enum class InventoryType {
    CHEST,
    FURNACE,
    HOPPER,
    GENERIC,
    BREWING,
}

data class FurnaceData(
    var litTimeRemaining: Int = 0,
    var litTotalTime: Int = 0,
    var cookingTimeSpent: Int = 0,
    var cookingTotalTime: Int = 200,
    val inventory: NonNullList<ItemStack> = NonNullList.withSize(3, ItemStack.EMPTY),
)

data class BrewingData(
    var brewTime: Int = 0,
    var fuel: Int = 0,
    val inventory: NonNullList<ItemStack> = NonNullList.withSize(5, ItemStack.EMPTY),
)

class DetailInfo : ConfigurableFeature(initialEnabled = false) {
    private val furnaceProgressData: MutableMap<BlockPos, FurnaceData> = mutableMapOf()
    private val brewingProgressData: MutableMap<BlockPos, BrewingData> = mutableMapOf()
    private val scannedInventoryData: MutableMap<String, MutableMap<BlockPos, InventoryData>> = mutableMapOf()

    fun handleFurnaceProgress(
        syncId: Int,
        pos: BlockPos,
        propertyId: Int,
        value: Int,
    ) {
        val data = furnaceProgressData.getOrPut(pos) { FurnaceData() }
        when (propertyId) {
            0 -> data.litTimeRemaining = value
            1 -> data.litTotalTime = value
            2 -> data.cookingTimeSpent = value
            3 -> data.cookingTotalTime = value
        }
    }

    fun handleBrewingProgress(
        syncId: Int,
        pos: BlockPos,
        propertyId: Int,
        value: Int,
    ) {
        val data = brewingProgressData.getOrPut(pos) { BrewingData() }
        when (propertyId) {
            0 -> data.brewTime = value
            1 -> data.fuel = value
        }
    }

    fun getFurnaceData(pos: BlockPos): FurnaceData = furnaceProgressData[pos] ?: FurnaceData()

    fun getBrewingData(pos: BlockPos): BrewingData = brewingProgressData[pos] ?: BrewingData()

    fun findCrosshairTarget(
        camera: Entity,
        blockInteractionRange: Double,
        entityInteractionRange: Double,
    ): HitResult {
        var d = max(blockInteractionRange, entityInteractionRange)
        var e = Mth.square(d)
        val vec3d = camera.getEyePosition(1f)
        val hitResult = camera.pick(d, 1f, false)
        val f = hitResult.location.distanceToSqr(vec3d)
        if (hitResult.type != HitResult.Type.MISS) {
            e = f
            d = sqrt(f)
        }

        val vec3d2 = camera.getViewVector(1f)
        val vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d)
        val box = camera.boundingBox.expandTowards(vec3d2.scale(d)).inflate(1.0, 1.0, 1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(camera, vec3d, vec3d3, box, EntitySelector.CAN_BE_PICKED, e)
        return if (entityHitResult != null && entityHitResult.location.distanceToSqr(vec3d) < f) {
            entityHitResult
        } else {
            hitResult
        }
    }

    override val settings: List<FeatureSetting<*>> =
        listOf(
            FeatureSetting.BooleanSetting("BlockInfo", true),
            FeatureSetting.BooleanSetting("InnerChest", true),
            FeatureSetting.BooleanSetting("EntityInfo", true),
            FeatureSetting.IntSetting("PaddingTop", 0, 0, 100),
            FeatureSetting.FloatSetting("Reach", 20f, 10f, 100f),
            FeatureSetting.IntSetting("Width", 50, 25, 100),
        )

    var shouldCancelScanScreen: Boolean = false
    var scanTargetBlockEntity: BlockEntity? = null
    var expectedScreenType: MenuType<*>? = null
    var scanTimer = 0

    var targetDetail: TargetDetail? = null
    var isTargetInReach: Boolean = false

    sealed class TargetDetail(
        val pos: BlockPos?,
        val name: String,
    ) {
        class BlockDetail(
            val block: Block,
            pos: BlockPos,
        ) : TargetDetail(pos, block.toString())

        class EntityDetail(
            val entity: Entity,
            pos: BlockPos,
            name: String,
        ) : TargetDetail(pos, name)
    }

    override fun onTick() {
        targetDetail = null
        isTargetInReach = true

        val client = Minecraft.getInstance() ?: return
        val world = client.level ?: return
        val dimension = getDimensionKey()
        if (!scannedInventoryData.containsKey(dimension)) {
            loadData(dimension)
        }
        val clientCommonNetworkHandler = client.connection ?: return
        var hitResult = client.hitResult ?: return
        if (hitResult.type == HitResult.Type.MISS) {
            val entity: Entity? = client.cameraEntity
            if (entity != null) {
                if (client.level != null && client.player != null) {
                    val reach = getSetting("Reach")?.value as? Double ?: 20.0
                    hitResult = findCrosshairTarget(entity, reach, reach)
                    isTargetInReach = false
                }
            }
        }
        when (hitResult.type) {
            HitResult.Type.ENTITY -> {
                if (getSetting("EntityInfo")?.value == true) {
                    val entityHitResult = hitResult as EntityHitResult
                    val entity = entityHitResult.entity
                    val entityPos = entity.blockPosition()
                    val entityName = entity.type.description.string
                    targetDetail = TargetDetail.EntityDetail(entity, entityPos, entityName)
                }
            }

            HitResult.Type.BLOCK -> {
                if (getSetting("BlockInfo")?.value == true) {
                    val blockHitResultCasted = hitResult as BlockHitResult
                    val blockPos = blockHitResultCasted.blockPos
                    val blockState = world.getBlockState(blockPos)
                    val blockEntity = world.getBlockEntity(blockPos)

                    targetDetail = TargetDetail.BlockDetail(blockState.block, blockPos)

                    if (blockEntity is RandomizableContainerBlockEntity ||
                        blockEntity is FurnaceBlockEntity ||
                        blockEntity is SmokerBlockEntity ||
                        blockEntity is BlastFurnaceBlockEntity ||
                        blockEntity is BrewingStandBlockEntity ||
                        blockEntity is EnderChestBlockEntity
                    ) {
                        if (scanTimer <= 0) {
                            if (getSetting("InnerChest")?.value == true) {
                                if (client.screen == null) {
                                    // Set expected screen type based on block entity
                                    expectedScreenType =
                                        when (blockEntity) {
                                            is ChestBlockEntity -> {
                                                val chestType = blockState.getValue(ChestBlock.TYPE)
                                                if (chestType ==
                                                    ChestType.SINGLE
                                                ) {
                                                    MenuType.GENERIC_9x3
                                                } else {
                                                    MenuType.GENERIC_9x6
                                                }
                                            }

                                            is BarrelBlockEntity -> {
                                                MenuType.GENERIC_9x3
                                            }

                                            is ShulkerBoxBlockEntity -> {
                                                MenuType.SHULKER_BOX
                                            }

                                            is EnderChestBlockEntity -> {
                                                MenuType.GENERIC_9x3
                                            }

                                            is HopperBlockEntity -> {
                                                MenuType.HOPPER
                                            }

                                            is FurnaceBlockEntity -> {
                                                MenuType.FURNACE
                                            }

                                            is SmokerBlockEntity -> {
                                                MenuType.SMOKER
                                            }

                                            is BlastFurnaceBlockEntity -> {
                                                MenuType.BLAST_FURNACE
                                            }

                                            is BrewingStandBlockEntity -> {
                                                MenuType.BREWING_STAND
                                            }

                                            else -> {
                                                null
                                            } // Should not happen with the check above, but for safety
                                        }

                                    if (expectedScreenType != null) {
                                        clientCommonNetworkHandler.send(
                                            ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, blockHitResultCasted, 0),
                                        )
                                        scanTargetBlockEntity = blockEntity
                                        shouldCancelScanScreen = true
                                        scanTimer = 20
                                    }
                                }
                            }
                        } else {
                            scanTimer--
                        }
                    } else {
                        val dimension = getDimensionKey()
                        scannedInventoryData[dimension]?.remove(blockPos)
                    }
                }
            }

            else -> {
                // 何もヒットしなかった場合
            }
        }
    }

    /**
     * 修正点:
     * - ChestBlockEntity以外でも、scannedInventoryDataにデータを格納し、
     * - scanTargetBlockEntityをnullにリセットし、
     * - 画面を閉じるパケットを送信するように共通化しました。
     */
    fun handleChestContents(
        syncId: Int,
        items: MutableList<ItemStack>,
    ) {
        if (scanTargetBlockEntity != null) {
            shouldCancelScanScreen = false
            val entity = scanTargetBlockEntity as BlockEntity
            val dimension = getDimensionKey()

            val inventoryType: InventoryType
            val containerSize: Int

            when (entity) {
                is FurnaceBlockEntity, is SmokerBlockEntity, is BlastFurnaceBlockEntity -> {
                    inventoryType = InventoryType.FURNACE
                    containerSize = 3 // 燃料、材料、出力
                }

                is HopperBlockEntity -> {
                    inventoryType = InventoryType.HOPPER
                    containerSize = 5
                }

                is BrewingStandBlockEntity -> {
                    inventoryType = InventoryType.BREWING
                    containerSize = 5 // 3ポーション、1材料、1燃料
                }

                is ChestBlockEntity, is ShulkerBoxBlockEntity, is BarrelBlockEntity, is EnderChestBlockEntity -> {
                    inventoryType = InventoryType.CHEST
                    // GENERIC_9X3: 27スロット, GENERIC_9X6: 54スロット, SHULKER_BOX: 27スロット
                    // items.sizeはコンテナ + プレイヤーインベントリ (36) なので、コンテナサイズは items.size - 36
                    containerSize = items.size - 36
                }

                else -> {
                    inventoryType = InventoryType.GENERIC
                    containerSize = 0 // 未知のコンテナはスキップ
                }
            }

            // プレイヤーインベントリを除いたコンテナの中身のみを取得
            val containerItems = items.take(containerSize)

            if (!scannedInventoryData.containsKey(dimension)) {
                scannedInventoryData[dimension] = mutableMapOf()
            }

            // チェストの結合処理
            if (entity is ChestBlockEntity) {
                val world = Minecraft.getInstance().level ?: return
                val blockState = world.getBlockState(entity.blockPos)
                if (blockState.block is ChestBlock && blockState.hasProperty(ChestBlock.TYPE)) {
                    val chestType = blockState.getValue(ChestBlock.TYPE)
                    if (chestType != ChestType.SINGLE) {
                        val facing = blockState.getValue(ChestBlock.FACING)
                        val otherOffset =
                            if (chestType == ChestType.RIGHT) facing.clockWise else facing.counterClockWise
                        val otherPos = entity.blockPos.relative(otherOffset)
                        val otherState = world.getBlockState(otherPos)

                        // ダブルチェストの場合、2つのBlockPosに分割して保存
                        if (otherState.block == Blocks.CHEST && otherState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                            val singleChestSize = 27
                            val firstHalf = containerItems.take(singleChestSize)
                            val secondHalf = containerItems.drop(singleChestSize)

                            val leftPos = if (chestType == ChestType.RIGHT) entity.blockPos else otherPos
                            val rightPos = if (chestType == ChestType.LEFT) entity.blockPos else otherPos

                            scannedInventoryData[dimension]!![leftPos] = InventoryData(inventoryType, firstHalf)
                            scannedInventoryData[dimension]!![rightPos] = InventoryData(inventoryType, secondHalf)
                        } else {
                            // シングルチェストとして保存
                            scannedInventoryData[dimension]!![entity.blockPos] = InventoryData(inventoryType, containerItems)
                        }
                    } else {
                        // シングルチェストとして保存
                        scannedInventoryData[dimension]!![entity.blockPos] = InventoryData(inventoryType, containerItems)
                    }
                } else {
                    // その他のチェストとして保存 (例: Trapped Chest)
                    scannedInventoryData[dimension]!![entity.blockPos] = InventoryData(inventoryType, containerItems)
                }
            } else if (containerSize > 0) {
                // チェスト以外のコンテナとして保存
                scannedInventoryData[dimension]!![entity.blockPos] = InventoryData(inventoryType, containerItems)
            }

            // 全てのコンテナのスキャン終了処理
            scanTargetBlockEntity = null
            Minecraft.getInstance().connection?.send(ServerboundContainerClosePacket(syncId))
        }
    }

    fun getChestContents(pos: BlockPos): InventoryData? {
        val dimension = getDimensionKey()
        return scannedInventoryData[dimension]?.get(pos)
    }

    private fun getDataDirectory(dimension: String? = null): Path {
        val gameDir = FabricLoader.getInstance().gameDir
        val isSinglePlayer = client.hasSingleplayerServer() // Check if integrated server is running
        val serverName =
            if (isSinglePlayer) {
                client.singleplayerServer
                    ?.getWorldPath(LevelResource.ROOT)
                    ?.parent
                    ?.fileName
                    ?.toString() ?: "single_player_world" // Use world name for single player
            } else {
                client.currentServer?.ip ?: "multi_player_server" // Use server address for multiplayer
            }
        val dataName = "inventories"
        // Use a default dimension key or the provided one
        val dimensionKey = dimension ?: getDimensionKey()
        return gameDir
            .resolve("infinite")
            .resolve("data")
            .resolve(if (isSinglePlayer) "single_player" else "multi_player")
            .resolve(serverName)
            .resolve(dimensionKey)
            .resolve(dataName)
    }

    // Helper function to get a clean dimension key
    private fun getDimensionKey(): String {
        val world = Minecraft.getInstance().level ?: return "minecraft_overworld"
        val dimensionId = world.dimension().identifier()
        return dimensionId?.toString()?.replace(":", "_") ?: "minecraft_overworld"
    }

    private fun getChunkKey(pos: BlockPos): String {
        val chunkX = pos.x shr 4
        val chunkZ = pos.z shr 4
        return "${chunkX}_$chunkZ"
    }

    @Serializable
    data class SerializableItemStack(
        val itemId: String,
        val count: Int,
    )

    @Serializable
    data class SerializableInventoryData(
        val type: String,
        val items: List<SerializableItemStack>,
    )

    @Serializable
    data class ChunkInventoryData(
        val inventories: Map<String, SerializableInventoryData>,
    )

    fun saveData() {
        val json = Json { prettyPrint = true }

        for ((dimension, invDataMap) in scannedInventoryData) {
            val dataDir = getDataDirectory(dimension).toFile()
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }

            val chunks = mutableMapOf<String, MutableMap<String, SerializableInventoryData>>()
            for ((pos, invData) in invDataMap) {
                val chunkKey = getChunkKey(pos)
                val posKey = "${pos.x}_${pos.y}_${pos.z}"
                val serialItems =
                    invData.items.map { SerializableItemStack(BuiltInRegistries.ITEM.getKey(it.item).toString(), it.count) }
                val serialData = SerializableInventoryData(invData.type.name, serialItems)
                if (!chunks.containsKey(chunkKey)) {
                    chunks[chunkKey] = mutableMapOf()
                }
                chunks[chunkKey]!![posKey] = serialData
            }

            for ((chunkKey, invs) in chunks) {
                val chunkFile = dataDir.resolve("chunk_$chunkKey.json")
                val chunkData = ChunkInventoryData(invs)
                val jsonString = json.encodeToString(ChunkInventoryData.serializer(), chunkData)
                chunkFile.writeText(jsonString)
            }
        }
    }

    fun loadData(dimension: String) {
        val json = Json { prettyPrint = true }

        val dataDir = getDataDirectory(dimension).toFile()
        if (!dataDir.exists()) {
            return
        }

        scannedInventoryData[dimension] = mutableMapOf()
        dataDir.listFiles()?.filter { it.name.startsWith("chunk_") && it.name.endsWith(".json") }?.forEach { file ->
            val jsonString = file.readText()
            val chunkData = json.decodeFromString(ChunkInventoryData.serializer(), jsonString)
            for ((posKey, serialData) in chunkData.inventories) {
                val (x, y, z) = posKey.split("_").map { it.toInt() }
                val pos = BlockPos(x, y, z)
                val items =
                    serialData.items.map {
                        val item = BuiltInRegistries.ITEM.getValue(Identifier.parse(it.itemId))
                        ItemStack(item, it.count)
                    }
                val invType = InventoryType.valueOf(serialData.type)
                scannedInventoryData[dimension]!![pos] = InventoryData(invType, items)
            }
        }
    }

    override fun render2d(graphics2D: Graphics2D) {
        DetailInfoRenderer.render(graphics2D, Minecraft.getInstance() ?: return, this)
    }

    override fun stop() {
        saveData()
    }
}
