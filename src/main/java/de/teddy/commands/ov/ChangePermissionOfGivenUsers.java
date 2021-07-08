package de.teddy.commands.ov;

import com.wetterquarz.DiscordClient;
import com.wetterquarz.command.Command;
import com.wetterquarz.command.CommandExecutable;
import de.teddy.Handler;
import de.teddy.commands.ov.util.HasPermission;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangePermissionOfGivenUsers implements CommandExecutable {
    private final PermissionSet add;
    private final PermissionSet remove;
    private final boolean editEveryone;

    public ChangePermissionOfGivenUsers(boolean editEveryone, PermissionSet add, PermissionSet remove){
        this.add = add;
        this.remove = remove;
        this.editEveryone = editEveryone;
    }

    @Override
    public @NotNull Mono<Message> execute(@NotNull String[] strings, String @NotNull [] args, @NotNull User user, @Nullable Command command, @NotNull MessageChannel messageChannel, @NotNull GatewayDiscordClient gatewayDiscordClient){
        if(user instanceof Member){
            return ((Member)user).getVoiceState()
                    .flatMap(voiceState -> voiceState.getChannel()
                            .filter(voiceChannel ->
                                    HasPermission.hasPermissionToEdit(voiceChannel.getId().asLong(), user.getId().asLong()))
                            .flatMap(voiceChannel -> {

                                Set<PermissionOverwrite> permissionOverwrites = new HashSet<>();

                                voiceChannel.getPermissionOverwrites().forEach(extendedPermissionOverwrite -> {
                                    extendedPermissionOverwrite
                                            .getRoleId()
                                            .ifPresent(snowflake ->
                                                    permissionOverwrites
                                                            .add(PermissionOverwrite.forRole(
                                                                    snowflake,
                                                                    extendedPermissionOverwrite.getAllowed(),
                                                                    extendedPermissionOverwrite.getDenied())));

                                    extendedPermissionOverwrite
                                            .getMemberId()
                                            .ifPresent(snowflake ->
                                                    permissionOverwrites
                                                            .add(PermissionOverwrite.forMember(
                                                                    snowflake,
                                                                    extendedPermissionOverwrite.getAllowed(),
                                                                    extendedPermissionOverwrite.getDenied())));

                                });

                                if(editEveryone){
                                    Long specificEveryoneRole = Handler.DEFAULT_ROLE_PER_GUILD.get(voiceChannel.getGuildId().asLong());

                                    if(specificEveryoneRole == null){
                                        return voiceChannel.getGuild()
                                                .flatMap(Guild::getEveryoneRole)
                                                .flatMap(role1 -> {
                                                    permissionOverwrites
                                                            .add(PermissionOverwrite
                                                                    .forRole(role1.getId(), add, remove));

                                                    return voiceChannel.edit(voiceChannelEditSpec ->
                                                            voiceChannelEditSpec
                                                                    .setPermissionOverwrites(permissionOverwrites));
                                                });
                                    }else{
                                        return DiscordClient.getDiscordClient()
                                                .getGatewayDiscordClient()
                                                .getRoleById(voiceChannel.getGuildId(), Snowflake.of(specificEveryoneRole))
                                                .flatMap(role1 -> {
                                                    permissionOverwrites
                                                            .add(PermissionOverwrite
                                                                    .forRole(role1.getId(), add, remove));

                                                    return voiceChannel.edit(voiceChannelEditSpec ->
                                                            voiceChannelEditSpec
                                                                    .setPermissionOverwrites(permissionOverwrites));
                                                });

                                    }
                                }else{
                                    StringBuilder builder = new StringBuilder();
                                    for(String arg : args)
                                        builder
                                                .append(arg)
                                                .append(" ");

                                    Matcher matcher = Pattern.compile("<@!?([0-9]+)>")
                                            .matcher(builder.toString());

                                    Tuple4<Long, Long, List<Long>, Long> tuple4
                                            = Handler.PRIVATE_VOICE_CHANNEL.get(voiceChannel.getId().asLong());//owner, ttl, admins, guild
                                    while(matcher.find()){
                                        //owner edit owner --> forbid
                                        //owner edit admin/user --> allow

                                        //admin edit owner/admin --> forbid
                                        //admin edit user --> allow
                                        if(tuple4.getT1().equals(Long.parseLong(matcher.group(1)))
                                                || tuple4.getT3().contains(Long.valueOf(matcher.group(1)))
                                                && tuple4.getT3().contains(user.getId().asLong()))
                                            continue;

                                        permissionOverwrites
                                                .add(PermissionOverwrite
                                                        .forMember(Snowflake.of(matcher.group(1)), add, remove));
                                    }

                                    Matcher matcher1 = Pattern.compile("<@&([0-9]+)>")
                                            .matcher(builder.toString());

                                    while(matcher1.find())
                                        permissionOverwrites
                                                .add(PermissionOverwrite
                                                        .forRole(Snowflake.of(matcher1.group(1)), add, remove));

                                    return voiceChannel.edit(voiceChannelEditSpec ->
                                            voiceChannelEditSpec
                                                    .setPermissionOverwrites(permissionOverwrites));
                                }
                            }).then(messageChannel.createMessage("Updated successfully the permissions.")));
        }

        return messageChannel.createMessage("Please send this command to a guild.");
    }
}
