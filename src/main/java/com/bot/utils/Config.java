package com.bot.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {

    private static Config config = null;
    private final File configFile;
    private Map<String, String> configs;

    public static final String DISCORD_TOKEN = "DISCORD_TOKEN";
    public static final String REDDIT_TOKEN = "REDDIT_TOKEN";
    public static final String REDDIT_CLIENT_ID = "REDDIT_CLIENT_ID";
    public static final String OWNER_ID = "OWNER_ID";
    public static final String P90_TOKEN = "P90_TOKEN";


    public static final String SILENT_DEPLOY = "SILENT_DEPLOY";
    public static final String DB_URI = "DB_URI";
    public static final String DB_USERNAME = "DB_USERNAME";
    public static final String DB_PASSWORD = "DB_PASSWORD";
    public static final String DB_SCHEMA = "DB_SCHEMA";

    public static final String NUM_SHARDS = "NUM_SHARDS";
    public static final String PREFIX = "PREFIX";
    public static final String DATA_LOADER = "DATA_LOADER";

    public static final String DATADOG_HOSTNAME = "DATADOG_HOSTNAME";

    public static final String GUILD_PREFS_CACHE_MAX_ITEMS = "GUILD_PREFS_CACHE_MAX_ITEMS";
    public static final String GUILD_PREFS_CACHE_OBJECT_LIFETIME = "GUILD_PREFS_OBJECT_LIFETIME";
    public static final String GUILD_PREFS_CACHE_CLEANUP_INTERVAL = "GUILD_PREFS_CACHE_CLEANUP_INTERVAL";

    public static final String MARKOV_CACHE_MAX_ITEMS = "MARKOV_CACHE_MAX_ITEMS";
    public static final String MARKOV_CACHE_OBJECT_LIFETIME = "MARKOV_CACHE_OBJECT_LIFETIME";
    public static final String MARKOV_CACHE_CLEANUP_INTERVAL = "MARKOV_CACHE_CLEANUP_INTERVAL";

    // Configs around posting user counts to external apis
    public static final String ENABLE_EXTERNAL_APIS = "ENABLE_EXTERNAL_APIS";
    public static final String DISCORD_BOT_ID = "BOT_ID";
    public static final String BOTS_FOR_DISCORD_API_TOKEN = "BOTS_FOR_DISCORD_API_TOKEN";
    public static final String DISCORD_BOT_LIST_API_TOKEN = "DISCORD_BOT_LIST_API_TOKEN";
    public static final String DISCORD_BOTS_ORG_API_TOKEN = "DISCORD_BOTS_ORG_API_TOKEN";
    public static final String BOTS_ON_DISCORD_API_TOKEN = "BOTS_ON_DISCORD_API_TOKEN";
    public static final String BOTS_GG_API_TOKEN = "BOTS_GG_API_TOKEN";
    public static final String DISCORD_BOATS_TOKEN = "DISCORD_BOATS_TOKEN";
    public static final String BOTLIST_SPACE_TOKEN = "BOTLIST_SPACE_TOKEN";

    public static final String ENABLE_LOGGING_CHANNELS = "ENABLE_LOGGING_CHANNELS";
    public static final String DEBUG_CHANNEL_ID = "DEBUG_CHANNEL_ID";
    public static final String ERROR_CHANNEL_ID = "ERROR_CHANNEL_ID";

    public static final String ONLINE_EMOJI = "ONLINE_EMOJI";
    public static final String IDLE_EMOJI = "IDLE_EMOJI";
    public static final String DND_EMOJI = "DND_EMOJI";
    public static final String OFFLINE_EMOJI = "OFFLINE_EMOJI";

    private Config() {
        this.configFile = new File("res/config/config.conf");
        configs = new HashMap<>();

        try {
            setConfig();
        }
        catch (FileNotFoundException f) {
            // If config file is gone we can try using env vars
            configs = System.getenv();
        }
    }

    public static Config getInstance() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private void setConfig() throws FileNotFoundException {
        Scanner scanner = new Scanner(configFile);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("***")){
                String key = line.substring(3, line.length()-3);
                configs.put(key, scanner.nextLine());
            }
        }
    }


    public String getConfig(String key) {
            return configs.get(key);
    }
}
