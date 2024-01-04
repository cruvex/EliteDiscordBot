package com.cruvex;

import com.cruvex.commands.AbstractCommand;
import com.cruvex.commands.call.CallLogParserCommand;
import com.cruvex.commands.call.CallsCommand;
import com.cruvex.commands.general.PingCommand;
import com.cruvex.config.Config;
import com.cruvex.eventlisteners.GuildEvents;
import com.cruvex.eventlisteners.GuildVoiceJoinLeaveEvents;
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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EliteDiscordBot {

    public static final @Getter Logger logger;


    public static @Getter HikariDataSource dataSource;
    public static Connection conn;


    public static boolean intelliJ = false;

    public static @Getter Config config;

    public static @Getter JDA jda = null;

    public static final @Getter Map<String, AbstractCommand> commandMap = new HashMap<>();

    private static final @Getter ArrayList<ListenerAdapter> eventListeners = new ArrayList<>();

    static {
        logger = LoggerFactory.getLogger(EliteDiscordBot.class);

        getEventListeners().add(new GuildEvents());
        getEventListeners().add(new GuildVoiceJoinLeaveEvents());
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
        //getCommandMap().put("parse-logs", new CallLogParserCommand());
        getCommandMap().put("calls", new CallsCommand());
    }

    private void registerEventListeners() {
        getEventListeners().forEach(eventListener -> {
            logger.info("[EVENT-LISTENERS][REGISTER] " + eventListener.getClass().getSimpleName());
            getJda().addEventListener(eventListener);
        });
    }

    private static boolean isRunningInIntelliJ(String[] args) {
        for (String argument : args) {
            if (argument.equals("-IntelliJ"))
                return true;
        }
        return false;
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

            // Check how to set logger on hikariPool if possible?
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
                connection = dataSource.getConnection();
                logger.info("[DB-CONNECTION] Successfully got connection from pool");
            } else {
                logger.error("[DB-CONNECTION] Error getting connection from pool - dataSource is null");
            }
        } catch (Exception e) {
            logger.error("[DB-CONNECTION] Error getting connection from pool: " + e.getMessage());
        }
        return connection;
    }
}