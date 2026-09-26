package eu.usrv.enhancedlootbags.client;

import eu.usrv.enhancedlootbags.core.stats.LootBagStats;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats.GroupStats;

public class ClientLootBagStats {

    private static volatile LootBagStats stats = new LootBagStats();

    public static LootBagStats getStats() {
        return stats;
    }

    public static synchronized void setStats(LootBagStats newStats) {
        stats = newStats;
    }

    public static synchronized void updateGroup(GroupStats group) {
        LootBagStats copy = new LootBagStats();
        for (GroupStats grp : stats.getGroups()) {
            copy.putGroup(grp);
        }
        copy.putGroup(group);
        stats = copy;
    }

    public static synchronized void clear() {
        stats = new LootBagStats();
    }
}
