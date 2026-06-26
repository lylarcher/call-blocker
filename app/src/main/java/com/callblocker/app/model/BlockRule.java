package com.callblocker.app.model;

import java.util.ArrayList;
import java.util.List;

public class BlockRule {
    // 匹配类型常量
    public static final int MATCH_EXACT = 0;     // 精确匹配
    public static final int MATCH_PREFIX = 1;    // 前缀匹配
    public static final int MATCH_BLOCK_ALL = 2; // 拦截所有
    public static final int MATCH_OVERSEAS = 3;  // 拦截海外号码
    public static final int MATCH_LANDLINE = 4;  // 拦截所有座机

    private long id;
    private String phoneNumber;
    private int matchType;
    private List<TimeRange> timeRanges;
    private boolean enabled;
    private String label;

    public BlockRule() {
        this.timeRanges = new ArrayList<>();
    }

    public BlockRule(String phoneNumber, int matchType, List<TimeRange> timeRanges,
                     boolean enabled, String label) {
        this.phoneNumber = phoneNumber;
        this.matchType = matchType;
        this.timeRanges = timeRanges != null ? timeRanges : new ArrayList<>();
        this.enabled = enabled;
        this.label = label;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public int getMatchType() { return matchType; }
    public void setMatchType(int matchType) { this.matchType = matchType; }

    public List<TimeRange> getTimeRanges() { return timeRanges; }
    public void setTimeRanges(List<TimeRange> timeRanges) {
        this.timeRanges = timeRanges != null ? timeRanges : new ArrayList<>();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getTimeRangeCount() {
        return timeRanges.size();
    }

    public String getTimeRangesDisplayStr() {
        if (timeRanges.isEmpty()) return "未设置";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < timeRanges.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(timeRanges.get(i).getTimeRangeStr());
        }
        return sb.toString();
    }

    public String getMatchTypeDescription() {
        switch (matchType) {
            case MATCH_EXACT:
                return "精确: " + phoneNumber;
            case MATCH_PREFIX:
                return "前缀: " + phoneNumber + "...";
            case MATCH_BLOCK_ALL:
                return "拦截所有来电";
            case MATCH_OVERSEAS:
                return "拦截海外号码";
            case MATCH_LANDLINE:
                return "拦截所有座机";
            default:
                return phoneNumber != null ? phoneNumber : "未知";
        }
    }

    public boolean isInAnyTimeRange(int currentHour, int currentMinute) {
        if (timeRanges.isEmpty()) return false;
        for (TimeRange range : timeRanges) {
            if (range.contains(currentHour, currentMinute)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldBlock(String incomingNumber, int currentHour, int currentMinute) {
        if (!enabled) return false;
        if (!isInAnyTimeRange(currentHour, currentMinute)) return false;

        String normalized = normalizeNumber(incomingNumber);

        switch (matchType) {
            case MATCH_BLOCK_ALL:
                return true;
            case MATCH_OVERSEAS:
                return isOverseasNumber(normalized);
            case MATCH_LANDLINE:
                return isLandlineNumber(normalized);
            case MATCH_EXACT:
                return matchExact(normalized);
            case MATCH_PREFIX:
                return matchPrefix(normalized);
            default:
                return false;
        }
    }

    private String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[\\s\\-]", "").trim();
    }

    private boolean matchExact(String normalized) {
        String[] parts = phoneNumber.split(",");
        for (String part : parts) {
            String ruleNum = normalizeNumber(part);
            if (ruleNum.isEmpty()) continue;
            if (normalized.equals(ruleNum)) return true;
            String stripped = stripChinaPrefix(normalized);
            String strippedRule = stripChinaPrefix(ruleNum);
            if (stripped.equals(strippedRule)) return true;
        }
        return false;
    }

    private boolean matchPrefix(String normalized) {
        String[] parts = phoneNumber.split(",");
        for (String part : parts) {
            String ruleNum = normalizeNumber(part);
            if (ruleNum.isEmpty()) continue;
            if (normalized.startsWith(ruleNum)) return true;
            String stripped = stripChinaPrefix(normalized);
            if (stripped.startsWith(ruleNum) || normalized.startsWith(stripped)) return true;
        }
        return false;
    }

    private String stripChinaPrefix(String number) {
        if (number.startsWith("0086")) {
            return number.substring(4);
        } else if (number.startsWith("+86")) {
            return number.substring(3);
        } else if (number.length() > 11 && number.startsWith("86")) {
            return number.substring(2);
        }
        return number;
    }

    /**
     * 判断是否为座机号码
     * 中国座机号特征：
     * - 区号+号码，如 010-12345678, 0212345678
     * - 区号3-4位（010, 021, 0755等），号码7-8位
     * - 去掉+86/0086/86前缀后以0开头且长度在9-12位之间
     * - 不含手机号段（1xx开头）
     */
    private boolean isLandlineNumber(String normalized) {
        if (normalized.isEmpty()) return false;
        String num = stripChinaPrefix(normalized);
        if (num.startsWith("0") && num.length() >= 10 && num.length() <= 12) {
            if (num.length() == 11 && num.charAt(1) == '1') {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isOverseasNumber(String normalized) {
        if (normalized.isEmpty()) return false;
        if (normalized.startsWith("+")) {
            if (normalized.startsWith("+86") && normalized.length() > 3) {
                return false;
            }
            return true;
        }
        if (normalized.startsWith("00")) {
            if (normalized.startsWith("0086") && normalized.length() > 4) {
                return false;
            }
            return true;
        }
        if (!Character.isDigit(normalized.charAt(0))) {
            return true;
        }
        return false;
    }
}
