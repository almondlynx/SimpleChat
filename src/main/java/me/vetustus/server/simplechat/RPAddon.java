package me.vetustus.server.simplechat;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import static net.minecraft.server.command.CommandManager.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RPAddon {
    public static Map<UUID, String> names = new HashMap() {};
    public static Random random = new Random();

    public static void Register()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(
                        CommandManager.literal("character")
                        .then(CommandManager.argument("player", EntityArgumentType.player()).executes(context -> {
                            try {
                                Entity e = EntityArgumentType.getPlayer(context, "player");
                                UUID uuid = e.getUuid();
                                if (names.containsKey(uuid)) {
                                    context.getSource().sendFeedback(
                                            new LiteralText("Name of " + e.getDisplayName().asString() + " is " + names.get(uuid) + "."),
                                            false
                                    );
                                }
                            }
                            catch (Exception e)
                            {
                                SimpleChat.LOGGER.info("Character command threw exception:");
                                SimpleChat.LOGGER.info(e.toString());
                            }
                            return 1;
                        }))
                        .then(CommandManager.argument("nickname", StringArgumentType.greedyString())
                        .executes(context -> {
                            Entity e = EntityArgumentType.getPlayer(context, "player");
                            String s = StringArgumentType.getString(context, "nickname");
                            UUID uuid = e.getUuid();
                            if (s == "clear")
                            {
                                if (names.containsKey(uuid))
                                {
                                    names.remove(uuid);
                                }
                                context.getSource().sendFeedback(
                                        new LiteralText("Name of " + e.getDisplayName().asString() + "cleared."),
                                        false
                                );
                            }
                            else
                            {
                                s = ChatColor.translateChatColors('&', s);
                                names.put(uuid, s);
                                context.getSource().sendFeedback(
                                        new LiteralText("Name of " + e.getDisplayName().asString() + " changed to " + s + "."),
                                        false
                                );
                            }
                            return 1;
                        })
                        )));

    }

    public static String GetPlayername(PlayerEntity player)
    {
        if (player == null) return "";
        UUID uuid = player.getUuid();
        if (names.containsKey(uuid))
            return names.get(uuid);
        return player.getName().asString();
    }

    public static void SendToNearbyPlayers(ServerWorld world, Vec3d pos, double distance, Text message)
    {
        world.getPlayers((p)->p.getPos().isInRange(pos, distance))
                .stream()
                .forEach((p) -> {
                    if (!p.isCreativeLevelTwoOp()) p.sendMessage(message, false);
                });
    }
}
