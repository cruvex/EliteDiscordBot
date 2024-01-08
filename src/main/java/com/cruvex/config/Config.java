package com.cruvex.config;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.Util;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import java.util.Objects;

import static com.cruvex.EliteDiscordBot.log;


@Setter
@Getter
public class Config {
    private boolean development;
    private String botToken;

    // Database config
    private String dataBaseUrl;
    private String dataBaseUser;
    private String dataBasePassword;

    public Config(Boolean intelliJ) {
        boolean _development;
        String _botToken;
        String _dataBaseUrl;
        String _dataBaseUser;
        String _dataBasePassword;

        try {
            if (intelliJ) {
                log("[CONFIG] Bot started in IntelliJ");
                log("[CONFIG] Getting Bot Token from .env file");

                Dotenv config = Dotenv.configure().load();

                _development = config.get("DEV").equals("true");
                _botToken = config.get("DEV_BOT_TOKEN");
                _dataBaseUrl = "jdbc:" + config.get("DB_URL");
                _dataBaseUser = config.get("DB_USER");
                _dataBasePassword = config.get("DB_PASSWORD");
            } else {
                log("[CONFIG] Bot started outside of IntelliJ");
                log("[CONFIG] Getting Bot Token from environment variable 'BOT_TOKEN'");

                _development = System.getenv("DEV").equals("true");
                _botToken = System.getenv("BOT_TOKEN");
                _dataBaseUrl = "jdbc:postgresql://" + System.getenv("PGHOST") + ":" + System.getenv("PGPORT") + "/" + System.getenv("PGDATABASE");
                _dataBaseUser = System.getenv("PGUSER");
                _dataBasePassword = System.getenv("PGPASSWORD");
            }

            setDevelopment(_development);
            setBotToken(_botToken);
            setDataBaseUrl(_dataBaseUrl);
            setDataBaseUser(_dataBaseUser);
            setDataBasePassword(_dataBasePassword);
        } catch (Exception e) {
            log("[CONFIG][Exception] Failed to load environment variables: " + e.getMessage());
        }
    }

    public boolean dataBaseConnectionInfoSet() {
        return !Util.isEmptyOrNull(getDataBaseUrl()) && !Util.isEmptyOrNull(getDataBaseUser()) && !Util.isEmptyOrNull(getDataBasePassword());
    }
}
