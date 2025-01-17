package com.bot.utils;

import com.bot.voice.QueuedAudioTrack;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FormattingUtils {

    public static ArrayList<String> splitTextIntoChunksByWords(String input, int chunkLength) {
        ArrayList<String> stringList = new ArrayList<>();

        String regex = "\\s*(.{" + chunkLength + "}[^.]*\\.|.+$)";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                stringList.add(matcher.group(i));
            }
        }

        return stringList;
    }

    public static String formattedRolesList(Member member) {
        StringBuilder rolesBuilder = new StringBuilder();
        for (Role r: member.getRoles()) {
            rolesBuilder.append(r.getAsMention());
        }
        return rolesBuilder.toString();
    }

    public static String formatOffsetDateTimeToDay(OffsetDateTime offsetDateTime) {
        // Return the yyyy-mm-dd only from the string
        return offsetDateTime.toString().split("T")[0];
    }

    public static String getOnlineStatusEmoji(Member member) {
        Config config = Config.getInstance();

        switch (member.getOnlineStatus()) {
            case ONLINE:
                return config.getConfig(Config.ONLINE_EMOJI);
            case IDLE:
                return config.getConfig(Config.IDLE_EMOJI);
            case DO_NOT_DISTURB:
                return config.getConfig(Config.DND_EMOJI);
            default:
                return config.getConfig(Config.OFFLINE_EMOJI);
        }
    }

    public static MessageEmbed getAudioTrackEmbed(QueuedAudioTrack queuedAudioTrack, int volume) {
        EmbedBuilder builder = new EmbedBuilder();

        AudioTrack track = queuedAudioTrack.getTrack();

        builder.setTitle("Now Playing: ");
        builder.setDescription("[" + track.getInfo().title + "](" + track.getInfo().uri + ")");
        builder.addField("Duration", msToMinSec(track.getInfo().length), false);
        builder.addField("Requested by", queuedAudioTrack.getRequesterName(), false);
        builder.setFooter("Volume: " + volume, null);

        builder.setColor(getColorForTrack(track.getInfo().uri));

        // If youtube, get the thumbnail
        if (track.getInfo().uri.contains("www.youtube.com")) {
            String videoID = track.getInfo().uri.split("=")[1];
            builder.setThumbnail("https://img.youtube.com/vi/" + videoID + "/0.jpg");
        }

        return builder.build();
    }

    //Helper method for song that takes length in Milliseconds and outputs it in a more readable HH:MM:SS format
    public static String msToMinSec(long length) {
        int totSeconds = (int)length/1000;
        String seconds = "";
        String minutes = "";
        String hours = "";
        if (totSeconds%60 < 10)
            seconds = "0" + totSeconds%60;
        else
            seconds += totSeconds%60;
        if (totSeconds/60 < 10)
            minutes = "0" + totSeconds/60;
        else if (totSeconds/60 > 59)
            minutes += (totSeconds/60)%60;
        else
            minutes += totSeconds/60;
        if (totSeconds/3600 < 10)
            hours = "0" + (totSeconds/60)/60;
        else
            hours += (totSeconds/60)/60;

        if ("00".equals(hours))
            return minutes + ":" + seconds;
        else {
            if (minutes.length() == 1)
                minutes = "0" + minutes;
            return hours + ":" + minutes + ":" + seconds;
        }
    }

    public static Color getColorForTrack(String uri) {
        Color toReturn = Color.BLACK; // Default
        if (uri.contains("youtube.com"))
            toReturn = Color.red;
        else if (uri.contains("soundcloud.com"))
            toReturn = Color.orange;
        else if (uri.contains("twitch.tv"))
            toReturn = new Color(100, 65, 165); // Twitch purple

        return toReturn;
    }

    public static List<String> getGamesPaginatedList(int pageSize, Map<String, List<Member>> gameMap) {
        List<String> list = new ArrayList<>();

        int maxPerGame = gameMap.size() >= 10 ? 3 : 5;
        for (Map.Entry<String, List<Member>> game : gameMap.entrySet()) {
            // TODO: Can we fit logic
            int remainingPageSpace = pageSize - (list.size() % pageSize);
            int requiredPageSpace = Math.min(game.getValue().size(), maxPerGame) + 2;

            if (remainingPageSpace < requiredPageSpace) {
                // If we don't have enough space then fill the page
                for (int i = remainingPageSpace; i > 0; i--) {
                    list.add(" ");
                }
            }
            list.add("**" + game.getKey() + "**");
            for (int i = 0; i < maxPerGame; i++) {
                if (i == game.getValue().size())
                    break;
                if (i == maxPerGame - 1 && game.getValue().size() - i > 1) {
                    list.add("and " + (game.getValue().size() - i) + " more.");
                    break;
                }
                list.add(getOnlineStatusEmoji(game.getValue().get(i)) + game.getValue().get(i).getEffectiveName());
            }
        }
        return list;
    }

    public static String cleanSayCommand(CommandEvent commandEvent) {
        StringBuilder sb = new StringBuilder();
        String[] words = commandEvent.getArgs().split(" ");

        Pattern inviteRegx = Pattern.compile("discord(?:app\\.com\\/invite|\\.gg)\\/([a-z0-9]{1,16})", Pattern.CASE_INSENSITIVE);
        Pattern userMentionRegx = Pattern.compile("\\<\\@([0-9]+)\\>");
        Pattern roleMentionRegx = Pattern.compile("\\<\\@\\&([0-9]+)\\>");

        for (String word: words) {

            // Escape everyone
            if (word.equalsIgnoreCase("@everyone")) {
                sb.append("(at)everyone ");
                continue;
            }

            // Escape here
            if (word.equalsIgnoreCase("@here")) {
                sb.append("(at)here ");
                continue;
            }

            // Escape all user mentions
            Matcher mentionMatcher = userMentionRegx.matcher(word);
            if (mentionMatcher.find()) {
                String userId = mentionMatcher.group(1);
                // If any users match the id
                List<User> mentionedUser = commandEvent.getMessage().getMentionedUsers().stream()
                        .filter(user -> user.getId().equals(userId))
                        .collect(Collectors.toList());


                // Check if user was mentioned
                if (!mentionedUser.isEmpty()) {
                    sb.append(mentionedUser.get(0).getName()).append(" ");
                    continue;
                }
            }

            // Escape all role mentions
            Matcher roleMatcher = roleMentionRegx.matcher(word);
            if (roleMatcher.find()) {
                String roleId = roleMatcher.group(1);

                List<Role> mentionedRole = commandEvent.getMessage().getMentionedRoles().stream()
                        .filter(role -> role.getId().equals(roleId))
                        .collect(Collectors.toList());

                if (!mentionedRole.isEmpty()) {
                   sb.append(mentionedRole.get(0).getName()).append(" ");
                   continue;
                }
            }

            // Get rid of invites
            Matcher matcher = inviteRegx.matcher(word);
            if (matcher.find()) {
                word = "`invite`";
            }

            sb.append(word).append(" ");
        }

        return sb.toString();
    }

    public static String clapify(String message) {
        StringBuilder sb = new StringBuilder();
        for (String s : message.split(" ")) {
            sb.append(" ").append(s).append(" :clap:");
        }
        return sb.toString();
    }
}
