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
    public Mono<Message> execute(@NotNull String[] strings, String[] strings1, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            Member member = (Member)user;

            return member.getBasePermissions()
                    .filter(permissions -> permissions.contains(Permission.MANAGE_GUILD))
                    .flatMap(s -> member.getGuild())
                    .flatMap(guild ->
                            guild.createCategory(categoryCreateSpec ->
                                    categoryCreateSpec.setName("PrivateVoiceChannels"))
                                    .flatMap(category ->
                                            guild.createVoiceChannel(voiceChannelCreateSpec ->
                                                    voiceChannelCreateSpec
                                                            .setParentId(category.getId())
                                                            .setName("Join to create own!"))
                                                    .flatMap(voiceChannel -> Handler.PRIVATE_VOICE_INITIALIZER.put(
                                                            category.getId().asLong(),
                                                            voiceChannel.getId().asLong(),
                                                            category.getGuildId().asLong()))))
                    .then(messageChannel.createMessage("Created successfully a private voice channel system."));
        }
        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
