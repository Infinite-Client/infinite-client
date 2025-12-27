package org.infinite.mixin.graphics;

import java.util.Map;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.infinite.UltimateClient;
import org.infinite.features.global.rendering.font.UltimateFontFeature;
import org.infinite.libs.graphics.graphics2d.text.IModernFontManager;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FontManager.class)
public abstract class FontManagerMixin implements IModernFontManager {
  @Shadow @Final private Map<Identifier, FontSet> fontSets;

  @Override
  public @NonNull FontSet ultimate$fontSetFromStyle(Style style) {
    String id;
    UltimateFontFeature ultimateFontFeature =
        UltimateClient.INSTANCE.getGlobalFeatures().getRendering().getUltimateFontFeature();
    if (style.isBold() && style.isItalic()) {
      id = ultimateFontFeature.getBoldItalicFont().getValue();
    } else if (style.isBold()) {
      id = ultimateFontFeature.getBoldFont().getValue();
    } else if (style.isItalic()) {
      id = ultimateFontFeature.getItalicFont().getValue();
    } else {
      id = ultimateFontFeature.getRegularFont().getValue();
    }
    Identifier targetId = Identifier.fromNamespaceAndPath("minecraft", id);
    FontSet set = this.fontSets.get(targetId);
    if (set == null) {
      return this.fontSets.get(Identifier.fromNamespaceAndPath("minecraft", "default"));
    }
    return set;
  }

  @Override
  public @NonNull FontSet ultimate$fontSetFromIdentifier(@NonNull String name) {
    Identifier targetId = Identifier.fromNamespaceAndPath("minecraft", name);
    FontSet set = this.fontSets.get(targetId);
    if (set == null) {
      return this.fontSets.get(Identifier.fromNamespaceAndPath("minecraft", "default"));
    }
    return set;
  }
}
