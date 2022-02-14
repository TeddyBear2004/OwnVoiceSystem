package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class BitrateCommand implements CommandExecutable {
    private final int[] bitratePerLevel = new int[]{96, 128, 256, 384};

    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(args.length < 1)
            return messageChannel.createMessage("The given argument is not an integer");

        if(!(user instanceof Member))
            return Mono.empty();

        int i;
        try{
            i = Integer.parseInt(args[0]);
        }catch(NumberFormatException ignore){
            return messageChannel.createMessage("The given argument is not an integer");
        }

        if(i < 7)
            return messageChannel.createMessage("The value should be greater than or equal to 8kbs.");

        var voiceStateMono = ((Member)user).getVoiceState();
        var bitrate = Mono.just(i);

        if(i > 97)
            bitrate = ((Member)user).getGuild()
                    .map(guild ->
                            guild.getPremiumTier().getValue() < 0 || guild.getPremiumTier().getValue() > bitratePerLevel.length
                                    ? bitratePerLevel[0]
                                    : bitratePerLevel[guild.getPremiumTier().getValue()])
                    .flatMap(integer -> {
                        if(i > integer)
                            return messageChannel.createMessage("The value should be lower than or equal to 96kbs.")
                                    .then(Mono.empty());
                        return Mono.just(i);
                    });


        return Mono.zip(voiceStateMono, bitrate)
                .flatMap(tuple2 -> {
                    Optional<Snowflake> channelId = tuple2.getT1().getChannelId();

                    if(channelId.isEmpty()
                            || !HasPermission.hasPermissionToEdit(channelId.get().asLong(), user.getId().asLong()))
                        return Mono.empty();

                    return tuple2.getT1().getChannel()
                            .flatMap(voiceChannel ->
                                    voiceChannel
                                            .edit()
                                            .withBitrate(tuple2.getT2() * 1000)
                                            .then(messageChannel.createMessage("Bitrate successfully updated from " + (voiceChannel.getBitrate() / 1000) + "kbs to " + tuple2.getT2() + "kbs.")));
                });
    }
}
