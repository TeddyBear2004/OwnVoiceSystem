package de.teddy.tables;

import com.wetterquarz.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

public class PrivateVoiceChannel {
    private final @NotNull DatabaseManager databaseManager;
    private final @NotNull Map<Long, Tuple4<Long, Long, List<Long>, Long>> cache; //voiceChannel //owner, ttl, admins, guild

    public PrivateVoiceChannel(@NotNull DatabaseManager databaseManager){
        this.databaseManager = databaseManager;

        this.cache = new HashMap<>();

        this.databaseManager.executeSQL("CREATE TABLE IF NOT EXISTS `privateVoiceChannel` (" +
                "`voiceChannel` BIGINT(19) NOT NULL," +
                "`owner` BIGINT(19) NULL," +
                "`guild` BIGINT(19) NULL," +
                "`ttl` BIGINT(19) DEFAULT '0'," +
                "`admins` TEXT NULL DEFAULT ''," +
                "PRIMARY KEY (`voiceChannel`)" +
                ")" +
                "COLLATE='utf8mb4_general_ci'" +
                ";")
                .then(loadAll())
                .subscribe();
    }

    public @NotNull Mono<Void> loadAll(){
        return this.databaseManager.executeSQL("SELECT * FROM privateVoiceChannel")
                .flatMapMany(result ->
                        Flux.from(result.map((row, rowMetadata) -> {
                            Long ttl = row.get("ttl", Long.class);

                            return Tuples.of(Objects.requireNonNull(row.get("voiceChannel", Long.class)),
                                    Tuples.of(Objects.requireNonNull(row.get("owner", Long.class)),
                                            ttl == null ? 0L : ttl,
                                            convertAdmin(Objects.requireNonNull(row.get("admins")).toString()),
                                            Objects.requireNonNull(row.get("guild", Long.class))));
                        })))
                .doOnNext(tuple2 -> this.cache.put(tuple2.getT1(), tuple2.getT2()))
                .then();
    }

    public @NotNull Mono<Void> put(long voiceChannel, long owner, long ttl, @NotNull String admins, long guild){
        return this.put(voiceChannel, owner, ttl, convertAdmin(admins), guild);
    }

    public @NotNull Mono<Void> put(long voiceChannel, long owner, long ttl, @NotNull List<Long> admins, long guild){
        return put(voiceChannel, Tuples.of(owner, ttl, admins, guild));
    }

    public @NotNull Mono<Void> put(@NotNull Long vc, @NotNull Tuple4<Long, Long, List<Long>, Long> tuple4){
        this.cache.put(vc, tuple4);

        return this.databaseManager.executeSQL("REPLACE INTO privateVoiceChannel (voiceChannel, owner, ttl, admins, guild) VALUES (?, ?, ?, ?, ?)",
                statement -> statement
                        .bind(0, vc)
                        .bind(1, tuple4.getT1())
                        .bind(2, tuple4.getT2())
                        .bind(3, convertAdmin(tuple4.getT3()))
                        .bind(4, tuple4.getT4()))
                .then();
    }

    public static @NotNull List<Long> convertAdmin(@NotNull String admins){
        return admins.equals("") ? new ArrayList<>() : Arrays.stream(admins.split(",")).mapToLong(Long::parseLong).boxed().collect(Collectors.toList());
    }

    public static @NotNull String convertAdmin(@NotNull List<Long> admins){
        StringBuilder adminBuilder = new StringBuilder();
        admins.forEach(aLong -> adminBuilder.append(aLong).append(","));
        return adminBuilder.toString();
    }

    public @NotNull Mono<Void> updateAdmins(long voiceChannel, @NotNull String admins){
        return this.updateAdmins(voiceChannel, convertAdmin(admins));
    }

    public @NotNull Mono<Void> updateAdmins(long voiceChannel, @NotNull List<Long> admins){
        Tuple4<Long, Long, List<Long>, Long> tuple4 = this.cache.get(voiceChannel);

        this.cache.put(voiceChannel, Tuples.of(tuple4.getT1(), tuple4.getT2(), admins, tuple4.getT4()));

        StringBuilder adminBuilder = new StringBuilder();
        admins.forEach(aLong -> adminBuilder.append(aLong).append(","));

        return this.databaseManager.executeSQL("UPDATE privateVoiceChannel SET admins = ? WHERE voiceChannel = ?",
                statement -> statement.bind(0, adminBuilder.toString()).bind(1, voiceChannel)).then();
    }

    public @NotNull Mono<Void> updateTtl(long voiceChannel, long ttl){
        Tuple4<Long, Long, List<Long>, Long> tuple4 = this.cache.get(voiceChannel);

        this.cache.put(voiceChannel, Tuples.of(tuple4.getT1(), ttl, tuple4.getT3(), tuple4.getT4()));

        return this.databaseManager.executeSQL("UPDATE privateVoiceChannel SET ttl = ? WHERE voiceChannel = ?",
                statement -> statement.bind(0, ttl).bind(1, voiceChannel)).then();
    }

    public boolean containsNot(long l){
        return !this.cache.containsKey(l);
    }

    public boolean hasOvertime(long vc){
        return this.cache.containsKey(vc)
                && this.cache.get(vc).getT2() < System.currentTimeMillis();
    }

    public @NotNull List<Long> getVoiceChannelsWithOvertime(long guildId){
        List<Long> list = new ArrayList<>();

        long l = System.currentTimeMillis();
        this.cache.forEach((aLong, objects) -> {
            if(objects.getT2() < l && guildId == objects.getT4())
                list.add(aLong);
        });
        return list;
    }

    public @NotNull List<Long> getVoiceChannelsWithOvertime(){
        List<Long> list = new ArrayList<>();

        long l = System.currentTimeMillis();
        this.cache.forEach((aLong, objects) -> {
            if(objects.getT2() < l)
                list.add(aLong);
        });
        return list;
    }

    public Tuple4<Long, Long, List<Long>, Long> get(long vc){
        return this.cache.get(vc);
    }

    public @NotNull Mono<Tuple3<Long, Long, List<Long>>> delete(long vc){
        Tuple3<Long, Long, List<Long>> tuple3 = this.cache.remove(vc);

        if(tuple3 == null)
            return Mono.empty();

        return this.databaseManager.executeSQL("DELETE FROM privateVoiceChannel WHERE voiceChannel = ?",
                statement -> statement.bind(0, vc))
                .then(Mono.just(tuple3));

    }
}