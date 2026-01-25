package org.infinite.libs.addon

import org.infinite.InfiniteClient

interface InfiniteAddon {
    /**
     * InfiniteClientの初期化中に呼び出されます。
     * ここでテーマの登録や、カスタム機能の注入を行います。
     */
    fun onInitializeAddon(client: InfiniteClient)
}
