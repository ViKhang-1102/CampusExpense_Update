package com.khanghv.campusexpense.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.model.Category;

import java.util.List;

public class CategorySelectionAdapter extends RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedCategoryId = -1;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategorySelectionAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setSelectedCategoryId(int id) {
        this.selectedCategoryId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_single_choice, parent, false);
        // Note: simple_list_item_single_choice is a CheckedTextView
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.textView.setText(category.getName());
        
        // Manual handling of radio button state since we are not in a ListView
        if (holder.textView instanceof android.widget.CheckedTextView) {
            ((android.widget.CheckedTextView) holder.textView).setChecked(category.getId() == selectedCategoryId);
        }
        
        holder.itemView.setOnClickListener(v -> {
            selectedCategoryId = category.getId();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
