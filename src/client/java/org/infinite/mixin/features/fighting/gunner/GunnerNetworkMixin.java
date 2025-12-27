package org.infinite.mixin.features.fighting.gunner;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPacketListener.class)
public class GunnerNetworkMixin {}
