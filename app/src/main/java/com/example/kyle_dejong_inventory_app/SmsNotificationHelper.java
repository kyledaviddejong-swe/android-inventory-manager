package com.example.kyle_dejong_inventory_app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import androidx.core.content.ContextCompat;

public final class SmsNotificationHelper {

    private static final String TAG = "SmsNotificationHelper";

    public static boolean sendZeroStockAlert(
            Context context,
            InventoryDatabaseHelper dbHelper,
            long userId,
            String itemName) {

        // App preference and Android permission are separate checks. Denying either
        // disables only SMS alerts and never blocks the inventory workflow.
        if (!dbHelper.isSmsEnabled(userId)) {
            return false;
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        if (!context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_MESSAGING
        )) {
            return false;
        }

        String phoneNumber = dbHelper.getPhoneNumber(userId);
        if (phoneNumber.isEmpty()) {
            return false;
        }

        SmsManager smsManager = context.getSystemService(SmsManager.class);
        if (smsManager == null) {
            return false;
        }

        String message = context.getString(
                R.string.zero_stock_sms_message,
                itemName
        );

        try {
            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            // Submission to Android is not confirmation of carrier delivery.
            return true;
        } catch (SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
            Log.e(TAG, "Unable to send zero-stock SMS alert", e);
            return false;
        }
    }

    private SmsNotificationHelper() {
        // Utility class; no instances needed.
    }
}
