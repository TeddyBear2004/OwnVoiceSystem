package de.teddy.events;

import com.wetterquarz.DiscordClient;
import de.teddy.Handler;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.channel.Channel;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class OnLoad {
    public static @NotNull Mono<Void> onLoadEvent(@NotNull GuildCreateEvent event){
        if(DiscordClient.getDiscordClient().getDatabaseManager() == null)
            return Mono.empty();

        GatewayDiscordClient gatewayDiscordClient = DiscordClient.getDiscordClient().getGatewayDiscordClient();


        List<Long> voiceChannelsWithOvertime = Handler.PRIVATE_VOICE_CHANNEL.getVoiceChannelsWithOvertime(event.getGuild().getId().asLong());

        var mono1 = Flux.fromIterable(voiceChannelsWithOvertime)
                .flatMap(aLong -> gatewayDiscordClient.getChannelById(Snowflake.of(aLong))
                        .onErrorResume(throwable -> Handler.PRIVATE_VOICE_CHANNEL.delete(aLong)
                                .then(Mono.empty())))
                .then();

        var mono2 =
                Flux.fromIterable(Handler.PRIVATE_VOICE_INITIALIZER.getByGuild(event.getGuild().getId().asLong()))
                        .flatMap(tuple2 -> {
                            Mono<Channel> categoryMono = gatewayDiscordClient
                                    .getChannelById(Snowflake.of(tuple2.getT2()));

                            return gatewayDiscordClient
                                    .getChannelById(Snowflake.of(tuple2.getT1()))
                                    .flatMap(channel ->
                                            categoryMono
                                                    .flatMap(channel1 -> Mono.<Void>empty())
                                                    .onErrorResume(throwable ->
                                                            Mono.zip(
                                                                    channel.delete(),
                                                                    doDeletion(tuple2.getT1()))
                                                                    .then()))
                                    .flatMap(channel -> Mono.empty())
                                    .onErrorResume(throwable ->
                                            categoryMono
                                                    .flatMap(channel ->
                                                            Mono.zip(
                                                                    channel.delete(),
                                                                    doDeletion(tuple2.getT1()))
                                                                    .then())
                                                    .onErrorResume(tCategory -> doDeletion(tuple2.getT1())))
                                    .doOnError(Throwable::printStackTrace);
                        })
                        .then();

        return Mono.zip(mono1, mono2).then();
    }

    private static @NotNull Mono<Void> doDeletion(long category){
        return Handler.PRIVATE_VOICE_INITIALIZER.delete(category).then();
    }
}