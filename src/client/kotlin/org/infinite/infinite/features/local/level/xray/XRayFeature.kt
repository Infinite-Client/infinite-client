package org.infinite.infinite.features.local.level.xray

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockListProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty

class XRayFeature : LocalFeature() {
    override val featureType = FeatureType.Cheat

    enum class Method {
        OnlyExposed, Full
    }

    val method by property(EnumSelectionProperty(Method.Full))
    val throughBlocks by property(
        BlockListProperty(
            listOf(
                "minecraft:water",
                "minecraft:lava",
                "minecraft:chest",
                "minecraft:trapped_chest",
                "minecraft:ender_chest",
                "minecraft:barrel",
                "minecraft:shulker_box", // 各種シュルカーボックスは必要に応じて追加
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
                "minecraft:stained_glass", // 各種ステンドグラスは必要に応じて追加
                "minecraft:glass_pane",
                "minecraft:stained_glass_pane",
            ),
        ),
    )
    val oreBlocks by property(
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
}
