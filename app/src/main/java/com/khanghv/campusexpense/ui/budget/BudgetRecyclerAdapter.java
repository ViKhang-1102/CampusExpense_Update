package com.khanghv.campusexpense.ui.budget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.List;

public class BudgetRecyclerAdapter extends RecyclerView.Adapter<BudgetRecyclerAdapter.ViewHolder> {
    private List<Budget> budgetsList;
    private List<String> categoryNames;
    private onEditClickListener onEditClickListener;
    private onDeleteClickListener onDeleteClickListener;
    private onAddAmountClickListener onAddAmountClickListener;
    private onTransferClickListener onTransferClickListener;
    private Context context;

    public interface onEditClickListener {
        void onEditClick(Budget budget);
    }
    public interface onDeleteClickListener {
        void onDeleteClick(Budget budget);
    }
    public interface onAddAmountClickListener {
        void onAddAmountClick(Budget budget);
    }
    public interface onTransferClickListener {
        void onTransferClick(Budget budget);
    }

    @Override
    public int getItemCount() {
        return budgetsList.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameText;
        TextView periodText;
        TextView amountText;
        ImageButton editButton;
        ImageButton deleteButton;
        ImageButton addAmountButton;
        ImageButton transferButton;
        ViewHolder(View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            periodText = itemView.findViewById(R.id.periodText);
            amountText = itemView.findViewById(R.id.amountText);

            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            addAmountButton = itemView.findViewById(R.id.addAmountButton);
            transferButton = itemView.findViewById(R.id.transferButton);
        }
    }

    public BudgetRecyclerAdapter(List<Budget> budgets, List<String> categoryNames, onEditClickListener onEditClickListener, onDeleteClickListener onDeleteClickListener, onAddAmountClickListener onAddAmountClickListener, onTransferClickListener onTransferClickListener) {
        this.budgetsList = budgets;
        this.categoryNames = categoryNames;
        this.onEditClickListener = onEditClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
        this.onAddAmountClickListener = onAddAmountClickListener;
        this.onTransferClickListener = onTransferClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_card, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgetsList.get(position);
        String categoryName = (position < categoryNames.size()) ? categoryNames.get(position) : "Unknown Category";
        holder.categoryNameText.setText(categoryName);
        holder.amountText.setText(CurrencyManager.formatDisplayCurrency(context, budget.getAmount()));
        holder.periodText.setText(budget.getPeriod());
        holder.periodText.setText(budget.getPeriod());
        holder.editButton.setOnClickListener(v -> onEditClickListener.onEditClick(budget));
        holder.deleteButton.setOnClickListener(v -> onDeleteClickListener.onDeleteClick(budget));
        if (holder.addAmountButton != null && onAddAmountClickListener != null) {
            holder.addAmountButton.setOnClickListener(v -> onAddAmountClickListener.onAddAmountClick(budget));
        }
        if (holder.transferButton != null && onTransferClickListener != null) {
            holder.transferButton.setOnClickListener(v -> onTransferClickListener.onTransferClick(budget));
        }
    }
}
