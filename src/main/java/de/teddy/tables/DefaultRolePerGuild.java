package de.teddy.tables;

import com.wetterquarz.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultRolePerGuild {
    private final @NotNull DatabaseManager databaseManager;
    private final @NotNull Map<Long, Long> cache;

    public DefaultRolePerGuild(@NotNull DatabaseManager databaseManager){
        this.databaseManager = databaseManager;

        this.cache = new HashMap<>();

        this.databaseManager.executeSQL("CREATE TABLE IF NOT EXISTS `defaultRolePerGuild` (" +
                "`guild` BIGINT(19) NULL DEFAULT NULL," +
                "`role` BIGINT(19) NULL DEFAULT NULL," +
                "PRIMARY KEY (`guild`)" +
                ")" +
                "COLLATE='utf8mb4_general_ci'" +
                ";")
                .then(this.loadAll())
                .subscribe();
    }

    private @NotNull Mono<Void> loadAll(){
        return this.databaseManager.executeSQL("SELECT * FROM defaultRolePerGuild")
                .flatMapMany(result ->
                        Flux.from(result.map((row, rowMetadata) ->
                                Tuples.of(Objects.requireNonNull(row.get("guild", Long.class)),
                                        Objects.requireNonNull(row.get("role", Long.class))))))
                .doOnNext(objects -> this.cache.put(objects.getT1(), objects.getT2()))
                .then();
    }

    public @NotNull Mono<Void> put(long guild, long role){
        this.cache.put(guild, role);

        return this.databaseManager.executeSQL("REPLACE INTO defaultRolePerGuild (guild, role) VALUES (?, ?)",
                statement ->
                        statement.bind(0, guild)
                                .bind(1, role))
                .then();
    }

    public Long get(long guild){
        return this.cache.get(guild);
    }

    public @NotNull Mono<Long> delete(long guild){
        Long l = this.cache.remove(guild);

        if(l == null)
            return Mono.empty();

        return this.databaseManager.executeSQL("DELETE FROM defaultRolePerGuild WHERE guild = ?",
                statement -> statement.bind(0, guild))
                .then(Mono.just(l));
    }
}
