package com.example.kyle_dejong_inventory_app;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private EditText editUsername;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonCreateAccount;
    private InventoryDatabaseHelper dbHelper;
    private final ExecutorService accountWorker = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        dbHelper = new InventoryDatabaseHelper(getApplicationContext());
        buttonCreateAccount.setOnClickListener(v -> submitAccount(true));
        buttonLogin.setOnClickListener(v -> submitAccount(false));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void submitAccount(boolean create) {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();
        if (username.isEmpty()) {
            editUsername.setError(getString(R.string.username_required));
            editUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            editPassword.setError(getString(R.string.password_required));
            editPassword.requestFocus();
            return;
        }
        setAccountButtonsEnabled(false);
        // Password derivation and database upgrades must not block the UI thread.
        accountWorker.execute(() -> {
            long result;
            boolean storageError = false;
            try {
                if (create && dbHelper.usernameExists(username)) {
                    result = -2;
                } else {
                    result = create ? dbHelper.createUser(username, password)
                            : dbHelper.validateUser(username, password);
                }
            } catch (SQLiteException | IllegalStateException e) {
                result = -1;
                storageError = true;
            }
            final long userId = result;
            final boolean failedStorage = storageError;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setAccountButtonsEnabled(true);
                if (userId == -2) {
                    editUsername.setError(getString(R.string.username_exists));
                } else if (userId == -1) {
                    int message = failedStorage ? R.string.account_storage_failed
                            : create ? R.string.account_creation_failed : R.string.invalid_login;
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                } else if (create) {
                    Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
                } else {
                    editPassword.setText("");
                    Intent intent = new Intent(this, InventoryActivity.class);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                }
            });
        });
    }

    private void setAccountButtonsEnabled(boolean enabled) {
        buttonLogin.setEnabled(enabled);
        buttonCreateAccount.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        // Close only after any already-running database work has finished.
        accountWorker.execute(dbHelper::close);
        accountWorker.shutdown();
        super.onDestroy();
    }
}
