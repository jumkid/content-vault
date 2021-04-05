package com.jumkid.vault.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created at 09 Jan$
 *
 * @author chooliyip
 **/
@Slf4j
public class DateTimeUtils {

    private static final CopyOnWriteArrayList<DateTimeFormatter> DATE_FORMATTERS = new CopyOnWriteArrayList<>();

    static {
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC));
    }

    // ISO_8601
    private static String toISOString(LocalDateTime dateTime) {
        Objects.requireNonNull(dateTime, "dateTime");

        return DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.of(dateTime, ZoneOffset.UTC));
    }

    public static String toISOString(LocalDate date) {
        Objects.requireNonNull(date, "date");

        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    public static String toISOString(LocalTime time) {
        Objects.requireNonNull(time, "time");

        return DateTimeFormatter.ISO_LOCAL_TIME.format(time);
    }

    public static String toISOString(Date date) {
        Objects.requireNonNull(date, "date");

        return toISOString(toLocalDateTime(date));
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        Objects.requireNonNull(date, "date");

        return date.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    public static Date toDate(LocalDate date) {
        Objects.requireNonNull(date, "date");

        return toDate(date.atStartOfDay());
    }

    private static Date toDate(LocalDateTime dateTime) {
        Objects.requireNonNull(dateTime, "dateTime");

        return Date.from(dateTime.atZone(ZoneOffset.UTC).toInstant());
    }

    private static LocalDateTime parseDate(String date) {
        Objects.requireNonNull(date, "date");

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // equals ISO_LOCAL_DATE
                if (formatter.equals(DATE_FORMATTERS.get(2))) {
                    LocalDate localDate = LocalDate.parse(date, formatter);

                    return localDate.atStartOfDay();
                } else {
                    return LocalDateTime.parse(date, formatter);
                }
            } catch (java.time.format.DateTimeParseException dpe) {
                log.error(dpe.getMessage());
            }
        }

        return null;
    }

    public static Date stringToDate(String date) {
        LocalDateTime localDateTime = parseDate(date);
        return toDate(localDateTime);
    }

    public static LocalDateTime stringToLocalDatetime(String datetime) {
        return datetime != null ? parseDate(datetime) : null;
    }

    public static Date createDate(int year, int month, int day) {
        return createDate(year, month, day, 0, 0, 0, 0);
    }

    public static Date createDate(int year, int month, int day, int hours, int min, int sec) {
        return createDate(year, month, day, hours, min, sec, 0);
    }

    private static Date createDate(int year, int month, int day, int hours, int min, int sec, int millis) {
        long nanos = TimeUnit.MILLISECONDS.toNanos(millis);
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hours, min, sec, (int) nanos);
        return DateTimeUtils.toDate(localDateTime);
    }

}
