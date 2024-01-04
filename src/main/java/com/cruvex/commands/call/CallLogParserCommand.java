package com.cruvex.commands.call;

import com.cruvex.util.Util;
import com.cruvex.commands.AbstractCommand;
import com.cruvex.dao.VoiceEventsDAO;
import lombok.Getter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CallLogParserCommand extends AbstractCommand {

    public final @Getter String description = "Show total time spent in call per user";

    @Override
    public void execute(SlashCommandInteraction slashCommandInteraction) {
//        if (true) {
//            slashCommandInteraction.reply("Only for cruvex use :)").setEphemeral(true).queue();
//            return;
//        }

        String guildId  = slashCommandInteraction.getGuild().getId();
        List<Message> messages = new ArrayList<>();
        TextChannel eventChannel = slashCommandInteraction.getChannel().asTextChannel();
        AtomicInteger i = new AtomicInteger();
        TextChannel noMic = eventChannel.getGuild().getTextChannelById("1113616143383531570");
//        TextChannel noMic = eventChannel.getGuild().getTextChannelById("1127586150710792212");
        log(noMic.getName());
//        MessageHistory noMicHistory = noMic.getHistory();


//        MessageHistory.MessageRetrieveAction history = noMic.getHistoryAfter("1113702313291415564", 10000);
//
//
//
//        MessageHistory messageHistory = history.complete();


//        MessageHistory messageHistory = noMic.getHistory();

//        ArrayList<Message> retrievedMessages = new ArrayList<>(messageHistory.getRetrievedHistory());

//        ArrayList<Message> filteredMessages = new ArrayList<>();

//        List<Message> retrievedMessages = messageHistory.getRetrievedHistory();
        OffsetDateTime beforeTimestamp = OffsetDateTime.parse("2023-11-30T00:00:00Z");
        OffsetDateTime afterTimestamp = OffsetDateTime.parse("2023-06-05T00:00:00Z");
//        for (Message message : retrievedMessages) {
//            if (message.getTimeCreated().isBefore(beforeTimestamp) && message.getTimeCreated().isAfter(afterTimestamp)) {
//                // Add the message to the list if it's before the specified timestamp
//                filteredMessages.add(message);
//            }
//        }
//        log(filteredMessages.size());

//        MessageHistory.MessageRetrieveAction test = eventChannel.getHistoryAround("1115438269283455046", 1);
//        MessageHistory messageHistorytest = test.complete();
//        ArrayList<Message> testmessage = new ArrayList<>(messageHistorytest.getRetrievedHistory());
//        log(testmessage.get(0).getTimeCreated());

//        history.queue(messages -> {
//            // Process retrieved messages here
//            for (Message message : messages) {
//                // Do something with each message
//                log(message.getContentDisplay());
//            }
//        });

        noMic.getIterableHistory().forEachAsync(message -> {
                    i.getAndIncrement();
                    if (/* message.getTimeCreated().isBefore(beforeTimestamp) && */ message.getTimeCreated().isAfter(afterTimestamp)) {
                        messages.add(message);
                    }
                    return true;
                })
                .whenComplete((_ignored1, _ignored2) -> {

                    messages.sort(Comparator.comparing(ISnowflake::getTimeCreated));
//                    for (Message message : filteredMessages) {
//                        // Access individual messages
//                        log(message.getId() + " - " + message.getTimeCreated());
//                    }
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
                                        log("----------------------------------------------------------------");
                                        log(message.getTimeCreated().toLocalDateTime());
                                        log(embed.getTimestamp().toLocalDateTime());
                                        String embedDescription = "";
                                        if (embed.getDescription() != null) {
                                            embedDescription = embed.getDescription().replace("\n", "").trim();
                                        }

                                        log("Title         : " + embedTitle);
                                        log("Description   : " + embedDescription);

                                        if (embed.getTitle().startsWith("Member joined") || embed.getTitle().startsWith("Member left")) {
                                            String mentionedUser = embed.getFooter().getText().substring(4).trim();
//                            String mentionedUser = embedDescription.replace(toRemove, "").trim();
//                            mentionedUser = mentionedUser.replace("<", "").replace(">", "").replace("@", "");
//
                                            log("mentionedUser : " + mentionedUser);
//
                                            if (embedTitle.startsWith("Member joined")) {
                                                if (!joinTimes.containsKey(mentionedUser) || joinTimes.get(mentionedUser) == null) {
                                                    joinTimes.put(mentionedUser, embed.getTimestamp().toInstant().toEpochMilli());
                                                    VoiceEventsDAO.insertVoiceJoinLeaveEvent(mentionedUser, guildId, "", VoiceEventsDAO.JOIN_EVENT, embed.getTimestamp().toInstant().toEpochMilli());
                                        joinTimes.put(mentionedUser, message.getTimeCreated().toInstant().toEpochMilli());
                                                    log("joinTime:       " + message.getTimeCreated().toInstant().toEpochMilli());
                                                } else {
                                                    log("User joined call but was already in call");
                                                }
                                            } else if (embedTitle.startsWith("Member left")) {
                                                if (joinTimes.containsKey(mentionedUser) && joinTimes.get(mentionedUser) != null) {
                                                    VoiceEventsDAO.insertVoiceJoinLeaveEvent(mentionedUser, guildId, "", VoiceEventsDAO.LEAVE_EVENT, embed.getTimestamp().toInstant().toEpochMilli());
                                                    Long joinTime = joinTimes.get(mentionedUser);
                                                    Long userTimeSpentInCall = timeSpentInCall.computeIfAbsent(mentionedUser, k -> 0L);
                                                    userTimeSpentInCall = userTimeSpentInCall + embed.getTimestamp().toInstant().toEpochMilli() - joinTime;
                                                    log("leaveTime - joinTime : " + embed.getTimestamp().toInstant().toEpochMilli() + " - " + joinTime + " = " + (embed.getTimestamp().toInstant().toEpochMilli() - joinTime));
                                        userTimeSpentInCall = userTimeSpentInCall + message.getTimeCreated().toInstant().toEpochMilli() - joinTime;
                                        log("leaveTime - joinTime : " + message.getTimeCreated().toInstant().toEpochMilli() + " - " + joinTime + " = " + (message.getTimeCreated().toInstant().toEpochMilli() - joinTime));
                                                    timeSpentInCall.put(mentionedUser, userTimeSpentInCall);
                                                    joinTimes.put(mentionedUser, null);
                                                } else {
                                                    log("User left call but was not in call");
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

//        EmbedBuilder embedBuilder = new EmbedBuilder();
//        embedBuilder.setTitle("Voice Chat Times");
//        embedBuilder.setDescription("Total time spent in calls parsed from bot logs");
//        embedBuilder.setColor(0xFFCF76);
                    log("voor loop door resultaat");
//                    Map<Integer, String> sortedMap =
//                            timeSpentInCall.entrySet().stream()
//                                    .sorted(Map.Entry.comparingByValue())
//                                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
//                                            (e1, e2) -> e1, HashMap::new));


                    // Sorting the HashMap by values in descending order
                    List<Map.Entry<String, Long>> sortedList = new ArrayList<>(timeSpentInCall.entrySet());

                    sortedList.sort((entry1, entry2) -> -entry1.getValue().compareTo(entry2.getValue()));

                    // Displaying the sorted entries
//                    System.out.println("Sorted HashMap by values:");
                    for (Map.Entry<String, Long> entry : sortedList) {
//                        System.out.println(entry.getKey() + ": " + entry.getValue());
                        User userObject = slashCommandInteraction.getJDA().getUserById(entry.getKey());
                        log((userObject != null ? userObject.getEffectiveName() : entry.getKey()) + ": " + Util.formatTime(entry.getValue()));
//                        VoiceLevels.addUserTimeSpentInVC(entry.getKey(), event.getGuild().getId(), entry.getValue());
                    }

//                    for (String user : timeSpentInCall.keySet()) {
//
//                        User userObject = event.getJDA().getUserById(user);
//                        log("User " + (userObject != null ? userObject.getEffectiveName() : user) + " has spent a total time of " + Util.formatTime(timeSpentInCall.get(user)) + " in call");
//
////            MessageEmbed.Field field = new MessageEmbed.Field(event.getJDA().getUserById(user).getEffectiveName(), Util.formatTime(timeSpentInCall.get(user)), false);
////            embedBuilder.addField(field);
//                    }

//        event.replyEmbeds(embedBuilder.build()).queue();

//                });
                });
    }


}
