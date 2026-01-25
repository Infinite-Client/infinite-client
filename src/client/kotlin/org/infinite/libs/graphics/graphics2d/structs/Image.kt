package org.infinite.libs.graphics.graphics2d.structs

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

class Image(
    path: String,
    customWidth: Int? = null,
    customHeight: Int? = null,
) {
    val identifier: Identifier = Identifier.parse(path)

    // 読み込み済みのサイズを保持するプロパティを lazy で定義
    private val dimensions: Pair<Int, Int> by lazy {
        if (customWidth != null && customHeight != null) {
            customWidth to customHeight
        } else {
            val mc = Minecraft.getInstance()
            val resourceManager = mc.resourceManager
            val resource = resourceManager.getResource(identifier)

            if (resource.isPresent) {
                // use で確実にストリームを閉じる
                resource.get().open().use { inputStream ->
                    NativeImage.read(inputStream).use { image ->
                        image.width to image.height
                    }
                }
            } else {
                // エラー時のフォールバック（0,0 を返すか例外を投げるかはお好みで）
                throw IllegalArgumentException("Image not found: $identifier")
            }
        }
    }

    val width: Int get() = dimensions.first
    val height: Int get() = dimensions.second
}
