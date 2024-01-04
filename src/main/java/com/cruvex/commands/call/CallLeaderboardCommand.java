package com.cruvex.commands.call;

import com.cruvex.util.Util;
import com.cruvex.commands.AbstractCommand;
import com.cruvex.database.tables.VoiceJoinLeaveEvent;
import com.cruvex.dao.VoiceEventsDAO;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;


import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class CallLeaderboardCommand extends AbstractCommand {

    public final String description = "Show leaderboard of the top 10 people that spent the most time in call";
    @Override
    public void execute(SlashCommandInteraction slashCommandInteraction) {
        Guild eventGuild = slashCommandInteraction.getGuild();

        HashMap<String, ArrayList<VoiceJoinLeaveEvent>> joinLeaveEventsPerUser = VoiceEventsDAO.getVoiceJoinLeaveEventsForGuildPerUser(eventGuild.getId());

        Role roleEliteClan = eventGuild.getRoleById("1110583443382882364");
        Role roleTrialMember = eventGuild.getRoleById("1110583437330497639");
        Role roleVeteranElite = eventGuild.getRoleById("1110583436458078268");

        List<Member> membersInEliteClan = eventGuild.getMembers().stream()
                .filter(member -> {
                    log(member);
                return        Util.memberHasRole(member, roleEliteClan) ||
                        Util.memberHasRole(member, roleTrialMember) ||
                        Util.memberHasRole(member, roleVeteranElite);
                })
                        .toList();


        ArrayList<String> membersInEliteClanIds = membersInEliteClan.stream().map(ISnowflake::getId).collect(Collectors.toCollection(ArrayList::new));

        HashMap<String, Long> totalTimeSpentInCallPerUser = new HashMap<>();

        HashMap<String, ArrayList<Long>> periodsSpentInCallPerUser = new HashMap<>();

        for (Map.Entry<String, ArrayList<VoiceJoinLeaveEvent>> entry : joinLeaveEventsPerUser.entrySet()) {
            ArrayList<Long> periodsSpentInCall = VoiceEventsDAO.getPeriodsSpentInCall(entry.getValue(), null);
            totalTimeSpentInCallPerUser.put(entry.getKey(), periodsSpentInCall.stream().mapToLong(Long::longValue).sum());
            periodsSpentInCallPerUser.put(entry.getKey(), periodsSpentInCall);
        }

        Map<String, Long> top10Entries = totalTimeSpentInCallPerUser.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .filter(map -> membersInEliteClanIds.contains(map.getKey()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("Call leaderboard");
        embed.setColor(Color.decode("#e2be43"));

        if (top10Entries.isEmpty()) {
            embed.setDescription("No information to display!");
            slashCommandInteraction.getHook().editOriginalEmbeds(embed.build()).queue();
            return;
        }

        int i = 1;
        String count;


        for (Map.Entry<String, Long> entry : top10Entries.entrySet()) {
            if (i == 1) {
                count = ":first_place:";
            } else if (i == 2) {
                count = ":second_place:";
            } else if (i == 3) {
                count = ":third_place:";
            } else {
                count = i + ". ";
            }

            String userName = "";

            User user = slashCommandInteraction.getJDA().retrieveUserById(entry.getKey()).complete();
            Member member = eventGuild.getMember(user);

            if (!Util.isEmptyOrNull(member))
                userName = member.getEffectiveName();

            if (Util.isEmptyOrNull(userName))
                userName = user.getEffectiveName();

            log(userName + ": " + Util.formatTime(entry.getValue(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS));


            String fieldDescription = "Total time  : " + Util.formatTime(entry.getValue(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS) + "\n"
                    + "Average Time: " + Util.formatTime(VoiceEventsDAO.calculateAverageLongsGreaterThanTenMinutes(periodsSpentInCallPerUser.get(entry.getKey())), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS) + "\n"
                    + "Unique Calls Joined: " + periodsSpentInCallPerUser.get(entry.getKey()).size();

            MessageEmbed.Field leaderboardPositionField = new MessageEmbed.Field("**" + count + " " + userName + "**", fieldDescription, false);
            embed.addField(leaderboardPositionField);
            i++;
        }
        slashCommandInteraction.getHook().editOriginalEmbeds(embed.build()).queue();
    }
}
