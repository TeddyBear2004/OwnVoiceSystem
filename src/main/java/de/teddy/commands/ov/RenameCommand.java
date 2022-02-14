package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public class RenameCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String @NotNull [] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient) {
        if (user instanceof Member) {
            return ((Member) user).getVoiceState()
                    .flatMap(voiceState -> voiceState.getChannel()
                            .filter(voiceChannel -> HasPermission.hasPermissionToEdit(voiceChannel.getId().asLong(), user.getId().asLong()))
                            .flatMap(voiceChannel -> {
                                StringBuilder builder = new StringBuilder();
                                for (String arg : args)
                                    builder.append(arg)
                                            .append(" ");

                                if (builder.toString().equals(""))
                                    return messageChannel.createMessage("The name may not be empty.");
                                return voiceChannel
                                        .edit()
                                        .withName(builder.toString())
                                        .then(messageChannel.createMessage("Name successfully updated."));
                            }));
        }

        return Mono.empty();
    }
}
