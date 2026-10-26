package com.example.kyle_dejong_inventory_app;

import android.content.Intent;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityTest {
    @Test public void accountCreationAndLoginWorkThroughTheScreen() throws Exception {
        RuntimeEnvironment.getApplication().deleteDatabase("warehouse_inventory.db");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            EditText username = activity.findViewById(R.id.editUsername);
            EditText password = activity.findViewById(R.id.editPassword);
            Button login = activity.findViewById(R.id.buttonLogin);
            username.setText("demo-user");
            password.setText("demo password");
            activity.findViewById(R.id.buttonCreateAccount).performClick();
            awaitAccountWork(login);
            password.setText("wrong password");
            login.performClick();
            awaitAccountWork(login);
            assertNull(shadowOf(activity).getNextStartedActivity());
            password.setText("demo password");
            login.performClick();
            awaitAccountWork(login);
            Intent destination = shadowOf(activity).getNextStartedActivity();
            assertNotNull(destination);
            assertEquals(InventoryActivity.class.getName(), destination.getComponent().getClassName());
            assertTrue(destination.getLongExtra("USER_ID", -1) > 0);
            assertEquals("", password.getText().toString());
        } finally {
            RuntimeEnvironment.getApplication().deleteDatabase("warehouse_inventory.db");
        }
    }

    private void awaitAccountWork(Button login) throws InterruptedException {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!login.isEnabled() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("Account operation did not finish", login.isEnabled());
    }
}
