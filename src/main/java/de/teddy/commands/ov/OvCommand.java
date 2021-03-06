package de.teddy.commands.ov;

import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class OvCommand implements CommandExecutable {
    private final static Consumer<EmbedCreateSpec> OV_COMMANDS;
    private final static Consumer<EmbedCreateSpec> OV_COMMANDS_FOR_ADMINS;

    static{
        OV_COMMANDS = embedCreateSpec ->
                embedCreateSpec.setTitle("Possible commands:")
                        .addField("!ov addAdmin [@User#1234], [@User2#1234]", "Gibt einem Nutzer das Recht, den Talk zu editieren.", false)
                        .addField("!ov removeAdmin [@User#1234], [@User2#1234]", "Entzieht einem Nutzer das Recht, den Talk zu editieren.", false)

                        .addField("!ov add [@User#1234], [@User2#1234]", "Das Gegenteil von !ov remove. Erlaubt den angegebenen Usern die Sichtbarkeit des Kanals, sowie sich zu Verbinden.", false)
                        .addField("!ov remove [@User#1234], [@User2#1234]", "Verbietet den angegegebenen Usern die Sichtbarkeit des Kanals, sowie sich zu Verbinden.", false)

                        .addField("!ov kick [@User#1234], [@User2#1234]", "Trennt die Verbindung von den angegegebenen Usern.", false)

                        .addField("!ov show", "Erlaubt allen Nutzern das sehen des Sprachkanals.", false)
                        .addField("!ov hide", "Verbietet allen Nutzern das sehen des Sprachkanals.", false)

                        .addField("!ov unlock", "Erlaubt allen Nutzern das Verbinden mit dem Sprachkanal", false)
                        .addField("!ov lock", "Verbietent allen Nutzern das Verbinden mit dem Sprachkanal", false)

                        .addField("!ov bitrate [8-96]", "??ndert die Bitrate des Sprachkanals.", false)

                        .addField("!ov rename [Name]", "Setzt den Namen des Voicechannels.", false)

                        .addField("!ov limit [0-99]", "Setzt die maximale Nutzeranzahl f??r den Talk. 0 f??r unbegrenzte Nutzer.", false)

                        .addField("!ov stayAlive [hours]", "Setzt die Zeit, die der Talk am Leben bleibt ohne Nutzer.", false)

                        .addField("!ov save", "Speichert die aktuelle Configuration. Diese wird mit !ov load wieder geladen oder durch das neu erstellen des Talks.", false)
                        .addField("!ov load", "L??dt die mit !ov save gespeicherte Konfiguration.", false);


        OV_COMMANDS_FOR_ADMINS = embedCreateSpec ->
                embedCreateSpec
                        .setTitle("Possible commands:")
                        .addField("!ov addAdmin [@User#1234], [@User2#1234]", "Gibt einem Nutzer das Recht, den Talk zu editieren.", false)
                        .addField("!ov removeAdmin [@User#1234], [@User2#1234]", "Entzieht einem Nutzer das Recht, den Talk zu editieren.", false)

                        .addField("!ov add [@User#1234], [@User2#1234]", "Das Gegenteil von !ov remove. Erlaubt den angegebenen Usern die Sichtbarkeit des Kanals, sowie sich zu Verbinden.", false)
                        .addField("!ov remove [@User#1234], [@User2#1234]", "Verbietet den angegegebenen Usern die Sichtbarkeit des Kanals, sowie sich zu Verbinden.", false)

                        .addField("!ov kick [@User#1234], [@User2#1234]", "Trennt die Verbindung von den angegegebenen Usern.", false)

                        .addField("!ov show", "Erlaubt allen Nutzern das sehen des Sprachkanals.", false)
                        .addField("!ov hide", "Verbietet allen Nutzern das sehen des Sprachkanals.", false)

                        .addField("!ov unlock", "Erlaubt allen Nutzern das Verbinden mit dem Sprachkanal", false)
                        .addField("!ov lock", "Verbietent allen Nutzern das Verbinden mit dem Sprachkanal", false)

                        .addField("!ov bitrate [8-96]", "??ndert die Bitrate des Sprachkanals.", false)

                        .addField("!ov rename [Name]", "Setzt den Namen des Voicechannels.", false)

                        .addField("!ov limit [0-99]", "Setzt die maximale Nutzeranzahl f??r den Talk. 0 f??r unbegrenzte Nutzer.", false)

                        .addField("!ov stayAlive [hours]", "Setzt die Zeit, die der Talk am Leben bleibt ohne Nutzer.", false)

                        .addField("!ov save", "Speichert die aktuelle Configuration. Diese wird mit !ov load wieder geladen oder durch das neu erstellen des Talks.", false)
                        .addField("!ov load", "L??dt die mit !ov save gespeicherte Konfiguration.", false)

                        .addField("** **", "** **", false)
                        .addField("*F??R ADMINS*", "** **", false)
                        .addField("!ov createSystem", "Erstellt ein OV-System auf diesem Server", false)
                        .addField("!ov deleteSystem [ID from the channel or category]", "L??scht das OV-System auf diesem Server", false)

                        .addField("!ov setDefaultRole [@Role]", "The role highest role, everyone have.", false);
    }

    @Override
    public Mono<Message> execute(@NotNull String[] strings, String[] strings1, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        return ((Member)user).getBasePermissions()
                .flatMap(permissions ->
                        permissions.contains(Permission.MANAGE_GUILD)
                                ? messageChannel.createEmbed(OV_COMMANDS_FOR_ADMINS)
                                : messageChannel.createEmbed(OV_COMMANDS));
    }
}
