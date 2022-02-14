package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangeAdminCommand implements CommandExecutable {
    private final boolean add;

    public ChangeAdminCommand(boolean add){
        this.add = add;
    }

    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] cmd, String @NotNull [] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            Member member = (Member)user;

            return member.getVoiceState()
                    .flatMap(voiceState -> {
                        Optional<Snowflake> channelId = voiceState.getChannelId();
                        if(channelId.isPresent()){
                            long voiceChannelId = channelId.get().asLong();
                            Tuple3<Long, Long, List<Long>> tuple3 = Handler.PRIVATE_VOICE_CHANNEL.get(voiceChannelId);//Owner, ttl, admins

                            if(tuple3 == null)
                                return Mono.empty();

                            if(tuple3.getT1().equals(member.getId().asLong())){
                                StringBuilder builder = new StringBuilder();
                                for(String arg : args)
                                    builder.append(arg);

                                Matcher matcher = Pattern.compile("<@!?([0-9]{1,19})>")
                                        .matcher(builder.toString());

                                boolean hasChanged = false;

                                while(matcher.find()){
                                    if(add){
                                        if(!tuple3.getT3().contains(Long.parseLong(matcher.group(1)))){
                                            hasChanged = true;
                                            tuple3.getT3().add(Long.parseLong(matcher.group(1)));
                                        }
                                    }else{
                                        if(tuple3.getT3().contains(Long.parseLong(matcher.group(1)))){
                                            hasChanged = true;
                                            tuple3.getT3().remove(Long.parseLong(matcher.group(1)));
                                        }
                                    }
                                }

                                if(hasChanged){
                                    StringBuilder adminsNew = new StringBuilder();
                                    tuple3.getT3().forEach(s -> adminsNew.append(s).append(","));

                                    return Handler.PRIVATE_VOICE_CHANNEL.updateAdmins(voiceChannelId, adminsNew.toString())
                                            .then(messageChannel.createMessage("Updated successfully the admins!"));
                                }

                            }
                        }
                        return Mono.empty();
                    });
        }
        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
