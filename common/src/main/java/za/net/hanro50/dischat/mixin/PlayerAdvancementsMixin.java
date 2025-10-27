package za.net.hanro50.dischat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

  @Shadow
  private ServerPlayer player;

  @Inject(at = @At(value = "TAIL", shift = Shift.BY, by = -2), method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z")
  private void award(AdvancementHolder advancementHolder, String string, CallbackInfoReturnable<Boolean> cir,
      @Local(ordinal = 0) AdvancementProgress advancementprogress, @Local(ordinal = 1) boolean bl2) {
    if (!advancementprogress.isDone() || bl2)
      return;
    advancementHolder.value().display().ifPresent((displayInfo) -> {
      if (!displayInfo.shouldAnnounceChat())
        return;
      String[] parts = advancementHolder.id().getPath().split("/");
      String category;
      String achievement;
      String namespace = advancementHolder.id().getNamespace();
      switch (parts.length) {
        case 1:
          category = namespace;
          achievement = parts[0];
          break;
        case 2:
          category = parts[0];
          achievement = parts[1];
          break;
        default:
        case 0:
          Constants.LOGGER.error("Could not decode acheivement");
          return;

      }

      Chater chater = new Chater(this.player.getStringUUID(), this.player.getName().getString());

      Constants.core.sendAdvancement(chater, namespace, category, achievement);
    });

  }
}
