package org.infinite.mixin.graphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.infinite.libs.graphics.text.ModernFontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {

  @Redirect(
      method = "<init>",
      at = @At(value = "NEW", target = "net/minecraft/client/gui/font/FontManager"))
  private FontManager redirectFontManager(
      TextureManager textureManager,
      AtlasManager atlasManager,
      PlayerSkinRenderCache playerSkinRenderCache) {
    return new ModernFontManager(textureManager, atlasManager, playerSkinRenderCache);
  }
}
