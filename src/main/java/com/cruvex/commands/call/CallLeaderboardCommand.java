package com.cruvex.commands.call;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.Util;
import com.cruvex.commands.AbstractCommand;
import com.cruvex.database.tables.VoiceJoinLeaveEvent;
import com.cruvex.dao.VoiceEventsDAO;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;


import java.awt.*;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cruvex.EliteDiscordBot.*;


@Getter
public class CallLeaderboardCommand extends AbstractCommand {

    public final String description = "Show leaderboard for the top 10 people that spent the most time in call";

    private Boolean hasPreviousPage = false;
    private Boolean hasNextPage = true;
    private int totalPages;

    public final HashMap<String, Method> buttonInteractionsMap;

    public CallLeaderboardCommand() {
        // define button handlers
        buttonInteractionsMap = new HashMap<>();
        try {
            buttonInteractionsMap.put("toggle-call-lb-mode", this.getClass().getMethod("onToggleModeButtonClicked", ButtonInteraction.class));
            buttonInteractionsMap.put("call-lb-prev-page", this.getClass().getMethod("onPrevPageButtonClicked", ButtonInteraction.class));
            buttonInteractionsMap.put("call-lb-next-page", this.getClass().getMethod("onNextPageButtonClicked", ButtonInteraction.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(SlashCommandInteraction slashCommandInteraction) {
        CallLeaderboardCommandMode commandMode = CallLeaderboardCommandMode.CURRENT_ELITES;

        Guild eventGuild = slashCommandInteraction.getGuild();

        EmbedBuilder embed = getLeaderBoardEmbed(eventGuild, commandMode, isStaffChannel(slashCommandInteraction.getChannel()), 1);

        slashCommandInteraction.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(getButtonsForMode(commandMode, isStaffChannel(slashCommandInteraction.getChannel())))
                .queue();
    }

    public void onToggleModeButtonClicked(ButtonInteraction buttonInteraction) {
        Guild eventGuild = buttonInteraction.getGuild();
        info("Executing onToggleModeButtonClicked button method");

        String buttonMessageEmbedDescription = buttonInteraction.getMessage().getEmbeds().get(0).getDescription();

        CallLeaderboardCommandMode mode = toggleModeFromDescription(buttonMessageEmbedDescription);

        EmbedBuilder embed = getLeaderBoardEmbed(eventGuild, mode, isStaffChannel(buttonInteraction.getChannel()), 1);

        buttonInteraction.editMessageEmbeds(embed.build())
                .setActionRow(getButtonsForMode(mode, isStaffChannel(buttonInteraction.getChannel())))
                .queue();
    }

    public void onPrevPageButtonClicked(ButtonInteraction buttonInteraction) {
        Guild eventGuild = buttonInteraction.getGuild();
        info("Executing onPrevPageButtonClicked button method");

        String buttonMessageEmbedDescription = buttonInteraction.getMessage().getEmbeds().get(0).getDescription();
        CallLeaderboardCommandMode mode = getModeFromDescription(buttonMessageEmbedDescription);

        String buttonMessageEmbedFooter = buttonInteraction.getMessage().getEmbeds().get(0).getFooter().getText().replaceAll("Page ", "");
        buttonMessageEmbedFooter = buttonMessageEmbedFooter.substring(0, 1);
        info("Page: " + buttonMessageEmbedFooter);

        buttonInteraction.editMessageEmbeds(getLeaderBoardEmbed(eventGuild, mode, isStaffChannel(buttonInteraction.getChannel()), Integer.parseInt(buttonMessageEmbedFooter) - 1).build())
                .setActionRow(getButtonsForMode(mode, isStaffChannel(buttonInteraction.getChannel())))
                .queue();
    }

    public void onNextPageButtonClicked(ButtonInteraction buttonInteraction) {
        Guild eventGuild = buttonInteraction.getGuild();
        info("Executing onNextPageButtonClicked button method");

        String buttonMessageEmbedDescription = buttonInteraction.getMessage().getEmbeds().get(0).getDescription();
        CallLeaderboardCommandMode mode = getModeFromDescription(buttonMessageEmbedDescription);

        String buttonMessageEmbedFooter = buttonInteraction.getMessage().getEmbeds().get(0).getFooter().getText().replaceAll("Page ", "").trim();
        buttonMessageEmbedFooter = buttonMessageEmbedFooter.substring(0, 1);
        info("Page: " + buttonMessageEmbedFooter);

        buttonInteraction.editMessageEmbeds(getLeaderBoardEmbed(eventGuild, mode, isStaffChannel(buttonInteraction.getChannel()), Integer.parseInt(buttonMessageEmbedFooter) + 1).build())
                .setActionRow(getButtonsForMode(mode, isStaffChannel(buttonInteraction.getChannel())))
                .queue();
    }

    private ArrayList<String> getAllTimeElitesIds() {
        ArrayList<String> result = new ArrayList<>();

        try (Connection conn = EliteDiscordBot.getDBConnection()) {
            PreparedStatement stmt;
            ResultSet rs;

            String sql = " SELECT DISTINCT user_id FROM all_time_elites ";

            stmt = conn.prepareStatement(sql);
            logSQL(stmt.toString());

            rs = stmt.executeQuery();

            while (rs.next()) {
                String userId = rs.getString(1);
                result.add(userId);
            }
        } catch (Exception e) {
            logSQL("Failed to get all time elites from database");
        }
        return result;
    }

    private ArrayList<String> getCurrentElitesIds(Guild guild) {
        ArrayList<String> result;

        Role roleEliteClan = guild.getRoleById("1110583443382882364");
        if (Util.isEmptyOrNull(roleEliteClan))
            throw new IllegalStateException("Role 'Elite Clan' not found in current server");
        Role roleTrialMember = guild.getRoleById("1110583437330497639");
        if (Util.isEmptyOrNull(roleTrialMember))
            throw new IllegalStateException("Role 'Trial Member' not found in current server");
        Role roleVeteranElite = guild.getRoleById("1110583436458078268");
        if (Util.isEmptyOrNull(roleVeteranElite))
            throw new IllegalStateException("Role 'Veteran Elite' not found in current server");

        List<Member> membersInEliteClan = guild.getMembers().stream()
                .filter(member -> Util.memberHasRole(member, roleEliteClan) ||
                        Util.memberHasRole(member, roleTrialMember) ||
                        Util.memberHasRole(member, roleVeteranElite)
                )
                .toList();

        result = membersInEliteClan.stream()
                .map(ISnowflake::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        return result;
    }

    private EmbedBuilder getLeaderBoardEmbed(Guild guild, CallLeaderboardCommandMode mode, Boolean inStaffChannel, Integer page) {
        HashMap<String, ArrayList<VoiceJoinLeaveEvent>> joinLeaveEventsPerUser = VoiceEventsDAO.getVoiceJoinLeaveEventsForGuildPerUser(guild.getId());

        ArrayList<String> filterIds = new ArrayList<>();
        ArrayList<String> exElites = new ArrayList<>();
        if (!EliteDiscordBot.getConfig().isDevelopment()) {
            filterIds.addAll(getCurrentElitesIds(guild));
        }

        if (mode.equals(CallLeaderboardCommandMode.ALL_PAST_AND_CURRENT_ELITES)) {
            exElites.addAll(getAllTimeElitesIds());
            filterIds.addAll(exElites);
        }


        ArrayList<LeaderBoardEntry> lbEntries = new ArrayList<>();

        joinLeaveEventsPerUser.forEach((key, value) -> {
            LeaderBoardEntry lbEntry = new LeaderBoardEntry(key, value);
            lbEntries.add(lbEntry);
        });

        Integer offset = (page - 1) * 10;

        List<LeaderBoardEntry> filteredLBEntries = lbEntries.stream()
                .sorted(Comparator.comparingLong(LeaderBoardEntry::getTotalTimeSpentInCall).reversed())
                .filter(entry -> EliteDiscordBot.getConfig().isDevelopment() || filterIds.contains(entry.getUserId()))
                .toList();

        List<LeaderBoardEntry> currentPage = filteredLBEntries.stream()
                .skip(offset)
                .limit(10)
                .toList();

        info("Page " + page + ": " + currentPage.size() + " users to show");
        info("Total       : " + filteredLBEntries.size());
        info("Current page: " + (1 + ((page - 1) * 10)) + " - " + (currentPage.size() + ((page - 1) * 10)));
        info("Remaining   : " + (filteredLBEntries.size() - offset - 10));

        hasNextPage = (filteredLBEntries.size() - offset - 10) > 0;
        hasPreviousPage = !page.equals(1);
        totalPages = (int) Math.ceil((double) filteredLBEntries.size() / 10);

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("Call leaderboard");
        embed.setColor(primaryColor);

        if (mode.equals(CallLeaderboardCommandMode.CURRENT_ELITES)) {
            embed.setDescription("leaderboard for current Elites\n");
        } else {
            embed.setDescription("leaderboard for all past and current Elites\n");
        }

        if (currentPage.isEmpty()) {
            embed.setDescription("No information to display!");
            return embed;
        }

        int i = 1 + ((page - 1) * 10);
        String count;

        for (LeaderBoardEntry lbEntry : currentPage) {
            if (i == 1)      count = ":first_place:";
            else if (i == 2) count = ":second_place:";
            else if (i == 3) count = ":third_place:";
            else             count = i + ".";

            String userName = "";
            if (EliteDiscordBot.isIntelliJ()) {
                userName = lbEntry.getUserId();
            } else {
                User user = EliteDiscordBot.getJda().retrieveUserById(lbEntry.getUserId()).complete();
                Member member = guild.getMember(user);

                if (!Util.isEmptyOrNull(member))
                    userName = member.getEffectiveName();

                if (Util.isEmptyOrNull(userName))
                    userName = user.getEffectiveName();

                info(userName + " (" + lbEntry.getUserId() +"): " + Util.formatTime(lbEntry.getTotalTimeSpentInCall(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS));
            }

            String fieldDescription =
                    "Total time  : " + Util.formatTime(lbEntry.getTotalTimeSpentInCall(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS) + "\n"
                            + "Average Time: " + Util.formatTime(lbEntry.getAverageTimeSpentInCall(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS) + "\n"
                            + "Unique Calls Joined: " + lbEntry.uniqueCallsJoined;

            MessageEmbed.Field leaderboardPositionField = new MessageEmbed.Field("**" + count + " " + (exElites.contains(lbEntry.getUserId()) ? ":ghost: " : "") + userName + "**", fieldDescription, false);
            embed.addField(leaderboardPositionField);
            i++;
        }

        if (inStaffChannel)
            embed.setFooter("Page " + page + "/" + totalPages);

        return embed;
    }

    private List<Button> getButtonsForMode(CallLeaderboardCommandMode mode, Boolean isStaffChannel) {
        String toggleButtonLabel;
        if (mode.equals(CallLeaderboardCommandMode.CURRENT_ELITES)) {
            toggleButtonLabel = "Show leaderboard for past and current Elites";
        } else {
            toggleButtonLabel = "Show leaderboard for current Elites";
        }

        List<Button> btnsToShow = new ArrayList<>();

        if (isStaffChannel) {
            Button prevPageBtn = Button.secondary("call-lb-prev-page", Emoji.fromUnicode("\u25C0"))
                    .withDisabled(!getHasPreviousPage());
            btnsToShow.add(prevPageBtn);

            Button nextPageBtn = Button.secondary("call-lb-next-page", Emoji.fromUnicode("\u25B6"))
                    .withDisabled(!getHasNextPage());
            btnsToShow.add(nextPageBtn);
        }

        Button toggleCommandModeBtn = Button.primary("toggle-call-lb-mode", toggleButtonLabel);
        btnsToShow.add(toggleCommandModeBtn);

        return btnsToShow;
    }

    @Getter @Setter
    private static class LeaderBoardEntry {
        private String userId;
        private Long totalTimeSpentInCall;
        private Long averageTimeSpentInCall;
        private Integer uniqueCallsJoined;

        public LeaderBoardEntry(String userId, ArrayList<VoiceJoinLeaveEvent> joinLeaveEvents) {
            setUserId(userId);
            ArrayList<Long> periodsSpentInCall = VoiceEventsDAO.getPeriodsSpentInCall(joinLeaveEvents, null);
            setTotalTimeSpentInCall(periodsSpentInCall.stream().mapToLong(Long::longValue).sum());
            setAverageTimeSpentInCall(VoiceEventsDAO.calculateAverageLongsGreaterThanTenMinutes(periodsSpentInCall));
            setUniqueCallsJoined(periodsSpentInCall.size());
        }
    }

    public enum CallLeaderboardCommandMode {
        ALL_PAST_AND_CURRENT_ELITES,
        CURRENT_ELITES
    }

    private CallLeaderboardCommandMode getModeFromDescription(String description) {
        if (!Util.isEmptyOrNull(description) &&
                description.contains("past")) {
            return CallLeaderboardCommandMode.ALL_PAST_AND_CURRENT_ELITES;
        } else {
            return CallLeaderboardCommandMode.CURRENT_ELITES;
        }
    }

    private CallLeaderboardCommandMode toggleModeFromDescription(String description) {
        if (!Util.isEmptyOrNull(description) &&
                description.contains("past")) {
            return CallLeaderboardCommandMode.CURRENT_ELITES;
        } else {
            return CallLeaderboardCommandMode.ALL_PAST_AND_CURRENT_ELITES;
        }
    }

    private Boolean isStaffChannel(MessageChannel channel) {
        ArrayList<String> staffChannels = new ArrayList<>();

        if (EliteDiscordBot.getConfig().isDevelopment()) {
            staffChannels.add("1182699084235161630");
        } else {
            staffChannels.add("1184883276041683005");
        }

        Boolean isStaffChannel = staffChannels.contains(channel.getId());

        info("isStaffChannel: " + isStaffChannel);
        return isStaffChannel;
    }
}