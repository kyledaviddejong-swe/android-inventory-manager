package com.example.kyle_dejong_inventory_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SmsPermissionActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST = 100;

    private InventoryDatabaseHelper dbHelper;
    private long userId;

    private EditText editPhoneNumber;
    private TextView textSmsStatus;

    private String pendingPhoneNumber;
    private Button buttonDisableSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sms_permission);

        userId = getIntent().getLongExtra("USER_ID", -1);
        if (userId < 0) {
            finish();
            return;
        }
        if (savedInstanceState != null) {
            pendingPhoneNumber = savedInstanceState.getString("pendingPhoneNumber");
        }

        dbHelper = new InventoryDatabaseHelper(this);

        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        textSmsStatus = findViewById(R.id.textSmsStatus);

        Button buttonEnableSms = findViewById(R.id.buttonEnableSms);
        Button buttonNotNow = findViewById(R.id.buttonNotNow);
        buttonDisableSms = findViewById(R.id.buttonDisableSms);
        buttonDisableSms.setOnClickListener(v ->
                saveSmsSettings(dbHelper.getPhoneNumber(userId), false));

        // Restore the user's saved SMS destination.
        String savedPhoneNumber = dbHelper.getPhoneNumber(userId);
        editPhoneNumber.setText(savedPhoneNumber);

        updateStatusDisplay();

        buttonEnableSms.setOnClickListener(v -> enableSmsAlerts());

        // Leave existing SMS settings unchanged and return to Inventory.
        buttonNotNow.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
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

    private void enableSmsAlerts() {

        String phoneNumber =
                editPhoneNumber.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            editPhoneNumber.setError(
                    getString(R.string.phone_number_required)
            );
            editPhoneNumber.requestFocus();
            return;
        }

        pendingPhoneNumber = phoneNumber;

        // Android permission controls whether sending is allowed; the saved user setting
        // separately records whether this account wants zero-stock alerts enabled.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED) {

            saveSmsSettings(phoneNumber, true);

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST
            );
        }
    }

    private void saveSmsSettings(
            String phoneNumber,
            boolean enabled) {

        if (!dbHelper.updateSmsSettings(userId, phoneNumber, enabled)) {
            Toast.makeText(this, R.string.sms_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        updateStatusDisplay();
    }

    private void updateStatusDisplay() {

        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED;

        boolean alertsEnabled =
                dbHelper.isSmsEnabled(userId);

        buttonDisableSms.setEnabled(alertsEnabled);
        boolean messagingAvailable = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_MESSAGING);

        if (!messagingAvailable) {
            textSmsStatus.setText(R.string.sms_unavailable);
            textSmsStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else if (permissionGranted && alertsEnabled) {

            textSmsStatus.setText(
                    R.string.sms_alerts_enabled
            );

            textSmsStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            android.R.color.holo_green_dark
                    )
            );

        } else if (permissionGranted) {

            textSmsStatus.setText(
                    R.string.sms_permission_granted
            );

            textSmsStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            android.R.color.holo_orange_dark
                    )
            );

        } else {

            textSmsStatus.setText(
                    R.string.sms_disabled
            );

            textSmsStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            android.R.color.holo_red_dark
                    )
            );
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("pendingPhoneNumber", pendingPhoneNumber);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null) {
            updateStatusDisplay();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == SMS_PERMISSION_REQUEST) {
            // An interrupted prompt must not erase previously saved settings.
            if (grantResults.length == 0 || pendingPhoneNumber == null
                    || pendingPhoneNumber.isEmpty()) {
                updateStatusDisplay();
                return;
            }
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                saveSmsSettings(
                        pendingPhoneNumber,
                        true
                );

            } else {

                saveSmsSettings(
                        pendingPhoneNumber,
                        false
                );

                textSmsStatus.setText(
                        R.string.sms_denied
                );

                textSmsStatus.setTextColor(
                        ContextCompat.getColor(
                                this,
                                android.R.color.holo_red_dark
                        )
                );
            }
        }
    }
}