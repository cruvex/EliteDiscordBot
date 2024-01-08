package com.cruvex.eventlisteners;

import com.cruvex.EliteDiscordBot;
import com.cruvex.dao.VoiceEventsDAO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;

import java.time.Instant;

public class GuildVoiceJoinLeaveEvents extends ListenerAdapter {

    final Logger logger = EliteDiscordBot.getLogger();
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        /**
         * Joined Voice channel
         */
        if(event.getChannelJoined() != null && event.getChannelLeft() == null) {
           voiceJoinEvent(event);
        }
        /**
         * Left Voice channel
         */
        else if (event.getChannelLeft() != null && event.getChannelJoined() == null){
            voiceLeaveEvent(event);
        }
        /**
         * Switched Voice channel
         */
        else {
            voiceSwitchEvent(event);
        }
    }

    private void voiceJoinEvent(GuildVoiceUpdateEvent event) {
        log("[" + event.getGuild().getName() + "] User " + event.getMember().getEffectiveName() + " joined channel " + event.getChannelJoined().getName());

        if (event.getChannelJoined().getId().equals(event.getGuild().getAfkChannel().getId())) {
            event.getGuild().kickVoiceMember(event.getMember()).queue(
                    success -> log("[" + event.getGuild().getName() + "] User " + event.getMember().getEffectiveName() + " kicked from channel " + event.getChannelJoined().getName()),
                    error -> log("[" + event.getGuild().getName() + "] Failed to kick user " + event.getMember().getEffectiveName() + " from " + event.getChannelJoined().getName())
            );

            return;
        }

        String userId = event.getMember().getId();
        String guildId = event.getGuild().getId();
        String channelId = event.getChannelJoined().getId();

        long joinTime = System.currentTimeMillis();

        VoiceEventsDAO.insertVoiceJoinEvent(userId, guildId, channelId, joinTime);

        // send embed when user joined voiceChannel
        // To be properly implemented
        if (false) {
            String message;
            EmbedBuilder embedBuilder = new EmbedBuilder();

            message = event.getMember().getAsMention() + " joined " + event.getChannelJoined().getAsMention();
            embedBuilder.setTitle("User joined Voice channel");
            embedBuilder.setColor(0x5cc944);

            embedBuilder.setDescription(message);
            embedBuilder.setTimestamp(Instant.now());

            event.getGuild().getTextChannelById("1127586150710792212").sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

    private void voiceLeaveEvent(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined().getId().equals(event.getGuild().getAfkChannel().getId()))
            return;

        log("[" + event.getGuild().getName() + "] User " + event.getMember().getEffectiveName() + " left channel " + event.getChannelLeft().getName());

        String userId = event.getEntity().getId();
        String guildId = event.getGuild().getId();
        String channelId = event.getChannelLeft().getId();

        long leaveTime = System.currentTimeMillis();

        VoiceEventsDAO.insertVoiceLeaveEvent(userId, guildId, channelId, leaveTime);

        // send embed when user leaves voiceChannel
        // To be properly implemented
        if (false) {
            String message;
            EmbedBuilder embedBuilder = new EmbedBuilder();

            message = event.getMember().getAsMention() + " joined " + event.getChannelLeft().getAsMention();
            embedBuilder.setTitle("User left Voice channel");
            embedBuilder.setColor(0xe54949);
            embedBuilder.setDescription(message);
            embedBuilder.setTimestamp(Instant.now());

            event.getGuild().getTextChannelById("1127586150710792212").sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

    private void voiceSwitchEvent(GuildVoiceUpdateEvent event) {
        voiceJoinEvent(event);
        voiceLeaveEvent(event);

        // send embed when user switches voiceChannel
        // To be properly implemented
        if (false) {
            String message;
            EmbedBuilder embedBuilder = new EmbedBuilder();

            message = "User " + event.getMember().getAsMention() + " left " + event.getChannelLeft().getAsMention() + " and joined " + event.getChannelJoined().getAsMention();
            embedBuilder.setTitle("User switched Voice channel");
            embedBuilder.setColor(0x5cc944);
            embedBuilder.setDescription(message);
            embedBuilder.setTimestamp(Instant.now());

            event.getGuild().getTextChannelById("1127586150710792212").sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

    private void log(String logMessage) {
        logger.info("[VOICE-CHANNELS] " + logMessage);
    }

    private void logSQL(String logMessage) {
        logger.info("[VOICE-CHANNELS][SQL] " + logMessage);
    }
}
