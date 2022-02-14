package de.teddy.commands.admin;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public class CreatePrivateVoiceSystemCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] strings1, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient) {
        if (user instanceof Member) {
            Member member = (Member) user;

            return member.getBasePermissions()
                    .filter(permissions -> permissions.contains(Permission.MANAGE_GUILD) || permissions.contains(Permission.ADMINISTRATOR))
                    .flatMap(s -> member.getGuild())
                    .flatMap(guild ->
                            guild.createCategory("PrivateVoiceChannels")
                                    .flatMap(category ->
                                            guild.createVoiceChannel("Join to create own!")
                                                    .withParentId(category.getId())
                                                    .flatMap(voiceChannel -> Handler.PRIVATE_VOICE_INITIALIZER.put(
                                                            category.getId().asLong(),
                                                            voiceChannel.getId().asLong(),
                                                            category.getGuildId().asLong()))))
                    .flatMap(s -> messageChannel.createMessage("Created successfully a private voice channel system."));
        }
        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
