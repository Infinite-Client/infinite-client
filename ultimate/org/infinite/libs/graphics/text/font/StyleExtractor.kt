package org.infinite.libs.graphics.text.font

import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSink

class StyleExtractor : FormattedCharSink {
    var foundStyle: Style = Style.EMPTY
    override fun accept(index: Int, style: Style, codePoint: Int): Boolean {
        this.foundStyle = style
        return false // 最初の1文字で終了
    }
}
