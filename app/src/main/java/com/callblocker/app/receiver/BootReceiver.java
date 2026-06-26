package com.callblocker.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.callblocker.app.db.RuleDatabase;
import com.callblocker.app.model.BlockRule;
import com.callblocker.app.service.CallBlockService;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            RuleDatabase db = RuleDatabase.getInstance(context);
            List<BlockRule> enabledRules = db.getEnabledRules();
            if (!enabledRules.isEmpty()) {
                CallBlockService.startServiceWithState(context);
            }
        }
    }
}
