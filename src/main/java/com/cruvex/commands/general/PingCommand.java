package com.cruvex.commands.general;

import com.cruvex.commands.AbstractCommand;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

import java.time.Instant;
import java.time.OffsetDateTime;

public class PingCommand extends AbstractCommand {

    // Attributes
    public final @Getter String description = "Measures and displays API latency and client latency.";
    @Override
    public void execute(SlashCommandInteraction slashCommandInteraction) {
        log("executing");

        // Begin latency measurement
        OffsetDateTime timeCreated = slashCommandInteraction.getTimeCreated();
        long start = timeCreated.toInstant().toEpochMilli();

        // End latency measurement
        long end = Instant.now().toEpochMilli();
        long apiLatency = (end - start) / 1000;

        EmbedBuilder pingEmbedBuilder = new EmbedBuilder();
        pingEmbedBuilder.setAuthor(slashCommandInteraction.getMember().getEffectiveName(),null, slashCommandInteraction.getMember().getAvatarUrl());
        pingEmbedBuilder.setTimestamp(Instant.now());


        pingEmbedBuilder.addField("API Latency", slashCommandInteraction.getJDA().getGatewayPing() + " ms", false);
        pingEmbedBuilder.addField("Client Latency", apiLatency + " ms", false);

        /*
         * Determine color for embed based on latency
         *
         * Less than 100    -> Green
         * Less than 200    -> Yellow
         * Greater than 200 -> Red
         */
        int color;
        if (apiLatency < 100) {
            color = 0x00ff00;
        } else if (apiLatency < 200) {
            color = 0xffff00;
        } else {
            color = 0xff0000;
        }
        pingEmbedBuilder.setColor(color);

        slashCommandInteraction.replyEmbeds(pingEmbedBuilder.build()).queue();

    }
    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isNSFW() {
        return false;
    }
}
