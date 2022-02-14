package de.teddy.events;

import com.wetterquarz.DiscordClient;
import de.teddy.Handler;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
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

        return Flux.fromIterable(voiceChannelsWithOvertime)
                .flatMap(aLong -> gatewayDiscordClient.getChannelById(Snowflake.of(aLong))
                        .onErrorResume(throwable -> Handler.PRIVATE_VOICE_CHANNEL.delete(aLong)
                                .then(Mono.empty())))
                .then();
    }

    private static @NotNull Mono<Void> doDeletion(long category){
        return Handler.PRIVATE_VOICE_INITIALIZER.delete(category).then();
    }
}