package com.bot;

import com.bot.commands.nsfw.R4cCommand;
import com.bot.commands.general.*;
import com.bot.commands.meme.AsciiCommand;
import com.bot.commands.meme.CommentCommand;
import com.bot.commands.nsfw.Rule34Command;
import com.bot.commands.owner.AvatarCommand;
import com.bot.commands.owner.UpdateGuildCountCommand;
import com.bot.commands.reddit.NewPostCommand;
import com.bot.commands.reddit.RandomPostCommand;
import com.bot.commands.reddit.ShitpostCommand;
import com.bot.commands.reddit.TopPostCommand;
import com.bot.commands.settings.*;
import com.bot.commands.voice.*;
import com.bot.models.InternalShard;
import com.bot.preferences.GuildPreferencesManager;
import com.bot.utils.Config;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShardingManager {

    private static ShardingManager instance;

    private Map<Integer, InternalShard> shards;

    private EventWaiter waiter;
    private List<Command.Category> commandCategories;
    private CommandClient client = null;

    public static ShardingManager getInstance() {
        return instance;
    }

    public static ShardingManager getInstance(int numShards) throws Exception {
        if (instance == null)
            instance = new ShardingManager(numShards);
        return instance;
    }

    // This adds a connection for each shard. Shards make it more efficient. ~1000 servers to shards is ideal
    // supportScript disables commands. Useful for running a supportScript simultaneously while the bot is going on prod
    private ShardingManager(int numShards) throws Exception {
        Config config = Config.getInstance();
        waiter = new EventWaiter();

        // Check if we are just doing a silent deploy (For debug and stress testing purposes)
        boolean silentDeploy = Boolean.parseBoolean(config.getConfig(Config.SILENT_DEPLOY));

        shards = new HashMap<>();
        Bot bot = null;
        bot = new Bot();

        CommandClientBuilder commandClientBuilder = new CommandClientBuilder();
        commandClientBuilder.setPrefix("~");
        commandClientBuilder.setAlternativePrefix("@mention");
        commandClientBuilder.setOwnerId(config.getConfig(Config.OWNER_ID));

        // If we are deploying silently we are not registering commands.
        if (!silentDeploy) {
            commandClientBuilder.addCommands(
                    // Voice Commands
                    new PlayCommand(bot),
                    new SearchCommand(bot, waiter),
                    new PauseCommand(),
                    new RepeatCommand(),
                    new StopCommand(),
                    new ResumeCommand(),
                    new VolumeCommand(),
                    new ListTracksCommand(),
                    new SkipCommand(),
                    new SaveMyPlaylistCommand(bot),
                    new ListMyPlaylistCommand(),
                    new LoadMyPlaylistCommand(bot),
                    new LoadGuildPlaylistCommand(bot),
                    new SaveGuildPlaylistCommand(bot),
                    new ListGuildPlaylistCommand(),

                    // Battle Royale
                    //new BattleRoyaleCommand(),

                    // General Commands
                    new InfoCommand(),
                    new InviteCommand(),
                    new SupportCommand(),
                    new StatsCommand(),
                    new ShardStatsCommand(),
                    new PingCommand(),
                    new GetSettingsCommand(),
                    new PrefixesCommand(),
                    new RollCommand(),
                    new UserCommand(),
                    new PermissionsCommand(waiter),
                    new ServerInfoCommand(),

                    // Meme Commands
                    new CommentCommand(),
                    new AsciiCommand(),
                    new ShitpostCommand(),

                    // Reddit Commands
                    new RandomPostCommand(),
                    new TopPostCommand(),
                    new NewPostCommand(),

                    // Guild Settings Commands
                    new DefaultVolumeCommand(),
                    new SetBaseRoleCommand(),
                    new SetModRoleCommand(),
                    new SetNSFWCommand(),
                    new SetVoiceRoleCommand(),
                    new EnableNSFWCommand(),
                    new DisableNSFWCommand(),
                    new AddPrefixCommand(),
                    new RemovePrefixCommand(),

                    // NSFW Commands
                    new Rule34Command(),

                    // 4chan commands
                    new R4cCommand()
            );
        } else {
            commandClientBuilder.useHelpBuilder(false);
        }

        // Owner commands are added regardless
        commandClientBuilder.addCommands(
                // Owner Commands -- All hidden
                new AvatarCommand(),
                new UpdateGuildCountCommand()
        );

        commandClientBuilder.setServerInvite("https://discord.gg/XMwyzxZ");
        commandClientBuilder.setEmojis("\u2714", "\u2757", "\u274c");
        commandClientBuilder.setGuildSettingsManager(new GuildPreferencesManager());
        client = commandClientBuilder.build();

        for (int i = 0; i < numShards; i++) {
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken(config.getConfig(Config.DISCORD_TOKEN))
                    .setGame(Game.playing("@Vinny help"))
                    .useSharding(i, numShards)
                    .build();

            jda.awaitReady();

            jda.addEventListener(waiter);
            jda.addEventListener(client);
            jda.addEventListener(bot);
            int shardId = jda.getShardInfo().getShardId();
            shards.put(shardId, new InternalShard(shardId, jda));

            System.out.println("Shard " + i + " built.");
        }
    }

    public Map<Integer, InternalShard> getShards() {
        return shards;
    }

    public int getTotalGuilds() {
        int guilds = 0;
        for (InternalShard shard : shards.values()) {
            guilds += shard.getServerCount();
        }
        return guilds;
    }

    public List<Command.Category> getCommandCategories() {
        if (commandCategories == null) {
            commandCategories = new ArrayList<>();
            for (Command c: client.getCommands()) {
                if (c.getCategory() == null)
                    continue;
                if (!commandCategories.contains(c.getCategory())) {
                    commandCategories.add(c.getCategory());
                }
            }
        }
        return commandCategories;
    }
}
