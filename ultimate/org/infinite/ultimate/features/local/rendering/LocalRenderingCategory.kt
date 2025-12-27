package org.infinite.ultimate.features.local.rendering

import org.infinite.libs.core.features.categories.category.LocalCategory
import org.infinite.ultimate.features.local.rendering.hello.HelloFeature

class LocalRenderingCategory : LocalCategory() {
    val helloFeature by feature(HelloFeature())
}
