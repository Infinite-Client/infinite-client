package org.infinite.infinite.ui.widget

import org.infinite.libs.core.features.feature.LocalFeature

/**
 * LocalCategory 内の各 Feature を表示するための具象ウィジェット。
 */
class LocalListFeatureWidget(
    x: Int,
    y: Int,
    width: Int,
    feature: LocalFeature,
) : ListFeatureWidget<LocalFeature>(x, y, width, feature = feature)
