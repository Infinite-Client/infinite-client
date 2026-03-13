package org.infinite.mixin.graphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
  @Accessor("fontManager")
  FontManager getFontManager();

  @Accessor("rightClickDelay")
  void setRightClickDelay(int delay);

  @Accessor("rightClickDelay")
  int getRightClickDelay();
}
