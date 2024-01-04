package com.cruvex.dao;

import com.cruvex.EliteDiscordBot;
import com.cruvex.util.Util;
import com.cruvex.database.tables.VoiceJoinLeaveEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import static com.cruvex.EliteDiscordBot.logger;

public class VoiceEventsDAO {

    public static final String JOIN_EVENT = "JOIN_EVENT";
    public static final String LEAVE_EVENT = "LEAVE_EVENT";


    public static Boolean insertVoiceJoinEvent(String userId, String guildId, String channelId, long timeStamp) {
        return insertVoiceJoinLeaveEvent(userId, guildId, channelId, JOIN_EVENT, timeStamp);
    }

    public static Boolean insertVoiceLeaveEvent(String userId, String guildId, String channelId, long timeStamp) {
        return insertVoiceJoinLeaveEvent(userId, guildId, channelId, LEAVE_EVENT, timeStamp);
    }

    public static Boolean insertVoiceJoinLeaveEvent(String userId, String guildId, String channelId, String typeJoinLeave, long timeStamp) {
        if (!typeJoinLeave.equals(JOIN_EVENT) && !typeJoinLeave.equals(LEAVE_EVENT)) {
            log("Tried to insert unknown JoinLeaveEvent: " + typeJoinLeave);
            return false;
        }

        try (Connection conn = EliteDiscordBot.getDBConnection()) {
            PreparedStatement stmt;

            String sql = " INSERT INTO voice_join_leave_events VALUES (?, ?, ?, ?, ?) ";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            stmt.setString(3, typeJoinLeave);
            stmt.setLong(4, timeStamp);
            stmt.setString(5, channelId);

            logSQL(stmt.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!e.getMessage().equals("No results were returned by the query."))
                logger.error("Error trying to add voice level : " + e.getMessage());
            return false;
        }
        return true;
    }

    public static ArrayList<VoiceJoinLeaveEvent> getVoiceJoinLeaveEventsForUserInGuild(String guildId, String userId) {
        ArrayList<VoiceJoinLeaveEvent> result = new ArrayList<>();

        try (Connection conn = EliteDiscordBot.getDBConnection()) {
            PreparedStatement stmt;
            ResultSet rs;

            String sql = " SELECT channel_id, type, timestamp FROM voice_join_leave_events " +
                         " WHERE guild_id = ? " +
                         " AND user_id = ? ";

            sql +=       " ORDER BY timestamp ASC ";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, guildId);
            stmt.setString(2, userId);


            logSQL(stmt.toString());
            rs = stmt.executeQuery();

            while (rs.next()) {
                String channelId = rs.getString(1);
                String eventType = rs.getString(2);
                Long timestamp = rs.getLong(3);

                result.add(new VoiceJoinLeaveEvent(guildId, userId,channelId, eventType, timestamp));
            }
        } catch (SQLException e) {
            if (!e.getMessage().equals("No results were returned by the query."))
                logger.error("Error trying to add voice level : " + e.getMessage());

            log(e.getMessage());
        }
        return result;
    }

    public static HashMap<String, ArrayList<VoiceJoinLeaveEvent>> getVoiceJoinLeaveEventsForGuildPerUser(String guildId) {
//        ArrayList<VoiceJoinLeaveEvent> result = new ArrayList<>();

        HashMap<String, ArrayList<VoiceJoinLeaveEvent>> result = new HashMap<>();

        try (Connection conn = EliteDiscordBot.getDBConnection()) {
            PreparedStatement stmt;
            ResultSet rs;

            String sql = " SELECT user_id, channel_id, type, timestamp FROM voice_join_leave_events " +
                    " WHERE guild_id = ? ";

            sql +=       " ORDER BY timestamp ASC ";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, guildId);


            logSQL(stmt.toString());
            rs = stmt.executeQuery();

            while (rs.next()) {
                String userId = rs.getString(1);
                String channelId = rs.getString(2);
                String eventType = rs.getString(3);
                Long timestamp = rs.getLong(4);

                ArrayList<VoiceJoinLeaveEvent> userJoinLeaveEvents = result.computeIfAbsent(userId, k -> new ArrayList<>());

                userJoinLeaveEvents.add(new VoiceJoinLeaveEvent(guildId, userId,channelId, eventType, timestamp));
            }
            log("");
        } catch (SQLException e) {
            if (!e.getMessage().equals("No results were returned by the query."))
                logger.error("Error trying to add voice level : " + e.getMessage());

            log(e.getMessage());
        }
        return result;
    }

    public static ArrayList<Long> getPeriodsSpentInCall(ArrayList<VoiceJoinLeaveEvent> joinLeaveEvents, Long filterTime) {
        ArrayList<Long> result = new ArrayList<>();

        if (!Util.isEmptyOrNull(filterTime))
            joinLeaveEvents = joinLeaveEvents.stream().filter(voiceJoinLeaveEvent -> voiceJoinLeaveEvent.getTimestamp() > filterTime).collect(Collectors.toCollection(ArrayList::new));

        VoiceJoinLeaveEvent joinEvent = null;
        for (VoiceJoinLeaveEvent e : joinLeaveEvents) {
            if (e.getEventType().equals(VoiceEventsDAO.JOIN_EVENT) && Util.isEmptyOrNull(joinEvent)) {
                joinEvent = e;
            } else if (e.getEventType().equals(VoiceEventsDAO.LEAVE_EVENT) && !Util.isEmptyOrNull(joinEvent)) {
                result.add(e.getTimestamp() - joinEvent.getTimestamp());
                joinEvent = null;
            }
        }

        return result;
    }

    public static long calculateAverageLongsGreaterThanTenMinutes(ArrayList<Long> list) {
        // Filter values longer than 10 minutes
        ArrayList<Long> filteredList = list.stream()
                .filter(value -> value > 10 * 60 * 1000) // Filter values longer than 10 minutes (10 * 60 * 1000 milliseconds)
                .collect(Collectors.toCollection(ArrayList::new));

        // Calculate the sum of filtered values
        long sum = filteredList.stream().mapToLong(Long::longValue).sum();

        // Calculate the count of filtered values
        long count = filteredList.size();

        // Calculate the average
        return count > 0 ? sum / count : 0;
    }

    public static VoiceJoinLeaveEvent findLastJoinEvent(ArrayList<VoiceJoinLeaveEvent> eventList) {
        for (int i = eventList.size() - 1; i >= 0; i--) {
            VoiceJoinLeaveEvent event = eventList.get(i);
            if (event.getEventType().equals(VoiceEventsDAO.JOIN_EVENT)) {
                return event;
            }
        }
        return null;
    }

    private static void log(String logMessage) {
        logger.info("[VoiceEventsDAO] " + logMessage);
    }

    private static void logSQL(String logMessage) {
        logger.info("[VoiceEventsDAO][SQL] " + logMessage);
    }
}
