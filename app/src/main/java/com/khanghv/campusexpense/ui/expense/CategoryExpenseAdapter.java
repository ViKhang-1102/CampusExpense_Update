package com.khanghv.campusexpense.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.util.CurrencyManager;
import java.util.List;


public class CategoryExpenseAdapter extends RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder> {

    private List<CategoryExpenseItem> categoryExpenseList;
    private OnCategoryClickListener onCategoryClickListener;

    public interface OnCategoryClickListener {
        void onCategoryClick(int categoryId, String categoryName);
    }

    public static class CategoryExpenseItem {
        public int categoryId;
        public String categoryName;
        public double totalExpense;
        public int expenseCount;
        public Budget budget;

        public CategoryExpenseItem(int categoryId, String categoryName, double totalExpense, int expenseCount, Budget budget) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.totalExpense = totalExpense;
            this.expenseCount = expenseCount;
            this.budget = budget;
        }
    }

    public CategoryExpenseAdapter(List<CategoryExpenseItem> categoryExpenseList, OnCategoryClickListener onCategoryClickListener) {
        this.categoryExpenseList = categoryExpenseList;
        this.onCategoryClickListener = onCategoryClickListener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryExpenseItem item = categoryExpenseList.get(position);
        Context holderContext = holder.itemView.getContext();

        holder.categoryNameText.setText(item.categoryName);
        holder.expenseAmountText.setText(CurrencyManager.formatDisplayCurrency(holderContext, item.totalExpense));
        String transactionText = holderContext.getString(R.string.transactions);
        holder.expenseCountText.setText(item.expenseCount + " " + transactionText);

        if (item.budget != null) {
            holder.budgetLayout.setVisibility(View.VISIBLE);
            holder.budgetAmountText.setText(CurrencyManager.formatDisplayCurrency(holderContext, item.budget.getAmount()));

            double percentage = (item.totalExpense / item.budget.getAmount()) * 100;
            int progress = (int) Math.min(Math.max(percentage, 0), 100);
            holder.progressBar.setProgress(progress);

            if (percentage > 100) {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
                String overText = holderContext.getString(R.string.over_budget, percentage - 100);
                holder.progressText.setText(overText);
                holder.progressText.setTextColor(0xFFD32F2F);
            } else {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(percentage > 80 ? 0xFFFF9800 : 0xFF4CAF50));
                holder.progressText.setText(holderContext.getString(R.string.budget_percentage_used, percentage));
                holder.progressText.setTextColor(0xFF757575);
            }
        } else {
            holder.budgetLayout.setVisibility(View.GONE);
            holder.progressBar.setProgress(0);
            holder.progressText.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (onCategoryClickListener != null) {
                onCategoryClickListener.onCategoryClick(item.categoryId, item.categoryName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryExpenseList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameText;
        TextView expenseAmountText;
        TextView expenseCountText;
        LinearLayout budgetLayout;
        TextView budgetAmountText;
        ProgressBar progressBar;
        TextView progressText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            expenseAmountText = itemView.findViewById(R.id.expenseAmountText);
            expenseCountText = itemView.findViewById(R.id.expenseCountText);
            budgetLayout = itemView.findViewById(R.id.budgetLayout);
            budgetAmountText = itemView.findViewById(R.id.budgetAmountText);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressText = itemView.findViewById(R.id.progressText);
        }
    }
}

