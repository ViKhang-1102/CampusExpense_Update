package com.khanghv.campusexpense.ui.analytics;

import android.content.Context;
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

public class AnalyticsDetailAdapter extends RecyclerView.Adapter<AnalyticsDetailAdapter.DetailViewHolder> {
    private final Context context;
    private final List<AnalyticsItem> items = new ArrayList<>();

    public AnalyticsDetailAdapter(Context context) {
        this.context = context;
    }

    public void submitItems(List<AnalyticsItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_analytics_detail, parent, false);
        return new DetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
        AnalyticsItem item = items.get(position);
        holder.tvCategory.setText(item.categoryName);
        holder.tvAmount.setText(CurrencyManager.formatDisplayCurrency(context, item.totalSpent));
        holder.tvPercent.setText(context.getString(R.string.analytics_percent_format, item.percentage));

        if (item.limitBudget != null && item.limitBudget > 0) {
            if (item.isOverLimit) {
                holder.tvWarning.setVisibility(View.VISIBLE);
                holder.tvWarning.setText(context.getString(R.string.analytics_warning_over_budget, item.categoryName));
                holder.tvWarning.setTextColor(context.getColor(android.R.color.holo_red_dark));
            } else {
                holder.tvWarning.setVisibility(View.VISIBLE);
                holder.tvWarning.setText(context.getString(R.string.analytics_warning_budget_ok));
                holder.tvWarning.setTextColor(context.getColor(android.R.color.holo_green_dark));
            }
        } else {
            holder.tvWarning.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DetailViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvAmount;
        TextView tvPercent;
        TextView tvWarning;

        public DetailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            tvWarning = itemView.findViewById(R.id.tvWarning);
        }
    }

    public static class AnalyticsItem {
        public String categoryName;
        public double totalSpent;
        public float percentage;
        public Double limitBudget;
        public boolean isOverLimit;
    }
}
