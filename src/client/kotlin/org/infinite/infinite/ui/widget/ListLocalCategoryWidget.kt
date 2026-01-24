package org.infinite.infinite.ui.widget

import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import org.infinite.libs.core.features.categories.category.LocalCategory

/**
 * ローカル（クライアント側）の機能をリスト表示するためのカテゴリウィジェット。
 * 抽象クラス ListCategoryWidget を継承し、具体的なコンテンツ構築ロジックを実装します。
 */
class ListLocalCategoryWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    category: LocalCategory,
) : ListCategoryWidget<LocalCategory>(
    x,
    y,
    width,
    height,
    category,
    Component.literal(category.name),
) {

    /**
     * カテゴリ内の機能（Feature）を垂直レイアウトに追加します。
     * 検索クエリ（query）によるフィルタリングもここで行われます。
     */
    override fun buildContent(layout: LinearLayout, width: Int, query: String) {
        val innerSpacing = 8
        val itemWidth = width - 2 * innerSpacing
        val normalized = query.trim()

        // category (data) 内の全機能をループ
        data.features.forEach { (_, feature) ->
            val matches = normalized.isEmpty() ||
                feature.name.contains(normalized, ignoreCase = true)

            if (matches) {
                // LocalFeatureWidget を生成してレイアウトに追加
                // 引数はプロジェクトの LocalFeatureWidget の定義に合わせて調整してください
                layout.addChild(
                    LocalListFeatureWidget(
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
