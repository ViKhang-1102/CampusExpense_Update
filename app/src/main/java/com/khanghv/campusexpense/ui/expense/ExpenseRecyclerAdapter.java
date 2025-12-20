package com.khanghv.campusexpense.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.model.Expense;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.data.model.Favorite;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.FavoriteDao;
import com.khanghv.campusexpense.util.CurrencyManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<ExpenseItem> expenseList;
    private List<Category> categoryList;
    private OnExpenseClickListener onExpenseClickListener;
    private OnExpenseLongClickListener onExpenseLongClickListener;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public interface OnExpenseLongClickListener {
        void onExpenseLongClick(Expense expense);
    }

    public static class ExpenseItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_EXPENSE = 1;

        public int type;
        public String headerText;
        public Expense expense;

        public ExpenseItem(String headerText) {
            this.type = TYPE_HEADER;
            this.headerText = headerText;
        }

        public ExpenseItem(Expense expense) {
            this.type = TYPE_EXPENSE;
            this.expense = expense;
        }
    }

    private android.content.Context context;
    private int currentUserId = -1;
    private FavoriteDao favoriteDao;

    public ExpenseRecyclerAdapter(List<Expense> expenseList,
                                  List<Category> categoryList,
                                  OnExpenseClickListener onExpenseClickListener,
                                  OnExpenseLongClickListener onExpenseLongClickListener) {
        this.onExpenseClickListener = onExpenseClickListener;
        this.onExpenseLongClickListener = onExpenseLongClickListener;
        this.categoryList = categoryList != null ? categoryList : new ArrayList<>();
        this.expenseList = new ArrayList<>();
    }

    public void setContext(android.content.Context context) {
        this.context = context;
        try {
            this.favoriteDao = AppDatabase.getInstance(context).favoriteDao();
        } catch (Exception ignored) {}
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    private List<ExpenseItem> groupExpensesByDate(List<Expense> expenses) {
        List<ExpenseItem> items = new ArrayList<>();
        if (expenses.isEmpty()) {
            return items;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat todayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String currentDateHeader = null;
        String today = todayFormat.format(new Date());

        for (Expense expense : expenses) {
            String expenseDate = todayFormat.format(new Date(expense.getDate()));
            String dateHeader;

            if (expenseDate.equals(today)) {
                dateHeader = context != null ? context.getString(R.string.today) : "Today";
            } else {
                dateHeader = dateFormat.format(new Date(expense.getDate()));
            }

            if (!dateHeader.equals(currentDateHeader)) {
                items.add(new ExpenseItem(dateHeader));
                currentDateHeader = dateHeader;
            }

            items.add(new ExpenseItem(expense));
        }

        return items;
    }

    @Override
    public int getItemViewType(int position) {
        return expenseList.get(position).type == ExpenseItem.TYPE_HEADER ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expense_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expense_date, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ExpenseItem item = expenseList.get(position);

        if (item.type == ExpenseItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerText.setText(item.headerText);
        } else {
            ExpenseViewHolder expenseHolder = (ExpenseViewHolder) holder;
            Expense expense = item.expense;
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String categoryName = getCategoryName(expense.getCategoryId());
            expenseHolder.categoryNameText.setText(categoryName);
            expenseHolder.amountText.setText(CurrencyManager.formatDisplayCurrency(context, expense.getAmount()));
            expenseHolder.timeText.setText(timeFormat.format(new Date(expense.getDate())));

            if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                expenseHolder.descriptionText.setText(expense.getDescription());
                expenseHolder.descriptionText.setVisibility(View.VISIBLE);
            } else {
                expenseHolder.descriptionText.setVisibility(View.GONE);
            }

            boolean fav = false;
            if (favoriteDao != null && currentUserId != -1) {
                try {
                    fav = favoriteDao.isFavorite(currentUserId, expense.getId());
                } catch (Exception ignored) {}
            }
            expenseHolder.favoriteButton.setImageResource(fav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            expenseHolder.favoriteButton.setOnClickListener(v -> {
                if (favoriteDao == null || currentUserId == -1) return;
                boolean isFav = false;
                try {
                    isFav = favoriteDao.isFavorite(currentUserId, expense.getId());
                } catch (Exception ignored) {}
                if (isFav) {
                    try {
                        favoriteDao.deleteByUserAndExpense(currentUserId, expense.getId());
                    } catch (Exception ignored) {}
                } else {
                    Favorite f = new Favorite();
                    f.setUserId(currentUserId);
                    f.setExpenseId(expense.getId());
                    f.setCreatedAt(System.currentTimeMillis());
                    try {
                        favoriteDao.insert(f);
                    } catch (Exception ignored) {}
                }
                notifyItemChanged(holder.getAdapterPosition());
            });

            expenseHolder.itemView.setOnClickListener(v -> {
                if (onExpenseClickListener != null) {
                    onExpenseClickListener.onExpenseClick(expense);
                }
            });

            expenseHolder.itemView.setOnLongClickListener(v -> {
                if (onExpenseLongClickListener != null) {
                    onExpenseLongClickListener.onExpenseLongClick(expense);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public void updateExpenses(List<Expense> expenses) {
        this.expenseList = groupExpensesByDate(expenses);
        notifyDataSetChanged();
    }

    public void setCategoryList(List<Category> categoryList) {
        this.categoryList = categoryList != null ? categoryList : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String getCategoryName(int categoryId) {
        for (Category category : categoryList) {
            if (category.getId() == categoryId) {
                return category.getName();
            }
        }
        return "Unknown";
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = (TextView) itemView;
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        View categoryColorView;
        TextView categoryNameText;
        TextView descriptionText;
        TextView amountText;
        TextView timeText;
        android.widget.ImageButton favoriteButton;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryColorView = itemView.findViewById(R.id.categoryColorView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            amountText = itemView.findViewById(R.id.amountText);
            timeText = itemView.findViewById(R.id.timeText);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
}


