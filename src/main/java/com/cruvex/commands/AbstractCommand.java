package com.cruvex.commands;

import com.cruvex.EliteDiscordBot;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * An abstract class representing a full command.
 */
public abstract class AbstractCommand {

    private static final Logger logger = EliteDiscordBot.getLogger();

    public abstract void execute(SlashCommandInteraction slashCommandInteraction);

    public final @Getter String description = null;
    public final @Getter String usage = null;
    public final @Getter CommandGroup commandGroup = CommandGroup.GENERAL;
    public final @Getter boolean isHidden = false;
    public final @Getter boolean isNSFW = false;
    public final @Getter Permission[] permissions = Permission.EMPTY_PERMISSIONS;
    public final @Getter OptionData optionData = null;
    public final @Getter HashMap<String, Method> buttonInteractionsMap = null;

    protected void log(Object message) {
        logger.info("[SLASH-COMMANDS][" + this.getClass().getSimpleName() + "] " + message);
    }
}
