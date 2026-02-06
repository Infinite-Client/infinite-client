package org.infinite.libs.core.features.property

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.infinite.libs.core.features.Property
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.ui.widgets.ListPropertyWidget
import org.infinite.libs.ui.widgets.PropertyWidget

/**
 * リスト形式の設定プロパティ
 * @param T リスト内の要素の型
 * @param default デフォルトのリスト内容
 */

abstract class ListProperty<T : Any>(
    default: List<T>,
) : Property<List<T>>(default.toList()) {

    protected val internalList = java.util.concurrent.CopyOnWriteArrayList<T>(default)

    override fun tryApply(anyValue: Any?) {
        if (anyValue == null) return

        val newList: List<T>? = when (anyValue) {
            // JSONからの読み込みでよく来るパターン
            is JsonArray -> {
                anyValue.mapNotNull { element ->
                    convertFromJsonElement(element)
                }
            }

            is List<*> -> {
                anyValue.mapNotNull { it?.let { convertElement(it) } }
            }

            is String -> {
                // カンマ区切り文字列からの復元（オプション。必要なければ削除可）
                anyValue.split(",")
                    .map { it.trim() }
                    .mapNotNull { convertElement(it) }
            }

            else -> null
        }

        if (newList != null) {
            internalList.clear()
            internalList.addAll(newList)
            sync()
        }
    }

    /**
     * JSONからの値（JsonElement）を T に変換する
     * これが重要！特に String の場合 .content を取り出す
     */
    protected open fun convertFromJsonElement(element: JsonElement): T? = when (element) {
        is JsonPrimitive -> {
            if (element.isString) {
                @Suppress("UNCHECKED_CAST")
                element.content as? T
            } else {
                convertElement(element.content) // 数値やboolなどもここで
            }
        }

        else -> convertElement(element) // フォールバック
    }

    /**
     * 従来の任意のオブジェクトから T への変換（既存コードを維持）
     */
    protected abstract fun convertElement(anyValue: Any): T?

    // 以下は変更なし
    abstract fun createInputWidget(
        x: Int,
        y: Int,
        width: Int,
        initialValue: T?,
        onComplete: (T?) -> Unit,
    ): net.minecraft.client.gui.components.AbstractWidget

    abstract fun renderElement(graphics2D: Graphics2D, item: T, x: Int, y: Int, width: Int, height: Int)

    fun add(element: T) {
        internalList.add(element)
        sync()
    }

    fun removeAt(index: Int) {
        if (index in internalList.indices) {
            internalList.removeAt(index)
            sync()
        }
    }

    fun replaceAt(index: Int, newValue: T) {
        if (index in internalList.indices) {
            internalList[index] = newValue
            sync()
        }
    }

    override fun widget(x: Int, y: Int, width: Int): PropertyWidget<ListProperty<T>> = ListPropertyWidget(x, y, width, this)

    private fun sync() {
        value = internalList.toList()
    }
}
