package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import de.teddy.commands.ov.util.HasPermission;
import de.teddy.tables.PrivateVoiceChannel;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Entity;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.util.List;
import java.util.stream.Collectors;

public class SaveCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            return ((Member)user).getVoiceState()
                    .flatMap(voiceState -> voiceState.getChannel()
                            .filter(voiceChannel -> HasPermission.isOwner(voiceChannel.getId().asLong(), user.getId().asLong()))
                            .flatMap(voiceChannel ->
                                    Flux.fromIterable(voiceChannel.getPermissionOverwrites())
                                            .flatMap(extendedPermissionOverwrite -> extendedPermissionOverwrite.getUser()
                                                    .map(user1 -> (Entity)user1)
                                                    .or(extendedPermissionOverwrite.getRole())
                                                    .map(entity -> entity.getId().asLong())
                                                    .map(aLong -> aLong
                                                            + "&"
                                                            + extendedPermissionOverwrite.getAllowed().asEnumSet()
                                                            .stream()
                                                            .map(Permission::getValue)
                                                            .mapToLong(aLong1 -> aLong1)
                                                            .sum()
                                                            + "&"
                                                            + extendedPermissionOverwrite.getDenied().asEnumSet()
                                                            .stream()
                                                            .map(Permission::getValue)
                                                            .mapToLong(aLong1 -> aLong1)
                                                            .sum()
                                                            + "|"))
                                            .collect(Collectors.joining())
                                            .flatMap(s -> {
                                                Tuple4<Long, Long, List<Long>, Long> tuple3 = Handler.PRIVATE_VOICE_CHANNEL.get(voiceChannel.getId().asLong());
                                                return Handler.DEFAULT_CHANNEL_CONFIGURATIONS.put(
                                                        voiceChannel.getGuildId().asLong(),
                                                        tuple3.getT1(), voiceChannel.getBitrate(),
                                                        voiceChannel.getUserLimit(),
                                                        voiceChannel.getName(),
                                                        PrivateVoiceChannel.convertAdmin(tuple3.getT3()),
                                                        s)
                                                        .then(messageChannel.createMessage("Saved successfully the channel settings."));
                                            })));
        }

        return Mono.empty();
    }
}
