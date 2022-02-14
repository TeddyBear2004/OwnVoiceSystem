package de.teddy.tables;

import com.wetterquarz.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;

public class PrivateVoiceInitializer {
    private final @NotNull DatabaseManager databaseManager;
    private final @NotNull Map<Long, Tuple2<Long, Long>> cache; //Channel, Channel, Guild

    public PrivateVoiceInitializer(@NotNull DatabaseManager databaseManager){
        this.databaseManager = databaseManager;

        this.cache = new HashMap<>();

        this.databaseManager.executeSQL("CREATE TABLE IF NOT EXISTS `privateVoiceInitializer` (" +
                        "`category` BIGINT(19) NULL DEFAULT NULL," +
                        "`guild` BIGINT(19) NULL DEFAULT NULL," +
                        "`channel` BIGINT(19) NULL DEFAULT NULL" +
                        ")" +
                        "COLLATE='utf8mb4_general_ci'" +
                        ";")
                .then(this.loadAll())
                .subscribe();
    }

    public @NotNull Mono<Void> loadAll(){
        return this.databaseManager.executeSQL("SELECT * FROM privateVoiceInitializer")
                .flatMapMany(result -> Flux.from(result.map((row, rowMetadata) -> Tuples.of(
                        Objects.requireNonNull(row.get("channel", Long.class)),
                        Objects.requireNonNull(row.get("category", Long.class)),
                        Objects.requireNonNull(row.get("guild", Long.class))))))
                .doOnNext(tuple3 -> this.cache.put(tuple3.getT1(),
                        Tuples.of(tuple3.getT2(), tuple3.getT3())))
                .doOnNext(tuple3 -> this.cache.put(tuple3.getT2(), Tuples.of(tuple3.getT1(), tuple3.getT3())))
                .then();
    }

    public @NotNull List<Tuple2<Long, Long>> getByGuild(long guild){
        List<Tuple2<Long, Long>> list = new ArrayList<>();
        this.cache.forEach((aLong, tuple2) -> {
            if(tuple2.getT2() == guild)
                list.add(Tuples.of(aLong, tuple2.getT1()));
        });
        return list;
    }

    public long getGuildByChannel(long channelId){
        for(Map.Entry<Long, Tuple2<Long, Long>> entry : this.cache.entrySet()){
            Tuple2<Long, Long> tuple2 = entry.getValue();
            if(entry.getKey() == channelId || tuple2.getT1() == channelId)
                return tuple2.getT2();
        }
        return 0;
    }

    public @NotNull Mono<Void> put(long category, long channel, long guild){
        this.cache.put(channel, Tuples.of(category, guild));

        return this.databaseManager.executeSQL("INSERT INTO privateVoiceInitializer (`category`, `channel`, `guild`) VALUES (?, ?, ?)",
                        statement -> statement.bind(0, category).bind(1, channel).bind(2, guild))
                .then();
    }

    public @Nullable Long getOtherChannel(long channel){
        return this.cache.get(channel) == null ? null : this.cache.get(channel).getT1();
    }

    public @NotNull Mono<Long> delete(long value){
        if(!this.cache.containsKey(value))
            return Mono.empty();

        long l = this.cache.remove(value).getT1();

        this.cache.remove(l);

        return this.databaseManager.executeSQL("DELETE FROM privateVoiceInitializer WHERE channel = ? OR channel = ?",
                        statement -> statement.bind(0, l).bind(1, value))
                .then(Mono.just(l));
    }
}
