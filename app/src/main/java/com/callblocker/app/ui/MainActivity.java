package com.callblocker.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.callblocker.app.R;
import com.callblocker.app.db.RuleDatabase;
import com.callblocker.app.model.BlockRule;
import com.callblocker.app.service.CallBlockService;
import com.callblocker.app.service.HybridCallBlocker;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RuleDatabase db;
    private RuleAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvServiceStatus;
    private TextView tvCapability;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = RuleDatabase.getInstance(this);

        initViews();
        setupRecyclerView();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CallBlockService.restoreServiceState(this);
        refreshRules();
        updateServiceStatus();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvServiceStatus = findViewById(R.id.tvServiceStatus);
        tvCapability = findViewById(R.id.tvCapability);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddRuleDialog());

        findViewById(R.id.btnToggleService).setOnClickListener(v -> toggleService());

        View btnSetDefault = findViewById(R.id.btnSetDefaultDialer);
        if (btnSetDefault != null) {
            btnSetDefault.setOnClickListener(v -> openDefaultDialerSettings());
        }

        updateCapabilityDisplay();

        // 拦截记录入口
        View tvViewHistory = findViewById(R.id.tvViewHistory);
        if (tvViewHistory != null) {
            tvViewHistory.setOnClickListener(v -> {
                startActivity(new Intent(this, BlockedCallsActivity.class));
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void updateCapabilityDisplay() {
        if (tvCapability != null) {
            String capability = HybridCallBlocker.getBlockCapability(this);
            tvCapability.setText("拦截方式: " + capability);
            View btnSetDefault = findViewById(R.id.btnSetDefaultDialer);
            if (capability.contains("系统级")) {
                tvCapability.setTextColor(ContextCompat.getColor(this, R.color.enabled_green));
                if (btnSetDefault != null) btnSetDefault.setVisibility(View.GONE);
            } else {
                tvCapability.setTextColor(ContextCompat.getColor(this, R.color.primary));
                if (btnSetDefault != null) btnSetDefault.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new RuleAdapter(new RuleAdapter.OnRuleActionListener() {
            @Override
            public void onToggle(BlockRule rule, boolean enabled) {
                db.toggleRule(rule.getId(), enabled);
                rule.setEnabled(enabled);
                updateServiceStatus();
            }

            @Override
            public void onDelete(BlockRule rule) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.confirm_delete)
                        .setMessage(R.string.confirm_delete_msg)
                        .setPositiveButton(R.string.delete_rule, (d, w) -> {
                            db.deleteRule(rule.getId());
                            refreshRules();
                            updateServiceStatus();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onEdit(BlockRule rule) {
                showEditRuleDialog(rule);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void refreshRules() {
        List<BlockRule> rules = db.getAllRules();
        adapter.setRules(rules);

        if (rules.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateServiceStatus() {
        boolean serviceRunning = CallBlockService.isServiceRunning();

        if (serviceRunning) {
            tvServiceStatus.setText(R.string.service_running);
            tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.enabled_green));
        } else {
            tvServiceStatus.setText(R.string.service_stopped);
            tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.disabled_gray));
        }

        android.widget.Button btnToggle = findViewById(R.id.btnToggleService);
        if (serviceRunning) {
            btnToggle.setText("停止拦截");
        } else {
            btnToggle.setText("启动拦截");
        }
    }

    private void toggleService() {
        if (CallBlockService.isServiceRunning()) {
            CallBlockService.stopServiceWithState(this);
        } else {
            List<BlockRule> enabledRules = db.getEnabledRules();
            if (enabledRules.isEmpty()) {
                Toast.makeText(this, "请先添加至少一条启用的规则", Toast.LENGTH_SHORT).show();
                return;
            }
            CallBlockService.startServiceWithState(this);
        }
        updateServiceStatus();
    }

    private void openDefaultDialerSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "请在「电话应用」中管理默认拨号应用", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "请在设置中管理来电拦截相关权限", Toast.LENGTH_LONG).show();
            Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            settingsIntent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(settingsIntent);
        }
    }

    private void showAddRuleDialog() {
        AddRuleDialogFragment dialog = new AddRuleDialogFragment();
        dialog.setOnRuleSavedListener(this::refreshRules);
        dialog.show(getSupportFragmentManager(), "add_rule");
    }

    private void showEditRuleDialog(BlockRule rule) {
        AddRuleDialogFragment dialog = AddRuleDialogFragment.newInstance(rule);
        dialog.setOnRuleSavedListener(this::refreshRules);
        dialog.show(getSupportFragmentManager(), "edit_rule");
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.ANSWER_PHONE_CALLS,
                    Manifest.permission.MANAGE_OWN_CALLS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.MODIFY_PHONE_STATE
            };
        }

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_required)
                        .setMessage(R.string.permission_phone_msg)
                        .setPositiveButton(R.string.go_to_settings, (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    }
}
