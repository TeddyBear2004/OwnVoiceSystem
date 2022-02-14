package de.teddy.scheduler;

import com.wetterquarz.DiscordClient;
import de.teddy.Handler;
import discord4j.common.store.action.read.ReadActions;
import discord4j.common.util.Snowflake;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static de.teddy.Handler.PRIVATE_VOICE_CHANNEL;

public class DeleteTalkScheduler implements Runnable {
    @Override
    public void run(){
        Flux.fromIterable(Handler.PRIVATE_VOICE_CHANNEL.getVoiceChannelsWithOvertime())
                .flatMap(tuple2 -> Flux.from(DiscordClient.getDiscordClient().getGatewayDiscordClient()
                                .getGatewayResources()
                                .getStore()
                                .execute(ReadActions.getVoiceStatesInChannel(tuple2.getT1(), tuple2.getT2())))
                        .map(data -> new VoiceState(DiscordClient.getDiscordClient().getGatewayDiscordClient(), data))
                        .hasElements()
                        .filter(b -> !b)
                        .flatMap(s -> Mono.zip(DiscordClient.getDiscordClient().getGatewayDiscordClient().getChannelById(Snowflake.of(tuple2.getT1()))
                                        .flatMap(Channel::delete),
                                PRIVATE_VOICE_CHANNEL.delete(tuple2.getT1()))))
                .subscribe();
    }
}