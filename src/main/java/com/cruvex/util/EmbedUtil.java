package com.cruvex.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class EmbedUtil {
    public static MessageEmbed getErrorEmbed(String message) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("An error occurred while executing this command!");
        embed.setDescription(message);
        embed.setColor(0xe54949);
        embed.setFooter("If you believe this is a bug please notify cruvex :)");

        return embed.build();
    }
}
