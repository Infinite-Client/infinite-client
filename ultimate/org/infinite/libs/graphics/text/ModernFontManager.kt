package org.infinite.libs.graphics.text

import net.minecraft.client.gui.font.FontManager
import net.minecraft.client.renderer.PlayerSkinRenderCache
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.client.resources.model.AtlasManager

class ModernFontManager(
    textureManager: TextureManager,
    atlasManager: AtlasManager,
    playerSkinRenderCache: PlayerSkinRenderCache,
) : FontManager(textureManager, atlasManager, playerSkinRenderCache)
