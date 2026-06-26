package com.callblocker.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.callblocker.app.model.BlockedCall;
import com.callblocker.app.model.BlockRule;
import com.callblocker.app.model.TimeRange;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class RuleDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "call_blocker.db";
    private static final int DATABASE_VERSION = 4;

    // 规则表
    private static final String TABLE_RULES = "block_rules";
    private static final String COL_ID = "id";
    private static final String COL_PHONE = "phone_number";
    private static final String COL_MATCH_TYPE = "match_type";
    private static final String COL_TIME_RANGES = "time_ranges";
    private static final String COL_ENABLED = "enabled";
    private static final String COL_LABEL = "label";

    // 拦截记录表
    private static final String TABLE_BLOCKED_CALLS = "blocked_calls";
    private static final String COL_BC_ID = "id";
    private static final String COL_BC_PHONE = "phone_number";
    private static final String COL_BC_TIMESTAMP = "timestamp";
    private static final String COL_BC_RULE_LABEL = "rule_label";
    private static final String COL_BC_MATCH_TYPE = "match_type";

    private static RuleDatabase instance;

    public static synchronized RuleDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new RuleDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private RuleDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createRuleTable(db);
        createBlockedCallsTable(db);
    }

    private void createRuleTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_RULES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PHONE + " TEXT, " +
                COL_MATCH_TYPE + " INTEGER NOT NULL DEFAULT 0, " +
                COL_TIME_RANGES + " TEXT NOT NULL DEFAULT '[]', " +
                COL_ENABLED + " INTEGER NOT NULL DEFAULT 1, " +
                COL_LABEL + " TEXT" +
                ")";
        db.execSQL(sql);
    }

    private void createBlockedCallsTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_BLOCKED_CALLS + " (" +
                COL_BC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BC_PHONE + " TEXT NOT NULL DEFAULT '', " +
                COL_BC_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_BC_RULE_LABEL + " TEXT, " +
                COL_BC_MATCH_TYPE + " INTEGER NOT NULL DEFAULT 0" +
                ")";
        db.execSQL(sql);
        db.execSQL("CREATE INDEX idx_bc_timestamp ON " + TABLE_BLOCKED_CALLS + "(" + COL_BC_TIMESTAMP + " DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_RULES + " ADD COLUMN " + COL_TIME_RANGES + " TEXT NOT NULL DEFAULT '[]'");
            } catch (Exception e) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_RULES);
                createRuleTable(db);
                return;
            }
            migrateOldTimeRanges(db);
        }
        if (oldVersion < 4) {
            createBlockedCallsTable(db);
        }
    }

    private void migrateOldTimeRanges(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT " + COL_ID +
                ", start_hour, start_minute, end_hour, end_minute FROM " + TABLE_RULES, null);
        if (cursor == null) return;
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            try {
                int sh = cursor.getInt(cursor.getColumnIndexOrThrow("start_hour"));
                int sm = cursor.getInt(cursor.getColumnIndexOrThrow("start_minute"));
                int eh = cursor.getInt(cursor.getColumnIndexOrThrow("end_hour"));
                int em = cursor.getInt(cursor.getColumnIndexOrThrow("end_minute"));
                TimeRange range = new TimeRange(sh, sm, eh, em);
                JSONArray arr = new JSONArray();
                arr.put(range.toJson());
                ContentValues values = new ContentValues();
                values.put(COL_TIME_RANGES, arr.toString());
                db.update(TABLE_RULES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
            } catch (Exception e) {
                // skip
            }
        }
        cursor.close();
    }

    // ============ 规则 CRUD ============

    public long insertRule(BlockRule rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHONE, rule.getPhoneNumber());
        values.put(COL_MATCH_TYPE, rule.getMatchType());
        values.put(COL_TIME_RANGES, serializeTimeRanges(rule.getTimeRanges()));
        values.put(COL_ENABLED, rule.isEnabled() ? 1 : 0);
        values.put(COL_LABEL, rule.getLabel());
        long id = db.insertWithOnConflict(TABLE_RULES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        rule.setId(id);
        return id;
    }

    public int updateRule(BlockRule rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHONE, rule.getPhoneNumber());
        values.put(COL_MATCH_TYPE, rule.getMatchType());
        values.put(COL_TIME_RANGES, serializeTimeRanges(rule.getTimeRanges()));
        values.put(COL_ENABLED, rule.isEnabled() ? 1 : 0);
        values.put(COL_LABEL, rule.getLabel());
        return db.update(TABLE_RULES, values, COL_ID + "=?", new String[]{String.valueOf(rule.getId())});
    }

    public int deleteRule(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_RULES, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public int toggleRule(long id, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ENABLED, enabled ? 1 : 0);
        return db.update(TABLE_RULES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    private BlockRule cursorToRule(Cursor cursor) {
        BlockRule rule = new BlockRule();
        rule.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        rule.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)));
        rule.setMatchType(cursor.getInt(cursor.getColumnIndexOrThrow(COL_MATCH_TYPE)));
        rule.setTimeRanges(deserializeTimeRanges(
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME_RANGES))));
        rule.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED)) == 1);
        rule.setLabel(cursor.getString(cursor.getColumnIndexOrThrow(COL_LABEL)));
        return rule;
    }

    public List<BlockRule> getAllRules() {
        List<BlockRule> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RULES, null, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                rules.add(cursorToRule(cursor));
            }
            cursor.close();
        }
        return rules;
    }

    public List<BlockRule> getEnabledRules() {
        List<BlockRule> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RULES, null, COL_ENABLED + "=1", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                rules.add(cursorToRule(cursor));
            }
            cursor.close();
        }
        return rules;
    }

    public boolean isDuplicateRule(long excludeId, String phoneNumber, int matchType,
                                   List<TimeRange> timeRanges) {
        SQLiteDatabase db = getReadableDatabase();
        String timeRangesJson = serializeTimeRanges(timeRanges);

        StringBuilder sb = new StringBuilder();
        List<String> argsList = new ArrayList<>();

        if (phoneNumber == null) {
            sb.append(COL_PHONE).append(" IS NULL");
        } else {
            sb.append(COL_PHONE).append("=?");
            argsList.add(phoneNumber);
        }

        sb.append(" AND ").append(COL_MATCH_TYPE).append("=?");
        argsList.add(String.valueOf(matchType));

        sb.append(" AND ").append(COL_TIME_RANGES).append("=?");
        argsList.add(timeRangesJson);

        if (excludeId != -1) {
            sb.append(" AND ").append(COL_ID).append("!=?");
            argsList.add(String.valueOf(excludeId));
        }

        String[] selectionArgs = argsList.toArray(new String[0]);
        Cursor cursor = db.query(TABLE_RULES, new String[]{COL_ID},
                sb.toString(), selectionArgs, null, null, null);
        boolean duplicate = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return duplicate;
    }

    // ============ 拦截记录 CRUD ============

    public long insertBlockedCall(BlockedCall call) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BC_PHONE, call.getPhoneNumber());
        values.put(COL_BC_TIMESTAMP, call.getTimestamp());
        values.put(COL_BC_RULE_LABEL, call.getRuleLabel());
        values.put(COL_BC_MATCH_TYPE, call.getMatchType());
        return db.insert(TABLE_BLOCKED_CALLS, null, values);
    }

    public List<BlockedCall> getAllBlockedCalls() {
        List<BlockedCall> calls = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BLOCKED_CALLS, null, null, null, null, null,
                COL_BC_TIMESTAMP + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                calls.add(cursorToBlockedCall(cursor));
            }
            cursor.close();
        }
        return calls;
    }

    public int getBlockedCallCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BLOCKED_CALLS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int deleteBlockedCall(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_BLOCKED_CALLS, COL_BC_ID + "=?", new String[]{String.valueOf(id)});
    }

    public int deleteAllBlockedCalls() {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_BLOCKED_CALLS, null, null);
    }

    private BlockedCall cursorToBlockedCall(Cursor cursor) {
        BlockedCall call = new BlockedCall();
        call.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_BC_ID)));
        call.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(COL_BC_PHONE)));
        call.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COL_BC_TIMESTAMP)));
        call.setRuleLabel(cursor.getString(cursor.getColumnIndexOrThrow(COL_BC_RULE_LABEL)));
        call.setMatchType(cursor.getInt(cursor.getColumnIndexOrThrow(COL_BC_MATCH_TYPE)));
        return call;
    }

    // ============ JSON 序列化工具 ============

    public static String serializeTimeRanges(List<TimeRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return "[]";
        try {
            JSONArray arr = new JSONArray();
            for (TimeRange r : ranges) {
                arr.put(r.toJson());
            }
            return arr.toString();
        } catch (JSONException e) {
            return "[]";
        }
    }

    public static List<TimeRange> deserializeTimeRanges(String json) {
        List<TimeRange> ranges = new ArrayList<>();
        if (json == null || json.isEmpty() || "null".equals(json)) return ranges;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                ranges.add(TimeRange.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // return empty list
        }
        return ranges;
    }
}
