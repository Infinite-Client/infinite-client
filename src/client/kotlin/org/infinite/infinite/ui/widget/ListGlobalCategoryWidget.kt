package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.categories.category.GlobalCategory

/**
 * グローバル（サーバー/共有側）の機能をリスト表示するためのカテゴリウィジェット。
 */
class ListGlobalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: GlobalCategory,
) : ListCategoryWidget<GlobalCategory>(
    x,
    y,
    width,
    height,
    category,
    Component.literal(category.name),
) {

    /**
     * カテゴリ内のグローバル機能を垂直レイアウトに追加します。
     */
    override fun buildContent(layout: LinearLayout, width: Int, query: String) {
        val innerSpacing = 8
        val itemWidth = width - 2 * innerSpacing
        val normalized = query.trim()

        data.features.forEach { (_, feature) ->
            val matches = normalized.isEmpty() ||
                feature.name.contains(normalized, ignoreCase = true)

            if (matches) {
                // 具象クラス GlobalListFeatureWidget を使用
                layout.addChild(
                    GlobalListFeatureWidget(
                        x = 0,
                        y = 0,
                        width = itemWidth,
                        feature = feature,
                    ),
                )
            }
        }
    }
}
