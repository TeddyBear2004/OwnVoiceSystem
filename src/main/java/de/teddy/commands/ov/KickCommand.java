package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KickCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String @NotNull [] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            return ((Member)user).getVoiceState()
                    .flatMap(voiceState -> {
                        Optional<Snowflake> channelId = voiceState.getChannelId();
                        if(channelId.isEmpty())
                            return Mono.empty();

                        if(HasPermission.hasPermissionToEdit(channelId.get().asLong(), user.getId().asLong())){
                            StringBuilder builder = new StringBuilder();
                            for(String arg : args)
                                builder.append(arg)
                                        .append(" ");

                            Matcher matcher = Pattern.compile("([0-9]{1,20})")
                                    .matcher(builder.toString());

                            List<Snowflake> snowflakes = new ArrayList<>();
                            while(matcher.find())
                                try{
                                    Tuple4<Long, Long, List<Long>, Long> tuple4 = Handler.PRIVATE_VOICE_CHANNEL.get(channelId.get().asLong());
                                    if(tuple4.getT1().equals(Long.parseLong(matcher.group(1)))
                                            || tuple4.getT3().contains(Long.valueOf(matcher.group(1)))
                                            && tuple4.getT3().contains(user.getId().asLong()))
                                        continue;
                                    snowflakes.add(Snowflake.of(matcher.group(1)));
                                }catch(NumberFormatException e){
                                    e.printStackTrace();
                                }

                            return voiceState.getGuild()
                                    .flatMap(guild ->
                                            Flux.fromIterable(snowflakes)
                                                    .flatMap(guild::getMemberById)
                                                    .flatMap(member ->
                                                            member.getVoiceState()
                                                                    .map(voiceState1 -> voiceState1.getChannelId().isPresent()
                                                                            ? voiceState1.getChannelId().get().asLong()
                                                                            : null)
                                                                    .filter(aLong -> aLong.equals(channelId.get().asLong()))
                                                                    .then(member.edit(guildMemberEditSpec -> guildMemberEditSpec.setNewVoiceChannel(null))))
                                                    .then(messageChannel.createMessage("Kicked successfully the given user(s).")));
                        }
                        return Mono.empty();
                    })
                    .then(Mono.empty());
        }

        return Mono.empty();
    }
}
