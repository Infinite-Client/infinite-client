package org.infinite.infinite.theme.stylish

import org.infinite.libs.ui.theme.ColorScheme

class StylishColorScheme : ColorScheme() {
    override val schemeType = SchemeType.Dark

    /**
     * 背景: 非常に深い紺/紫がかった黒
     * 以前の 0.05f は暗すぎたため、少し持ち上げてディテールが見えるように調整
     */
    override val backgroundColor: Int
        get() = color(260f, 0.2f, 0.08f, 0.85f)

    /**
     * サーフェス (パネルなど):
     * 背景よりわずかに明るくし、UIの境界を明確にする
     */
    override val surfaceColor: Int
        get() = color(260f, 0.15f, 0.14f, 1f)

    /**
     * アクセント: スタイリッシュなネオンバイオレット
     * 彩度を上げ、輝度を 0.6f 程度にすることで目に刺さらない鮮やかさを確保
     */
    override val accentColor: Int
        get() = color(275f, 0.8f, 0.65f, 1f)

    /**
     * フォアグラウンド (メインテキスト):
     * 完全に白だとコントラストが強すぎるため、わずかに青みを入れたオフホワイト
     */
    override val foregroundColor: Int
        get() = color(260f, 0.05f, 0.95f, 1f)

    /**
     * セカンダリ (Muted/サブテキスト):
     * テキストとして読める限界の明るさを維持しつつ、彩度を落とす
     */
    override val secondaryColor: Int
        get() = color(260f, 0.1f, 0.6f, 1f)
}
