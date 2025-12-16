package com.khanghv.campusexpense.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.ExpenseRepository;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.List;

public class BudgetBreakdownAdapter extends RecyclerView.Adapter<BudgetBreakdownAdapter.ViewHolder> {
    private List<ExpenseRepository.BudgetBreakdownItem> breakdownList;

    public BudgetBreakdownAdapter(List<ExpenseRepository.BudgetBreakdownItem> breakdownList) {
        this.breakdownList = breakdownList;
    }

    public void updateBreakdownList(List<ExpenseRepository.BudgetBreakdownItem> newList) {
        this.breakdownList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_breakdown, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseRepository.BudgetBreakdownItem item = breakdownList.get(position);
        
        holder.tvCategoryName.setText(item.categoryName);
        String percentText = holder.itemView.getContext().getString(R.string.percentage_format, item.percentage);
        holder.tvPercentage.setText(percentText);
        holder.progressBar.setProgress(Math.min(item.percentage, 100));
        
        holder.tvSpent.setText(CurrencyManager.formatDisplayCurrency(holder.itemView.getContext(), item.spentAmount));
        holder.tvBudget.setText(CurrencyManager.formatDisplayCurrency(holder.itemView.getContext(), item.budgetAmount));
    }

    @Override
    public int getItemCount() {
        return breakdownList == null ? 0 : breakdownList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvPercentage, tvSpent, tvBudget;
        ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            tvSpent = itemView.findViewById(R.id.tvSpent);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}

