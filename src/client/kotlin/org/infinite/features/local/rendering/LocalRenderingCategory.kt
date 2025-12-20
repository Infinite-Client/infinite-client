package org.infinite.features.local.rendering

import org.infinite.features.local.rendering.hello.HelloFeature
import org.infinite.libs.core.features.categories.category.LocalCategory

class LocalRenderingCategory : LocalCategory() {
    init {
        insert(HelloFeature())
    }
}
