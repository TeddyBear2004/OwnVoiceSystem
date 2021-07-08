package de.teddy.commands.ov.util;

import de.teddy.Handler;
import reactor.util.function.Tuple4;

import java.util.List;

public class HasPermission {
    public static boolean hasPermissionToEdit(long channelId, long userId){
        if(Handler.PRIVATE_VOICE_CHANNEL.containsNot(channelId))
            return false;

        Tuple4<Long, Long, List<Long>, Long> tuple3 = Handler.PRIVATE_VOICE_CHANNEL.get(channelId);
        if(tuple3.getT1() == userId)
            return true;

        return tuple3.getT3().contains(userId);
    }

    public static boolean isOwner(long channelId, long userId){
        return Handler.PRIVATE_VOICE_CHANNEL.get(channelId) != null && Handler.PRIVATE_VOICE_CHANNEL.get(channelId).getT1() == userId;
    }
}
