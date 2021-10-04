package de.teddy.events;

import com.wetterquarz.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

public class OnJoin {
    public static @NotNull Mono<Void> onMemberJoinEvent(@NotNull MemberJoinEvent event){
        if(event.getMember().getId().equals(DiscordClient.getDiscordClient().getGatewayDiscordClient().getSelfId())){
            return event.getGuild()
                    .flatMap(Guild::getSystemChannel)
                    .flatMap(textChannel -> textChannel.createEmbed(embedCreateSpec ->
                            embedCreateSpec
                                    .setTitle("Hey, what's up?")
                                    .setDescription("Thank you for using this Discord bot.")
                                    .setColor(Color.of(0x005eff))
                                    .addField("Commands", "With `!ov` you can display a list of subcommands.", false)
                                    .addField("Support", "You are welcome to join our support discord to get help: https://discord.gg/j547HBda53", false)
                                    .setFooter("Have fun", null)))
                    .then();
        }
        return Mono.empty();
    }
}
