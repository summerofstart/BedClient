package com.yuki920.bedwarsstats;

import com.google.gson.JsonObject;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.apache.ApacheHttpClient;
import net.hypixel.api.reply.PlayerReply;
import net.hypixel.api.reply.StatusReply;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HypixelApiHandler {
    private static HypixelAPI hypixelAPI;

    public static void initializeApi() {
        if (BedwarsStatsConfig.apiKey != null && !BedwarsStatsConfig.apiKey.isEmpty()) {
            try {
                hypixelAPI = new HypixelAPI(new ApacheHttpClient(UUID.fromString(BedwarsStatsConfig.apiKey)));
            } catch (IllegalArgumentException e) {
                sendMessageToPlayer(EnumChatFormatting.RED + "[BedwarsStats] Invalid API Key format!");
                hypixelAPI = null;
            }
        } else {
            hypixelAPI = null;
        }
    }

    public static void checkApiKeyValidity() {
        if (hypixelAPI == null) return;
        // We check the key's validity by making a request for a known valid UUID.
        // If it fails with a BadResponseException indicating an invalid key, we know the key is bad.
        hypixelAPI.getPlayerByUuid(UUID.fromString("f7c77d99-9f15-4a66-a87d-c4a51ef30d19")).whenCompleteAsync((reply, throwable) -> {
            if (throwable != null) {
                if (throwable.getCause() instanceof net.hypixel.api.exceptions.BadResponseException) {
                    if (throwable.getCause().getMessage().contains("Invalid API key")) {
                        sendMessageToPlayer(EnumChatFormatting.RED + "[BedwarsStats] Your Hypixel API key appears to be invalid or expired!");
                        sendMessageToPlayer(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GREEN + "/bwm settings apikey <key> " + EnumChatFormatting.YELLOW + "to set a new one.");
                    }
                } else {
                    // Don't bother the user with other potential errors during this background check.
                    System.err.println("Error while checking API key validity:");
                    throwable.printStackTrace();
                }
            }
            // If the request succeeds, the key is valid. We don't need to do anything.
        });
    }

    public static void processPlayer(String username) {
        processPlayer(username, BedwarsStatsConfig.bedwarsMode);
    }

    public static void processPlayer(String username, BedwarsStatsConfig.BedwarsMode mode) {
        if (hypixelAPI == null) {
            sendMessageToPlayer(EnumChatFormatting.RED + "Hypixel API Key not set!");
            return;
        }

        CompletableFuture<PlayerReply> playerFuture;

        String myNick = BedwarsStatsConfig.myNick;
        if (myNick != null && !myNick.isEmpty() && myNick.equalsIgnoreCase(username)) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                playerFuture = hypixelAPI.getPlayerByUuid(mc.thePlayer.getUniqueID());
            } else {
                return; // Cannot get own UUID
            }
        } else {
            playerFuture = hypixelAPI.getPlayerByName(username);
        }

        playerFuture.whenCompleteAsync((playerReply, playerError) -> {
            if (playerError != null) {
                sendMessageToPlayer(EnumChatFormatting.RED + "[BedwarsStats] Error fetching player data!");
                playerError.printStackTrace();
                return;
            }

            PlayerReply.Player player = playerReply.getPlayer();
            if (player == null || !player.exists()) {
                sendMessageToPlayer(EnumChatFormatting.YELLOW + username + EnumChatFormatting.RESET + " is nicked, stats cannot be retrieved.");
                return;
            }

            hypixelAPI.getStatus(player.getUuid()).whenCompleteAsync((statusReply, statusError) -> {
                if (statusError != null) {
                    // Log and ignore, we can still show stats without status
                    System.err.println("Error fetching player status:");
                    statusError.printStackTrace();
                }

                // The API returns the correct capitalized name, but for nicked-self case, we want the nick.
                // For others, we want the correct name.
                final String finalUsername = (myNick != null && !myNick.isEmpty() && myNick.equalsIgnoreCase(username)) ? username : player.getName();

                StatusReply.Session session = (statusReply != null && statusReply.isSuccess()) ? statusReply.getSession() : null;

                String chatMessage = formatStats(player, finalUsername, session, mode);
                if (chatMessage != null) {
                    sendMessageToPlayer(chatMessage);
                }
            });
        });
    }

    private static String formatStats(PlayerReply.Player player, String username, StatusReply.Session status, BedwarsStatsConfig.BedwarsMode mode) {
        String rankPrefix = getRankPrefix(player);

        String statusString;
        if (status == null) {
            statusString = ""; // Status not available
        } else if (status.isOnline()) {
            statusString = EnumChatFormatting.GREEN + " Online" + EnumChatFormatting.RESET + " in " + status.getServerType().getName();
        } else {
            statusString = EnumChatFormatting.RED + " Offline";
        }


        if (player.getObjectProperty("stats") == null || player.getObjectProperty("stats").getAsJsonObject().get("Bedwars") == null) {
            return rankPrefix + username + EnumChatFormatting.GRAY + ": No Bedwars stats found." + statusString;
        }

        JsonObject bedwars = player.getObjectProperty("stats").getAsJsonObject().getAsJsonObject("Bedwars");
        String prefix = mode.getApiPrefix();

        int stars = player.getObjectProperty("achievements") != null && player.getObjectProperty("achievements").getAsJsonObject().has("bedwars_level") ? player.getObjectProperty("achievements").getAsJsonObject().get("bedwars_level").getAsInt() : 0;
        int wins = bedwars.has(prefix + "wins_bedwars") ? bedwars.get(prefix + "wins_bedwars").getAsInt() : 0;
        int losses = bedwars.has(prefix + "losses_bedwars") ? bedwars.get(prefix + "losses_bedwars").getAsInt() : 0;
        int finalKills = bedwars.has(prefix + "final_kills_bedwars") ? bedwars.get(prefix + "final_kills_bedwars").getAsInt() : 0;
        int finalDeaths = bedwars.has(prefix + "final_deaths_bedwars") ? bedwars.get(prefix + "final_deaths_bedwars").getAsInt() : 0;

        double wlr = (losses == 0) ? wins : (double) wins / losses;
        double fkdr = (finalDeaths == 0) ? finalKills : (double) finalKills / finalDeaths;

        String prestige = PrestigeFormatter.formatPrestige(stars);
        String winsColor = getWinsColor(wins);
        String wlrColor = getWlrColor(wlr);
        String finalsColor = getFinalsColor(finalKills);
        String fkdrColor = getFkdrColor(fkdr);

        return String.format("%s %s%s%s%s: Wins %s%s%s | WLR %s%.2f%s | Finals %s%s%s | FKDR %s%.2f",
                prestige, rankPrefix, username, EnumChatFormatting.RESET, statusString,
                winsColor, String.format("%,d", wins), EnumChatFormatting.RESET,
                wlrColor, wlr, EnumChatFormatting.RESET,
                finalsColor, String.format("%,d", finalKills), EnumChatFormatting.RESET,
                fkdrColor, fkdr);
    }

    private static String getRankPrefix(PlayerReply.Player player) {
        // ランクに関連する情報を取得
        String rank = player.getHighestRank();
        String monthlyPackageRank = player.getStringProperty("monthlyPackageRank", "NONE");
        String newPackageRank = player.getStringProperty("newPackageRank", "NONE");
    
        // Youtuberランクを最優先で処理
        if (rank != null && rank.equals("YOUTUBER")) {
            return EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "YOUTUBE" + EnumChatFormatting.RED + "] ";
        }
    
        // 表示するランクを決定 (MVP++, MVP+ など)
        String displayRank = !monthlyPackageRank.equals("NONE") ? monthlyPackageRank : newPackageRank;
        if (displayRank.equals("NONE")) {
            return EnumChatFormatting.GRAY.toString(); // ランクなし
        }
    
        // --- ここが「+」の色を正しく処理する部分です ---
        // plusColor変数を一度だけ宣言します
        EnumChatFormatting plusColor = EnumChatFormatting.RED; // デフォルトは赤色
        String rankPlusColor = player.getStringProperty("rankPlusColor", "RED");
        try {
            // APIから返ってきた色の名前 ("LIGHT_PURPLE"など) を EnumChatFormatting に変換
            plusColor = EnumChatFormatting.valueOf(rankPlusColor);
        } catch (IllegalArgumentException e) {
            // もし未知の色が来てもエラーにならないように、デフォルトの赤を使う
            plusColor = EnumChatFormatting.RED;
        }
        // -----------------------------------------
    
        // ランクに応じて最終的な文字列を生成
        switch (displayRank) {
            case "VIP":
                return EnumChatFormatting.GREEN + "[VIP] ";
            case "VIP_PLUS":
                return EnumChatFormatting.GREEN + "[VIP" + EnumChatFormatting.GOLD + "+" + EnumChatFormatting.GREEN + "] ";
            case "MVP":
                return EnumChatFormatting.AQUA + "[MVP] ";
            case "MVP_PLUS":
                return EnumChatFormatting.AQUA + "[MVP" + plusColor + "+" + EnumChatFormatting.AQUA + "] ";
            case "SUPERSTAR": // MVP++ のことです
                return EnumChatFormatting.GOLD + "[MVP" + plusColor + "++" + EnumChatFormatting.GOLD + "] ";
            default:
                return EnumChatFormatting.GRAY.toString(); // その他の場合は灰色
        }
    }
    // ★★★ 2. Statsごとの色付け用ヘルパーメソッド ★★★
    private static String getFkdrColor(double fkdr) {
        if (fkdr >= 20) return EnumChatFormatting.DARK_PURPLE.toString();
        if (fkdr >= 15) return EnumChatFormatting.LIGHT_PURPLE.toString();
        if (fkdr >= 10) return EnumChatFormatting.DARK_RED.toString();
        if (fkdr >= 8)  return EnumChatFormatting.RED.toString();
        if (fkdr >= 6)  return EnumChatFormatting.GOLD.toString();
        if (fkdr >= 4)  return EnumChatFormatting.YELLOW.toString();
        if (fkdr >= 2)  return EnumChatFormatting.DARK_GREEN.toString();
        if (fkdr >= 1)  return EnumChatFormatting.GREEN.toString();
        if (fkdr >= 0.5) return EnumChatFormatting.WHITE.toString();
        return EnumChatFormatting.GRAY.toString();
    }

    private static String getWlrColor(double wlr) {
        if (wlr >= 10) return EnumChatFormatting.DARK_PURPLE.toString();
        if (wlr >= 8)  return EnumChatFormatting.LIGHT_PURPLE.toString();
        if (wlr >= 6)  return EnumChatFormatting.DARK_RED.toString();
        if (wlr >= 5)  return EnumChatFormatting.RED.toString();
        if (wlr >= 4)  return EnumChatFormatting.GOLD.toString();
        if (wlr >= 3)  return EnumChatFormatting.YELLOW.toString();
        if (wlr >= 2)  return EnumChatFormatting.DARK_GREEN.toString();
        if (wlr >= 1)  return EnumChatFormatting.GREEN.toString();
        if (wlr >= 0.5) return EnumChatFormatting.WHITE.toString();
        return EnumChatFormatting.GRAY.toString();
    }

    private static String getWinsColor(int wins) {
        if (wins >= 50000) return EnumChatFormatting.DARK_PURPLE.toString();
        if (wins >= 25000) return EnumChatFormatting.LIGHT_PURPLE.toString();
        if (wins >= 10000) return EnumChatFormatting.DARK_RED.toString();
        if (wins >= 5000) return EnumChatFormatting.RED.toString();
        if (wins >= 2500) return EnumChatFormatting.GOLD.toString();
        if (wins >= 1000) return EnumChatFormatting.YELLOW.toString();
        if (wins >= 500) return EnumChatFormatting.DARK_GREEN.toString();
        if (wins >= 250) return EnumChatFormatting.GREEN.toString();
        if (wins >= 50) return EnumChatFormatting.WHITE.toString();
        return EnumChatFormatting.GRAY.toString();
    }

    private static String getFinalsColor(int finals) {
        if (finals >= 100000) return EnumChatFormatting.DARK_PURPLE.toString();
        if (finals >= 50000) return EnumChatFormatting.LIGHT_PURPLE.toString();
        if (finals >= 25000) return EnumChatFormatting.DARK_RED.toString();
        if (finals >= 10000) return EnumChatFormatting.RED.toString();
        if (finals >= 5000) return EnumChatFormatting.GOLD.toString();
        if (finals >= 2500) return EnumChatFormatting.YELLOW.toString();
        if (finals >= 1000) return EnumChatFormatting.DARK_GREEN.toString();
        if (finals >= 500) return EnumChatFormatting.GREEN.toString();
        if (finals >= 100) return EnumChatFormatting.WHITE.toString();
        return EnumChatFormatting.GRAY.toString();
    }


    private static void sendMessageToPlayer(String message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
            }
        });
    }
}
