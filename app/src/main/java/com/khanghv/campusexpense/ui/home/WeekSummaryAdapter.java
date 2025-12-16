package com.khanghv.campusexpense.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.ArrayList;
import java.util.List;

public class WeekSummaryAdapter extends RecyclerView.Adapter<WeekSummaryAdapter.ViewHolder> {
    public static class WeekSummaryItem {
        public final String label;
        public final double totalSpent;
        public final int transactionCount;
        public final boolean isCurrentWeek;
        public WeekSummaryItem(String label, double totalSpent, int transactionCount, boolean isCurrentWeek) {
            this.label = label;
            this.totalSpent = totalSpent;
            this.transactionCount = transactionCount;
            this.isCurrentWeek = isCurrentWeek;
        }
    }

    private final List<WeekSummaryItem> items = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeekSummaryItem item = items.get(position);
        holder.tvWeekLabel.setText(item.label);
        holder.tvWeekSpent.setText(CurrencyManager.formatDisplayCurrency(holder.itemView.getContext(), item.totalSpent));
        holder.tvWeekCount.setText(holder.itemView.getContext().getString(R.string.transactions_count_format, item.transactionCount));
        holder.currentWeekDot.setVisibility(item.isCurrentWeek ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<WeekSummaryItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeekLabel;
        TextView tvWeekSpent;
        TextView tvWeekCount;
        View currentWeekDot;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWeekLabel = itemView.findViewById(R.id.tvWeekLabel);
            tvWeekSpent = itemView.findViewById(R.id.tvWeekSpent);
            tvWeekCount = itemView.findViewById(R.id.tvWeekCount);
            currentWeekDot = itemView.findViewById(R.id.currentWeekDot);
        }
    }
}
