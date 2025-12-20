package com.khanghv.campusexpense.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.ArrayList;
import java.util.List;

public class YearSummaryAdapter extends RecyclerView.Adapter<YearSummaryAdapter.ViewHolder> {
    public static class Item {
        public final int year;
        public final int month; // 1-12
        public final double spent;
        public Item(int year, int month, double spent) {
            this.year = year;
            this.month = month;
            this.spent = spent;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private double maxSpent = 0;

    public void setItems(List<Item> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setMaxSpent(double max) {
        this.maxSpent = max;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_year_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        String[] monthNames = holder.itemView.getContext().getResources().getStringArray(R.array.months_names);
        String label = monthNames[item.month - 1];
        holder.tvMonthLabel.setText(label);
        holder.tvMonthSpent.setText(CurrencyManager.formatDisplayCurrency(holder.itemView.getContext(), item.spent));
        int percent = maxSpent > 0 ? (int) Math.round(item.spent / maxSpent * 100.0) : 0;
        holder.barSpent.setProgress(percent);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthLabel;
        ProgressBar barSpent;
        TextView tvMonthSpent;

        ViewHolder(View itemView) {
            super(itemView);
            tvMonthLabel = itemView.findViewById(R.id.tvMonthLabel);
            barSpent = itemView.findViewById(R.id.barSpent);
            tvMonthSpent = itemView.findViewById(R.id.tvMonthSpent);
        }
    }
}

