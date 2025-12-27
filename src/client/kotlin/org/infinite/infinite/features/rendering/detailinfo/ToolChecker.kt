package org.infinite.infinite.features.rendering.detailinfo

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object ToolChecker {
    enum class ToolKind {
        Sword,
        Axe,
        PickAxe,
        Shovel,
        Hoe,
    }

    class CorrectTool(
        val toolKind: ToolKind?,
        val toolLevel: Int,
        val isSilkTouchRequired: Boolean = false,
    ) {
        fun checkPlayerToolStatus(): Int {
            val client = Minecraft.getInstance()
            val player = client.player ?: return 2
            val heldItem: ItemStack = player.mainHandItem

            if (toolKind == null) return 0

            val toolId = BuiltInRegistries.ITEM.getKey(heldItem.item).toString()
            val materialStr = toolId.substringAfter("minecraft:").substringBeforeLast("_")
            val isCorrectToolKind =
                when (toolKind) {
                    ToolKind.PickAxe -> toolId.endsWith("_pickaxe")
                    ToolKind.Axe -> toolId.endsWith("_axe")
                    ToolKind.Shovel -> toolId.endsWith("_shovel")
                    ToolKind.Sword -> toolId.endsWith("_sword")
                    ToolKind.Hoe -> toolId.endsWith("_hoe")
                }

            if (!isCorrectToolKind) return 2

            val actualLevel =
                when (materialStr) {
                    "wooden", "golden" -> 0
                    "stone" -> 1
                    "iron" -> 2
                    "diamond" -> 3
                    "netherite" -> 4
                    else -> -1
                }

            if (actualLevel < 0 || actualLevel < toolLevel) return 2

            if (isSilkTouchRequired) {
                val hasSilkTouch = heldItem.enchantments.keySet().any { it == Enchantments.SILK_TOUCH }
                if (!hasSilkTouch) return 1
            }

            return 0
        }

        fun getId(): String? {
            if (toolKind == null) return null
            val material =
                when {
                    toolLevel >= 4 -> "netherite"
                    toolLevel == 3 -> "diamond"
                    toolLevel == 2 -> "iron"
                    toolLevel == 1 -> "stone"
                    else -> "wooden"
                }
            val toolSuffix =
                when (toolKind) {
                    ToolKind.PickAxe -> "pickaxe"
                    ToolKind.Shovel -> "shovel"
                    ToolKind.Axe -> "axe"
                    ToolKind.Hoe -> "hoe"
                    ToolKind.Sword -> "sword"
                }
            return "minecraft:${material}_$toolSuffix"
        }
    }

    fun isSilkTouchRequiredClient(block: Block): Boolean {
        val state = block.defaultBlockState()
        val id = BuiltInRegistries.BLOCK.getKey(block).path

        if (id.endsWith("_ore") || id == "ancient_debris") return true
        if (block == Blocks.STONE || block == Blocks.DEEPSLATE) return true
        if (block == Blocks.GILDED_BLACKSTONE) return true
        if (id.contains("glass") ||
            id.contains("ice") ||
            block == Blocks.BLUE_ICE ||
            block == Blocks.PACKED_ICE ||
            block == Blocks.FROSTED_ICE
        ) {
            return block != Blocks.FROSTED_ICE
        }
        if (block == Blocks.GLOWSTONE) return true
        if (block == Blocks.COBWEB) return true
        if (block == Blocks.SEA_LANTERN) return true
        if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM || block == Blocks.PODZOL || block == Blocks.DIRT_PATH) return true
        if (block == Blocks.ENDER_CHEST) return true
        if (block == Blocks.BEEHIVE || block == Blocks.BEE_NEST) return true
        if (state.`is`(BlockTags.LEAVES)) return true
        val amethystId = BuiltInRegistries.BLOCK.getKey(block).path
        if (amethystId.startsWith("small_amethyst_bud") ||
            amethystId.startsWith("medium_amethyst_bud") ||
            amethystId.startsWith("large_amethyst_bud") ||
            amethystId == "amethyst_cluster"
        ) {
            return true
        }
        if (state.`is`(BlockTags.CORAL_BLOCKS) || state.`is`(BlockTags.CORAL_PLANTS)) return true
        return false
    }

    fun getCorrectTool(block: Block): CorrectTool {
        val state = block.defaultBlockState()
        val toolLevel =
            when {
                state.`is`(BlockTags.NEEDS_STONE_TOOL) -> 1
                state.`is`(BlockTags.NEEDS_IRON_TOOL) -> 2
                state.`is`(BlockTags.NEEDS_DIAMOND_TOOL) -> 3
                else -> 0
            }
        val toolKind =
            when {
                state.`is`(BlockTags.MINEABLE_WITH_AXE) -> ToolKind.Axe

                state.`is`(BlockTags.MINEABLE_WITH_PICKAXE) -> ToolKind.PickAxe

                state.`is`(BlockTags.MINEABLE_WITH_SHOVEL) -> ToolKind.Shovel

                state.`is`(BlockTags.MINEABLE_WITH_HOE) -> ToolKind.Hoe

                state.`is`(BlockTags.LEAVES) || BuiltInRegistries.BLOCK
                    .getKey(block)
                    .toString() == "minecraft:cobweb" -> ToolKind.Sword

                else -> null
            }
        val isSilkTouchRequired = isSilkTouchRequiredClient(block)
        return if (toolKind == null) {
            CorrectTool(null, -1, false)
        } else {
            CorrectTool(toolKind, toolLevel, isSilkTouchRequired)
        }
    }

    fun getItemStackFromId(id: String): ItemStack = try {
        val identifier = Identifier.parse(id)
        val item = BuiltInRegistries.ITEM.getValue(identifier)
        if (item != Items.AIR) ItemStack(item) else ItemStack(Items.BARRIER)
    } catch (_: Exception) {
        ItemStack(Items.BARRIER)
    }
}
