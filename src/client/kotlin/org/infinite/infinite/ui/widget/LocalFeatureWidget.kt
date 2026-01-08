package org.infinite.infinite.ui.widget

import org.infinite.libs.core.features.feature.LocalFeature

/**
 * LocalFeature 用の具体的なウィジェット実装
 */
class LocalFeatureWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int = FONT_SIZE + PADDING * 2,
    feature: LocalFeature,
) : FeatureWidget<LocalFeature>(x, y, width, height, feature)
