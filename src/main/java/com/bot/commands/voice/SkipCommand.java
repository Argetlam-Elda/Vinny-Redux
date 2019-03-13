package com.bot.commands.voice;

import com.bot.commands.VoiceCommand;
import com.bot.utils.CommandPermissions;
import com.bot.voice.VoiceSendHandler;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.util.logging.Logger;

public class SkipCommand extends VoiceCommand {
	private static final Logger LOGGER = Logger.getLogger(SkipCommand.class.getName());

	public SkipCommand() {
		this.name = "skip";
		this.arguments = "";
		this.help = "skips to the next track";
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		metricsManager.markCommand(this, commandEvent.getAuthor(), commandEvent.getGuild());
		// Check the permissions to do the command
		if (!CommandPermissions.canExecuteCommand(this, commandEvent))
			return;

		VoiceSendHandler handler = (VoiceSendHandler) commandEvent.getGuild().getAudioManager().getSendingHandler();
		if (handler == null) {
			commandEvent.reply(commandEvent.getClient().getWarning() + " I am not connected to a voice channel.");
		}
		else {
			if (handler.skipTrack()) {
				commandEvent.getMessage().addReaction(commandEvent.getClient().getSuccess()).queue();
			}
			else {
				commandEvent.reply(commandEvent.getClient().getWarning() + " Unable to skip to the next track");
			}
		}
	}
}
