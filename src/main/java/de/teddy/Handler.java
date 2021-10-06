package de.teddy;

import com.wetterquarz.DiscordClient;
import com.wetterquarz.command.CommandBuilder;
import com.wetterquarz.plugin.Plugin;
import de.teddy.commands.admin.CreatePrivateVoiceSystemCommand;
import de.teddy.commands.admin.RemovePrivateVoiceSystemCommand;
import de.teddy.commands.admin.SetDefaultRole;
import de.teddy.commands.ov.*;
import de.teddy.events.OnChannelDelete;
import de.teddy.events.OnJoin;
import de.teddy.events.OnLoad;
import de.teddy.events.OnVoiceChannel;
import de.teddy.scheduler.DeleteTalkScheduler;
import de.teddy.tables.DefaultChannelConfigurations;
import de.teddy.tables.DefaultRolePerGuild;
import de.teddy.tables.PrivateVoiceChannel;
import de.teddy.tables.PrivateVoiceInitializer;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.ChannelEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Handler extends Plugin {
    static{
        if(DiscordClient.getDiscordClient().getDatabaseManager() == null)
            throw new NullPointerException("The databaseManager may not be null to use the OwnVoice System");
    }

    private final @NotNull List<Disposable> disposables = new ArrayList<>();
    public static final @NotNull PrivateVoiceInitializer PRIVATE_VOICE_INITIALIZER =
            new PrivateVoiceInitializer(DiscordClient.getDiscordClient().getDatabaseManager());

    public static final @NotNull DefaultChannelConfigurations DEFAULT_CHANNEL_CONFIGURATIONS =
            new DefaultChannelConfigurations(DiscordClient.getDiscordClient().getDatabaseManager());

    public static final @NotNull PrivateVoiceChannel PRIVATE_VOICE_CHANNEL =
            new PrivateVoiceChannel(DiscordClient.getDiscordClient().getDatabaseManager());

    public static final @NotNull DefaultRolePerGuild DEFAULT_ROLE_PER_GUILD =
            new DefaultRolePerGuild(DiscordClient.getDiscordClient().getDatabaseManager());


    @Override
    public void onLoad(){
        disposables.add(
                DiscordClient
                        .getDiscordClient()
                        .getEventDispatcher()
                        .on(VoiceStateUpdateEvent.class)
                        .flatMap(OnVoiceChannel::onChannelJoinEvent)
                        .then()
                        .subscribe());

        disposables.add(
                DiscordClient
                        .getDiscordClient()
                        .getEventDispatcher()
                        .on(VoiceStateUpdateEvent.class)
                        .flatMap(OnVoiceChannel::onChannelLeaveEvent)
                        .subscribe());
        disposables.add(
                DiscordClient
                        .getDiscordClient()
                        .getEventDispatcher()
                        .on(GuildCreateEvent.class)
                        .flatMap(OnLoad::onLoadEvent)
                        .subscribe());
        disposables.add(
                DiscordClient
                        .getDiscordClient()
                        .getEventDispatcher()
                        .on(ChannelEvent.class)
                        .flatMap(OnChannelDelete::onChannelDelete)
                        .subscribe());
        disposables.add(
                DiscordClient
                        .getDiscordClient()
                        .getEventDispatcher()
                        .on(GuildCreateEvent.class)
                        .flatMap(OnJoin::onMemberJoinEvent)
                        .subscribe());


        disposables.add(Schedulers.single().schedulePeriodically(new DeleteTalkScheduler(), 0, 10, TimeUnit.SECONDS));

        DiscordClient.getDiscordClient()
                .getCommandManager()
                .registerCommands(
                        new CommandBuilder("ov", null)
                                .addSubCommandLevel(
                                        "addAdmin",
                                        new ChangeAdminCommand(true),
                                        commandSegmentBuilder -> {})
                                .addSubCommandLevel(
                                        "removeAdmin",
                                        new ChangeAdminCommand(false),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("createSystem",
                                        new CreatePrivateVoiceSystemCommand(),
                                        commandSegmentBuilder -> {})
                                .addSubCommandLevel("deleteSystem",
                                        new RemovePrivateVoiceSystemCommand(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("invite",
                                        new ChangePermissionOfGivenUsers(
                                                false,
                                                PermissionSet.of(Permission.VIEW_CHANNEL, Permission.CONNECT),
                                                PermissionSet.none()),
                                        commandSegmentBuilder -> {})
                                .addSubCommandLevel("remove",
                                        new ChangePermissionOfGivenUsers(
                                                false,
                                                PermissionSet.none(),
                                                PermissionSet.of(Permission.VIEW_CHANNEL, Permission.CONNECT)),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("kick",
                                        new KickCommand(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("show",
                                        new ChangePermissionOfGivenUsers(
                                                true,
                                                PermissionSet.of(Permission.VIEW_CHANNEL),
                                                PermissionSet.none()),
                                        commandSegmentBuilder -> {})
                                .addSubCommandLevel("hide",
                                        new ChangePermissionOfGivenUsers(
                                                true,
                                                PermissionSet.none(),
                                                PermissionSet.of(Permission.VIEW_CHANNEL)),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("unlock",
                                        new ChangePermissionOfGivenUsers(
                                                true,
                                                PermissionSet.of(Permission.CONNECT),
                                                PermissionSet.none()),
                                        commandSegmentBuilder -> {})
                                .addSubCommandLevel("lock",
                                        new ChangePermissionOfGivenUsers(
                                                true,
                                                PermissionSet.none(),
                                                PermissionSet.of(Permission.CONNECT)),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("bitrate",
                                        new BitrateCommand(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("rename",
                                        new RenameCommand(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("limit",
                                        new LimitCommand(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("stayAlive",
                                        new StayAliveCommand(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("setDefaultRole",
                                        new SetDefaultRole(),
                                        commandSegmentBuilder -> {})

                                .addSubCommandLevel("save",
                                        new SaveCommand(),
                                        commandSegmentBuilder -> {})
                                .addSubCommandLevel("load",
                                        new LoadCommand(),
                                        commandSegmentBuilder -> {})
                                .build());

    }

    @Override
    public void onUnload(){
        disposables.forEach(Disposable::dispose);
    }
}
