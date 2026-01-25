package org.infinite.utils

import java.util.*

/**
 * 文字列を lower_snake_case に変換します。
 * 例: "MyFeatureName" -> "my_feature_name"
 */
fun String.toLowerSnakeCase(): String {
    if (this.isEmpty()) return this
    return this.replace("([a-z0-9])([A-Z])".toRegex(), "$1_$2")
        .lowercase(Locale.ROOT)
}

/**
 * 文字列を PascalCase (UpperCamelCase) に変換します。
 * 例: "my_feature_name" -> "MyFeatureName"
 */
fun String.toPascalCase(): String = this.split("_")
    .joinToString("") { it.lowercase(Locale.ROOT).replaceFirstChar { char -> char.uppercase() } }

/**
 * 文字列を camelCase (LowerCamelCase) に変換します。
 * 例: "MyFeatureName" -> "myFeatureName"
 */
fun String.toCamelCase(): String {
    val pascal = this.toPascalCase()
    return pascal.replaceFirstChar { it.lowercase(Locale.ROOT) }
}

/**
 * 文字列を kebab-case に変換します（翻訳キーやファイル名に便利）。
 * 例: "MyFeature" -> "my-feature"
 */
fun String.toKebabCase(): String = this.toLowerSnakeCase().replace("_", "-")
