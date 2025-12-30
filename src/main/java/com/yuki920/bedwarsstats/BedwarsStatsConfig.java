package com.yuki920.bedwarsstats;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class BedwarsStatsConfig {

    private static Configuration config;

    public static String apiKey = "";
    public static String myNick = "";
    public static BedwarsMode bedwarsMode = BedwarsMode.OVERALL;
    public static boolean sendToBedwarsLove = false;
    public static String bedwarsLoveApiUrl = "http://localhost:3000/api/live";

    public enum BedwarsMode {
        OVERALL("Overall", ""),
        SOLO("Solo", "eight_one_"),
        DOUBLES("Doubles", "eight_two_"),
        THREES("Threes", "four_three_"),
        FOURS("Fours", "four_four_");

        private final String displayName;
        private final String apiPrefix;

        BedwarsMode(String displayName, String apiPrefix) {
            this.displayName = displayName;
            this.apiPrefix = apiPrefix;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getApiPrefix() {
            return apiPrefix;
        }
    }

    public static void init(File configFile) {
        config = new Configuration(configFile);
        syncConfig();
    }

    public static void syncConfig() {
        config.load();

        apiKey = config.getString("apiKey", Configuration.CATEGORY_GENERAL, "", "Your Hypixel API key.");
        myNick = config.getString("myNick", Configuration.CATEGORY_GENERAL, "", "If you are nicked, enter your nick here to see your own stats.");
        sendToBedwarsLove = config.getBoolean("sendToBedwarsLove", Configuration.CATEGORY_GENERAL, false, "Enable sending data to bedwars.love for real-time stats.");
        bedwarsLoveApiUrl = config.getString("bedwarsLoveApiUrl", Configuration.CATEGORY_GENERAL, "http://localhost:3000/api/live", "The API endpoint for bedwars.love.");

        String modeString = config.getString("bedwarsMode", Configuration.CATEGORY_GENERAL, "OVERALL", "The Bedwars mode to display stats for.",
                new String[]{"OVERALL", "SOLO", "DOUBLES", "THREES", "FOURS"});
        try {
            bedwarsMode = BedwarsMode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            bedwarsMode = BedwarsMode.OVERALL;
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void setApiKey(String newApiKey) {
        apiKey = newApiKey;
        config.get(Configuration.CATEGORY_GENERAL, "apiKey", "").set(newApiKey);
        config.save();
    }

    public static void setMyNick(String newNick) {
        myNick = newNick;
        config.get(Configuration.CATEGORY_GENERAL, "myNick", "").set(newNick);
        config.save();
    }

    public static void setBedwarsMode(BedwarsMode newMode) {
        bedwarsMode = newMode;
        config.get(Configuration.CATEGORY_GENERAL, "bedwarsMode", "OVERALL").set(newMode.name());
        config.save();
    }

    public static void setSendToBedwarsLove(boolean enabled) {
        sendToBedwarsLove = enabled;
        config.get(Configuration.CATEGORY_GENERAL, "sendToBedwarsLove", false).set(enabled);
        config.save();
    }

    public static void setBedwarsLoveApiUrl(String newUrl) {
        bedwarsLoveApiUrl = newUrl;
        config.get(Configuration.CATEGORY_GENERAL, "bedwarsLoveApiUrl", "http://localhost:3000/api/live").set(newUrl);
        config.save();
    }
}