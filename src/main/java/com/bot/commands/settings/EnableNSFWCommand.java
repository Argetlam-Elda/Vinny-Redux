package com.bot.commands.settings;

import com.bot.commands.ModerationCommand;
import com.bot.db.ChannelDAO;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;

public class EnableNSFWCommand extends ModerationCommand {

    private ChannelDAO channelDAO;

    public EnableNSFWCommand() {
        this.name = "enablensfw";
        this.arguments = "";
        this.help = "Enables NSFW commands in the text channel it is posted in.";
        this.botPermissions = new Permission[]{Permission.MESSAGE_HISTORY};

        this.channelDAO = ChannelDAO.getInstance();
    }

    @Override
    protected void executeCommand(CommandEvent commandEvent) {
        if (channelDAO.setTextChannelNSFW(commandEvent.getTextChannel(), true)) {
            commandEvent.getMessage().addReaction(commandEvent.getClient().getSuccess()).queue();
        } else {
            commandEvent.reply(commandEvent.getClient().getError() + " Something went wrong, please try again later or contact an admin on the support server.");
            metricsManager.markCommandFailed(this, commandEvent.getAuthor(), commandEvent.getGuild());
        }
    }
}
