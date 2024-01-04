package com.cruvex.eventlisteners;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.Util;
import com.cruvex.commands.AbstractCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuildEvents extends ListenerAdapter {

    final Logger logger = EliteDiscordBot.getLogger();

    ArrayList<String> whitelistedGuilds;


    /**
     * EVENTS
     */

    @Override
    public void onReady(ReadyEvent event) {
        log("Ready - Bot is currently in " + event.getJDA().getGuilds().size() + " guild(s)");
        if (!event.getJDA().getGuilds().isEmpty()) {
            log("Checking whitelisted guilds");
            for (Guild guild : event.getJDA().getGuilds()) {
                handleIsWhitelistedGuild(guild);
            }
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild eventGuild = event.getGuild();
        registerCommandsOnGuild(eventGuild);
    }


    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        log("########## [GuildJoinEvent] ##########");
        Guild eventGuild = event.getGuild();

        handleIsWhitelistedGuild(eventGuild);
        registerCommandsOnGuild(eventGuild);
    }

    /**
     * END EVENTS
     */

    private void registerCommandsOnGuild(Guild guild) {
        List<CommandData> commandData = new ArrayList<>();
        for (String commandName : EliteDiscordBot.getCommandMap().keySet()) {
            log("[SLASH-COMMANDS][REGISTER][" + guild.getName() + "] Registering command '" + commandName + "'");
            AbstractCommand command = EliteDiscordBot.commandMap.get(commandName);

            if (Util.isEmptyOrNull(command.getDescription())) {
                logError("Description for command " + commandName + " is empty");
                continue;
            }

            SlashCommandData commandToAdd = Commands.slash(commandName, command.getDescription());

            // Add optionData if set
            if (!Util.isEmptyOrNull(command.getOptionData())) {
                commandToAdd.addOptions(command.getOptionData());
            }

            commandToAdd.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VIEW_AUDIT_LOGS));

            commandData.add(commandToAdd);
        }
        log("[SLASH-COMMANDS][REGISTER][" + guild.getName() + "] Registered " + commandData.size() + " command(s)");
        guild.updateCommands().addCommands(commandData).queue();
    }

    private void handleIsWhitelistedGuild(Guild guild) {
        if (whitelistedGuilds == null) {
            try (Connection conn = EliteDiscordBot.getDBConnection()) {
                whitelistedGuilds = new ArrayList<>();

                PreparedStatement stmt;
                ResultSet rs;

                String sql = "SELECT guild_id FROM whitelisted_guilds";
                logSQL(sql);
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    String whitelistedGuildId = rs.getString(1);
                    whitelistedGuilds.add(whitelistedGuildId);
                }
            } catch (SQLException e) {
                logger.error("Error getting whitelisted guilds from database: " + e.getMessage());
            }
            log("Whitelisted guilds: " + String.join(", ", whitelistedGuilds));
        }
        if (!whitelistedGuilds.contains(guild.getId())) {
            EmbedBuilder embedBuilder = new EmbedBuilder();

            embedBuilder.setColor(0xFFCF76);
            embedBuilder.setDescription("""
                    Hi there!
                    This is a privately developed Bot and is only meant for use in Elite Clan related servers.
                    Contact **cruvex** on Discord for more information.
                    I'll be leaving this server now!
                    """);

            guild.getSystemChannel().sendMessageEmbeds(embedBuilder.build()).queue();

            log("Leaving guild " + guild.getName() + " (" + guild.getId() + ") as it is not whitelisted");
            guild.leave().queue();
        }
    }

    private void log(String logMessage) {
        logger.info("[" + this.getClass().getSimpleName() + "] " + logMessage);
    }

    private void logError(String logMessage) {
        logger.error("[" + this.getClass().getSimpleName() + "] " + logMessage);
    }

    private void logSQL(String logMessage) {
        logger.info("[" + this.getClass().getSimpleName() + "][SQL] " + logMessage);
    }
}