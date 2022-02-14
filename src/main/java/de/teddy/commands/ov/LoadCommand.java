package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadCommand implements CommandExecutable {
    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String[] strings1, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient) {
        if (user instanceof Member) {
            return ((Member) user).getVoiceState()
                    .flatMap(voiceState -> {
                        Optional<Snowflake> channelId = voiceState.getChannelId();
                        if (channelId.isEmpty())
                            return Mono.empty();

                        if (!HasPermission.hasPermissionToEdit(channelId.get().asLong(), user.getId().asLong()))
                            return Mono.empty();

                        Tuple5<Integer, Integer, String, String, String> tuple5 = Handler.DEFAULT_CHANNEL_CONFIGURATIONS.get(((Member) user).getGuildId().asLong(), user.getId().asLong());

                        if (tuple5 == null)
                            return Mono.empty();

                        Matcher matcher = Pattern.compile("(.*?)&(.*?)&(.*?)\\|").matcher(tuple5.getT5());
                        Set<PermissionOverwrite> permissionOverwrites = new HashSet<>();

                        while (matcher.find())
                            permissionOverwrites
                                    .add(PermissionOverwrite.forMember(Snowflake.of(matcher.group(1)),
                                            PermissionSet.of(Long.parseLong(matcher.group(2))),
                                            PermissionSet.of(Long.parseLong(matcher.group(3)))));

                        var voiceChannelMono = voiceState.getChannel()
                                .flatMap(voiceChannel ->
                                        voiceChannel
                                                .edit()
                                                .withBitrate(tuple5.getT1() * 1000)
                                                .withUserLimit(tuple5.getT2())
                                                .withPermissionOverwrites(permissionOverwrites)
                                                .withName(tuple5.getT3()));
                        return Mono.zip(voiceChannelMono,
                                Handler.PRIVATE_VOICE_CHANNEL.updateAdmins(channelId.get().asLong(), tuple5.getT4()));
                    }).then(messageChannel.createMessage("Channel successfully loaded."));
        }
        return Mono.empty();
    }
}
