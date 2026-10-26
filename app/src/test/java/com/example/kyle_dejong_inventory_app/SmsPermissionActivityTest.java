package com.example.kyle_dejong_inventory_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SmsPermissionActivityTest {
    private Context context;
    private InventoryDatabaseHelper helper;
    private long userId;
    private ActivityController<SmsPermissionActivity> controller;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("warehouse_inventory.db");
        helper = new InventoryDatabaseHelper(context);
        userId = helper.createUser("demo-user", "demo password");
    }

    private ActivityController<SmsPermissionActivity> createController() {
        return Robolectric.buildActivity(SmsPermissionActivity.class,
                new Intent(context, SmsPermissionActivity.class).putExtra("USER_ID", userId));
    }

    @After public void tearDown() {
        if (controller != null) controller.pause().stop().destroy();
        helper.close();
        context.deleteDatabase("warehouse_inventory.db");
    }

    @Test public void disableButtonTurnsOffAlertsWithoutDeletingDestination() {
        helper.updateSmsSettings(userId, "+12025550123", true);
        controller = createController().setup();
        controller.get().findViewById(R.id.buttonDisableSms).performClick();
        assertFalse(helper.isSmsEnabled(userId));
        assertEquals("+12025550123", helper.getPhoneNumber(userId));
    }

    @Test public void pendingPhoneSurvivesRecreationBeforePermissionResult() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.SEND_SMS);
        controller = createController().setup();
        ((EditText) controller.get().findViewById(R.id.editPhoneNumber)).setText("+12025550123");
        controller.get().findViewById(R.id.buttonEnableSms).performClick();
        Bundle state = new Bundle();
        controller.saveInstanceState(state).pause().stop().destroy();
        controller = createController().create(state).start().restoreInstanceState(state).resume().visible();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.SEND_SMS);
        controller.get().onRequestPermissionsResult(100,
                new String[]{Manifest.permission.SEND_SMS}, new int[]{PackageManager.PERMISSION_GRANTED});
        assertEquals("+12025550123", helper.getPhoneNumber(userId));
        assertTrue(helper.isSmsEnabled(userId));
    }

    @Test public void deniedSmsDoesNotBlockInventoryAndCancelledPromptKeepsSettings() {
        helper.updateSmsSettings(userId, "+12025550123", true);
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.SEND_SMS);
        assertFalse(SmsNotificationHelper.sendZeroStockAlert(context, helper, userId, "Bolts"));
        assertTrue(helper.addInventoryItem("Bolts", 0, "A1") > 0);
        controller = createController().setup();
        controller.get().onRequestPermissionsResult(100, new String[]{Manifest.permission.SEND_SMS}, new int[]{});
        assertEquals("+12025550123", helper.getPhoneNumber(userId));
        assertTrue(helper.isSmsEnabled(userId));
    }
}
