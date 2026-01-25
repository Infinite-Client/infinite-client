package org.infinite.infinite.ui.widget

import org.infinite.libs.core.features.feature.GlobalFeature

/**
 * LocalFeature 用の具体的なウィジェット実装
 */
class GlobalCarouselFeatureWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int = FONT_SIZE + PADDING * 2,
    feature: GlobalFeature,
) : CarouselFeatureWidget<GlobalFeature>(x, y, width, height, feature)
