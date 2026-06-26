package com.callblocker.app.service;

import android.content.Context;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

@SuppressWarnings("deprecation")
public class HybridCallBlocker {

    private static final String TAG = "HybridCallBlocker";

    public static boolean blockCall(Context context) {
        if (trySystemApi(context)) {
            Log.i(TAG, "拦截成功：系统 API (TelecomManager.endCall)");
            return true;
        }
        if (tryReflectionEndCall(context)) {
            Log.i(TAG, "拦截成功：反射 ITelephony.endCall");
            return true;
        }
        if (tryTelephonyManagerEndCall(context)) {
            Log.i(TAG, "拦截成功：反射 TelephonyManager.endCall");
            return true;
        }
        Log.e(TAG, "所有拦截方法均失败，来电未被拦截");
        return false;
    }

    private static boolean trySystemApi(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (telecomManager != null && telecomManager.isInCall()) {
                    telecomManager.endCall();
                    return true;
                }
            } catch (SecurityException e) {
                Log.w(TAG, "TelecomManager.endCall 权限不足: " + e.getMessage());
            } catch (Exception e) {
                Log.w(TAG, "TelecomManager.endCall 失败: " + e.getMessage());
            }
        }
        return false;
    }

    private static boolean tryReflectionEndCall(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Method getTelephony = TelephonyManager.class.getDeclaredMethod("getTelephony");
                getTelephony.setAccessible(true);
                Object telephony = getTelephony.invoke(telephonyManager);
                if (telephony != null) {
                    Method endCall = telephony.getClass().getDeclaredMethod("endCall");
                    endCall.setAccessible(true);
                    endCall.invoke(telephony);
                    return true;
                }
            } else {
                Method getITelephony = TelephonyManager.class.getDeclaredMethod("getITelephony");
                getITelephony.setAccessible(true);
                Object iTelephony = getITelephony.invoke(telephonyManager);
                if (iTelephony != null) {
                    Method endCall = iTelephony.getClass().getDeclaredMethod("endCall");
                    endCall.setAccessible(true);
                    endCall.invoke(iTelephony);
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "反射 ITelephony 权限不足: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "反射 ITelephony 失败: " + e.getMessage());
        }
        return false;
    }

    private static boolean tryTelephonyManagerEndCall(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager == null) return false;

                Method endCall = TelephonyManager.class.getDeclaredMethod("endCall");
                endCall.setAccessible(true);
                endCall.invoke(telephonyManager);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "TelephonyManager.endCall 反射失败: " + e.getMessage());
            }
        }
        return false;
    }

    public static String getBlockCapability(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (telecomManager != null) {
                    String defaultDialer = telecomManager.getDefaultDialerPackage();
                    if (context.getPackageName().equals(defaultDialer)) {
                        return "系统级拦截（默认拨号应用）";
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return "应用层拦截（反射方式，部分设备可能受限）";
        }

        return "应用层拦截（反射方式）";
    }
}
