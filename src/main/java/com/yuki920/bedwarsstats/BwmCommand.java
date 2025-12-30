package com.yuki920.bedwarsstats;

import net.minecraft.client.Minecraft; // ★★★ この行を追加 ★★★
import net.minecraft.client.network.NetworkPlayerInfo; // ★★★ この行を追加 ★★★
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList; // 念の為追加（ストリームを使わない場合）
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BwmCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "bwm";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bwm <stats|settings>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Accessible by everyone
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("stats")) {
            handleStatsCommand(sender, args);
        } else if (subCommand.equals("settings")) {
            handleSettingsCommand(sender, args);
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
        }
    }

    private void handleStatsCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm stats <username> [mode]"));
            return;
        }
        String username = args[1];
        if (args.length > 2) {
            try {
                BedwarsStatsConfig.BedwarsMode mode = BedwarsStatsConfig.BedwarsMode.valueOf(args[2].toUpperCase());
                HypixelApiHandler.processPlayer(username, mode);
            } catch (IllegalArgumentException e) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid mode."));
            }
        } else {
            HypixelApiHandler.processPlayer(username);
        }
    }

    private void handleSettingsCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings <apikey|mode|nick|bedwarslove>"));
            return;
        }
        String setting = args[1].toLowerCase();
        switch (setting) {
            case "apikey":
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings apikey <key>"));
                    return;
                }
                String apiKey = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                BedwarsStatsConfig.setApiKey(apiKey);
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "API Key set successfully!"));
                break;
            case "mode":
                 if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings mode <mode>"));
                    return;
                }
                try {
                    BedwarsStatsConfig.BedwarsMode mode = BedwarsStatsConfig.BedwarsMode.valueOf(args[2].toUpperCase());
                    BedwarsStatsConfig.setBedwarsMode(mode);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bedwars mode set to " + mode.getDisplayName()));
                } catch (IllegalArgumentException e) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid mode."));
                }
                break;
            case "nick":
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings nick <nickname>"));
                    return;
                }
                String nick = args[2];
                BedwarsStatsConfig.setMyNick(nick);
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Your nick has been set to: " + nick));
                break;
            case "bedwarslove":
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings bedwarslove <on|off|url>"));
                    return;
                }
                String action = args[2].toLowerCase();
                if (action.equals("on")) {
                    BedwarsStatsConfig.setSendToBedwarsLove(true);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "bedwars.love integration enabled."));
                } else if (action.equals("off")) {
                    BedwarsStatsConfig.setSendToBedwarsLove(false);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "bedwars.love integration disabled."));
                } else if (action.equals("url")) {
                    if (args.length < 4) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings bedwarslove url <new_url>"));
                        return;
                    }
                    String newUrl = args[3];
                    BedwarsStatsConfig.setBedwarsLoveApiUrl(newUrl);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "bedwars.love API URL set to: " + newUrl));
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings bedwarslove <on|off|url>"));
                }
                break;
            default:
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /bwm settings <apikey|mode|nick|bedwarslove>"));
                break;
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "stats", "settings");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("settings")) {
                return getListOfStringsMatchingLastWord(args, "apikey", "mode", "nick", "bedwarslove");
            }
            if (args[0].equalsIgnoreCase("stats")) {
                // ▼▼▼▼▼ ここを修正 ▼▼▼▼▼
                // サーバーにいるプレイヤー名を取得する
                if (Minecraft.getMinecraft().getNetHandler() != null) {
                    List<String> playerNames = Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream()
                            .map(networkPlayerInfo -> networkPlayerInfo.getGameProfile().getName())
                            .collect(Collectors.toList());
                    return getListOfStringsMatchingLastWord(args, playerNames);
                }
                // ▲▲▲▲▲ ここまで修正 ▲▲▲▲▲
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("settings") && args[1].equalsIgnoreCase("bedwarslove")) {
                return getListOfStringsMatchingLastWord(args, "on", "off", "url");
            }
             if (args[0].equalsIgnoreCase("stats") || (args[0].equalsIgnoreCase("settings") && args[1].equalsIgnoreCase("mode"))) {
                List<String> modes = Arrays.stream(BedwarsStatsConfig.BedwarsMode.values())
                                           .map(Enum::name)
                                           .collect(Collectors.toList());
                return getListOfStringsMatchingLastWord(args, modes);
            }
        }
        return Collections.emptyList();
    }
}
