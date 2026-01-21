package org.infinite.infinite.features.global.control

import org.infinite.infinite.features.global.control.ime.ImeInputFeature
import org.infinite.libs.core.features.categories.category.GlobalCategory

class GlobalControlCategory : GlobalCategory() {
    val imeInputFeature by feature(ImeInputFeature())
}
