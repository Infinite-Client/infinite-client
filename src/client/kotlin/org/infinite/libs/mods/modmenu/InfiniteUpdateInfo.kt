package org.infinite.libs.mods.modmenu
import com.terraformersmc.modmenu.api.UpdateChannel
import com.terraformersmc.modmenu.api.UpdateInfo
import net.minecraft.network.chat.Component
import org.jetbrains.annotations.Nullable

data class InfiniteUpdateInfo(
    private val isUpdate: Boolean,
    private val link: String,
    private val channel: UpdateChannel,
    private val message: Component? = null,
) : UpdateInfo {
    override fun isUpdateAvailable(): Boolean = isUpdate

    override fun getUpdateMessage(): @Nullable Component? = message

    override fun getDownloadLink(): String = link

    override fun getUpdateChannel(): UpdateChannel = channel
}
