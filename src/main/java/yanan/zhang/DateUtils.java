package yanan.zhang;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Date tools
 *
 * @author Yanan Zhang
 **/
public class DateUtils {

    public static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FORMATTER_DATE_TIME_WHOLE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00:00");
    public static final DateTimeFormatter FORMATTER_DATE_TIME_WHOLE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
    public static final DateTimeFormatter FORMATTER_DATE_TIME_WITHOUT_SYMBOL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FORMATTER_DATE_WITHOUT_SYMBOL = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter FORMATTER_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter FORMATTER_HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter FORMATTER_WHOLE_HOUR = DateTimeFormatter.ofPattern("HH:00");

    /**
     * date to string
     *
     * @param date
     * @param dateTimeFormatter
     * @return
     */
    public static String format(Date date, DateTimeFormatter dateTimeFormatter) {
        return asLocalDateTime(date).format(dateTimeFormatter);
    }

    /**
     * minus n days
     *
     * @param date date
     * @param days dats
     * @return
     */
    public static Date minusDays(Date date, long days) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusDays(days);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }


    /**
     * calculate the time difference
     *
     * @param before
     * @param after
     * @return unit: second
     */
    public static int interval(Date before, Date after) {
        Instant i1 = asInstant(before);
        Instant i2 = asInstant(after);
        int seconds = (int) Duration.between(i1, i2).getSeconds();
        return seconds;
    }

    /**
     * calculate the difference between two time
     *
     * @param before
     * @param after
     * @return unit:day
     */
    public static int intervalDays(Date before, Date after) {
        int seconds = interval(before, after);
        return seconds > 0 ? seconds / (60 * 60 * 24) : 0;
    }

    /**
     * calculate the difference between two day
     *
     * @param farDate
     * @param nearDate farDate > nearDate positive integer
     *                 farDate = nearDate 0
     *                 farDate < nearDate negative
     * @return
     */
    public static int dateInterval(Date farDate, Date nearDate) {
        LocalDate farDateLocal = asLocalDate(farDate);
        LocalDate nearDateLocal = asLocalDate(nearDate);
        Long day = nearDateLocal.toEpochDay() - farDateLocal.toEpochDay();
        return Integer.parseInt(String.valueOf(day));
    }

    /**
     * check if it is today
     *
     * @param date
     * @return
     */
    public static boolean isToday(Date date) {
        return asLocalDate(date).isEqual(LocalDate.now());
    }

    /**
     * Calls {@link #asLocalDate(Date, ZoneId)} with the system default time zone.
     */
    public static LocalDate asLocalDate(Date date) {
        return asLocalDate(date, ZoneId.systemDefault());
    }

    /**
     * Creates {@link LocalDate} from {@code java.util.Date} or it's subclasses. Null-safe.
     */
    public static LocalDate asLocalDate(Date date, ZoneId zone) {
        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        } else {
            return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
        }
    }

    /**
     * Calls {@link #asLocalDateTime(Date, ZoneId)} with the system default time zone.
     */
    public static LocalDateTime asLocalDateTime(Date date) {
        return asLocalDateTime(date, ZoneId.systemDefault());
    }

    /**
     * Creates {@link LocalDateTime} from {@code java.util.Date} or it's subclasses. Null-safe.
     */
    public static LocalDateTime asLocalDateTime(Date date, ZoneId zone) {
        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) date).toLocalDateTime();
        } else {
            return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDateTime();
        }
    }

    /**
     * Calls {@link #asUtilDate(Object, ZoneId)} with the system default time zone.
     */
    public static Date asUtilDate(Object date) {
        return asUtilDate(date, ZoneId.systemDefault());
    }

    /**
     * Creates a {@link Date} from various date objects. Is null-safe. Currently supports:<ul>
     * <li>{@link Date}
     * <li>{@link java.sql.Date}
     * <li>{@link java.sql.Timestamp}
     * <li>{@link LocalDate}
     * <li>{@link LocalDateTime}
     * <li>{@link ZonedDateTime}
     * <li>{@link Instant}
     * </ul>
     *
     * @param zone Time zone, used only if the input object is LocalDate or LocalDateTime.
     * @return {@link Date} (exactly this class, not a subclass, such as java.sql.Date)
     */
    public static Date asUtilDate(Object date, ZoneId zone) {
        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date || date instanceof java.sql.Timestamp) {
            return new Date(((Date) date).getTime());
        }
        if (date instanceof Date) {
            return (Date) date;
        }
        if (date instanceof LocalDate) {
            return Date.from(((LocalDate) date).atStartOfDay(zone).toInstant());
        }
        if (date instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) date).atZone(zone).toInstant());
        }
        if (date instanceof ZonedDateTime) {
            return Date.from(((ZonedDateTime) date).toInstant());
        }
        if (date instanceof Instant) {
            return Date.from((Instant) date);
        }

        throw new UnsupportedOperationException("Don't know hot to convert " + date.getClass().getName() + " to java.util.Date");
    }

    /**
     * Creates an {@link Instant} from {@code java.util.Date} or it's subclasses. Null-safe.
     */
    public static Instant asInstant(Date date) {
        if (date == null) {
            return null;
        } else {
            return Instant.ofEpochMilli(date.getTime());
        }
    }

    /**
     * Calls {@link #asZonedDateTime(Date, ZoneId)} with the system default time zone.
     */
    public static ZonedDateTime asZonedDateTime(Date date) {
        return asZonedDateTime(date, ZoneId.systemDefault());
    }

    /**
     * Creates {@link ZonedDateTime} from {@code java.util.Date} or it's subclasses. Null-safe.
     */
    public static ZonedDateTime asZonedDateTime(Date date, ZoneId zone) {
        if (date == null) {
            return null;
        } else {
            return asInstant(date).atZone(zone);
        }
    }

}