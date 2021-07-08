package de.teddy.events;

import com.wetterquarz.DiscordClient;
import de.teddy.Handler;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.CategoryDeleteEvent;
import discord4j.core.event.domain.channel.ChannelEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.object.entity.channel.Channel;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

public class OnChannelDelete {

    public static @NotNull Mono<Void> onChannelDelete(ChannelEvent channelEvent){
        if(channelEvent instanceof VoiceChannelDeleteEvent)
            return onChannelDelete(((VoiceChannelDeleteEvent)channelEvent).getChannel());
        else if(channelEvent instanceof CategoryDeleteEvent)
            return onChannelDelete(((CategoryDeleteEvent)channelEvent).getCategory());
        return Mono.empty();
    }

    private static @NotNull Mono<Void> onChannelDelete(@NotNull Channel channel){
        return Handler.PRIVATE_VOICE_INITIALIZER.delete(channel.getId().asLong())
                .flatMap(aLong -> DiscordClient.getDiscordClient().getGatewayDiscordClient().getChannelById(Snowflake.of(aLong))
                        .flatMap(Channel::delete));
    }
}
