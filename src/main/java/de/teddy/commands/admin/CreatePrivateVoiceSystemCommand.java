package de.teddy.commands.admin;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class CreatePrivateVoiceSystemCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] strings1, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient) {
        if (user instanceof Member) {
            Member member = (Member) user;

            return member
                    .getBasePermissions()
                    .filter(permissions -> permissions.contains(Permission.MANAGE_GUILD))
                    .flatMap(s -> member.getGuild())
                    .flatMap(guild ->
                            guild.getMemberById(gatewayDiscordClient.getSelfId())
                                    .flatMap(Member::getBasePermissions)
                                    .flatMap(permissions -> {
                                        if (permissions.contains(Permission.MANAGE_CHANNELS) || permissions.contains(Permission.ADMINISTRATOR)) {
                                            return guild.createCategory("PrivateVoiceChannels")
                                                    .flatMap(category ->
                                                            guild.createVoiceChannel("Join to create own!")
                                                                    .flatMap(voiceChannel -> voiceChannel.edit().withParentId(Possible.of(Optional.of(category.getId()))))
                                                                    .flatMap(voiceChannel -> Handler.PRIVATE_VOICE_INITIALIZER.put(
                                                                            category.getId().asLong(),
                                                                            voiceChannel.getId().asLong(),
                                                                            category.getGuildId().asLong())))
                                                    .and(messageChannel.createMessage("Private Voice System created!"));
                                        }else{
                                            return messageChannel.createMessage("I don't have the permission to create a private voice channel!");
                                        }
                                    })).then(Mono.empty());
        }
        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
