package za.net.hanro50.dischat;

import net.fabricmc.api.ModInitializer;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class Dischat implements ModInitializer {
	MinecraftServer server;

	@Override
	public void onInitialize() {
		Path path = FabricLoader.getInstance().getConfigDir();
		String version = FabricLoader.getInstance().getRawGameVersion();
		path = Path.of(new File(path.toFile(), Constants.MOD_ID).toURI());
		Core core = new Core(path, version, (chater, message) -> {
			if (server == null)
				return;
			PlayerChatMessage chatMessage;
			String name = chater.name;
			if (chater.minecraftID != null) {
				UUID uuid = UUID.fromString(chater.minecraftID);
				if (server.services().profileResolver().fetchById(uuid).isPresent()) {
					GameProfile user = server.services().profileResolver().fetchById(uuid).get();
					name = user.name();
				}
			}
			chatMessage = PlayerChatMessage.system(message);
			final var fname = name;
			server.getPlayerList().getPlayers().forEach((serverplayer) -> {
				OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(chatMessage);

				ChatType.Bound bind = ChatType.bind(ChatType.CHAT, server.registryAccess(),
						Component.literal(fname));
				serverplayer.sendChatMessage(outgoingChatMessage, false, bind);
			});

		});

		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer _server) -> {
			this.server = _server;
		});
		ServerMessageEvents.CHAT_MESSAGE
				.register((PlayerChatMessage message, ServerPlayer player, ChatType.Bound type) -> {
					Thread.startVirtualThread(
							() -> core.sendChat(new Chater(player.getStringUUID(), player.getName().getString()),
									message.decoratedContent().plainCopy().getString()));
				});
		ServerPlayerEvents.JOIN.register((ServerPlayer player) -> {
			Thread.startVirtualThread(
					() -> core.sendJoin(new Chater(player.getStringUUID(), player.getName().getString())));
		});
		ServerPlayerEvents.LEAVE.register((ServerPlayer player) -> {
			Thread.startVirtualThread(
					() -> core.sendLeave(new Chater(player.getStringUUID(), player.getName().getString())));
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity instanceof Player))
				return;

			Thread.startVirtualThread(
					() -> {

						Chater victem = new Chater(entity.getStringUUID(), entity.getName().getString());

						Deathcause dc = new Deathcause();
						dc.cause = "death.attack." + damageSource.getMsgId();

						Entity attackerEntity = damageSource.getEntity();

						if (attackerEntity != null) {
							if (attackerEntity instanceof Player) {
								dc.cause += ".player";
								Player player = (Player) attackerEntity;
								dc.playerAttacker = new Chater(player.getStringUUID(), player.getName().getString());
								dc.name = dc.playerAttacker.name;
							} else {
								dc.attacker = attackerEntity.getType().getDescriptionId();
								if (attackerEntity.hasCustomName())
									dc.name = attackerEntity.getCustomName().getString();
							}
						}

						core.sendDeath(victem, dc);
					});
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("linkme").executes(context -> {
				var source = context.getSource().getEntity();
				if (source instanceof ServerPlayer) {
					ServerPlayer player = (ServerPlayer) source;
					core.data.requestLink(player.getStringUUID(), (code) -> {
						context.getSource().sendSystemMessage(Component.literal(
								"Link code is <" + code + ">\nUse the /link command on the bot to complete linking"));
					});
				}
				return 1;
			}));
		});

	}
}