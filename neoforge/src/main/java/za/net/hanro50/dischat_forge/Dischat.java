package za.net.hanro50.dischat_forge;

import java.nio.file.Path;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import za.net.hanro50.dischat.Chater;
import za.net.hanro50.dischat.Constants;
import za.net.hanro50.dischat.Core;
import za.net.hanro50.dischat.Deathcause;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = Constants.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class Dischat {
    ModContainer modContainer;
    Core core;
    MinecraftServer server;

    public Dischat(IEventBus modEventBus, ModContainer modContainer) {
        this.modContainer = modContainer;
        NeoForge.EVENT_BUS.register(this);

    }

    private void broadcast(Chater chater, String message) {
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
    }

    @SubscribeEvent
    // Heals an entity by half a heart every time they jump.
    private void onChatEvent(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Thread.startVirtualThread(
                () -> core.sendChat(new Chater(player.getStringUUID(), player.getName().getString()),
                        event.getMessage().plainCopy().getString()));

    }

    @SubscribeEvent
    private void onDeathEvent(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        DamageSource damageSource = event.getSource();
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
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        server = event.getServer();

        Thread.startVirtualThread(
                () -> {
                    Constants.LOGGER
                            .info(event.getServer().getFile("config/" + Constants.MOD_ID).toAbsolutePath().toString());

                    Path config = server.getFile("config/" + Constants.MOD_ID).toAbsolutePath();
                    core = new Core(config, server.getServerVersion(), this::broadcast);
                });

    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        Thread.startVirtualThread(
                () -> core.sendJoin(new Chater(player.getStringUUID(), player.getName().getString())));
    }

    @SubscribeEvent
    public void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();

        Thread.startVirtualThread(
                () -> core.sendLeave(new Chater(player.getStringUUID(), player.getName().getString())));
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("linkme").executes((command) -> execute(command)));
    }

    private int execute(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) command.getSource().getEntity();

            core.data.requestLink(player.getStringUUID(), (code) -> {
                player.sendSystemMessage(Component.literal(
                        "Link code is <" + code + ">\nUse the /link command on the bot to complete linking"));
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}
