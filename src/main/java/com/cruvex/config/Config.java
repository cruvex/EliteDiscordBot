package com.cruvex.config;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.Util;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;


public class Config {

    final Logger logger = EliteDiscordBot.getLogger();

    private @Getter @Setter String botToken;

    // Database config
    private @Getter @Setter String dataBaseUrl;
    private @Getter @Setter String dataBaseUser;
    private @Getter @Setter String dataBasePassword;

    public Config(Boolean intelliJ) {
        String _botToken;
        String _dataBaseUrl;
        String _dataBaseUser;
        String _dataBasePassword;

        try {
            if (intelliJ) {
                logger.info("[CONFIG] Bot started in IntelliJ");
                logger.info("[CONFIG] Getting Bot Token from .env file");

                Dotenv config = Dotenv.configure().load();

                _botToken = config.get("DEV_BOT_TOKEN");
                _dataBaseUrl = "jdbc:" + config.get("DB_URL");
                _dataBaseUser = config.get("DB_USER");
                _dataBasePassword = config.get("DB_PASSWORD");
            } else {
                logger.info("[CONFIG] Bot started outside of IntelliJ");
                logger.info("[CONFIG] Getting Bot Token from environment variable 'BOT_TOKEN'");
                _botToken = System.getenv("BOT_TOKEN");
                _dataBaseUrl = "jdbc:postgresql://" + System.getenv("PGHOST") + ":" + System.getenv("PGPORT") + "/" + System.getenv("PGDATABASE");
                _dataBaseUser = System.getenv("PGUSER");
                _dataBasePassword = System.getenv("PGPASSWORD");
            }

            setBotToken(_botToken);
            setDataBaseUrl(_dataBaseUrl);
            setDataBaseUser(_dataBaseUser);
            setDataBasePassword(_dataBasePassword);
        } catch (Exception e) {
            logger.info("[CONFIG][Exception] Failed to load environment variables: " + e.getMessage());
        }
    }

    public boolean dataBaseConnectionInfoSet() {
        return !Util.isEmptyOrNull(getDataBaseUrl()) && !Util.isEmptyOrNull(getDataBaseUser()) && !Util.isEmptyOrNull(getDataBasePassword());
    }
}
