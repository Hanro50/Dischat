package za.net.hanro50.dischat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import za.net.hanro50.dischat.core.Constants;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
  @Inject(method = "stopServer", at = @At(value = "HEAD"))
  private void stopServer(CallbackInfo info) {
    Thread.startVirtualThread(
        () -> Constants.core.kill());
  }
}
