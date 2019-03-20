package com.bot.utils;

import com.bot.db.ChannelDAO;
import com.bot.db.GuildDAO;
import com.bot.db.MembershipDAO;
import com.bot.models.InternalGuild;
import com.bot.models.InternalGuildMembership;
import com.bot.models.InternalTextChannel;
import com.bot.models.InternalVoiceChannel;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Role;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandPermissions {
    private static Logger LOGGER = Logger.getLogger("Command Permissions");

    private static ChannelDAO channelDAO = ChannelDAO.getInstance();
    private static MembershipDAO membershipDAO = MembershipDAO.getInstance();
    private static GuildDAO guildDAO = GuildDAO.getInstance();


    public static boolean canExecuteCommand(Command command, CommandEvent commandEvent) {
        return canExecuteCommand(command.getCategory(), commandEvent);
    }

    public static boolean canExecuteCommand(Command.Category commandCategory, CommandEvent commandEvent) {
        // If its a PM then screw permissions
        if (commandEvent.isFromType(ChannelType.PRIVATE))
            return true;

        InternalGuild guild;

        try {
            guild = guildDAO.getGuildById(commandEvent.getGuild().getId());
            if (guild == null) { // Membership is missing
                throw new SQLException("Guild is missing");
            }
        }  catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get guild from db: " + commandEvent.getGuild().getId() + " attempting to add", e.getMessage());
            // Alert the user that there is an issue and that we will need to start an emergency sync
            commandEvent.reply(commandEvent.getClient().getError() + " There is a problem with this guild in the db. " +
                    "I will attempt to fix it, please try again later. If this issue persists please contact the devs on the support server.");
            guildDAO.addFreshGuild(commandEvent.getGuild());
            return false;
        }

        // Check the roles returned
        Role requiredRole = commandEvent.getGuild().getRoleById(guild.getRequiredPermission(commandCategory));

        // Get users role, if they have none then use default
        List<Role> roleList = commandEvent.getMember().getRoles();
        Role highestRole;
        if (roleList.isEmpty())
            highestRole = commandEvent.getGuild().getPublicRole();
        else
            highestRole = commandEvent.getMember().getRoles().get(0);

        if (highestRole.getPosition() < requiredRole.getPosition()) {
            // Reply to the command event stating that they do not hold the position required.
            commandEvent.reply(commandEvent.getClient().getWarning() +
                    " You do not have the required role to use this command. You must have at least the " +
                    requiredRole.getName() + " role or higher to use " + commandCategory.getName() + " commands.");
            return false;
        }

        // Checking Membership permissions
        InternalGuildMembership membership;
        try {
            membership = membershipDAO.getUserMembershipByIdInGuild(commandEvent.getAuthor().getId(), commandEvent.getGuild().getId());
            if (membership == null) { // Membership is missing
                throw new SQLException("Membership is missing");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to get membership from db when checking command perms. " + e.getMessage());
            commandEvent.reply(commandEvent.getClient().getError() + " There is a problem with your association to the guild in the db. " +
                    "I will attempt to fix it, please try again later. If this issue persists please contact the devs on the support server.");
            membershipDAO.addUserToGuild(commandEvent.getAuthor(), commandEvent.getGuild());
            return false;
        }

        if (!membership.canUseBot()) {
            commandEvent.reply(commandEvent.getClient().getWarning() + " Your ability to use commands has been disabled. " +
                    "To unlock commands please talk to a guild admin.");
            return false;
        }

        // TODO: Check channel permissions
        if (commandCategory == CommandCategories.VOICE) {
            // If their in a voice channel the doesn't allow voice, then dont let them use it
            if (commandEvent.getMember().getVoiceState().inVoiceChannel()) {
                InternalVoiceChannel voiceChannel;
                // Get voice channel, if not present add it
                try {
                    voiceChannel = channelDAO.getVoiceChannelForId(
                            commandEvent.getMember().getVoiceState().getChannel().getId());
                    if (voiceChannel == null) {
                        throw new SQLException("Voice channel is missing");
                    }
                } catch (SQLException e) {
                    LOGGER.severe("Failed to get voice channel: " + e.getMessage());
                    commandEvent.reply(commandEvent.getClient().getError() + " There is a problem with the voice channel in the db. " +
                            "I will attempt to fix it, please try again later. If this issue persists please contact the devs on the support server.");
                    channelDAO.addVoiceChannel(commandEvent.getMember().getVoiceState().getChannel());
                    return false;
                }

                if (!voiceChannel.isVoiceEnabled()) {
                    commandEvent.reply(commandEvent.getClient().getError() + " This voice channel has commands disabled. Please contact" +
                            " a mod in your server to enable them.");
                    return false;
                }
            } else {
                commandEvent.reply(commandEvent.getClient().getError() + " You must be in a voice channel to use a voice command");
                return false;
            }
        }

        InternalTextChannel textChannel;
        // Try to get the text channel and check, if its none, add it
        try {
            textChannel = channelDAO.getTextChannelForId(commandEvent.getTextChannel().getId());
            if (textChannel == null) {
                throw new SQLException("Text channel is missing");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to get text channel: " + e.getMessage());
            commandEvent.reply(commandEvent.getClient().getError() + " There is a problem with the text channel in the db. " +
                    "I will attempt to fix it, please try again later. If this issue persists please contact the devs on the support server.");
            channelDAO.addTextChannel(commandEvent.getTextChannel());
            return false;
        }

        if (commandCategory == CommandCategories.NSFW && !textChannel.isNSFWEnabled()) {
            commandEvent.reply(commandEvent.getClient().getWarning() + " NSFW commands are not enabled on this channel. " +
                    "To enable it, use the `~enableNSFW` command.");
            return false;
        } else if (commandCategory == CommandCategories.NSFW && !commandEvent.getTextChannel().isNSFW()) {
            commandEvent.reply(commandEvent.getClient().getWarning() + " This channel is not marked in discord as nsfw. " +
                    "I am now honoring both discords flag and my own. To enable it, please go into the channel settings in discord and enable nsfw.");
            return false;
        }

        return textChannel.isCommandsEnabled();
    }
}