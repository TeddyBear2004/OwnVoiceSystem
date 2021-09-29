package de.teddy.scheduler;

import com.wetterquarz.DiscordClient;
import de.teddy.Handler;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DeleteTalkScheduler implements Runnable {
    @Override
    public void run(){
        Flux.fromIterable(Handler.PRIVATE_VOICE_CHANNEL.getVoiceChannelsWithOvertime())
                .flatMap(aLong ->
                        DiscordClient.getDiscordClient().getGatewayDiscordClient().getChannelById(Snowflake.of(aLong))
                                .onErrorResume(throwable ->
                                        Handler.PRIVATE_VOICE_CHANNEL
                                                .delete(aLong)
                                                .then(Mono.empty())))
                .ofType(VoiceChannel.class)
                .flatMap(voiceChannel -> voiceChannel.getVoiceStates()
                        .hasElements()
                        .filter(aBoolean -> !aBoolean)
                        .flatMap(a -> Mono.zip(
                                voiceChannel.delete(),
                                Handler.PRIVATE_VOICE_CHANNEL.delete(voiceChannel.getId().asLong()))))
                .subscribe();
    }
}