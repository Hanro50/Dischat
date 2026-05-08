package za.net.hanro50.dischat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import za.net.hanro50.dischat.core.Constants;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
  @Inject(method = "stopServer", at = @At(value = "HEAD"))
  private void stopServer(CallbackInfo info) {
    if (Constants.core != null)
      Thread.startVirtualThread(
          () -> Constants.core.kill());
  }

  @Inject(method = "buildServerStatus", at = @At("TAIL"))
  private void onBuildServerStatus(CallbackInfoReturnable<ServerStatus> cir) {
    ServerStatus status = cir.getReturnValue();
    if (status != null && Constants.core != null) {
      ServerStatus.Favicon favicon = status.favicon().orElse(null);
      Constants.LOGGER.debug("[Dischat] Server status cache rebuilt! Icon present: " + (favicon != null));
    }
  }
}
