package me.vetustus.server.simplechat;

import com.google.gson.Gson;
import me.vetustus.server.simplechat.api.event.PlayerChatCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static me.vetustus.server.simplechat.ChatColor.translateChatColors;

public class SimpleChat implements ModInitializer {
    private ChatConfig config;
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {

        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PlayerChatCallback.EVENT.register((player, message) -> {
            PlayerChatCallback.ChatMessage chatMessage = new PlayerChatCallback.ChatMessage(player, message);

            /*
             * If someone wants to use the mod as a library,
             * they must disable the "enable_chat_mod" parameter,
             * then the chat will not be handled by the mod.
             */
            if (!config.isChatModEnabled())
                return chatMessage;

            chatMessage.setCancelled(true);

            boolean isGlobalMessage = false;
            String chatFormat = "&f%name% &7> &f%message%";;
            if (message.startsWith("!!!")) {
                isGlobalMessage = true;
                chatFormat = config.getGlobalChatFormat();
                message = message.substring(3);
            }
            else if (message.startsWith("+")) {
                isGlobalMessage = true;
                chatFormat = "&f[&aOOC&f] %player% &7> &f%message%";
                message = message.substring(1);
            }
            else if (message.startsWith("$")) {
                isGlobalMessage = false;
                chatFormat = "&f[&aLOOC&f] %player% &7> &f%message%";
                message = message.substring(1);
            }
            else if (message.startsWith("*")) {
                isGlobalMessage = false;
                chatFormat = "&f%name% кричит: &f%message%";
                message = message.substring(1);
            } else if (message.startsWith("==")) {
                isGlobalMessage = false;
                chatFormat = "&f%name% громко кричит: &f%message%";
                message = message.substring(2);
            }
            chatFormat = translateChatColors('&', chatFormat);
            String stringMessage = chatFormat
                    .replaceAll("%player%", player.getName().asString())
                    .replaceAll("%name%", RPAddon.GetPlayername(player))
                    .replaceAll("%message%", message);
            if (config.isChatColorsEnabled())
                stringMessage = translateChatColors('&', stringMessage);

            Text resultMessage = literal(stringMessage);

            List<ServerPlayerEntity> players = Objects.requireNonNull(player.getServer(), "The server cannot be null.")
                    .getPlayerManager().getPlayerList();
            for (ServerPlayerEntity p : players) {
                if (isGlobalMessage) {
                    p.sendMessage(resultMessage, false);
                } else {
                    if (p.squaredDistanceTo(player) <= config.getChatRange()) {
                        p.sendMessage(resultMessage, false);
                    }
                }
            }
            LOGGER.info(stringMessage);
            return chatMessage;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(CommandManager.literal("simplechat").executes(context -> {
                    if (context.getSource().hasPermissionLevel(1)) {
                        try {
                            loadConfig();
                            context.getSource().sendFeedback(literal("Settings are reloaded!"), false);
                        } catch (IOException e) {
                            context.getSource().sendFeedback(literal("An error occurred while reloading the settings (see the console)!"), false);
                            e.printStackTrace();
                        }
                    } else {
                        context.getSource().sendFeedback(literal("You don't have the right to do this! If you think this is an error, contact your server administrator.")
                                .copy().formatted(Formatting.RED), false);
                    }
                    return 1;
                })));

        RPAddon.Register();
    }

    private void loadConfig() throws IOException {
        File configFile = new File(ChatConfig.CONFIG_PATH);
        if (!configFile.exists()) {
            Files.copy(Objects.requireNonNull(
                    this.getClass().getClassLoader().getResourceAsStream("simplechat.json"),
                    "Couldn't find the configuration file in the JAR"), configFile.toPath());
        }
        try {
            config = new Gson().fromJson(new FileReader(ChatConfig.CONFIG_PATH), ChatConfig.class);
        } catch (FileNotFoundException e) {
            config = new ChatConfig();
            e.printStackTrace();
        }
    }

    private Text literal(String text) {
        return new LiteralText(text);
    }
}
