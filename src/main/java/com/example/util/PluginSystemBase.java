package com.example.util;

import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PluginSystemBase extends JavaPlugin {
    static int shortLimit = 1, longLimit = 10;
    static long shortDuration = 30, longDuration = 60 * 60;
    static Map<Long, List<Long>> usageHistory = new HashMap<>();

    public enum RejectReason {
        Accepted,
        ShortLimitReached,
        LongLimitReached;
    }

    public PluginSystemBase(@NotNull JvmPluginDescription description) {
        super(description);
    }

    public class ResponseInfo {
        public RejectReason rejectReason;
        public int remainShort, remainLong;

        ResponseInfo(RejectReason rejectReason, int remainShort, int remainLong) {
            this.rejectReason = rejectReason;
            this.remainShort = remainShort;
            this.remainLong = remainLong;
        }
    }

    public ResponseInfo request(long user) {
        if (!usageHistory.containsKey(user)) {
            usageHistory.put(user, new ArrayList<>());
            usageHistory.get(user).add(new Date().getTime()/1000);
            return new ResponseInfo(RejectReason.Accepted, 0, longLimit - 1);
        }

        long now = new Date().getTime()/1000;
        List<Long> userHistory = usageHistory.get(user);
        int shortR = (now - userHistory.get(userHistory.size() - 1) > shortDuration) ? 1 : 0;

        userHistory.removeIf(time -> now - time > longDuration);

        if (userHistory.size() >= longLimit) {
            return new ResponseInfo(RejectReason.LongLimitReached, shortR, 0);
        }
        if (shortR == 0) {
            return new ResponseInfo(RejectReason.ShortLimitReached, 0, longLimit - userHistory.size());
        }

        userHistory.add(now);
        return new ResponseInfo(RejectReason.Accepted, 0, longLimit - userHistory.size());
    }
}
