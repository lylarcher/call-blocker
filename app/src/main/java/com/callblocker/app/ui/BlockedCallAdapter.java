package com.callblocker.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.callblocker.app.R;
import com.callblocker.app.model.BlockedCall;

import java.util.ArrayList;
import java.util.List;

public class BlockedCallAdapter extends RecyclerView.Adapter<BlockedCallAdapter.ViewHolder> {

    private List<BlockedCall> calls = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(BlockedCall call);
    }

    public BlockedCallAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setCalls(List<BlockedCall> calls) {
        this.calls = calls;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_call, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedCall call = calls.get(position);
        holder.bind(call);
    }

    @Override
    public int getItemCount() {
        return calls.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPhoneNumber;
        private final TextView tvMatchType;
        private final TextView tvRuleLabel;
        private final TextView tvTime;
        private final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvMatchType = itemView.findViewById(R.id.tvMatchType);
            tvRuleLabel = itemView.findViewById(R.id.tvRuleLabel);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(BlockedCall call) {
            tvPhoneNumber.setText(call.getPhoneNumberDisplay());
            tvMatchType.setText(call.getMatchTypeDescription());

            String label = call.getRuleLabel();
            if (label != null && !label.isEmpty()) {
                tvRuleLabel.setText(label);
                tvRuleLabel.setVisibility(View.VISIBLE);
            } else {
                tvRuleLabel.setVisibility(View.GONE);
            }

            tvTime.setText(call.getFormattedTime());

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(call);
                }
            });
        }
    }
}
