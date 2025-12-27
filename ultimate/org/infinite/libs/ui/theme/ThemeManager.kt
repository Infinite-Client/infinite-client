package org.infinite.libs.ui.theme

/**
 * 利用可能なテーマをクラス名で管理するマネージャー
 */
class ThemeManager(defaultTheme: Theme) {
    // Key: クラスの単純名 (例: "DefaultTheme"), Value: インスタンス
    private val themes = mutableMapOf<String, Theme>()

    // デフォルトテーマの名称を保持
    private val defaultThemeName: String = defaultTheme::class.simpleName ?: "UnknownTheme"

    init {
        register(defaultTheme)
    }

    /**
     * テーマを登録する。クラス名をキーとして自動取得する。
     */
    fun register(theme: Theme) {
        val className = theme::class.simpleName
        if (className != null) {
            themes[className] = theme
        }
    }

    /**
     * クラス名でテーマを取得。存在しない場合はデフォルトを返す。
     */
    fun getTheme(className: String): Theme {
        return themes[className] ?: themes[defaultThemeName]
            ?: themes.values.first() // 万が一のためのフォールバック
    }

    /**
     * 現在登録されている全テーマのクラス名（識別子）リストを返す
     * StringSelectionProperty の options に渡すために使用
     */
    fun getRegisteredThemeNames(): List<String> {
        return themes.keys.toList()
    }
}
