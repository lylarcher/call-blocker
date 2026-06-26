package com.callblocker.app.ui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.callblocker.app.R;
import com.callblocker.app.db.RuleDatabase;
import com.callblocker.app.model.BlockRule;
import com.callblocker.app.model.TimeRange;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddRuleDialogFragment extends DialogFragment {

    private static final String ARG_RULE_ID = "rule_id";
    private static final String ARG_RULE_PHONE = "rule_phone";
    private static final String ARG_RULE_MATCH_TYPE = "rule_match_type";
    private static final String ARG_RULE_TIME_RANGES = "rule_time_ranges";
    private static final String ARG_RULE_LABEL = "rule_label";

    private EditText etPhoneNumber;
    private EditText etLabel;
    private RadioGroup rgMatchType;
    private View tilPhoneNumber;
    private LinearLayout timeRangeListContainer;
    private ChipGroup chipGroupNumbers;

    private List<TimeRange> timeRanges = new ArrayList<>();
    private List<String> phoneNumbers = new ArrayList<>();
    private long editRuleId = -1;

    private OnRuleSavedListener listener;

    public interface OnRuleSavedListener {
        void onRuleSaved();
    }

    public void setOnRuleSavedListener(OnRuleSavedListener listener) {
        this.listener = listener;
    }

    public static AddRuleDialogFragment newInstance(BlockRule rule) {
        AddRuleDialogFragment fragment = new AddRuleDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_RULE_ID, rule.getId());
        args.putString(ARG_RULE_PHONE, rule.getPhoneNumber());
        args.putInt(ARG_RULE_MATCH_TYPE, rule.getMatchType());
        args.putString(ARG_RULE_TIME_RANGES, RuleDatabase.serializeTimeRanges(rule.getTimeRanges()));
        args.putString(ARG_RULE_LABEL, rule.getLabel());
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_rule, null);

        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        etLabel = view.findViewById(R.id.etLabel);
        rgMatchType = view.findViewById(R.id.rgMatchType);
        tilPhoneNumber = view.findViewById(R.id.tilPhoneNumber);
        timeRangeListContainer = view.findViewById(R.id.timeRangeListContainer);
        chipGroupNumbers = view.findViewById(R.id.chipGroupNumbers);

        // 添加号码按钮
        TextView btnAddNumber = view.findViewById(R.id.btnAddNumber);
        btnAddNumber.setOnClickListener(v -> addNumberToChipGroup());

        // 快捷时间段
        Chip chipAllDay = view.findViewById(R.id.chipAllDay);
        Chip chipMorning = view.findViewById(R.id.chipMorning);
        Chip chipNoon = view.findViewById(R.id.chipNoon);
        Chip chipAfternoon = view.findViewById(R.id.chipAfternoon);

        chipAllDay.setOnClickListener(v -> { timeRanges.clear(); timeRanges.add(TimeRange.allDay()); refreshTimeRangeList(); });
        chipMorning.setOnClickListener(v -> { timeRanges.clear(); timeRanges.add(TimeRange.morning()); refreshTimeRangeList(); });
        chipNoon.setOnClickListener(v -> { timeRanges.clear(); timeRanges.add(TimeRange.noon()); refreshTimeRangeList(); });
        chipAfternoon.setOnClickListener(v -> { timeRanges.clear(); timeRanges.add(TimeRange.afternoon()); refreshTimeRangeList(); });

        TextView btnAddTimeRange = view.findViewById(R.id.btnAddTimeRange);
        btnAddTimeRange.setOnClickListener(v -> showAddTimeRangePicker());

        // 编辑模式：加载已有数据
        Bundle args = getArguments();
        final String title;
        if (args != null) {
            editRuleId = args.getLong(ARG_RULE_ID, -1);
            if (editRuleId != -1) {
                title = "编辑规则";
                String phone = args.getString(ARG_RULE_PHONE, "");
                etLabel.setText(args.getString(ARG_RULE_LABEL, ""));

                // 解析已有号码列表
                if (!phone.isEmpty()) {
                    String[] existing = phone.split(",");
                    for (String num : existing) {
                        String trimmed = num.trim();
                        if (!trimmed.isEmpty()) {
                            phoneNumbers.add(trimmed);
                        }
                    }
                }

                int matchType = args.getInt(ARG_RULE_MATCH_TYPE, BlockRule.MATCH_EXACT);
                switch (matchType) {
                    case BlockRule.MATCH_EXACT: rgMatchType.check(R.id.rbExact); break;
                    case BlockRule.MATCH_PREFIX: rgMatchType.check(R.id.rbPrefix); break;
                    case BlockRule.MATCH_OVERSEAS: rgMatchType.check(R.id.rbOverseas); break;
                    case BlockRule.MATCH_LANDLINE: rgMatchType.check(R.id.rbLandline); break;
                    case BlockRule.MATCH_BLOCK_ALL: rgMatchType.check(R.id.rbBlockAll); break;
                }

                String timeRangesJson = args.getString(ARG_RULE_TIME_RANGES, "[]");
                timeRanges = RuleDatabase.deserializeTimeRanges(timeRangesJson);
            } else {
                title = getString(R.string.add_rule);
                Calendar now = Calendar.getInstance();
                int h = now.get(Calendar.HOUR_OF_DAY);
                int endH = h + 1 > 23 ? 23 : h + 1;
                timeRanges.add(new TimeRange(h, 0, endH, 0));
            }
        } else {
            title = getString(R.string.add_rule);
        }

        refreshPhoneChips();
        refreshTimeRangeList();

        // 匹配类型切换
        rgMatchType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean needNumber = (checkedId == R.id.rbExact || checkedId == R.id.rbPrefix);
            tilPhoneNumber.setVisibility(needNumber ? View.VISIBLE : View.GONE);
        });
        boolean needNumber = rgMatchType.getCheckedRadioButtonId() == R.id.rbExact
                || rgMatchType.getCheckedRadioButtonId() == R.id.rbPrefix;
        tilPhoneNumber.setVisibility(needNumber ? View.VISIBLE : View.GONE);

        String finalTitle = title;
        return new AlertDialog.Builder(getContext())
                .setTitle(finalTitle)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> saveRule())
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    /**
     * 将输入框中的号码添加到 ChipGroup
     */
    private void addNumberToChipGroup() {
        String input = etPhoneNumber.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(getContext(), "请输入号码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumbers.contains(input)) {
            Toast.makeText(getContext(), "该号码已添加", Toast.LENGTH_SHORT).show();
            return;
        }

        phoneNumbers.add(input);
        etPhoneNumber.setText("");
        refreshPhoneChips();
    }

    /**
     * 刷新号码 ChipGroup 显示
     */
    private void refreshPhoneChips() {
        chipGroupNumbers.removeAllViews();
        for (int i = 0; i < phoneNumbers.size(); i++) {
            String num = phoneNumbers.get(i);
            Chip chip = new Chip(getContext());
            chip.setText(num);
            chip.setCloseIconVisible(true);
            chip.setClickable(false);

            final int index = i;
            chip.setOnCloseIconClickListener(v -> {
                phoneNumbers.remove(index);
                refreshPhoneChips();
            });

            chipGroupNumbers.addView(chip);
        }
    }

    private void showAddTimeRangePicker() {
        Calendar now = Calendar.getInstance();
        int startH = now.get(Calendar.HOUR_OF_DAY);
        int startM = 0;

        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            final int selectedStartH = hourOfDay;
            final int selectedStartM = minute;

            new TimePickerDialog(getContext(), (view2, endH, endM) -> {
                if (selectedStartH == endH && selectedStartM == endM) {
                    Toast.makeText(getContext(), "开始时间和结束时间不能相同", Toast.LENGTH_SHORT).show();
                    return;
                }
                timeRanges.add(new TimeRange(selectedStartH, selectedStartM, endH, endM));
                refreshTimeRangeList();
            }, startH, startM + 1 > 23 ? 23 : startH, true).show();
        }, startH, startM, true).show();
    }

    private void refreshTimeRangeList() {
        timeRangeListContainer.removeAllViews();
        for (int i = 0; i < timeRanges.size(); i++) {
            TimeRange range = timeRanges.get(i);
            View row = LayoutInflater.from(getContext()).inflate(R.layout.item_time_range, timeRangeListContainer, false);
            TextView tvRangeText = row.findViewById(R.id.tvRangeText);
            ImageButton btnRemove = row.findViewById(R.id.btnRemoveRange);

            tvRangeText.setText(range.getTimeRangeStr());

            final int index = i;
            btnRemove.setOnClickListener(v -> {
                timeRanges.remove(index);
                refreshTimeRangeList();
            });

            timeRangeListContainer.addView(row);
        }
    }

    /**
     * 获取所有号码用逗号连接
     */
    private String getPhoneNumbersJoined() {
        if (phoneNumbers.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phoneNumbers.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(phoneNumbers.get(i));
        }
        return sb.toString();
    }

    private void saveRule() {
        try {
            int matchType;
            int checkedId = rgMatchType.getCheckedRadioButtonId();
            if (checkedId == R.id.rbExact) {
                matchType = BlockRule.MATCH_EXACT;
            } else if (checkedId == R.id.rbPrefix) {
                matchType = BlockRule.MATCH_PREFIX;
            } else if (checkedId == R.id.rbOverseas) {
                matchType = BlockRule.MATCH_OVERSEAS;
            } else if (checkedId == R.id.rbLandline) {
                matchType = BlockRule.MATCH_LANDLINE;
            } else {
                matchType = BlockRule.MATCH_BLOCK_ALL;
            }

            String phoneNumber = null;
            if (matchType == BlockRule.MATCH_EXACT || matchType == BlockRule.MATCH_PREFIX) {
                phoneNumber = getPhoneNumbersJoined();
                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    Toast.makeText(getContext(), "请至少添加一个号码", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String label = etLabel.getText().toString().trim();

            if (timeRanges.isEmpty()) {
                Toast.makeText(getContext(), "请至少添加一个时间段", Toast.LENGTH_SHORT).show();
                return;
            }

            RuleDatabase db = RuleDatabase.getInstance(getContext());

            if (db.isDuplicateRule(editRuleId, phoneNumber, matchType, timeRanges)) {
                Toast.makeText(getContext(), "已存在相同规则，不可重复添加", Toast.LENGTH_SHORT).show();
                return;
            }

            BlockRule rule = new BlockRule(phoneNumber, matchType, timeRanges, true, label);

            if (editRuleId != -1) {
                rule.setId(editRuleId);
                db.updateRule(rule);
                Toast.makeText(getContext(), "规则已更新", Toast.LENGTH_SHORT).show();
            } else {
                long id = db.insertRule(rule);
                if (id == -1) {
                    Toast.makeText(getContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (listener != null) {
                listener.onRuleSaved();
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "保存出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
