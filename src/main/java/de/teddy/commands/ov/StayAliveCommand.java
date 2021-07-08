package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public class StayAliveCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            return ((Member)user).getVoiceState()
                    .flatMap(voiceState ->
                            voiceState
                                    .getChannel()
                                    .filter(voiceChannel -> HasPermission.hasPermissionToEdit(voiceChannel.getId().asLong(), user.getId().asLong()))
                                    .flatMap(voiceChannel -> {
                                        try{
                                            long l = Long.parseLong(args[0]);

                                            if(l > 25)
                                                return messageChannel.createMessage("The value should be lower than or equal to 24.");

                                            if(l < 0)
                                                return messageChannel.createMessage("The value should be lower than or equal to 24.");

                                            return Handler.PRIVATE_VOICE_CHANNEL.updateTtl(
                                                    voiceChannel.getId().asLong(),
                                                    System.currentTimeMillis() + l * 3_600_000L)
                                                    .then(messageChannel.createMessage("The time to live counter was updated."));

                                        }catch(ArrayIndexOutOfBoundsException e){
                                            return messageChannel.createMessage("The size of arguments is too low.");
                                        }catch(NumberFormatException e){
                                            return messageChannel.createMessage("The given argument is not an integer");
                                        }
                                    }));
        }

        return Mono.empty();
    }
}
