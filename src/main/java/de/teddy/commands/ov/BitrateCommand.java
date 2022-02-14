package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class BitrateCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient) {
        if (user instanceof Member) {
            return ((Member) user).getVoiceState()
                    .flatMap(voiceState -> {
                        Optional<Snowflake> channelId = voiceState.getChannelId();

                        if (channelId.isEmpty()
                                || !HasPermission.hasPermissionToEdit(channelId.get().asLong(), user.getId().asLong()))
                            return Mono.empty();
                        int i;
                        try{
                            i = Integer.parseInt(args[0]);

                            if (i < 7)
                                return messageChannel.createMessage("The value should be greater than or equal to 8kbs.");
                            if (i > 97)
                                return messageChannel.createMessage("The value should be lower than or equal to 96kbs.");
                        }catch(ArrayIndexOutOfBoundsException e){
                            i = 64;
                        }catch(NumberFormatException e){
                            return messageChannel.createMessage("The given argument is not an integer");
                        }

                        int finalI = i;
                        return voiceState.getChannel()
                                .flatMap(voiceChannel ->
                                        voiceChannel.edit()
                                                .withBitrate(finalI * 1000)
                                                .then(messageChannel.createMessage("Bitrate successfully updated from " + voiceChannel.getBitrate() + " to " + finalI + "kbs.")));
                    });
        }
        return Mono.empty();
    }
}
