package org.infinite.libs.core.features.property

import kotlinx.serialization.json.JsonPrimitive
import org.infinite.libs.core.features.Property
import org.infinite.libs.ui.widgets.PropertyWidget
import org.infinite.libs.ui.widgets.StringPropertyWidget

/**
 * 自由な文字列入力を管理するプロパティ
 * @param default デフォルトの文字列
 * @param regex バリデーション用の正規表現（任意）
 */
class StringProperty(
    default: String,
    val regex: Regex? = null,
) : Property<String>(default) {
    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return
        val newValue = when (anyValue) {
            is JsonPrimitive -> anyValue.content
            else -> anyValue.toString()
        }
        this.value = newValue
    }

    override fun filterValue(newValue: String): String {
        // 正規表現にマッチしない場合は、変更を拒否して現在の値を返す
        return if (regex == null || regex.matches(newValue)) {
            newValue
        } else {
            value
        }
    }

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<out Property<String>> = StringPropertyWidget(x, y, width, this)
}
