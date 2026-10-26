package com.example.kyle_dejong_inventory_app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InventoryDatabaseHelperTest {
    private Context context;
    private InventoryDatabaseHelper helper;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("warehouse_inventory.db");
        helper = new InventoryDatabaseHelper(context);
    }

    @After public void tearDown() {
        helper.close();
        context.deleteDatabase("warehouse_inventory.db");
    }

    @Test public void accountsPersistAndPasswordsAreNotStoredAsText() {
        long id = helper.createUser("demo-user", "demo password");
        assertTrue(id > 0);
        helper.close();
        helper = new InventoryDatabaseHelper(context);
        assertTrue(helper.usernameExists("demo-user"));
        assertEquals(id, helper.validateUser("demo-user", "demo password"));
        assertEquals(-1, helper.validateUser("demo-user", "wrong"));
        assertEquals(-1, helper.validateUser("' OR 1=1 --", "demo password"));
        assertEquals(-1, helper.createUser("demo-user", "another password"));
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT password_hash FROM users", null)) {
            assertTrue(cursor.moveToFirst());
            assertNotEquals("demo password", cursor.getString(0));
            assertTrue(PasswordHasher.verify("demo password", cursor.getString(0)));
        }
    }

    @Test public void crudPersistsAndUsesIdsForDuplicateVisibleNames() {
        long first = helper.addInventoryItem("Bolts", 8, "A1");
        long second = helper.addInventoryItem("Bolts", 4, "B2");
        assertTrue(helper.updateInventoryItem(new InventoryItem(first, "Bolts", 0, "A1")));
        helper.close();
        helper = new InventoryDatabaseHelper(context);
        List<InventoryItem> items = helper.getAllInventoryItems();
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> i.getId() == first && i.getQuantity() == 0));
        assertTrue(items.stream().anyMatch(i -> i.getId() == second && i.getQuantity() == 4));
        assertTrue(helper.deleteInventoryItem(first));
        assertFalse(helper.deleteInventoryItem(first));
        assertEquals(second, helper.getAllInventoryItems().get(0).getId());
    }

    @Test public void negativeQuantitiesCannotChangeStoredInventory() {
        assertEquals(-1, helper.addInventoryItem("Bolts", -1, "A1"));
        long id = helper.addInventoryItem("Bolts", 3, "A1");
        assertFalse(helper.updateInventoryItem(new InventoryItem(id, "Bolts", -1, "A1")));
        assertEquals(3, helper.getAllInventoryItems().get(0).getQuantity());
    }

    @Test public void searchMatchesNamesAndLocationsWithoutSqlInjection() {
        helper.addInventoryItem("Bolts", 1, "A1");
        helper.addInventoryItem("Washers", 0, "B2");
        assertEquals("Bolts", helper.searchInventoryItems("bolt").get(0).getName());
        assertEquals("Washers", helper.searchInventoryItems("B2").get(0).getName());
        assertTrue(helper.searchInventoryItems("' OR 1=1 --").isEmpty());
    }

    @Test public void smsPreferencesRemainAccountSpecific() {
        long a = helper.createUser("alice", "demo password");
        long b = helper.createUser("bob", "demo password");
        assertTrue(helper.updateSmsSettings(a, "+12025550123", true));
        assertTrue(helper.isSmsEnabled(a));
        assertFalse(helper.isSmsEnabled(b));
        assertEquals("", helper.getPhoneNumber(b));
        assertTrue(helper.updateSmsSettings(a, "+12025550123", false));
        assertFalse(helper.isSmsEnabled(a));
        assertFalse(helper.updateSmsSettings(-1, "", true));
    }

    @Test public void migratesVersionTwoWithoutLosingAccountsStockOrPreferences() {
        helper.close();
        try (SQLiteDatabase db = context.openOrCreateDatabase("warehouse_inventory.db", 0, null)) {
            db.execSQL("CREATE TABLE users (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, "
                    + "phone_number TEXT, sms_enabled INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE inventory (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "item_name TEXT NOT NULL, quantity INTEGER NOT NULL DEFAULT 0, location TEXT NOT NULL)");
            db.execSQL("INSERT INTO users VALUES (7, 'legacy', 'old demo password', '+12025550123', 1)");
            db.execSQL("INSERT INTO inventory VALUES (12, 'Bolts', 9, 'A1')");
            db.setVersion(2);
        }
        helper = new InventoryDatabaseHelper(context);
        assertEquals(7, helper.validateUser("legacy", "old demo password"));
        assertEquals(-1, helper.validateUser("legacy", "wrong"));
        assertTrue(helper.isSmsEnabled(7));
        assertEquals("+12025550123", helper.getPhoneNumber(7));
        assertEquals(12, helper.getAllInventoryItems().get(0).getId());
        assertEquals(9, helper.getAllInventoryItems().get(0).getQuantity());
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT * FROM users", null)) {
            assertEquals(-1, cursor.getColumnIndex("password"));
            assertTrue(cursor.moveToFirst());
            String stored = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
            assertNotEquals("old demo password", stored);
            assertTrue(PasswordHasher.verify("old demo password", stored));
        }
    }
}
