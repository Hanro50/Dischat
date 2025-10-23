package za.net.hanro50.dischat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
  @Accessor("statusIcon")
  void dischat$setStatusIcon(ServerStatus.Favicon statusIcon);
}
