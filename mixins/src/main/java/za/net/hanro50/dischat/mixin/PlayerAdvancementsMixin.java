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
      String[] parts = advancementHolder.id().toShortLanguageKey().split("/");
      if (parts.length != 2 || Constants.core == null)
        return;
      Chater chater = new Chater(this.player.getStringUUID(), this.player.getPlainTextName());

      Constants.core.sendAdvancement(chater, parts[0], parts[1]);
    });

  }
}
