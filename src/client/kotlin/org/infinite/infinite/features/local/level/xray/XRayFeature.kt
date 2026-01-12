package org.infinite.infinite.features.local.level.xray

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockListProperty
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.lwjgl.glfw.GLFW

class XRayFeature : LocalFeature() {
    override val featureType = FeatureType.Cheat
    override val defaultToggleKey: Int = GLFW.GLFW_KEY_X

    enum class Method {
        OnlyExposed, Full, TransparencyExposed, TransparencyFull,
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
                "minecraft:ancient_debris",
                "minecraft:anvil",
                "minecraft:beacon",
                "minecraft:bone_block",
                "minecraft:bookshelf",
                "minecraft:brewing_stand",
                "minecraft:chain_command_block",
                "minecraft:chest", // ThroughBlockListにもあるが、ExposedBlockListにも残すことで、XRayが有効な時に描画されるようになる
                "minecraft:clay",
                "minecraft:coal_block",
                "minecraft:coal_ore",
                "minecraft:command_block",
                "minecraft:copper_ore",
                "minecraft:crafting_table",
                "minecraft:deepslate_coal_ore",
                "minecraft:deepslate_copper_ore",
                "minecraft:deepslate_diamond_ore",
                "minecraft:deepslate_emerald_ore",
                "minecraft:deepslate_gold_ore",
                "minecraft:deepslate_iron_ore",
                "minecraft:deepslate_lapis_ore",
                "minecraft:deepslate_redstone_ore",
                "minecraft:diamond_block",
                "minecraft:diamond_ore",
                "minecraft:dispenser",
                "minecraft:dropper",
                "minecraft:emerald_block",
                "minecraft:emerald_ore",
                "minecraft:enchanting_table",
                "minecraft:end_portal",
                "minecraft:end_portal_frame",
                "minecraft:ender_chest",
                "minecraft:furnace",
                "minecraft:glowstone",
                "minecraft:gold_block",
                "minecraft:gold_ore",
                "minecraft:hopper",
                "minecraft:iron_block",
                "minecraft:iron_ore",
                "minecraft:ladder",
                "minecraft:lapis_block",
                "minecraft:lapis_ore",
                "minecraft:lava",
                "minecraft:lodestone",
                "minecraft:mossy_cobblestone",
                "minecraft:nether_gold_ore",
                "minecraft:nether_portal",
                "minecraft:nether_quartz_ore",
                "minecraft:raw_copper_block",
                "minecraft:raw_gold_block",
                "minecraft:raw_iron_block",
                "minecraft:redstone_block",
                "minecraft:redstone_ore",
                "minecraft:repeating_command_block",
                "minecraft:spawner",
                "minecraft:suspicious_sand",
                "minecraft:tnt",
                "minecraft:torch",
                "minecraft:trapped_chest",
                "minecraft:water",
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

    fun getBlockId(state: BlockState): String {
        return BuiltInRegistries.BLOCK.getKey(state.block).toString()
    }

    fun getNeighborBlockId(world: BlockGetter, pos: BlockPos, direction: Direction): String {
        val neighborPos = pos.relative(direction)
        val neighborState = world.getBlockState(neighborPos)
        return getBlockId(neighborState)
    }

    /**
     * 指定された FluidState から ID (例: "minecraft:water") を抽出します。
     */
    fun getFluidId(state: FluidState): String {
        return BuiltInRegistries.FLUID.getKey(state.type).toString()
    }

    /**
     * LiquidBlockRenderer 用の X-Ray 判定ロジック
     */
    fun atLiquid(
        fluidState: FluidState, // 現在の流体
        blockState: BlockState, // 現在のブロック
        direction: Direction, // 描画しようとしている面
        neighborFluid: FluidState, // 隣接する流体
        original: Boolean, // バニラの描画判定結果
    ): Boolean {
        if (!isEnabled()) return original

        // 1. IDの抽出 (FluidState から取得するのが正確)
        val currentFluidId = getFluidId(fluidState)

        // 2. リストに含まれているか判定
        val isOre = targetBlocks.value.contains(currentFluidId)
        val isThrough = whiteListBlock.value.contains(currentFluidId)
        return (isOre || isThrough) && original
    }

    fun atModelBlockRenderer(
        blockAndTintGetter: BlockAndTintGetter,
        blockState: BlockState,
        bl: Boolean,
        direction: Direction,
        blockPos: BlockPos,
        original: Boolean,
    ): Boolean {
        if (!isEnabled()) return original
        val level = level ?: return original

        val currentBlockId = getBlockId(blockState)
        val isOreCurrent = targetBlocks.value.contains(currentBlockId)
        val isThroughCurrent = whiteListBlock.value.contains(currentBlockId)

        // 1. 現在判定している「面 (direction)」の隣が、描画を遮るブロック（鉱石やチェスト等）か判定
        val neighborBlockId = getNeighborBlockId(level, blockPos, direction)
        val neighborIsSolidXray =
            targetBlocks.value.contains(neighborBlockId) || whiteListBlock.value.contains(neighborBlockId)

        // 隣が鉱石系なら、パフォーマンスと視認性のためにこの面は絶対に描画しない（内部の面をカット）
        if (neighborIsSolidXray) return false
        // 2. 「露出系モード」の場合、そのブロックが周囲6面のどこかで「透けるブロック」に触れているか判定
        // neighborIsSolidXray が既に false なので、この direction 自身も露出の候補になります
        val isExposedAnywhere = Direction.entries.any { dir ->
            val nId = getNeighborBlockId(level, blockPos, dir)
            nId == "minecraft:air"
        }

        return when (method.value) {
            Method.OnlyExposed -> isOreCurrent && isExposedAnywhere || isThroughCurrent
            Method.TransparencyExposed -> (isOreCurrent && isExposedAnywhere) || original || isThroughCurrent
            Method.Full -> isOreCurrent || isThroughCurrent
            Method.TransparencyFull -> isOreCurrent || isThroughCurrent || original
        }
    }

    /**
     * ItemBlockRenderTypesMixin から呼び出され、
     * そのブロックを半透明レイヤーで描画すべきか判定します。
     */
    fun shouldIsolate(state: BlockState): Boolean {
        if (!isEnabled()) return false

        val id = getBlockId(state)

        // targetBlocks (鉱石など) に含まれている場合は、はっきり見せたいので隔離(Isolate)しない = false
        // それ以外のブロック（石など）は、透かしたいので隔離(Isolate)する = true
        return !targetBlocks.value.contains(id)
    }
}
