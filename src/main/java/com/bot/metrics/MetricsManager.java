package com.bot.metrics;

import com.bot.models.InternalGuild;
import com.bot.models.InternalTextChannel;
import com.bot.models.InternalUser;
import com.bot.utils.Config;
import com.jagrosh.jdautilities.command.Command;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class MetricsManager {

    // TODO: Health checks
    private final StatsDClient statsd;
    private Config config;
    private static MetricsManager instance;

    public static MetricsManager getInstance() {
        if (instance == null) {
            instance = new MetricsManager();
        }
        return instance;
    }

    private MetricsManager() {
        config = Config.getInstance();
        statsd = new NonBlockingStatsDClient(
                "vinny-redux.live",                          /* prefix to any stats; may be null or empty string */
                config.getConfig(Config.DATADOG_HOSTNAME),         /* common case: localhost */
                8125,                                         /* port */
                new String[] {"vinny:live"}                        /* Datadog extension: Constant tags, always applied */
            );
    }

    public void markCommand(Command command, User user, Guild guild) {
        String userTag = "user:" + user.getId();
        String commandTag = "command:" + command.getName();
        String categoryTag = "category:" + command.getCategory().getName();

        // Support guild being null (use in PMs)
        String guildOrPM = guild == null ? "PM" : guild.getId();
        String guildTag = "guild:" + guildOrPM;

        statsd.incrementCounter("command", userTag, guildTag, commandTag, categoryTag);
    }

    public void markGuildAliasExecuted(InternalGuild guild) {
        String sourceTag = "source:guild";
        String sourceIdTag = "sourceid:" + guild.getId();
        statsd.incrementCounter("alias", sourceTag, sourceIdTag);
    }

    public void markChannelAliasExecuted(InternalTextChannel channel) {
        String sourceTag = "source:channel";
        String sourceIdTag = "sourceid:" + channel.getId();
        statsd.incrementCounter("alias", sourceTag, sourceIdTag);
    }

    public void markUserAliasExecuted(InternalUser user) {
        String sourceTag = "source:user";
        String sourceIdTag = "sourceid:" + user.getId();
        statsd.incrementCounter("alias", sourceTag, sourceIdTag);
    }

    public void markCommandFailed(Command command, User user, Guild guild) {
        String userTag = "user:" + user.getId();
        String guildTag = "guild:" + guild.getId();
        String commandTag = "command:" + command.getName();
        String categoryTag = "category:" + command.getCategory().getName();
        statsd.incrementCounter("command.failed", userTag, guildTag, commandTag, categoryTag);
    }

    public void updateCacheSize(String name, int count, int limit) {
        statsd.recordGaugeValue("cache." + name + ".size", count);
        statsd.recordGaugeValue("cache." + name + ".max", limit);
    }

    public void markDiscordEvent(int shard) {
        String shardTag = "shard:" + shard;
        statsd.incrementCounter("discord.event", shardTag);
    }

    public void markCacheHit(String name) {
        statsd.incrementCounter("cache." + name + ".hit");
    }

    public void markCacheMiss(String name) {
        statsd.incrementCounter("cache." + name + ".miss");
    }

    public void updateGuildCount(int count) {statsd.recordGaugeValue("guild.count", count);}

    public void updateUserCount(int count) {statsd.recordGaugeValue("users.count", count);}

    public void updateActiveVoiceConnectionsCount(int count) {statsd.recordGaugeValue("connections.voice.active", count);}

    public void updateIdleVoiceConnectionsCount(int count) {statsd.recordGaugeValue("connections.voice.idle", count);}

    public void updateUsersInVoice(int count) {statsd.recordGaugeValue("connections.voice.users", count);}

    public void updateQueuedTracks(int count) {statsd.recordGaugeValue("connections.voice.tracks", count);}

    public void updatePing(int shard, long ping) {
        String shardTag = "shard:" + shard;
        statsd.recordGaugeValue("discord.ping", ping, shardTag);
    }

    public void updateShards(int healthy, int unhealthy) {
        statsd.recordGaugeValue("shards.healthy", healthy);
        statsd.recordGaugeValue("shards.unhealthy", unhealthy);
    }
}
