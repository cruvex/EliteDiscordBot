package com.cruvex.commands.call;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.EmbedUtil;
import com.cruvex.util.Util;
import com.cruvex.commands.AbstractCommand;
import com.cruvex.dao.VoiceEventsDAO;
import lombok.Getter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cruvex.EliteDiscordBot.*;

public class CallLogParserCommand extends AbstractCommand {

    public final @Getter String description = "Show total time spent in call per user";

    @Override
    public void execute(SlashCommandInteraction slashCommandInteraction) {
        if (!slashCommandInteraction.getMember().getId().equals("255361968037167105")) {
            slashCommandInteraction.getHook().sendMessage("Only for cruvex use :)").setEphemeral(true).queue();
            return;
        }

        String guildId = slashCommandInteraction.getGuild().getId();
        List<Message> messages = new ArrayList<>();

        TextChannel eventChannel = slashCommandInteraction.getChannel().asTextChannel();

        TextChannel toParse;
        if (!config.isDevelopment())
            toParse = eventChannel.getGuild().getTextChannelById("1113616143383531570");
        else
            toParse = eventChannel.getGuild().getTextChannelById("1127586150710792212");

        info(toParse.getName());

        OffsetDateTime afterTimestamp = OffsetDateTime.parse("2023-06-05T00:00:00Z");

        CompletableFuture<?> operationCompletion = toParse.getIterableHistory().forEachAsync(message -> {
            if (/* message.getTimeCreated().isBefore(beforeTimestamp) && */ message.getTimeCreated().isAfter(afterTimestamp) &&
                    !message.getEmbeds().isEmpty()) {
                messages.add(message);
            }
            return true;
        });

        operationCompletion.join();

        messages.sort(Comparator.comparing(ISnowflake::getTimeCreated));
        HashMap<String, Long> timeSpentInCall = new HashMap<>();
        HashMap<String, Long> joinTimes = new HashMap<>();


        for (Message message : messages) {
            try {
                if (!message.getEmbeds().isEmpty()) {
                    for (MessageEmbed embed : message.getEmbeds()) {
                        if (embed.getTitle() != null) {
                            String embedTitle = embed.getTitle().trim();
                            if (!embedTitle.startsWith("Member")) {
                                continue;
                            }
                            info("----------------------------------------------------------------");
                            info(message.getTimeCreated().toLocalDateTime());
                            info(embed.getTimestamp().toLocalDateTime());
                            String embedDescription = "";
                            if (embed.getDescription() != null) {
                                embedDescription = embed.getDescription().replace("\n", "").trim();
                            }

                            info("Title         : " + embedTitle);
                            info("Description   : " + embedDescription);

                            if (embed.getTitle().startsWith("Member joined") || embed.getTitle().startsWith("Member left")) {
                                String mentionedUser = embed.getFooter().getText().substring(4).trim();
//                            String mentionedUser = embedDescription.replace(toRemove, "").trim();
//                            mentionedUser = mentionedUser.replace("<", "").replace(">", "").replace("@", "");
//
                                info("mentionedUser : " + mentionedUser);
//
                                if (embedTitle.startsWith("Member joined")) {
                                    if (!joinTimes.containsKey(mentionedUser) || joinTimes.get(mentionedUser) == null) {
                                        joinTimes.put(mentionedUser, embed.getTimestamp().toInstant().toEpochMilli());
                                        VoiceEventsDAO.insertVoiceJoinLeaveEvent(mentionedUser, guildId, "", VoiceEventsDAO.JOIN_EVENT, embed.getTimestamp().toInstant().toEpochMilli());
                                        joinTimes.put(mentionedUser, message.getTimeCreated().toInstant().toEpochMilli());
                                        info("joinTime:       " + message.getTimeCreated().toInstant().toEpochMilli());
                                    } else {
                                        info("User joined call but was already in call");
                                    }
                                } else if (embedTitle.startsWith("Member left")) {
                                    if (joinTimes.containsKey(mentionedUser) && joinTimes.get(mentionedUser) != null) {
                                        VoiceEventsDAO.insertVoiceJoinLeaveEvent(mentionedUser, guildId, "", VoiceEventsDAO.LEAVE_EVENT, embed.getTimestamp().toInstant().toEpochMilli());
                                        Long joinTime = joinTimes.get(mentionedUser);
                                        Long userTimeSpentInCall = timeSpentInCall.computeIfAbsent(mentionedUser, k -> 0L);
                                        userTimeSpentInCall = userTimeSpentInCall + embed.getTimestamp().toInstant().toEpochMilli() - joinTime;
                                        info("leaveTime - joinTime : " + embed.getTimestamp().toInstant().toEpochMilli() + " - " + joinTime + " = " + (embed.getTimestamp().toInstant().toEpochMilli() - joinTime));
                                        userTimeSpentInCall = userTimeSpentInCall + message.getTimeCreated().toInstant().toEpochMilli() - joinTime;
                                        info("leaveTime - joinTime : " + message.getTimeCreated().toInstant().toEpochMilli() + " - " + joinTime + " = " + (message.getTimeCreated().toInstant().toEpochMilli() - joinTime));
                                        timeSpentInCall.put(mentionedUser, userTimeSpentInCall);
                                        joinTimes.put(mentionedUser, null);
                                    } else {
                                        info("User left call but was not in call");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
