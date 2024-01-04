package com.cruvex.database.tables;

import lombok.Getter;
import lombok.Setter;

public class VoiceJoinLeaveEvent implements Comparable<VoiceJoinLeaveEvent> {
    @Getter @Setter
    private String guildId;

    @Getter @Setter
    private String userId;

    @Getter @Setter
    private String channelId;

    @Getter @Setter
    private String eventType;

    @Getter @Setter
    private Long timestamp;

    public VoiceJoinLeaveEvent(String guildId, String userId, String channelId, String eventType, Long timestamp) {
        this.guildId = guildId;
        this.userId = userId;
        this.channelId = channelId;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }


    @Override
    public int compareTo(VoiceJoinLeaveEvent other) {
        return this.timestamp.compareTo(other.timestamp);
    }

    @Override
    public String toString() {
        return "VoiceJoinLeaveEvent{" +
                "guildId='" + guildId + '\'' +
                ", userId='" + userId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
