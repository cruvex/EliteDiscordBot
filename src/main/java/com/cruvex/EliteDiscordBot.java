package com.cruvex;

import com.cruvex.commands.AbstractCommand;
import com.cruvex.commands.SlashCommandsManager;
import com.cruvex.commands.call.CallLeaderboardCommand;
import com.cruvex.commands.call.CallLogParserCommand;
import com.cruvex.commands.call.CallsCommand;
import com.cruvex.commands.general.PingCommand;
import com.cruvex.commands.message.MessageParserCommand;
import com.cruvex.config.Config;
import com.cruvex.eventlisteners.GuildEvents;
import com.cruvex.eventlisteners.GuildVoiceJoinLeaveEvents;
import com.cruvex.util.Util;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EliteDiscordBot {

    public static final @Getter Logger logger;


    public static @Getter HikariDataSource dataSource;

    public static @Getter boolean intelliJ = false;

    public static @Getter Config config;

    public static @Getter JDA jda = null;

    public static final @Getter Map<String, AbstractCommand> commandMap = new HashMap<>();

    private static final @Getter ArrayList<ListenerAdapter> eventListeners = new ArrayList<>();

    public static final Color primaryColor = Color.decode("#e2be43");

    static {
        logger = LoggerFactory.getLogger(EliteDiscordBot.class);
    }

    public EliteDiscordBot() throws LoginException {
        jda = JDABuilder.createDefault(config.getBotToken())
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("all Elites"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .setEventPassthrough(true)
                .build();

        //commands need to be added to commandMap before GuildEvents Listener is added
        registerCommands();

        registerEventListeners();
    }

    public static void main(String[] args) {
        intelliJ = isRunningInIntelliJ(args);
        config = new Config(intelliJ);

        if (config.dataBaseConnectionInfoSet()) {
            initalizePool();
            logger.warn("[DB-CONNECTION] Pool successfully initialized");
        } else {
            logger.warn("[DB-CONNECTION] Not all DataBase connection info is set");
        }

        if (config.getBotToken() == null) {
            logger.info("[CONFIG] Failed to retrieve Bot Token - Exiting");
            System.exit(0);
        } else {
            try {
                EliteDiscordBot bot = new EliteDiscordBot();
            } catch (LoginException e) {
                logger.info("Invaled bot token provided");
                e.printStackTrace();
            }
        }
    }

    private void registerCommands() {
        getCommandMap().put("ping", new PingCommand());
        // Command only needed to be used once to parse #no-mic and insert all events into database
        //getCommandMap().put("parse-call-logs", new CallLogParserCommand());
        getCommandMap().put("calls", new CallsCommand());
        getCommandMap().put("call-lb", new CallLeaderboardCommand());
        // Command for setting base for amount of messages sent per user - work in progress
        //getCommandMap().put("parse-messages", new MessageParserCommand());
    }

    private void registerEventListeners() {
        getJda().addEventListener(new SlashCommandsManager());
        getJda().addEventListener(new GuildEvents());
        getJda().addEventListener(new GuildVoiceJoinLeaveEvents());
    }

    private static boolean isRunningInIntelliJ(String[] args) {
        return new ArrayList<>(List.of(args)).contains("-IntelliJ");
    }

    /*
        Make connection to Database
     */

    private static boolean initalizePool() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getDataBaseUrl());
            hikariConfig.setUsername(config.getDataBaseUser());
            hikariConfig.setPassword(config.getDataBasePassword());

            //TODO Check how to set logger on hikariPool if possible?
            hikariConfig.addDataSourceProperty("dataSource.logger", "Slf4JLogger");
            hikariConfig.addDataSourceProperty("dataSource.logStatements", "true");

            dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            logger.warn("[DB-CONNECTION] Error while trying to initialize pool: " + e.getMessage());
            return false;
        }

        return true;
    }
    public static Connection getDBConnection() {
        Connection connection = null;
        try {
            if (dataSource != null) {
                long start = System.currentTimeMillis();
                connection = dataSource.getConnection();
                long duration = System.currentTimeMillis() - start;
                logger.info("[DB-CONNECTION] Successfully got connection from pool - " + Util.formatTime(duration));
            } else {
                logger.error("[DB-CONNECTION] Error getting connection from pool - dataSource is null");
            }
        } catch (Exception e) {
            logger.error("[DB-CONNECTION] Error getting connection from pool: " + e.getMessage());
        }
        return connection;
    }

    public static void info(Object logMessage) {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.info("[" + className.substring(className.lastIndexOf('.') + 1) + "] " + logMessage);
    }

    public static void warn(Object logMessage) {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.warn("[" + className.substring(className.lastIndexOf('.') + 1) + "] " + logMessage);
    }

    public static void error(Object logMessage) {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.error("[" + className.substring(className.lastIndexOf('.') + 1) + "] " + logMessage);
    }

    public static void logSQL(Object logMessage) {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.info("[" + className.substring(className.lastIndexOf('.') + 1) + "][SQL] " + logMessage);
    }
}