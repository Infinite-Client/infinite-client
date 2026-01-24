package org.infinite.infinite.ui.widget

import org.infinite.libs.core.features.feature.GlobalFeature

/**
 * LocalCategory 内の各 Feature を表示するための具象ウィジェット。
 */
class GlobalListFeatureWidget(
    x: Int,
    y: Int,
    width: Int,
    feature: GlobalFeature,
) : ListFeatureWidget<GlobalFeature>(x, y, width, feature = feature)
