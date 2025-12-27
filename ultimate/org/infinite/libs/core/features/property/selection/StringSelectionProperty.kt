package org.infinite.libs.core.features.property.selection

import org.infinite.libs.core.features.property.SelectionProperty

/**
 * 文字列のリストから1つを選択するプロパティ
 */
class StringSelectionProperty(
    default: String,
    options: List<String>,
) : SelectionProperty<String>(default, options)
