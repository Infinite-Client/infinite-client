package org.infinite.infinite.features.utils

import org.infinite.infinite.features.Feature
import org.infinite.infinite.features.FeatureCategory
import org.infinite.infinite.features.utils.afk.AfkMode
import org.infinite.infinite.features.utils.backpack.BackPackManager // 追加
import org.infinite.infinite.features.utils.food.FoodManager
import org.infinite.infinite.features.utils.map.HyperMap
import org.infinite.infinite.features.utils.map.MapFeature
import org.infinite.infinite.features.utils.noattack.NoAttack
import org.infinite.infinite.features.utils.playermanager.PlayerManager
import org.infinite.infinite.features.utils.tool.AutoTool

class UtilsFeatureCategory :
    FeatureCategory(
        "Utils",
        mutableListOf(
            Feature(AfkMode()),
            Feature(AutoTool()),
            Feature(BackPackManager()),
            Feature(NoAttack()),
            Feature(PlayerManager()),
            Feature(HyperMap()),
            Feature(MapFeature()),
            Feature(FoodManager()),
        ),
    )
