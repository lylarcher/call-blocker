package com.callblocker.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.callblocker.app.R;
import com.callblocker.app.db.RuleDatabase;
import com.callblocker.app.model.BlockedCall;

import java.util.List;

public class BlockedCallsActivity extends AppCompatActivity {

    private RuleDatabase db;
    private BlockedCallAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_calls);

        db = RuleDatabase.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);
        tvCount = findViewById(R.id.tvCount);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new BlockedCallAdapter(call -> {
            db.deleteBlockedCall(call.getId());
            refreshList();
            Toast.makeText(this, "记录已删除", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清空记录")
                    .setMessage("确定要清空所有拦截记录吗？")
                    .setPositiveButton("清空", (d, w) -> {
                        db.deleteAllBlockedCalls();
                        refreshList();
                        Toast.makeText(this, "所有记录已清空", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        refreshList();
    }

    private void refreshList() {
        List<BlockedCall> calls = db.getAllBlockedCalls();
        adapter.setCalls(calls);

        tvCount.setText("共 " + calls.size() + " 条记录");

        if (calls.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
