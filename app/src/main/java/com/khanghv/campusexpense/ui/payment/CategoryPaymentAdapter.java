package com.khanghv.campusexpense.ui.payment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.util.CurrencyManager;
import java.util.List;

public class CategoryPaymentAdapter extends RecyclerView.Adapter<CategoryPaymentAdapter.ViewHolder> {

    private List<CategoryPaymentItem> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CategoryPaymentItem item);
    }

    public static class CategoryPaymentItem {
        public int categoryId;
        public String categoryName;
        public double totalAmount;
        public int count;

        public CategoryPaymentItem(int categoryId, String categoryName, double totalAmount, int count) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.totalAmount = totalAmount;
            this.count = count;
        }
    }

    public CategoryPaymentAdapter(List<CategoryPaymentItem> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse item_expense_category layout if possible, or create a new one.
        // item_expense_category has progress bar for budget, which we might not need.
        // Let's assume we can reuse it and hide the budget part, or better, use a simpler layout.
        // For now, I'll try to reuse item_expense_category and hide unused views if they exist.
        // Wait, I should check item_expense_category.xml content first.
        // To be safe and "beautiful", I'll create a new layout item_payment_category.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryPaymentItem item = itemList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvCategoryName.setText(item.categoryName);
        holder.tvAmount.setText(CurrencyManager.formatDisplayCurrency(context, item.totalAmount));
        holder.tvCount.setText(item.count + " " + context.getString(R.string.transactions)); // Ensure "transactions" string exists or use literal/resource

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvAmount, tvCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCount = itemView.findViewById(R.id.tvCount);
        }
    }
}
