package com.example.kyle_dejong_inventory_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView recyclerInventory;
    private InventoryDatabaseHelper dbHelper;

    private EditText editSearch;

    private long userId;

    private boolean showZeroOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory);

        // The logged-in user's database ID keeps SMS settings tied to the correct account.
        userId = getIntent().getLongExtra("USER_ID", -1);

        recyclerInventory = findViewById(R.id.recyclerInventory);
        recyclerInventory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        dbHelper = new InventoryDatabaseHelper(this);

        editSearch = findViewById(R.id.editSearch);

        Button buttonAddItem =
                findViewById(R.id.buttonAddItem);

        Button buttonSmsAlerts =
                findViewById(R.id.buttonSmsAlerts);

        Button buttonZeroStock =
                findViewById(R.id.buttonZeroStock);

        buttonAddItem.setOnClickListener(
                v -> showAddItemDialog()
        );

        buttonSmsAlerts.setOnClickListener(v -> {

            Intent intent = new Intent(
                    InventoryActivity.this,
                    SmsPermissionActivity.class
            );

            intent.putExtra("USER_ID", userId);

            startActivity(intent);
        });

        buttonZeroStock.setOnClickListener(v -> {

            showZeroOnly = !showZeroOnly;

            refreshInventory(
                    editSearch.getText().toString()
            );
        });

        editSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        refreshInventory(
                                s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );

        refreshInventory();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void refreshInventory() {
        refreshInventory("");
    }

    // Reload the grid from SQLite so the UI always reflects the persistent database state.
    // The current search text and zero-stock filter are reapplied after every CRUD operation.
    private void refreshInventory(String query) {

        List<InventoryItem> allInventoryItems =
                dbHelper.getAllInventoryItems();

        int zeroStockCount = 0;

        for (InventoryItem item : allInventoryItems) {

            if (item.getQuantity() == 0) {
                zeroStockCount++;
            }
        }

        if (showZeroOnly && zeroStockCount == 0) {
            showZeroOnly = false;
        }

        List<InventoryItem> inventoryItems;

        if (query.trim().isEmpty()) {

            inventoryItems = allInventoryItems;

        } else {

            inventoryItems =
                    dbHelper.searchInventoryItems(
                            query.trim()
                    );
        }

        if (showZeroOnly) {

            inventoryItems.removeIf(
                    item -> item.getQuantity() != 0
            );
        }

        Button buttonZeroStock =
                findViewById(R.id.buttonZeroStock);

        if (showZeroOnly) {

            buttonZeroStock.setText(
                    R.string.show_all_items
            );

            buttonZeroStock.setEnabled(true);

        } else if (zeroStockCount == 0) {

            buttonZeroStock.setText(
                    R.string.no_items_out_of_stock
            );

            buttonZeroStock.setEnabled(false);

        } else {

            buttonZeroStock.setText(
                    getString(
                            R.string.show_out_of_stock_items,
                            zeroStockCount
                    )
            );

            buttonZeroStock.setEnabled(true);
        }

        InventoryAdapter inventoryAdapter =
                new InventoryAdapter(
                        inventoryItems,
                        new InventoryAdapter.OnInventoryItemActionListener() {

                            @Override
                            public void onEditItem(
                                    InventoryItem item) {

                                showEditItemDialog(item);
                            }

                            @Override
                            public void onDeleteItem(
                                    InventoryItem item) {

                                confirmDeleteItem(item);
                            }
                        }
                );

        recyclerInventory.setAdapter(
                inventoryAdapter
        );
    }

    private void showAddItemDialog() {

        View dialogView = getLayoutInflater()
                .inflate(
                        R.layout.dialog_inventory_item,
                        null
                );

        EditText editItemName =
                dialogView.findViewById(
                        R.id.editItemName
                );

        EditText editQuantity =
                dialogView.findViewById(
                        R.id.editQuantity
                );

        EditText editLocation =
                dialogView.findViewById(
                        R.id.editLocation
                );

        Button buttonCancel =
                dialogView.findViewById(
                        R.id.buttonCancelItem
                );

        Button buttonSave =
                dialogView.findViewById(
                        R.id.buttonSaveItem
                );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        buttonCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        buttonSave.setOnClickListener(v -> {

            String name =
                    editItemName
                            .getText()
                            .toString()
                            .trim();

            String quantityText =
                    editQuantity
                            .getText()
                            .toString()
                            .trim();

            String location =
                    editLocation
                            .getText()
                            .toString()
                            .trim();

            if (name.isEmpty()) {

                editItemName.setError(
                        getString(
                                R.string.item_name_required
                        )
                );

                editItemName.requestFocus();
                return;
            }

            if (quantityText.isEmpty()) {

                editQuantity.setError(
                        getString(
                                R.string.quantity_required
                        )
                );

                editQuantity.requestFocus();
                return;
            }

            if (location.isEmpty()) {

                editLocation.setError(
                        getString(
                                R.string.location_required
                        )
                );

                editLocation.requestFocus();
                return;
            }

            int quantity;

            try {

                quantity =
                        Integer.parseInt(
                                quantityText
                        );

            } catch (NumberFormatException e) {

                editQuantity.setError(
                        getString(
                                R.string.quantity_invalid
                        )
                );

                editQuantity.requestFocus();
                return;
            }

            if (quantity < 0) {
                editQuantity.setError(
                        getString(R.string.quantity_nonnegative)
                );
                editQuantity.requestFocus();
                return;
            }

            long itemId =
                    dbHelper.addInventoryItem(
                            name,
                            quantity,
                            location
                    );

            if (itemId != -1) {

                // A newly created zero-stock item qualifies for an immediate alert.
                if (quantity == 0) {

                    SmsNotificationHelper
                            .sendZeroStockAlert(
                                    this,
                                    dbHelper,
                                    userId,
                                    name
                            );
                }

                Toast.makeText(
                        this,
                        R.string.item_added,
                        Toast.LENGTH_SHORT
                ).show();

                refreshInventory(
                        editSearch.getText().toString()
                );

                dialog.dismiss();

            } else {

                Toast.makeText(
                        this,
                        R.string.item_add_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        dialog.show();
    }

    private void showEditItemDialog(
            InventoryItem item) {

        View dialogView = getLayoutInflater()
                .inflate(
                        R.layout.dialog_inventory_item,
                        null
                );

        TextView textTitle =
                dialogView.findViewById(
                        R.id.textItemDialogTitle
                );

        EditText editItemName =
                dialogView.findViewById(
                        R.id.editItemName
                );

        EditText editQuantity =
                dialogView.findViewById(
                        R.id.editQuantity
                );

        EditText editLocation =
                dialogView.findViewById(
                        R.id.editLocation
                );

        Button buttonCancel =
                dialogView.findViewById(
                        R.id.buttonCancelItem
                );

        Button buttonSave =
                dialogView.findViewById(
                        R.id.buttonSaveItem
                );

        textTitle.setText(
                R.string.edit_inventory_item
        );

        editItemName.setText(
                item.getName()
        );

        editQuantity.setText(
                String.valueOf(
                        item.getQuantity()
                )
        );

        editLocation.setText(
                item.getLocation()
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        buttonCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        buttonSave.setOnClickListener(v -> {

            String name =
                    editItemName
                            .getText()
                            .toString()
                            .trim();

            String quantityText =
                    editQuantity
                            .getText()
                            .toString()
                            .trim();

            String location =
                    editLocation
                            .getText()
                            .toString()
                            .trim();

            if (name.isEmpty()
                    || quantityText.isEmpty()
                    || location.isEmpty()) {

                Toast.makeText(
                        this,
                        R.string.complete_all_fields,
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            int quantity;

            try {

                quantity =
                        Integer.parseInt(
                                quantityText
                        );

            } catch (NumberFormatException e) {

                editQuantity.setError(
                        getString(
                                R.string.quantity_invalid
                        )
                );

                editQuantity.requestFocus();
                return;
            }

            if (quantity < 0) {
                editQuantity.setError(
                        getString(R.string.quantity_nonnegative)
                );
                editQuantity.requestFocus();
                return;
            }

            // Keep the prior quantity so SMS is sent only on a real transition to zero.
            int oldQuantity =
                    item.getQuantity();

            InventoryItem updatedItem =
                    new InventoryItem(
                            item.getId(),
                            name,
                            quantity,
                            location
                    );

            if (dbHelper.updateInventoryItem(
                    updatedItem)) {

                // Do not resend an alert merely because an already-empty item was edited.
                if (oldQuantity > 0
                        && quantity == 0) {

                    SmsNotificationHelper
                            .sendZeroStockAlert(
                                    this,
                                    dbHelper,
                                    userId,
                                    name
                            );
                }

                refreshInventory(
                        editSearch.getText().toString()
                );

                dialog.dismiss();

            } else {

                Toast.makeText(
                        this,
                        R.string.item_update_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        dialog.show();
    }

    private void confirmDeleteItem(
            InventoryItem item) {

        new AlertDialog.Builder(this)
                .setTitle(
                        R.string.delete_inventory_item
                )
                .setMessage(
                        getString(
                                R.string.delete_item_confirmation,
                                item.getName()
                        )
                )
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) -> {

                            if (dbHelper
                                    .deleteInventoryItem(
                                            item.getId()
                                    )) {

                                refreshInventory(
                                        editSearch
                                                .getText()
                                                .toString()
                                );

                            } else {

                                Toast.makeText(
                                        this,
                                        R.string.item_delete_failed,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
    }
}