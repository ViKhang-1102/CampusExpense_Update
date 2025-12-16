package com.khanghv.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.ui.category.CategoryRecyclerAdapter;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.database.ExpenseDao;
import com.khanghv.campusexpense.data.database.BudgetDao;

import java.util.ArrayList;
import java.util.List;

public class CategoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private CategoryRecyclerAdapter adapter;
    private List<Category> categoriesList;
    private CategoryDao categoryDao;
    private ExpenseDao expenseDao;
    private BudgetDao budgetDao;
    private com.khanghv.campusexpense.data.database.MonthlyBudgetDao monthlyBudgetDao;
    private TextView emptyView;
    private android.content.SharedPreferences sharedPreferences;
    private int currentUserId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_category, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyView = view.findViewById(R.id.emptyView);
        AppDatabase db = AppDatabase.getInstance(requireContext());
        categoryDao = db.categoryDao();
        expenseDao = db.expenseDao();
        budgetDao = db.budgetDao();
        monthlyBudgetDao = db.monthlyBudgetDao();
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("userId", -1);

        categoriesList = new ArrayList<>();
        adapter = new CategoryRecyclerAdapter(
                categoriesList,
                this::showEditDialog,
                this::showDeleteDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddDialog());
        refreshList();
        return view;
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
        EditText nameInput = view.findViewById(R.id.nameInput);
        Button saveButton = view.findViewById(R.id.saveButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        builder.setView(view);
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_category_name), Toast.LENGTH_SHORT).show();
                return;
            }
            Category category = new Category(currentUserId, name);
            categoryDao.insert(category);
            refreshList();
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.category_added), Toast.LENGTH_SHORT).show();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void refreshList(){
        categoriesList.clear();
        categoriesList.addAll(categoryDao.getAllByUser(currentUserId));
        adapter.notifyDataSetChanged();

        if (categoriesList.isEmpty()){
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showDeleteDialog(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_category))
                .setMessage(getString(R.string.confirm_delete_category))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    // Xóa tất cả expenses và budgets liên quan đến category này trước
                    expenseDao.deleteExpensesByCategoryId(category.getId());
                    budgetDao.deleteBudgetsByCategoryId(category.getId());
                    if (monthlyBudgetDao != null) monthlyBudgetDao.deleteBudgetsByCategoryId(category.getId());
                    // Sau đó mới xóa category
                    categoryDao.delete(category);
                    refreshList();
                    Toast.makeText(requireContext(), getString(R.string.category_deleted),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();

    }

    private void showEditDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
        EditText nameInput = view.findViewById(R.id.nameInput);
        Button saveButton = view.findViewById(R.id.saveButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        nameInput.setText(category.getName());
        builder.setView(view);
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_category_name), Toast.LENGTH_SHORT).show();
                return;
            }
            category.setName(name);
            categoryDao.update(category);
            refreshList();
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.category_updated), Toast.LENGTH_SHORT).show();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
