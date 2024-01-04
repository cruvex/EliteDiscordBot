package com.cruvex.commands.call;

import com.cruvex.util.Util;
import com.cruvex.commands.AbstractCommand;
import com.cruvex.database.tables.VoiceJoinLeaveEvent;
import com.cruvex.dao.VoiceEventsDAO;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.cruvex.dao.VoiceEventsDAO.findLastJoinEvent;

public class CallsCommand extends AbstractCommand {

    public final @Getter String description = "Shows call info for selected user";

    public final @Getter OptionData optionData = new OptionData(OptionType.USER, "target", "User you want to retrieve call info from", true);
    @Override
    public void execute(SlashCommandInteraction slashCommandInteraction) {
        Guild eventGuild = slashCommandInteraction.getGuild();
        User target = slashCommandInteraction.getJDA().retrieveUserById(slashCommandInteraction.getOption("target").getAsString()).complete();
        Member targetMember = eventGuild.getMember(target);

        if (target.isBot()) {
            log("Target user is bot.");
            slashCommandInteraction.reply("Cannot get call info for bots!").setEphemeral(true).queue();
            return;
        }

        if (Util.isEmptyOrNull(target)) {
            slashCommandInteraction.reply("Something went wrong while trying to get the call info for this user").setEphemeral(true).queue();
            return;
        }

        String guildId = slashCommandInteraction.getGuild().getId();

        ArrayList<VoiceJoinLeaveEvent> joinLeaveEvents = VoiceEventsDAO.getVoiceJoinLeaveEventsForUserInGuild(guildId, target.getId());

        if (joinLeaveEvents.isEmpty()) {
            slashCommandInteraction.reply("No call info found for this user!").setEphemeral(true).queue();
            return;
        }

        Long currentTimeMillis = System.currentTimeMillis();
        Long sevenDaysAgoMillis = currentTimeMillis - (7 * 24 * 60 * 60 * 1000);
        Long thirtyDaysAgoMillis = currentTimeMillis - (30L * 24 * 60 * 60 * 1000);

        ArrayList<Long> totalTimeSpentInCall = VoiceEventsDAO.getPeriodsSpentInCall(joinLeaveEvents, null);
        ArrayList<Long> last7DaysTimeSpentInCall = VoiceEventsDAO.getPeriodsSpentInCall(joinLeaveEvents, sevenDaysAgoMillis);
        ArrayList<Long> last30DaysTimeSpentInCall = VoiceEventsDAO.getPeriodsSpentInCall(joinLeaveEvents, thirtyDaysAgoMillis);

        EmbedBuilder embed = new EmbedBuilder();

        String userName = "";

        if (!Util.isEmptyOrNull(targetMember))
            userName = targetMember.getEffectiveName();

        if (Util.isEmptyOrNull(userName))
            userName = target.getEffectiveName();

        embed.setTitle("Call info for " + userName);
        embed.setColor(Color.decode("#e2be43"));
        embed.setAuthor(target.getName(), null, eventGuild.getIconUrl());

//        embed.setImage(target.getAvatarUrl());
//        embed.setThumbnail(target.getAvatarUrl());
//        embed.setThumbnail(target.getEffectiveAvatarUrl());
        embed.setImage(target.getEffectiveAvatarUrl());

        MessageEmbed.Field totalTimeField = new MessageEmbed.Field("Total Time" , Util.formatTime(totalTimeSpentInCall.stream().mapToLong(Long::longValue).sum(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS), true);
        embed.addField(totalTimeField);

        embed.addBlankField(true);

        MessageEmbed.Field timesJoinedField = new MessageEmbed.Field("Unique Calls Joined" , String.valueOf(totalTimeSpentInCall.size()), true);
        embed.addField(timesJoinedField);


        String lastSevenDaysTimeValue = Util.formatTime(last7DaysTimeSpentInCall.stream().mapToLong(Long::longValue).sum(), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS);
        MessageEmbed.Field lastSevenDaysTimeField = new MessageEmbed.Field("Last 7 Days" , (Util.isEmptyOrNull(lastSevenDaysTimeValue) ? "N/A" : lastSevenDaysTimeValue), true);
        embed.addField(lastSevenDaysTimeField);

        embed.addBlankField(true);

        String lastThirtyDaysTimeValue = Util.formatTime(last30DaysTimeSpentInCall.stream().mapToLong(Long::longValue).sum() , TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS);
        MessageEmbed.Field lastThirtyDaysTimeField = new MessageEmbed.Field("Last 30 Days", Util.isEmptyOrNull(lastThirtyDaysTimeValue) ? "N/A" : lastThirtyDaysTimeValue, true);
        embed.addField(lastThirtyDaysTimeField);


        MessageEmbed.Field averageField = new MessageEmbed.Field("Average time in call" , Util.formatTime(VoiceEventsDAO.calculateAverageLongsGreaterThanTenMinutes(totalTimeSpentInCall), TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS), true);
        embed.addField(averageField);

        embed.addBlankField(true);

        String timeStamp = "<t:" + String.valueOf(findLastJoinEvent(joinLeaveEvents).getTimestamp()).substring(0, 10) + ">";
        String relativeTimeStamp = "<t:" + String.valueOf(findLastJoinEvent(joinLeaveEvents).getTimestamp()).substring(0, 10) + ":R>";
        MessageEmbed.Field lastJoinTimeField = new MessageEmbed.Field("Last Joined Call" , timeStamp + " | " + relativeTimeStamp, true);
        embed.addField(lastJoinTimeField);


        embed.setFooter(target.getId());
        embed.setTimestamp(new Date().toInstant());

        slashCommandInteraction.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
