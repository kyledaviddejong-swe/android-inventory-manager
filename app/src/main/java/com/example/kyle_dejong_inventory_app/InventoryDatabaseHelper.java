package com.example.kyle_dejong_inventory_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class InventoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "warehouse_inventory.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_INVENTORY = "inventory";

    public InventoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // User credentials and SMS preferences are stored separately from inventory rows.
        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT UNIQUE NOT NULL, " +
                        "password_hash TEXT NOT NULL, " +
                        "phone_number TEXT, " +
                        "sms_enabled INTEGER NOT NULL DEFAULT 0" +
                        ")";

        String createInventoryTable =
                "CREATE TABLE " + TABLE_INVENTORY + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "item_name TEXT NOT NULL, " +
                        "quantity INTEGER NOT NULL DEFAULT 0, " +
                        "location TEXT NOT NULL" +
                        ")";

        db.execSQL(createUsersTable);
        db.execSQL(createInventoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Version 2 adds SMS settings without destroying existing users or inventory.
        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE " + TABLE_USERS +
                            " ADD COLUMN phone_number TEXT"
            );
            db.execSQL(
                    "ALTER TABLE " + TABLE_USERS +
                            " ADD COLUMN sms_enabled INTEGER NOT NULL DEFAULT 0"
            );
        }
        // Preserve the submitted app's IDs, preferences and inventory. The helper
        // runs upgrades in a transaction. Login never accepts plaintext credentials.
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_USERS
                    + " RENAME COLUMN password TO password_hash");
            try (Cursor cursor = db.query(TABLE_USERS,
                    new String[]{"_id", "password_hash"},
                    null, null, null, null, null)) {
                while (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("password_hash", PasswordHasher.hash(cursor.getString(1)));
                    db.update(TABLE_USERS, values, "_id = ?",
                            new String[]{String.valueOf(cursor.getLong(0))});
                }
            }
        }
    }

    public long createUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password_hash", PasswordHasher.hash(password));
        return db.insert(TABLE_USERS, null, values);
    }

    public boolean usernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{"_id"},
                "username = ?",
                new String[]{username},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst();
        }
    }

    public long validateUser(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();

        // Selection arguments keep user input separate from the SQL statement itself.
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{"_id", "password_hash"},
                "username = ?",
                new String[]{username},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst() && PasswordHasher.verify(password,
                    cursor.getString(cursor.getColumnIndexOrThrow("password_hash")))) {
                return cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            }
            return -1;
        }
    }

    public List<InventoryItem> getAllInventoryItems() {
        SQLiteDatabase db = getReadableDatabase();
        List<InventoryItem> inventoryItems = new ArrayList<>();

        try (Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{"_id", "item_name", "quantity", "location"},
                null,
                null,
                null,
                null,
                "item_name ASC"
        )) {
            while (cursor.moveToNext()) {
                inventoryItems.add(readInventoryItem(cursor));
            }
        }

        return inventoryItems;
    }

    public long addInventoryItem(String name, int quantity, String location) {
        if (quantity < 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("item_name", name);
        values.put("quantity", quantity);
        values.put("location", location);
        return db.insert(TABLE_INVENTORY, null, values);
    }

    public boolean updateInventoryItem(InventoryItem item) {
        if (item.getQuantity() < 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("item_name", item.getName());
        values.put("quantity", item.getQuantity());
        values.put("location", item.getLocation());

        // Internal row IDs make update/delete unambiguous even when visible values repeat.
        int rowsUpdated = db.update(
                TABLE_INVENTORY,
                values,
                "_id = ?",
                new String[]{String.valueOf(item.getId())}
        );
        return rowsUpdated > 0;
    }

    public boolean deleteInventoryItem(long itemId) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted = db.delete(
                TABLE_INVENTORY,
                "_id = ?",
                new String[]{String.valueOf(itemId)}
        );
        return rowsDeleted > 0;
    }

    public boolean updateSmsSettings(long userId, String phoneNumber, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone_number", phoneNumber);
        values.put("sms_enabled", enabled ? 1 : 0);

        int rowsUpdated = db.update(
                TABLE_USERS,
                values,
                "_id = ?",
                new String[]{String.valueOf(userId)}
        );
        return rowsUpdated > 0;
    }

    public String getPhoneNumber(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{"phone_number"},
                "_id = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                String phoneNumber = cursor.getString(
                        cursor.getColumnIndexOrThrow("phone_number")
                );
                return phoneNumber == null ? "" : phoneNumber;
            }
        }
        return "";
    }

    public boolean isSmsEnabled(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{"sms_enabled"},
                "_id = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(
                        cursor.getColumnIndexOrThrow("sms_enabled")
                ) == 1;
            }
        }
        return false;
    }

    public List<InventoryItem> searchInventoryItems(String query) {
        SQLiteDatabase db = getReadableDatabase();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        String searchValue = "%" + query + "%";

        // Search reuses the same grid and matches either the item name or warehouse location.
        try (Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{"_id", "item_name", "quantity", "location"},
                "item_name LIKE ? OR location LIKE ?",
                new String[]{searchValue, searchValue},
                null,
                null,
                "item_name ASC"
        )) {
            while (cursor.moveToNext()) {
                inventoryItems.add(readInventoryItem(cursor));
            }
        }

        return inventoryItems;
    }

    // Centralizing row conversion avoids duplicating cursor parsing in every query method.
    private InventoryItem readInventoryItem(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
        int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
        String location = cursor.getString(cursor.getColumnIndexOrThrow("location"));
        return new InventoryItem(id, name, quantity, location);
    }
}
