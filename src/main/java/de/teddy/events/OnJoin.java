package de.teddy.events;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class OnJoin {
    public static @NotNull Mono<Void> onMemberJoinEvent(@NotNull GuildCreateEvent event) {
        Instant instant = Instant.now().minusSeconds(300);
        if (event.getGuild().getJoinTime().isAfter(instant))
            return event
                    .getGuild()
                    .getSystemChannel()
                    .flatMap(textChannel -> textChannel.createMessage(
                            EmbedCreateSpec.builder()
                                    .title("Hey, what's up?")
                                    .description("Thank you for using this Discord bot.")
                                    .color(Color.of(0x005eff))
                                    .addField("Commands", "With `!ov` you can display a list of subcommands.", false)
                                    .addField("Support", "You are welcome to join our support discord to get help: [Click here](https://discord.gg/j547HBda53)", false)
                                    .footer("Have fun", null)
                                    .build()))
                    .then();
        return Mono.empty();
    }
}
