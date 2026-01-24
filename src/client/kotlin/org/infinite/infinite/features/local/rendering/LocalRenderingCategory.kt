package org.infinite.infinite.features.local.rendering

import org.infinite.infinite.features.local.rendering.brightsight.BrightSightFeature
import org.infinite.infinite.features.local.rendering.clearsight.ClearSightFeature
import org.infinite.infinite.features.local.rendering.hello.HelloFeature
import org.infinite.infinite.features.local.rendering.toughsight.ToughSightFeature
import org.infinite.infinite.features.local.rendering.ui.UltraUiFeature
import org.infinite.infinite.features.local.rendering.zoomsight.ZoomSightFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

@Suppress("Unused")
class LocalRenderingCategory : LocalCategory() {
    val brightSightFeature by feature(BrightSightFeature())
    val clearSightFeature by feature(ClearSightFeature())
    val toughSightFeature by feature(ToughSightFeature())
    val helloFeature by feature(HelloFeature())
    val ultraUiFeature by feature(UltraUiFeature())
    val zoomSightFeature by feature(ZoomSightFeature())
}
