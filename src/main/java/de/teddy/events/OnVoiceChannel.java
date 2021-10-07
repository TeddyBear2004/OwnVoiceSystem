package de.teddy.events;

import de.teddy.Handler;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.teddy.Handler.PRIVATE_VOICE_CHANNEL;
import static de.teddy.Handler.PRIVATE_VOICE_INITIALIZER;

public class OnVoiceChannel {
    public static @NotNull Mono<Void> onChannelJoinEvent(@NotNull VoiceStateUpdateEvent event){
        if(!event.isJoinEvent() && !event.isMoveEvent())
            return Mono.empty();

        var tuple5Mono = Handler.DEFAULT_CHANNEL_CONFIGURATIONS.get(
                event.getCurrent().getGuildId().asLong(), event.getCurrent().getUserId().asLong());

        var category = event
                .getCurrent()
                .getChannel()
                .filter(Objects::nonNull)
                .flatMap(voiceChannel ->
                        Mono.justOrEmpty(PRIVATE_VOICE_INITIALIZER.getOtherChannel(voiceChannel.getId().asLong())))
                .doOnError(Throwable::printStackTrace);

        return Mono.zip(category, event.getCurrent().getGuild(), event.getCurrent().getMember())
                .flatMap(tuple3 -> {
                    Set<PermissionOverwrite> permissionOverwrites = new HashSet<>();

                    if(tuple5Mono != null){
                        Matcher matcher = Pattern.compile("(.*?)&(.*?)&(.*?)\\|").matcher(tuple5Mono.getT5());
                        while(matcher.find())
                            permissionOverwrites
                                    .add(PermissionOverwrite.forMember(Snowflake.of(matcher.group(1)),
                                            PermissionSet.of(Long.parseLong(matcher.group(2))),
                                            PermissionSet.of(Long.parseLong(matcher.group(3)))));
                    }

                    return tuple3.getT2()
                            .createVoiceChannel(voiceChannelCreateSpec ->
                                    voiceChannelCreateSpec
                                            .setParentId(Snowflake.of(tuple3.getT1()))
                                            .setName(tuple5Mono == null ? tuple3.getT3().getDisplayName() + "'s Channel" : tuple5Mono.getT3())
                                            .setUserLimit(tuple5Mono == null ? 0 : tuple5Mono.getT2())
                                            .setBitrate(tuple5Mono == null ? 64_000 : tuple5Mono.getT1() * 1000)
                                            .setPermissionOverwrites(permissionOverwrites))

                            .flatMap(voiceChannel1 -> {
                                var mono1 = PRIVATE_VOICE_CHANNEL.put(
                                        voiceChannel1.getId().asLong(),
                                        tuple3.getT3().getId().asLong(),
                                        System.currentTimeMillis(),
                                        tuple5Mono == null ? "" : tuple5Mono.getT4(),
                                        event.getCurrent().getGuildId().asLong());
                                Mono<Void> edit = tuple3.getT3().edit(guildMemberEditSpec ->
                                        guildMemberEditSpec.setNewVoiceChannel(voiceChannel1.getId()));

                                return Mono.zip(mono1, edit);
                            });

                })
                .doOnError(Throwable::printStackTrace)
                .then();
    }

    public static @NotNull Mono<Void> onChannelLeaveEvent(@NotNull VoiceStateUpdateEvent event){
        if(!event.isLeaveEvent() && !event.isMoveEvent())
            return Mono.empty();

        Optional<VoiceState> old = event.getOld();
        if(old.isEmpty())
            return Mono.empty();
        VoiceState voiceState = old.get();
        if(voiceState.getChannelId().isEmpty())
            return Mono.empty();
        if(PRIVATE_VOICE_CHANNEL.containsNot(voiceState.getChannelId().get().asLong()))
            return Mono.empty();
        if(!PRIVATE_VOICE_CHANNEL.hasOvertime(voiceState.getChannelId().get().asLong()))
            return Mono.empty();

        return event.getClient().getChannelById(voiceState.getChannelId().get())
                .ofType(VoiceChannel.class)
                .flatMap(voiceChannel ->
                        voiceChannel
                                .getVoiceStates()
                                .hasElements()
                                .filter(aBoolean -> !aBoolean)
                                .flatMap(s -> Mono.zip(voiceChannel.delete(),
                                        PRIVATE_VOICE_CHANNEL.delete(voiceChannel.getId().asLong()))))
                .doOnError(throwable -> PRIVATE_VOICE_CHANNEL.delete(voiceState.getChannelId().get().asLong()))
                .then();
    }
}
