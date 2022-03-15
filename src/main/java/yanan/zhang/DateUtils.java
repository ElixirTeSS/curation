package yanan.zhang;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Date;

/**
 * 日期时间工具类
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
     * 字符串转时间
     *
     * @param dateStr
     * @return
     */
    public static Date parse(String dateStr) {
        LocalDateTime parse = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME);
        ZonedDateTime zonedDateTime = parse.atZone((ZoneId.systemDefault()));
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 字符串转时间
     *
     * @param dateStr
     * @param dateTimeFormatter
     * @return
     */
    public static Date parse(String dateStr, DateTimeFormatter dateTimeFormatter) {
        LocalDateTime parse = LocalDateTime.parse(dateStr, dateTimeFormatter);
        ZonedDateTime zonedDateTime = parse.atZone((ZoneId.systemDefault()));
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 时间转字符串
     *
     * @param date
     * @return
     */
    public static String format(Date date) {
        return asLocalDateTime(date).format(FORMATTER_DATE_TIME);
    }

    /**
     * 时间转字符串
     *
     * @param date
     * @param dateTimeFormatter
     * @return
     */
    public static String format(Date date, DateTimeFormatter dateTimeFormatter) {
        return asLocalDateTime(date).format(dateTimeFormatter);
    }

    /**
     * 获取年
     *
     * @param date
     * @return
     */
    public static int getYear(Date date) {
        LocalDate localDate = asLocalDate(date);
        return localDate.getYear();
    }

    /**
     * 获取年
     *
     * @param dateStr
     * @return
     */
    public static int getYear(String dateStr) {
        return LocalDate.parse(dateStr, FORMATTER_DATE_TIME).getYear();
    }

    /**
     * 获取月
     *
     * @param date
     * @return
     */
    public static int getMonth(Date date) {
        return asLocalDate(date).getMonthValue();
    }

    /**
     * 获取月
     *
     * @param dateStr
     * @return
     */
    public static int getMonth(String dateStr) {
        return LocalDate.parse(dateStr, FORMATTER_DATE_TIME).getMonthValue();
    }

    /**
     * 获取当前年份自然周
     *
     * @param date
     * @return
     */
    public static int getWeekOfYear(Date date) {
        return asLocalDate(date).get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * 获取天
     *
     * @param date
     * @return
     */
    public static int getDayOfYear(Date date) {
        return asLocalDate(date).getDayOfYear();
    }

    /**
     * 获取天
     *
     * @param dateStr
     * @return
     */
    public static int getDayOfYear(String dateStr) {
        return LocalDate.parse(dateStr, FORMATTER_DATE_TIME).getDayOfYear();
    }

    /**
     * 获取天
     *
     * @param date
     * @return
     */
    public static int getDayOfMonth(Date date) {
        return asLocalDate(date).getDayOfMonth();
    }

    /**
     * 获取天
     *
     * @param dateStr
     * @return
     */
    public static int getDayOfMonth(String dateStr) {
        return LocalDate.parse(dateStr, FORMATTER_DATE_TIME).getDayOfMonth();
    }

    /**
     * 获取天
     *
     * @param date
     * @return
     */
    public static int getDayOfWeek(Date date) {
        return asLocalDate(date).getDayOfWeek().getValue();
    }

    /**
     * 获取天
     *
     * @param dateStr
     * @return
     */
    public static int getDayOfWeek(String dateStr) {
        return LocalDate.parse(dateStr, FORMATTER_DATE_TIME).getDayOfWeek().getValue();
    }

    /**
     * 加n年
     *
     * @param date
     * @param years 年数
     * @return
     */
    public static Date plusYears(Date date, long years) {
        LocalDateTime localDateTime = asLocalDateTime(date).plusYears(years);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 加n年
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param years   年数
     * @return
     */
    public static String plusYears(String dateStr, long years) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).plusYears(years);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }


    /**
     * 减n年
     *
     * @param date
     * @param years 年数
     * @return
     */
    public static Date minusYears(Date date, long years) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusYears(years);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 减n年
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param years   年数
     * @return
     */
    public static String minusYears(String dateStr, long years) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).minusYears(years);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 加n月
     *
     * @param date
     * @param months 月数
     * @return
     */
    public static Date plusMonths(Date date, long months) {
        LocalDateTime localDateTime = asLocalDateTime(date).plusMonths(months);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 加n月
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param months  月数
     * @return
     */
    public static String plusMonths(String dateStr, long months) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).plusMonths(months);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 减n月
     *
     * @param date
     * @param months 月数
     * @return
     */
    public static Date minusMonths(Date date, long months) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusMonths(months);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 减n月
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param months  月数
     * @return
     */
    public static String minusMonths(String dateStr, long months) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).minusMonths(months);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 加n天
     *
     * @param date 日期
     * @param days 天数
     * @return
     */
    public static Date plusDays(Date date, long days) {
        LocalDateTime localDateTime = asLocalDateTime(date).plusDays(days);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 加n天
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param days    天数
     * @return
     */
    public static String plusDays(String dateStr, long days) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).plusDays(days);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 减n天
     *
     * @param date 日期
     * @param days 天数
     * @return
     */
    public static Date minusDays(Date date, long days) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusDays(days);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 减n天
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param days    天数
     * @return
     */
    public static String minusDays(String dateStr, long days) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).minusDays(days);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 加n小时
     *
     * @param date
     * @param hours 小时数
     * @return
     */
    public static Date plusHours(Date date, long hours) {
        LocalDateTime localDateTime = asLocalDateTime(date).plusHours(hours);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 加n小时
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param hours   小时数
     * @return
     */
    public static String plusHours(String dateStr, long hours) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).plusHours(hours);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 减n小时
     *
     * @param date
     * @param hours 小时数
     * @return
     */
    public static Date minusHours(Date date, long hours) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusHours(hours);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 减n小时
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param hours   小时数
     * @return
     */
    public static String minusHours(String dateStr, long hours) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).minusHours(hours);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 加n分钟
     *
     * @param date
     * @param minutes 分钟数
     * @return
     */
    public static Date plusMinutes(Date date, long minutes) {
        LocalDateTime localDateTime = asLocalDateTime(date).plusMinutes(minutes);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 加n分钟
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param minutes 分钟数
     * @return
     */
    public static String plusMinutes(String dateStr, long minutes) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).plusMinutes(minutes);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 减n分钟
     *
     * @param date
     * @param minutes 分钟数
     * @return
     */
    public static Date minusMinutes(Date date, long minutes) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusMinutes(minutes);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 减n分钟
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param minutes 分钟数
     * @return
     */
    public static String minusMinutes(String dateStr, long minutes) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).minusMinutes(minutes);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 加n秒
     *
     * @param date
     * @param seconds 秒数
     * @return
     */
    public static Date plusSeconds(Date date, long seconds) {
        LocalDateTime localDateTime = asLocalDateTime(date).plusSeconds(seconds);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 加n秒
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param seconds 秒数
     * @return
     */
    public static String plusSeconds(String dateStr, long seconds) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).plusSeconds(seconds);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 减n秒
     *
     * @param date
     * @param seconds 秒数
     * @return
     */
    public static Date minusSeconds(Date date, long seconds) {
        LocalDateTime localDateTime = asLocalDateTime(date).minusSeconds(seconds);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 减n秒
     *
     * @param dateStr 日期格式：yyyy-MM-dd HH:mm:ss
     * @param seconds 秒数
     * @return
     */
    public static String minusSeconds(String dateStr, long seconds) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, FORMATTER_DATE_TIME).minusSeconds(seconds);
        return localDateTime.format(FORMATTER_DATE_TIME);
    }

    /**
     * 判断先后
     *
     * @param before
     * @param after
     * @return
     */
    public static boolean isBefore(Date before, Date after) {
        return asLocalDateTime(before).isBefore(asLocalDateTime(after));
    }

    /**
     * 计算时间差
     *
     * @param before
     * @param after
     * @return 单位:秒
     */
    public static int interval(Date before, Date after) {
        Instant i1 = asInstant(before);
        Instant i2 = asInstant(after);
        int seconds = (int) Duration.between(i1, i2).getSeconds();
        return seconds;
    }

    /**
     * 计算时间差
     *
     * @param before
     * @param after
     * @return 单位:分钟
     */
    public static int intervalMinutes(Date before, Date after) {
        int seconds = interval(before, after);
        return seconds > 0 ? seconds / 60 : 0;
    }

    /**
     * 计算时间差
     *
     * @param before
     * @param after
     * @return 单位:小时
     */
    public static int intervalHours(Date before, Date after) {
        int seconds = interval(before, after);
        return seconds > 0 ? seconds / (60 * 60) : 0;
    }

    /**
     * 计算时间差
     *
     * @param before
     * @param after
     * @return 单位:天
     */
    public static int intervalDays(Date before, Date after) {
        int seconds = interval(before, after);
        return seconds > 0 ? seconds / (60 * 60 * 24) : 0;
    }

    /**
     * 计算天差
     *
     * @param farDate
     * @param nearDate farDate > nearDate 返回正整数
     *                 farDate = nearDate 返回0
     *                 farDate < nearDate 返回负数
     * @return
     */
    public static int dateInterval(Date farDate, Date nearDate) {
        LocalDate farDateLocal = asLocalDate(farDate);
        LocalDate nearDateLocal = asLocalDate(nearDate);
        Long day = nearDateLocal.toEpochDay() - farDateLocal.toEpochDay();
        return Integer.parseInt(String.valueOf(day));
    }

    /**
     * 判断是否为今天
     *
     * @param date
     * @return
     */
    public static boolean isToday(Date date) {
        return asLocalDate(date).isEqual(LocalDate.now());
    }

    /**
     * 判断是否为明天
     *
     * @param date
     * @return
     */
    public static boolean isTomorrow(Date date) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return asLocalDate(date).isEqual(tomorrow);
    }

    /**
     * 获取当前月的第一天
     *
     * @return
     */
    public static Date currentMonthOneDay() {
        LocalDate today = LocalDate.now();
        LocalDate firstday = LocalDate.of(today.getYear(), today.getMonth(), 1);
        ZonedDateTime zonedDateTime = firstday.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 获取上一月的第一天
     *
     * @return
     */
    public static Date lastMonthOneDay() {
        LocalDate localDate = LocalDate.now().minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 获取上一月的最后一天
     *
     * @return
     */
    public static Date lastMonthLastDay() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1L);
        //本月的最后一天
        LocalDate lastDay = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
        ZonedDateTime zonedDateTime = lastDay.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 判断是否为昨天
     *
     * @param date
     * @return
     */
    public static boolean isYesterday(Date date) {
        LocalDate tomorrow = LocalDate.now().minusDays(1);
        return asLocalDate(date).isEqual(tomorrow);
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