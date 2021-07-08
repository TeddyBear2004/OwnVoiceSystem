package de.teddy.commands.admin;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import discord4j.common.util.Snowflake;
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

public class RemovePrivateVoiceSystemCommand implements CommandExecutable {
    @Override
    public Mono<Message> execute(@NotNull String[] strings, String[] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            Member member = (Member)user;

            try{
                long l = Long.parseLong(args[0]);
                Long other = Handler.PRIVATE_VOICE_INITIALIZER.getOtherChannel(l);

                return member.getBasePermissions()
                        .filter(permissions ->
                                permissions.contains(Permission.ADMINISTRATOR))
                        .flatMap(s ->
                                other == null ?
                                        messageChannel.createMessage("Does not found any private voice channel system with the given id.") :
                                        Flux.fromArray(new Long[]{l, other})
                                                .flatMap(aLong -> gatewayDiscordClient.getChannelById(Snowflake.of(aLong)))
                                                .flatMap(channel -> Mono.zip(channel.delete(),
                                                        Handler.PRIVATE_VOICE_INITIALIZER.delete(channel.getId().asLong())))
                                                .then(messageChannel.createMessage("Deleted successfully the private voice channel system.")));
            }catch(NumberFormatException e){
                return messageChannel.createMessage("The given argument is not an integer.");
            }catch(ArrayIndexOutOfBoundsException e){
                return messageChannel.createMessage("Too few arguments.");
            }
        }
        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
