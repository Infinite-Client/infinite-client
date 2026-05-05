package org.infinite.infinite.features.local.level.xray

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockListProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.lwjgl.glfw.GLFW

class XRayFeature : LocalFeature() {
    override val featureType = FeatureLevel.Cheat
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_X

    enum class Method {
        OnlyExposed,
        Full,
        TransparencyExposed,
        TransparencyFull,
    }

    val method by property(EnumSelectionProperty(Method.Full))
    val transparency by property(FloatProperty(0.5f, 0f, 1f))

    init {
        method.addListener { _, _ ->
            if (isEnabled()) {
                reload()
            }
        }
    }

    val whiteListBlock by property(
        BlockListProperty(
            listOf(
                "minecraft:water",
                "minecraft:lava",
                "minecraft:chest",
                "minecraft:trapped_chest",
                "minecraft:ender_chest",
                "minecraft:barrel",
                "minecraft:shulker_box",
                "minecraft:white_shulker_box",
                "minecraft:orange_shulker_box",
                "minecraft:magenta_shulker_box",
                "minecraft:light_blue_shulker_box",
                "minecraft:yellow_shulker_box",
                "minecraft:lime_shulker_box",
                "minecraft:pink_shulker_box",
                "minecraft:gray_shulker_box",
                "minecraft:light_gray_shulker_box",
                "minecraft:cyan_shulker_box",
                "minecraft:purple_shulker_box",
                "minecraft:blue_shulker_box",
                "minecraft:brown_shulker_box",
                "minecraft:green_shulker_box",
                "minecraft:red_shulker_box",
                "minecraft:black_shulker_box",
                "minecraft:glass",
                "minecraft:glass_pane",
                "minecraft:white_stained_glass",
                "minecraft:orange_stained_glass",
                "minecraft:magenta_stained_glass",
                "minecraft:light_blue_stained_glass",
                "minecraft:yellow_stained_glass",
                "minecraft:lime_stained_glass",
                "minecraft:pink_stained_glass",
                "minecraft:gray_stained_glass",
                "minecraft:light_gray_stained_glass",
                "minecraft:cyan_stained_glass",
                "minecraft:purple_stained_glass",
                "minecraft:blue_stained_glass",
                "minecraft:brown_stained_glass",
                "minecraft:green_stained_glass",
                "minecraft:red_stained_glass",
                "minecraft:black_stained_glass",
                // --- 全16色のステンドグラス板 ---
                "minecraft:white_stained_glass_pane",
                "minecraft:orange_stained_glass_pane",
                "minecraft:magenta_stained_glass_pane",
                "minecraft:light_blue_stained_glass_pane",
                "minecraft:yellow_stained_glass_pane",
                "minecraft:lime_stained_glass_pane",
                "minecraft:pink_stained_glass_pane",
                "minecraft:gray_stained_glass_pane",
                "minecraft:light_gray_stained_glass_pane",
                "minecraft:cyan_stained_glass_pane",
                "minecraft:purple_stained_glass_pane",
                "minecraft:blue_stained_glass_pane",
                "minecraft:brown_stained_glass_pane",
                "minecraft:green_stained_glass_pane",
                "minecraft:red_stained_glass_pane",
                "minecraft:black_stained_glass_pane",
            ),
        ),
    )
    val targetBlocks by property(
        BlockListProperty(
            listOf(
                "minecraft:coal_ore",
                "minecraft:deepslate_coal_ore",
                "minecraft:iron_ore",
                "minecraft:deepslate_iron_ore",
                "minecraft:gold_ore",
                "minecraft:deepslate_gold_ore",
                "minecraft:redstone_ore",
                "minecraft:deepslate_redstone_ore",
                "minecraft:lapis_ore",
                "minecraft:deepslate_lapis_ore",
                "minecraft:diamond_ore",
                "minecraft:deepslate_diamond_ore",
                "minecraft:emerald_ore",
                "minecraft:deepslate_emerald_ore",
                "minecraft:copper_ore",
                "minecraft:deepslate_copper_ore",
                "minecraft:nether_gold_ore",
                "minecraft:nether_quartz_ore",
                "minecraft:ancient_debris",
                "minecraft:amethyst_cluster",
                "minecraft:budding_amethyst",
                "minecraft:spawner",
                "minecraft:chest",
                "minecraft:trapped_chest",
                "minecraft:ender_chest",
                "minecraft:barrel",
                "minecraft:shulker_box",
                "minecraft:white_shulker_box",
                "minecraft:orange_shulker_box",
                "minecraft:magenta_shulker_box",
                "minecraft:light_blue_shulker_box",
                "minecraft:yellow_shulker_box",
                "minecraft:lime_shulker_box",
                "minecraft:pink_shulker_box",
                "minecraft:gray_shulker_box",
                "minecraft:light_gray_shulker_box",
                "minecraft:cyan_shulker_box",
                "minecraft:purple_shulker_box",
                "minecraft:blue_shulker_box",
                "minecraft:brown_shulker_box",
                "minecraft:green_shulker_box",
                "minecraft:red_shulker_box",
                "minecraft:black_shulker_box",
                "minecraft:raw_iron_block",
                "minecraft:raw_gold_block",
                "minecraft:raw_copper_block",
                "minecraft:tnt",
                "minecraft:anvil",
                "minecraft:beacon",
                "minecraft:brewing_stand",
                "minecraft:crafting_table",
                "minecraft:dispenser",
                "minecraft:dropper",
                "minecraft:enchanting_table",
                "minecraft:furnace",
                "minecraft:hopper",
                "minecraft:ladder",
                "minecraft:torch",
                "minecraft:water",
                "minecraft:lava",
            ),
        ),
    )

    override fun onEnabled() {
        reload()
    }

    override fun onDisabled() {
        reload()
    }

    // チャンクリロード用
    fun reload() {
        minecraft.levelRenderer.allChanged()
    }

    fun getBlockId(state: BlockState): String = BuiltInRegistries.BLOCK.getKey(state.block).toString()

    fun getNeighborBlockId(world: BlockGetter, pos: BlockPos, direction: Direction): String {
        val neighborPos = pos.relative(direction)
        val neighborState = world.getBlockState(neighborPos)
        return getBlockId(neighborState)
    }

    /**
     * LiquidBlockRenderer 用の X-Ray 判定ロジック
     */
    fun shouldApply(
        blockState: BlockState, // 現在の流体
    ): Boolean {
        if (!isEnabled()) return false

        // 1. IDの抽出 (FluidState から取得するのが正確)
        val blockId = getBlockId(blockState)

        // 2. リストに含まれているか判定
        val isOre = targetBlocks.value.contains(blockId)
        val isThrough = whiteListBlock.value.contains(blockId)
        return (isOre || isThrough)
    }

    fun atModelBlockRenderer(
        blockState: BlockState,
        direction: Direction,
        blockPos: BlockPos,
        original: Boolean,
    ): Boolean {
        if (!isEnabled()) return original

        val currentBlockId = getBlockId(blockState)
        val isTarget = targetBlocks.value.contains(currentBlockId)
        val isWhitelist = whiteListBlock.value.contains(currentBlockId)

        return when (method.value) {
            Method.OnlyExposed, Method.Full -> isTarget || isWhitelist
            Method.TransparencyExposed, Method.TransparencyFull -> isTarget || isWhitelist || original
        }
    }
}
