package com.callblocker.app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 时间范围：表示一个拦截时间段
 */
public class TimeRange {
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    public TimeRange(int startHour, int startMinute, int endHour, int endMinute) {
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }

    public int getStartHour() { return startHour; }
    public int getStartMinute() { return startMinute; }
    public int getEndHour() { return endHour; }
    public int getEndMinute() { return endMinute; }

    public String getStartTimeStr() {
        return String.format("%02d:%02d", startHour, startMinute);
    }

    public String getEndTimeStr() {
        return String.format("%02d:%02d", endHour, endMinute);
    }

    public String getTimeRangeStr() {
        return getStartTimeStr() + " - " + getEndTimeStr();
    }

    /**
     * 检查当前时间是否在此范围内
     */
    public boolean contains(int currentHour, int currentMinute) {
        int current = currentHour * 60 + currentMinute;
        int start = startHour * 60 + startMinute;
        int end = endHour * 60 + endMinute;

        if (start <= end) {
            return current >= start && current <= end;
        } else {
            // 跨午夜，如 23:00 - 06:00
            return current >= start || current <= end;
        }
    }

    /**
     * 快捷预设：全天 00:00 - 23:59
     */
    public static TimeRange allDay() {
        return new TimeRange(0, 0, 23, 59);
    }

    /**
     * 快捷预设：上午 00:00 - 12:00
     */
    public static TimeRange morning() {
        return new TimeRange(0, 0, 12, 0);
    }

    /**
     * 快捷预设：中午 12:00 - 14:00
     */
    public static TimeRange noon() {
        return new TimeRange(12, 0, 14, 0);
    }

    /**
     * 快捷预设：下午 14:00 - 23:59
     */
    public static TimeRange afternoon() {
        return new TimeRange(14, 0, 23, 59);
    }

    // JSON 序列化/反序列化
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("sh", startHour);
        obj.put("sm", startMinute);
        obj.put("eh", endHour);
        obj.put("em", endMinute);
        return obj;
    }

    public static TimeRange fromJson(JSONObject obj) throws JSONException {
        return new TimeRange(
                obj.getInt("sh"),
                obj.getInt("sm"),
                obj.getInt("eh"),
                obj.getInt("em")
        );
    }

    /**
     * 判断两个时间范围是否完全相同
     */
    public boolean equalsRange(TimeRange other) {
        return startHour == other.startHour && startMinute == other.startMinute
                && endHour == other.endHour && endMinute == other.endMinute;
    }
}
