package com.cruvex.commands;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.EmbedUtil;
import com.cruvex.util.Util;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SlashCommandsManager extends ListenerAdapter {

    final Logger logger = EliteDiscordBot.getLogger();

    private static final Map<String, AbstractCommand> COMMAND_MAP = EliteDiscordBot.getCommandMap();
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent slashCommandInteractionEvent) {
        SlashCommandInteraction slashCommandInteraction = slashCommandInteractionEvent.getInteraction();
        log("############ SlashCommandInteractionEvent ############");
        String commandName = slashCommandInteraction.getName();
        log("New SlashCommand event registered " + commandName + " | " + slashCommandInteraction.getTimeCreated());
        AbstractCommand command = null;

        if (COMMAND_MAP.containsKey(commandName)) {
            command = COMMAND_MAP.get(commandName);
            log("command with name '" + commandName + "' found: " + command.getClass());
        } else {
            log("No command found with name '" + commandName + "'");
        }

        if (command != null) {
            log("Executing command " + command.getClass());
            try {
                slashCommandInteraction.deferReply().queue();
                command.execute(slashCommandInteraction);
            } catch (Exception e) {
                log("An error occurred while executing this command: " + e.getMessage());
                e.printStackTrace();
                slashCommandInteraction.getHook().deleteOriginal().queue();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    e.printStackTrace();
                }
                slashCommandInteraction.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed(e.getMessage())).setEphemeral(true).queue();
            } finally {
                log("############ END ############");
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        ButtonInteraction buttonInteraction = event.getInteraction();
        log("############ ButtonInteractionEvent ############");
        log("New ButtonInteractionEvent registered " + buttonInteraction.getComponentId() + " | " + event.getTimeCreated());
        COMMAND_MAP.forEach((_ignored, command) -> {
            if (Util.isEmptyOrNull(command.getButtonInteractionsMap()))
                return;

            command.getButtonInteractionsMap().forEach((buttonId, method) -> {
                if (buttonId.equals(buttonInteraction.getComponentId())) {
                    log("Method found for button '" + buttonId + "': " + command.getClass() + " - " + method.getName());
                    try {
                        method.invoke(command, buttonInteraction);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        log("############ END ############");
    }

    private void log(Object message) {
        logger.info("[SLASH-COMMANDS][MANAGER] " + message);
    }
}
