package com.callblocker.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.callblocker.app.db.RuleDatabase;
import com.callblocker.app.model.BlockedCall;
import com.callblocker.app.model.BlockRule;
import com.callblocker.app.service.CallBlockService;
import com.callblocker.app.service.HybridCallBlocker;

import java.util.Calendar;
import java.util.List;

public class CallBlockReceiver extends BroadcastReceiver {

    private static final String TAG = "CallBlockReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) return;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (incomingNumber == null) incomingNumber = "";

            Log.d(TAG, "来电振铃: " + incomingNumber);

            BlockRule matchedRule = findMatchedRule(context, incomingNumber);
            if (matchedRule != null) {
                boolean blocked = HybridCallBlocker.blockCall(context);
                if (blocked) {
                    Log.i(TAG, "来电已成功拦截");
                    // 记录拦截事件
                    recordBlockedCall(context, incomingNumber, matchedRule);
                } else {
                    Log.e(TAG, "来电拦截失败，所有拦截方法均不可用");
                }
            }
        }
    }

    private BlockRule findMatchedRule(Context context, String incomingNumber) {
        if (!CallBlockService.isServiceRunning()) return null;

        RuleDatabase db = RuleDatabase.getInstance(context);
        List<BlockRule> enabledRules = db.getEnabledRules();

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        for (BlockRule rule : enabledRules) {
            if (rule.shouldBlock(incomingNumber, currentHour, currentMinute)) {
                Log.d(TAG, "匹配拦截规则: " + rule.getMatchTypeDescription());
                return rule;
            }
        }
        return null;
    }

    private void recordBlockedCall(Context context, String incomingNumber, BlockRule matchedRule) {
        try {
            BlockedCall call = new BlockedCall(
                    incomingNumber,
                    System.currentTimeMillis(),
                    matchedRule.getLabel(),
                    matchedRule.getMatchType()
            );
            RuleDatabase db = RuleDatabase.getInstance(context);
            db.insertBlockedCall(call);
            Log.i(TAG, "拦截记录已保存");
        } catch (Exception e) {
            Log.e(TAG, "保存拦截记录失败", e);
        }
    }
}
