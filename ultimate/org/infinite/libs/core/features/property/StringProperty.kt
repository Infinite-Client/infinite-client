package org.infinite.libs.core.features.property

import org.infinite.libs.core.features.Property

/**
 * 自由な文字列入力を管理するプロパティ
 * @param default デフォルトの文字列
 * @param regex バリデーション用の正規表現（任意）
 */
class StringProperty(
    default: String,
    val regex: Regex? = null,
) : Property<String>(default) {
    private var _value: String = default

    override var value: String
        get() = _value
        set(newValue) {
            // 正規表現が指定されている場合、マッチしたときのみ更新
            if (regex == null || regex.matches(newValue)) {
                _value = newValue
            }
        }
}
