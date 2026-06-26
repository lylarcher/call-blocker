package com.callblocker.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.callblocker.app.R;
import com.callblocker.app.model.BlockRule;

import java.util.ArrayList;
import java.util.List;

public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.RuleViewHolder> {

    private List<BlockRule> rules = new ArrayList<>();
    private OnRuleActionListener listener;

    public interface OnRuleActionListener {
        void onToggle(BlockRule rule, boolean enabled);
        void onDelete(BlockRule rule);
        void onEdit(BlockRule rule);
    }

    public RuleAdapter(OnRuleActionListener listener) {
        this.listener = listener;
    }

    public void setRules(List<BlockRule> rules) {
        this.rules = rules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        BlockRule rule = rules.get(position);
        holder.bind(rule);
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    class RuleViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPhoneNumber;
        private final TextView tvTimeRange;
        private final TextView tvStatus;
        private final TextView tvLabel;
        private final Switch switchEnabled;
        private final ImageButton btnDelete;
        private final ImageButton btnEdit;

        RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvLabel = itemView.findViewById(R.id.tvLabel);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }

        void bind(BlockRule rule) {
            tvPhoneNumber.setText(rule.getMatchTypeDescription());

            if (rule.getMatchType() == BlockRule.MATCH_BLOCK_ALL ||
                rule.getMatchType() == BlockRule.MATCH_OVERSEAS) {
                tvPhoneNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.delete_red));
            } else {
                tvPhoneNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }

            tvTimeRange.setText(rule.getTimeRangesDisplayStr());

            if (rule.getLabel() != null && !rule.getLabel().isEmpty()) {
                tvLabel.setText(rule.getLabel());
                tvLabel.setVisibility(View.VISIBLE);
            } else {
                tvLabel.setVisibility(View.GONE);
            }

            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(rule.isEnabled());
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggle(rule, isChecked);
                }
            });

            if (rule.isEnabled()) {
                tvStatus.setText(itemView.getContext().getString(R.string.rule_active));
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.enabled_green));
            } else {
                tvStatus.setText(itemView.getContext().getString(R.string.rule_inactive));
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.disabled_gray));
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(rule);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(rule);
                }
            });
        }
    }
}
