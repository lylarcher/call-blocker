package com.callblocker.app.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BlockedCall {
    private long id;
    private String phoneNumber;
    private long timestamp;
    private String ruleLabel;
    private int matchType;

    public BlockedCall() {}

    public BlockedCall(String phoneNumber, long timestamp, String ruleLabel, int matchType) {
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        this.timestamp = timestamp;
        this.ruleLabel = ruleLabel != null ? ruleLabel : "";
        this.matchType = matchType;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getRuleLabel() { return ruleLabel; }
    public void setRuleLabel(String ruleLabel) { this.ruleLabel = ruleLabel; }

    public int getMatchType() { return matchType; }
    public void setMatchType(int matchType) { this.matchType = matchType; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getMatchTypeDescription() {
        switch (matchType) {
            case BlockRule.MATCH_EXACT: return "精确匹配";
            case BlockRule.MATCH_PREFIX: return "前缀匹配";
            case BlockRule.MATCH_BLOCK_ALL: return "拦截所有";
            case BlockRule.MATCH_OVERSEAS: return "拦截海外";
            case BlockRule.MATCH_LANDLINE: return "拦截座机";
            default: return "未知";
        }
    }

    public String getPhoneNumberDisplay() {
        if (phoneNumber.isEmpty()) return "未知号码";
        return phoneNumber;
    }
}
