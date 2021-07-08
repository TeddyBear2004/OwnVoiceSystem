package de.teddy.tables;

import com.wetterquarz.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultChannelConfigurations {
    private final @NotNull DatabaseManager databaseManager;
    private final @NotNull Map<Tuple2<Long, Long>, Tuple5<Integer, Integer, String, String, String>> cache; //guild, owner // bitrate, userlimit, name, admins, permission

    public DefaultChannelConfigurations(@NotNull DatabaseManager databaseManager){
        this.databaseManager = databaseManager;

        this.cache = new HashMap<>();

        this.databaseManager.executeSQL("CREATE TABLE IF NOT EXISTS `defaultChannelConfigurations` (" +
                "`guild` BIGINT NOT NULL DEFAULT '0'," +
                "`owner` BIGINT NOT NULL DEFAULT '0'," +
                "`bitrate` TINYINT NULL DEFAULT NULL," +
                "`userlimit` TINYINT NULL DEFAULT NULL," +
                "`name` TINYTEXT NULL DEFAULT NULL," +
                "`admins` TEXT NULL DEFAULT ''," +
                "`permissions` MEDIUMTEXT NULL DEFAULT NULL," +
                "PRIMARY KEY (`guild`, `owner`)" +
                ")" +
                "COLLATE='utf8mb4_general_ci'" +
                ";")
                .then(this.loadAll())
                .subscribe();
    }

    public @NotNull Mono<Void> loadAll(){
        return this.databaseManager.executeSQL("SELECT * FROM defaultChannelConfigurations")
                .flatMapMany(result ->
                        Flux.from(result.map((row, rowMetadata) ->
                                Tuples.of(
                                        Tuples.of(Objects.requireNonNull(row.get("guild", Long.class)), Objects.requireNonNull(row.get("owner", Long.class))),
                                        Tuples.of(Objects.requireNonNull(row.get("bitrate", Integer.class)),
                                                Objects.requireNonNull(row.get("userlimit", Integer.class)),
                                                Objects.requireNonNull(row.get("name")).toString(),
                                                Objects.requireNonNull(row.get("admins")).toString(),
                                                Objects.requireNonNull(row.get("permissions")).toString())))))
                .doOnNext(objects -> this.cache.put(objects.getT1(), objects.getT2()))
                .then();
    }

    public @NotNull Mono<Void> put(long guild, long owner, int bitrate, int userlimit, @NotNull String name, @NotNull String admins, @NotNull String permissions){
        return this.put(Tuples.of(guild, owner), Tuples.of(bitrate, userlimit, name, admins, permissions));
    }

    public @NotNull Mono<Void> put(@NotNull Tuple2<Long, Long> tuple2, @NotNull Tuple5<Integer, Integer, String, String, String> tuple5){
        this.cache.put(tuple2, tuple5);

        return this.databaseManager.executeSQL("REPLACE INTO defaultChannelConfigurations (guild, owner, bitrate, userlimit, name, admins, permissions) VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> statement
                        .bind(0, tuple2.getT1())
                        .bind(1, tuple2.getT2())
                        .bind(2, tuple5.getT1())
                        .bind(3, tuple5.getT2())
                        .bind(4, tuple5.getT3())
                        .bind(5, tuple5.getT4())
                        .bind(6, tuple5.getT5()))
                .then();
    }

    public @Nullable Tuple5<Integer, Integer, String, String, String> get(Tuple2<Long, Long> tuple2){
        return this.cache.get(tuple2);
    }

    public @Nullable Tuple5<Integer, Integer, String, String, String> get(long guild, long user){
        return this.get(Tuples.of(guild, user));
    }

    public @NotNull Mono<Tuple5<Integer, Integer, String, String, String>> delete(@NotNull Tuple2<Long, Long> tuple2){
        Tuple5<Integer, Integer, String, String, String> tuple5 = this.cache.remove(tuple2);

        if(tuple5 == null)
            return Mono.empty();

        return this.databaseManager.executeSQL("DELETE FROM defaultChannelConfigurations WHERE guild = ? AND owner = ?",
                statement -> statement.bind(0, tuple2.getT1()).bind(1, tuple2.getT2()))
                .then(Mono.just(tuple5));
    }
}
