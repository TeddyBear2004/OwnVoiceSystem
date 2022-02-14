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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SetDefaultRole implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String @NotNull [] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            Member member = (Member)user;

            return member.getBasePermissions()
                    .filter(permissions -> permissions.contains(Permission.MANAGE_GUILD) || permissions.contains(Permission.ADMINISTRATOR))
                    .flatMap(s -> Flux.fromArray(args)
                            .collect(Collectors.joining()))
                    .map(s -> Pattern.compile("<@&?([0-9]{1,19})>").matcher(s))
                    .flatMap(matcher ->
                            matcher.find()
                                    ? Handler.DEFAULT_ROLE_PER_GUILD
                                    .put(member.getGuildId().asLong(), Long.parseLong(matcher.group(1)))
                                    .then(messageChannel.createMessage("Updated successfully the default role."))
                                    : Handler.DEFAULT_ROLE_PER_GUILD.delete(member.getGuildId().asLong())
                                    .then(messageChannel.createMessage("Updated successfully the default role.")));
        }

        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
