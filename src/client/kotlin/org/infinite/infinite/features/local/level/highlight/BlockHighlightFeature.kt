package org.infinite.infinite.features.local.level.highlight

import org.infinite.libs.core.features.feature.LocalFeature
import org.infinite.libs.core.features.property.list.BlockAndColorListProperty
import org.infinite.libs.core.features.property.list.serializer.BlockAndColor
import org.infinite.libs.core.features.property.number.FloatProperty
import org.infinite.libs.core.features.property.number.IntProperty
import org.infinite.libs.core.features.property.selection.EnumSelectionProperty
import org.infinite.libs.graphics.Graphics3D

class BlockHighlightFeature : LocalFeature() {
    override val featureType = FeatureLevel.Utils

    enum class RenderStyle { Lines, Faces, Both }
    enum class ViewFocus { None, Balanced, Strict }
    enum class Animation { None, Pulse, FadeIn }

    val blocksToHighlight by property(
        BlockAndColorListProperty(
            listOf(
                // --- 超重要・貴重ブロック ---
                BlockAndColor("minecraft:ancient_debris", 0x80583431.toInt()),
                BlockAndColor("minecraft:diamond_ore", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:deepslate_diamond_ore", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:diamond_block", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:emerald_ore", 0x8000FF00.toInt()),
                BlockAndColor("minecraft:deepslate_emerald_ore", 0x8000FF00.toInt()),
                BlockAndColor("minecraft:emerald_block", 0x8000FF00.toInt()),

                // --- 一般鉱石 ---
                BlockAndColor("minecraft:gold_ore", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:deepslate_gold_ore", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:nether_gold_ore", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:gold_block", 0x80FFD700.toInt()),
                BlockAndColor("minecraft:iron_ore", 0x80D8AF93.toInt()),
                BlockAndColor("minecraft:deepslate_iron_ore", 0x80D8AF93.toInt()),
                BlockAndColor("minecraft:iron_block", 0x80C0C0C0.toInt()),
                BlockAndColor("minecraft:copper_ore", 0x80E77C5E.toInt()),
                BlockAndColor("minecraft:deepslate_copper_ore", 0x80E77C5E.toInt()),
                BlockAndColor("minecraft:coal_ore", 0x80333333.toInt()),
                BlockAndColor("minecraft:deepslate_coal_ore", 0x80333333.toInt()),
                BlockAndColor("minecraft:lapis_ore", 0x800000FF.toInt()),
                BlockAndColor("minecraft:deepslate_lapis_ore", 0x800000FF.toInt()),
                BlockAndColor("minecraft:redstone_ore", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:deepslate_redstone_ore", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:nether_quartz_ore", 0x80FFFFFF.toInt()),

                // --- ストレージ・ユーティリティ ---
                BlockAndColor("minecraft:chest", 0x80FFA500.toInt()),
                BlockAndColor("minecraft:trapped_chest", 0x80FF4500.toInt()),
                BlockAndColor("minecraft:ender_chest", 0x80800080.toInt()),
                BlockAndColor("minecraft:barrel", 0x808B4513.toInt()),
                BlockAndColor("minecraft:shulker_box", 0x80FF00FF.toInt()),
                BlockAndColor("minecraft:white_shulker_box", 0x80FFFFFF.toInt()),
                BlockAndColor("minecraft:orange_shulker_box", 0x80FFA500.toInt()),
                BlockAndColor("minecraft:magenta_shulker_box", 0x80FF00FF.toInt()),
                BlockAndColor("minecraft:light_blue_shulker_box", 0x80ADD8E6.toInt()),
                BlockAndColor("minecraft:yellow_shulker_box", 0x80FFFF00.toInt()),
                BlockAndColor("minecraft:lime_shulker_box", 0x8000FF00.toInt()),
                BlockAndColor("minecraft:pink_shulker_box", 0x80FFC0CB.toInt()),
                BlockAndColor("minecraft:gray_shulker_box", 0x80808080.toInt()),
                BlockAndColor("minecraft:light_gray_shulker_box", 0x80D3D3D3.toInt()),
                BlockAndColor("minecraft:cyan_shulker_box", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:purple_shulker_box", 0x80800080.toInt()),
                BlockAndColor("minecraft:blue_shulker_box", 0x800000FF.toInt()),
                BlockAndColor("minecraft:brown_shulker_box", 0x808B4513.toInt()),
                BlockAndColor("minecraft:green_shulker_box", 0x80008000.toInt()),
                BlockAndColor("minecraft:red_shulker_box", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:black_shulker_box", 0x80000000.toInt()),

                // --- 構造物・重要地点 ---
                BlockAndColor("minecraft:spawner", 0x801E90FF.toInt()),
                BlockAndColor("minecraft:beacon", 0x8000FFFF.toInt()),
                BlockAndColor("minecraft:enchanting_table", 0x80FF00FF.toInt()),
                BlockAndColor("minecraft:tnt", 0x80FF0000.toInt()),
                BlockAndColor("minecraft:respawn_anchor", 0x80FFFF00.toInt()),
                BlockAndColor("minecraft:lodestone", 0x80C0C0C0.toInt()),

                // --- ポータル・レアブロック ---
                BlockAndColor("minecraft:nether_portal", 0x809932CC.toInt()),
                BlockAndColor("minecraft:end_portal_frame", 0x80006400.toInt()),
                BlockAndColor("minecraft:end_portal", 0x80000000.toInt()),
                BlockAndColor("minecraft:suspicious_sand", 0x80EEDC82.toInt()),
                BlockAndColor("minecraft:suspicious_gravel", 0x80808080.toInt()),
                BlockAndColor("minecraft:budding_amethyst", 0x80A45AEE.toInt()),
                BlockAndColor("minecraft:trial_spawner", 0x80FF8C00.toInt()),
                BlockAndColor("minecraft:vault", 0x80B8860B.toInt()),
            ),
        ),
    )

    val scanRange by property(IntProperty(12, 1, 32, " chunks"))
    val renderRange by property(IntProperty(128, 8, 512, " blocks"))
    val renderStyle by property(EnumSelectionProperty(RenderStyle.Lines))
    val maxDrawCount by property(IntProperty(20000, 1000, 100000, " elements"))

    val lineWidth by property(FloatProperty(1.5f, 0.1f, 5.0f, " px"))
    val viewFocus by property(EnumSelectionProperty(ViewFocus.Balanced))
    val animation by property(EnumSelectionProperty(Animation.Pulse))

    override fun onEndTick() {
        BlockHighlightRenderer.tick(this)
    }

    override fun onDisabled() {
        BlockHighlightRenderer.clear()
    }
}
