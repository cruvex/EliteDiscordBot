package com.cruvex.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public class Util {
    public static Boolean isEmptyOrNull(Object o) {
        if(o == null)
            return true;
        String s;
        if (o instanceof String) {
            s = (String) o;
            if (s.isEmpty())
                return true;
            s = s.trim();
        } else
            s = o.toString().trim();
        return (s.isEmpty() || s.equals("null"));
    }

    public static String formatTime(long ms, TimeUnit... outputUnits) {
        if (outputUnits.length == 0)
            outputUnits = new TimeUnit[]{ DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS };

        Comparator<TimeUnit> unitComparator = Comparator
                .<TimeUnit>comparingInt(unit ->
                    switch (unit) {
                        case DAYS -> 5;
                        case HOURS -> 4;
                        case MINUTES -> 3;
                        case SECONDS -> 2;
                        case MILLISECONDS -> 1;
                        default -> 0;
                    })
                .reversed(); // Sort in descending order (DAYS > HOURS > MINUTES > SECONDS > MILLISECONDS)

        Arrays.sort(outputUnits, unitComparator);

        Duration duration = Duration.ofMillis(ms);

        StringBuilder formattedDuration = new StringBuilder();

        for (TimeUnit unit : outputUnits) {
            switch (unit) {
                case DAYS:
                    long days = duration.toDays();
                    if (days > 0) {
                        formattedDuration.append(days).append(" day");
                        if (days != 1) {
                            formattedDuration.append('s');
                        }
                        duration = duration.minusDays(days);
                    }
                    break;
                case HOURS:
                    long hours = duration.toHours();
                    if (hours > 0) {
                        formattedDuration.append(hours).append(" hour");
                        if (hours != 1) {
                            formattedDuration.append('s');
                        }
                        duration = duration.minusHours(hours);
                    }
                    break;
                case MINUTES:
                    long minutes = duration.toMinutes();
                    if (minutes != 0) {
                        if (minutes > 0) {
                            formattedDuration.append(minutes).append(" minute");
                            if (minutes != 1) {
                                formattedDuration.append('s');
                            }
                        }
                        duration = duration.minusMinutes(minutes);
                    }
                    break;
                case SECONDS:
                    long seconds = duration.toSeconds();
                    if (seconds > 0) {
                        formattedDuration.append(seconds).append(" second");
                        if (seconds != 1) {
                            formattedDuration.append('s');
                        }
                        duration = duration.minusSeconds(seconds);
                    }
                    break;
                case MILLISECONDS:
                    long millis = duration.toMillis();
                    if (millis > 0)
                        formattedDuration.append(millis).append(" ms");
                    break;
                default:
                    // Handle other units as needed
                    break;
            }

            if (formattedDuration.length() > 0) {
                formattedDuration.append(" ");
            }
        }

        return formattedDuration.toString();
    }

    public static Boolean memberHasRole(Member member, Role role) {
        return member.getRoles().contains(role);
    }
}
