package com.khanghv.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.BudgetDao;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.model.Budget;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.ui.budget.BudgetRecyclerAdapter;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.util.ArrayList;
import java.util.List;

public class BudgetFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private BudgetRecyclerAdapter adapter;
    private List<Budget> budgetList;
    private List<Category> categoryList;
    private List<String> categoryNames;
    private BudgetDao budgetDao;
    private CategoryDao categoryDao;
    private com.khanghv.campusexpense.data.database.ExpenseDao expenseDao;
    private TextView emptyView;
    private SharedPreferences sharedPreferences;
    private int currentUserId;
    private com.khanghv.campusexpense.data.database.MonthlyBudgetDao monthlyBudgetDao;
    private int currentMonth;
    private int currentYear;
    private com.khanghv.campusexpense.data.ExpenseRepository repository;
    private int selectedCategoryId = -1;
    private String selectedCategoryName;


    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyView = view.findViewById(R.id.emptyView);
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("userId", -1);
        AppDatabase db = AppDatabase.getInstance(requireContext());
        budgetDao = db.budgetDao();
        expenseDao = db.expenseDao();
        monthlyBudgetDao = db.monthlyBudgetDao();
        categoryDao = db.categoryDao();
        budgetList = new ArrayList<>();
        categoryList = new ArrayList<>();
        categoryNames = new ArrayList<>();
        adapter = new BudgetRecyclerAdapter(budgetList, categoryNames, this::showEditDialog, this::showDeleteDialog, this::showAddAmountDialog, this::showTransferDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        fabAdd.setOnClickListener(v -> showAddBudgetDialog());
        // Tap emptyView to open month/year filter dialog
        emptyView.setOnClickListener(v -> showMonthYearPickerDialog());
        View btnSelectMonth = view.findViewById(R.id.btnSelectMonth);
        if (btnSelectMonth != null) {
            btnSelectMonth.setOnClickListener(v -> showMonthYearPickerDialog());
        }
        View btnFilterCategory = view.findViewById(R.id.btnFilterCategory);
        if (btnFilterCategory != null) {
            btnFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());
        }
        CurrencyManager.refreshRateIfNeeded(requireContext(), false, null);
        repository = new com.khanghv.campusexpense.data.ExpenseRepository((android.app.Application) requireContext().getApplicationContext());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        currentMonth = cal.get(java.util.Calendar.MONTH) + 1; // 1-12
        currentYear = cal.get(java.util.Calendar.YEAR);
        selectedCategoryName = getString(R.string.all_categories);
        refreshBudgetList();
        return view;
    }

    private void showAddAmountDialog(Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget_add_amount, null);
        com.google.android.material.textfield.TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        com.google.android.material.textfield.TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        com.google.android.material.textfield.TextInputEditText noteInput = dialogView.findViewById(R.id.noteInput);
        android.widget.Button saveButton = dialogView.findViewById(R.id.saveButton);
        android.widget.Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        android.widget.Button viewHistoryButton = dialogView.findViewById(R.id.viewHistoryButton);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        CurrencyManager.attachInputFormatter(amountInput);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            double displayAmount;
            try {
                displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
            } catch (Exception e) {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            double baseAmount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
            new Thread(() -> {
                com.khanghv.campusexpense.data.database.AppDatabase db = com.khanghv.campusexpense.data.database.AppDatabase.getInstance(requireContext());
                com.khanghv.campusexpense.data.database.MonthlyBudgetDao mbDao = db.monthlyBudgetDao();
                com.khanghv.campusexpense.data.database.BudgetAdjustmentDao adjDao = db.budgetAdjustmentDao();
                int m = currentMonth;
                int y = currentYear;
                com.khanghv.campusexpense.data.model.MonthlyBudget mb = mbDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), m, y);
                if (mb == null) {
                    mb = new com.khanghv.campusexpense.data.model.MonthlyBudget(currentUserId, budget.getCategoryId(), m, y, 0, 0);
                    long id = mbDao.insert(mb);
                    mb.setId((int) id);
                }
                mb.setTotalBudget(mb.getTotalBudget() + baseAmount);
                mb.setRemainingBudget(mb.getRemainingBudget() + baseAmount);
                mbDao.update(mb);
                com.khanghv.campusexpense.data.model.BudgetAdjustment adj = new com.khanghv.campusexpense.data.model.BudgetAdjustment();
                adj.setUserId(currentUserId);
                adj.setMonthlyBudgetId(mb.getId());
                adj.setCategoryId(budget.getCategoryId());
                adj.setMonth(m);
                adj.setYear(y);
                adj.setAmount(baseAmount);
                adj.setNote(noteInput.getText() != null ? noteInput.getText().toString().trim() : null);
                adj.setCreatedAt(System.currentTimeMillis());
                adjDao.insert(adj);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    refreshBudgetList();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), getString(R.string.budget_added_success), Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        viewHistoryButton.setOnClickListener(v -> showAddAmountHistoryDialog(budget));
        dialog.show();
    }

    private void showTransferDialog(Budget fromBudget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget_transfer, null);
        TextView fromCategory = dialogView.findViewById(R.id.fromCategory);
        Spinner toSpinner = dialogView.findViewById(R.id.toCategorySpinner);
        com.google.android.material.textfield.TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        com.google.android.material.textfield.TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        android.widget.Button saveButton = dialogView.findViewById(R.id.saveButton);
        android.widget.Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        Category fromCat = categoryDao.getById(fromBudget.getCategoryId());
        fromCategory.setText(fromCat != null ? fromCat.getName() : getString(R.string.unknown_category));

        List<com.khanghv.campusexpense.data.model.MonthlyBudget> mbs = monthlyBudgetDao.getBudgetsByUserAndMonth(currentUserId, currentMonth, currentYear);
        List<Category> toCategories = new ArrayList<>();
        List<String> toNames = new ArrayList<>();
        for (com.khanghv.campusexpense.data.model.MonthlyBudget mb : mbs) {
            if (mb.getCategoryId() != fromBudget.getCategoryId()) {
                Category c = categoryDao.getById(mb.getCategoryId());
                if (c != null) {
                    toCategories.add(c);
                    toNames.add(c.getName());
                }
            }
        }
        if (toCategories.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_target_budget, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayAdapter<String> toAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, toNames);
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinner.setAdapter(toAdapter);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        CurrencyManager.attachInputFormatter(amountInput);

        builder.setTitle(getString(R.string.transfer));
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            double displayAmount;
            try {
                displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
            } catch (Exception e) {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            double baseAmount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
            int toIndex = toSpinner.getSelectedItemPosition();
            Category toCat = toCategories.get(toIndex);
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                com.khanghv.campusexpense.data.database.MonthlyBudgetDao mbDao = db.monthlyBudgetDao();
                com.khanghv.campusexpense.data.database.BudgetAdjustmentDao adjDao = db.budgetAdjustmentDao();
                com.khanghv.campusexpense.data.database.ExpenseDao expDao = db.expenseDao();
                int m = currentMonth;
                int y = currentYear;
                com.khanghv.campusexpense.data.model.MonthlyBudget fromMb = mbDao.getBudgetByCategoryUserMonth(currentUserId, fromBudget.getCategoryId(), m, y);
                com.khanghv.campusexpense.data.model.MonthlyBudget toMb = mbDao.getBudgetByCategoryUserMonth(currentUserId, toCat.getId(), m, y);
                if (fromMb == null) {
                    fromMb = new com.khanghv.campusexpense.data.model.MonthlyBudget(currentUserId, fromBudget.getCategoryId(), m, y, 0, 0);
                    long id = mbDao.insert(fromMb);
                    fromMb.setId((int) id);
                }
                if (toMb == null) {
                    toMb = new com.khanghv.campusexpense.data.model.MonthlyBudget(currentUserId, toCat.getId(), m, y, 0, 0);
                    long id = mbDao.insert(toMb);
                    toMb.setId((int) id);
                }
                double maxTransferable = Math.min(fromMb.getTotalBudget(), fromMb.getRemainingBudget());
                if (baseAmount > maxTransferable) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.transfer_over_limit), Toast.LENGTH_SHORT).show());
                    return;
                }
                double newFromTotal = fromMb.getTotalBudget() - baseAmount;
                if (newFromTotal < 0) newFromTotal = 0;
                long[] range = getMonthRange(y, m);
                Double fromSpent = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, fromBudget.getCategoryId(), range[0], range[1]);
                double fromSpentVal = fromSpent == null ? 0.0 : fromSpent;
                double newFromRemaining = newFromTotal - fromSpentVal;
                if (newFromRemaining < 0) newFromRemaining = 0;
                fromMb.setTotalBudget(newFromTotal);
                fromMb.setRemainingBudget(newFromRemaining);
                mbDao.update(fromMb);

                double newToTotal = toMb.getTotalBudget() + baseAmount;
                Double toSpent = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, toCat.getId(), range[0], range[1]);
                double toSpentVal = toSpent == null ? 0.0 : toSpent;
                double newToRemaining = newToTotal - toSpentVal;
                if (newToRemaining < 0) newToRemaining = 0;
                toMb.setTotalBudget(newToTotal);
                toMb.setRemainingBudget(newToRemaining);
                mbDao.update(toMb);

                com.khanghv.campusexpense.data.model.BudgetAdjustment adjFrom = new com.khanghv.campusexpense.data.model.BudgetAdjustment();
                adjFrom.setUserId(currentUserId);
                adjFrom.setMonthlyBudgetId(fromMb.getId());
                adjFrom.setCategoryId(fromBudget.getCategoryId());
                adjFrom.setMonth(m);
                adjFrom.setYear(y);
                adjFrom.setAmount(-baseAmount);
                adjFrom.setNote(getString(R.string.transfer_to, toCat.getName()));
                adjFrom.setCreatedAt(System.currentTimeMillis());
                adjDao.insert(adjFrom);

                com.khanghv.campusexpense.data.model.BudgetAdjustment adjTo = new com.khanghv.campusexpense.data.model.BudgetAdjustment();
                adjTo.setUserId(currentUserId);
                adjTo.setMonthlyBudgetId(toMb.getId());
                adjTo.setCategoryId(toCat.getId());
                adjTo.setMonth(m);
                adjTo.setYear(y);
                adjTo.setAmount(baseAmount);
                adjTo.setNote(getString(R.string.transfer_from, fromCat != null ? fromCat.getName() : ""));
                adjTo.setCreatedAt(System.currentTimeMillis());
                adjDao.insert(adjTo);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    refreshBudgetList();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.transfer_done, Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAddAmountHistoryDialog(Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget_view_history, null);
        androidx.recyclerview.widget.RecyclerView rv = root.findViewById(R.id.breakdownRecyclerView);
        android.widget.Button closeButton = root.findViewById(R.id.closeButton);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        builder.setView(root);
        AlertDialog dialog = builder.create();

        class AdjustmentAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<AdjustmentAdapter.VH> {
            java.util.List<com.khanghv.campusexpense.data.model.BudgetAdjustment> list = new java.util.ArrayList<>();
            @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                android.view.View item = android.view.LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_budget_adjustment, parent, false);
                android.widget.TextView tvMain = item.findViewById(R.id.tvMain);
                android.widget.TextView tvNote = item.findViewById(R.id.tvNote);
                android.widget.ImageButton btnEdit = item.findViewById(R.id.btnEdit);
                android.widget.ImageButton btnDelete = item.findViewById(R.id.btnDelete);
                return new VH(item, tvMain, tvNote, btnEdit, btnDelete);
            }
            @Override public void onBindViewHolder(VH h, int pos) {
                com.khanghv.campusexpense.data.model.BudgetAdjustment a = list.get(pos);
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                String main = df.format(new java.util.Date(a.getCreatedAt())) + " • " + CurrencyManager.formatDisplayCurrency(requireContext(), a.getAmount());
                h.tvMain.setText(main);
                String note = a.getNote() != null ? a.getNote().trim() : "";
                h.tvNote.setVisibility(note.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
                h.tvNote.setText(note);
                h.btnEdit.setOnClickListener(v -> showEditAdjustmentDialog(a, budget, this));
                h.btnDelete.setOnClickListener(v -> deleteAdjustment(a, budget, this));
            }
            @Override public int getItemCount() { return list.size(); }
            class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
                android.widget.TextView tvMain; android.widget.TextView tvNote; android.widget.ImageButton btnEdit, btnDelete;
                VH(android.view.View itemView, android.widget.TextView tvMain, android.widget.TextView tvNote, android.widget.ImageButton e, android.widget.ImageButton d) { super(itemView); this.tvMain = tvMain; this.tvNote = tvNote; this.btnEdit = e; this.btnDelete = d; }
            }
        }

        AdjustmentAdapter adapter = new AdjustmentAdapter();
        rv.setAdapter(adapter);

        new Thread(() -> {
            com.khanghv.campusexpense.data.database.AppDatabase db = com.khanghv.campusexpense.data.database.AppDatabase.getInstance(requireContext());
            com.khanghv.campusexpense.data.database.MonthlyBudgetDao mbDao = db.monthlyBudgetDao();
            com.khanghv.campusexpense.data.model.MonthlyBudget mb = mbDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
            java.util.List<com.khanghv.campusexpense.data.model.BudgetAdjustment> list = mb != null ? db.budgetAdjustmentDao().getByMonthlyBudget(mb.getId()) : new java.util.ArrayList<>();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                adapter.list.clear();
                adapter.list.addAll(list);
                adapter.notifyDataSetChanged();
            });
        }).start();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditAdjustmentDialog(com.khanghv.campusexpense.data.model.BudgetAdjustment adj, Budget budget, androidx.recyclerview.widget.RecyclerView.Adapter<?> adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget_edit_adjustment, null);
        com.google.android.material.textfield.TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        com.google.android.material.textfield.TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        com.google.android.material.textfield.TextInputEditText noteInput = dialogView.findViewById(R.id.noteInput);
        android.widget.Button saveButton = dialogView.findViewById(R.id.saveButton);
        android.widget.Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        amountInput.setText(CurrencyManager.formatEditableValue(requireContext(), adj.getAmount()));
        CurrencyManager.attachInputFormatter(amountInput);
        noteInput.setText(adj.getNote() == null ? "" : adj.getNote());

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
            try {
                double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
                double newAmount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
                double oldAmount = adj.getAmount();
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    com.khanghv.campusexpense.data.database.MonthlyBudgetDao mbDao = db.monthlyBudgetDao();
                    com.khanghv.campusexpense.data.database.ExpenseDao expDao = db.expenseDao();
                    com.khanghv.campusexpense.data.model.MonthlyBudget mb = mbDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
                    if (mb != null) {
                        double newTotal = mb.getTotalBudget() + (newAmount - oldAmount);
                        long[] range = getMonthRange(currentYear, currentMonth);
                        Double spent = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, budget.getCategoryId(), range[0], range[1]);
                        double spentVal = spent == null ? 0.0 : spent;
                        double newRemaining = newTotal - spentVal;
                        if (newRemaining < 0) newRemaining = 0;
                        mb.setTotalBudget(newTotal);
                        mb.setRemainingBudget(newRemaining);
                        mbDao.update(mb);
                    }
                    boolean isTransferTo = startsWithTransferTo(adj.getNote());
                    boolean isTransferFrom = startsWithTransferFrom(adj.getNote());
                    double delta = newAmount - oldAmount;
                    if (isTransferTo || isTransferFrom) {
                        String otherName = isTransferTo ? extractTargetNameFromNote(adj.getNote()) : extractFromNameFromNote(adj.getNote());
                        if (otherName != null) {
                            java.util.List<com.khanghv.campusexpense.data.model.Category> cats = categoryDao.getAllByUser(currentUserId);
                            Integer otherCatId = null;
                            for (com.khanghv.campusexpense.data.model.Category c : cats) {
                                if (otherName.equals(c.getName())) { otherCatId = c.getId(); break; }
                            }
                            if (otherCatId != null) {
                                com.khanghv.campusexpense.data.model.MonthlyBudget otherMb = mbDao.getBudgetByCategoryUserMonth(currentUserId, otherCatId, currentMonth, currentYear);
                                if (otherMb != null) {
                                    double otherNewTotal = otherMb.getTotalBudget() - delta;
                                    long[] range2 = getMonthRange(currentYear, currentMonth);
                                    Double otherSpent = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, otherCatId, range2[0], range2[1]);
                                    double otherSpentVal = otherSpent == null ? 0.0 : otherSpent;
                                    double otherNewRemaining = otherNewTotal - otherSpentVal;
                                    if (otherNewRemaining < 0) otherNewRemaining = 0;
                                    otherMb.setTotalBudget(otherNewTotal);
                                    otherMb.setRemainingBudget(otherNewRemaining);
                                    mbDao.update(otherMb);

                                    java.util.List<com.khanghv.campusexpense.data.model.BudgetAdjustment> otherList = db.budgetAdjustmentDao().getByMonthlyBudget(otherMb.getId());
                                    com.khanghv.campusexpense.data.model.BudgetAdjustment counterpart = null;
                                    if (isTransferFrom) {
                                        for (com.khanghv.campusexpense.data.model.BudgetAdjustment a2 : otherList) {
                                            if (a2.getAmount() == -oldAmount && startsWithTransferTo(a2.getNote()) && a2.getNote().endsWith(budget != null ? categoryDao.getById(budget.getCategoryId()).getName() : "")) {
                                                counterpart = a2; break;
                                            }
                                        }
                                        if (counterpart != null) {
                                            counterpart.setAmount(-newAmount);
                                            db.budgetAdjustmentDao().update(counterpart);
                                        }
                                    } else if (isTransferTo) {
                                        for (com.khanghv.campusexpense.data.model.BudgetAdjustment a2 : otherList) {
                                            if (a2.getAmount() == Math.abs(oldAmount) && startsWithTransferFrom(a2.getNote()) && a2.getNote().endsWith(budget != null ? categoryDao.getById(budget.getCategoryId()).getName() : "")) {
                                                counterpart = a2; break;
                                            }
                                        }
                                        if (counterpart != null) {
                                            counterpart.setAmount(Math.abs(newAmount));
                                            db.budgetAdjustmentDao().update(counterpart);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    adj.setAmount(newAmount);
                    adj.setNote(noteInput.getText() != null ? noteInput.getText().toString().trim() : null);
                    db.budgetAdjustmentDao().update(adj);
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (adapter instanceof androidx.recyclerview.widget.RecyclerView.Adapter) {
                            ((androidx.recyclerview.widget.RecyclerView.Adapter) adapter).notifyDataSetChanged();
                        }
                        refreshBudgetList();
                        dialog.dismiss();
                    });
                }).start();
            } catch (Exception e) {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void deleteAdjustment(com.khanghv.campusexpense.data.model.BudgetAdjustment adj, Budget budget, androidx.recyclerview.widget.RecyclerView.Adapter<?> adapter) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.confirm_delete_budget))
                .setPositiveButton(getString(R.string.delete), (d,w)-> {
                    new Thread(() -> {
                        com.khanghv.campusexpense.data.database.AppDatabase db = com.khanghv.campusexpense.data.database.AppDatabase.getInstance(requireContext());
                        com.khanghv.campusexpense.data.database.MonthlyBudgetDao mbDao = db.monthlyBudgetDao();
                        com.khanghv.campusexpense.data.database.ExpenseDao expDao = db.expenseDao();
                        com.khanghv.campusexpense.data.model.MonthlyBudget mb = mbDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
                        boolean isTransferTo = startsWithTransferTo(adj.getNote());
                        boolean isTransferFrom = startsWithTransferFrom(adj.getNote());

                        if (mb != null) {
                            double newTotal = mb.getTotalBudget() - adj.getAmount();
                            if (newTotal < 0) newTotal = 0;
                            long[] range = getMonthRange(currentYear, currentMonth);
                            Double spent = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, budget.getCategoryId(), range[0], range[1]);
                            double spentVal = spent == null ? 0.0 : spent;
                            double newRemaining = newTotal - spentVal;
                            if (newRemaining < 0) newRemaining = 0;
                            mb.setTotalBudget(newTotal);
                            mb.setRemainingBudget(newRemaining);
                            mbDao.update(mb);
                        }

                        if (isTransferTo || isTransferFrom) {
                            String currentCategoryName = null;
                            try {
                                Category currentCat = categoryDao.getById(budget.getCategoryId());
                                currentCategoryName = currentCat != null ? currentCat.getName() : null;
                            } catch (Exception ignored) {}

                            if (isTransferTo) {
                                String toName = extractTargetNameFromNote(adj.getNote());
                                if (toName != null) {
                                    java.util.List<com.khanghv.campusexpense.data.model.Category> cats = categoryDao.getAllByUser(currentUserId);
                                    Integer toCatId = null;
                                    for (com.khanghv.campusexpense.data.model.Category c : cats) {
                                        if (toName.equals(c.getName())) { toCatId = c.getId(); break; }
                                    }
                                    if (toCatId != null) {
                                        com.khanghv.campusexpense.data.model.MonthlyBudget toMb = mbDao.getBudgetByCategoryUserMonth(currentUserId, toCatId, currentMonth, currentYear);
                                        if (toMb != null) {
                                            double newTotalTo = toMb.getTotalBudget() - Math.abs(adj.getAmount());
                                            if (newTotalTo < 0) newTotalTo = 0;
                                            long[] rangeTo = getMonthRange(currentYear, currentMonth);
                                            Double spentTo = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, toMb.getCategoryId(), rangeTo[0], rangeTo[1]);
                                            double spentValTo = spentTo == null ? 0.0 : spentTo;
                                            double newRemainingTo = newTotalTo - spentValTo;
                                            if (newRemainingTo < 0) newRemainingTo = 0;
                                            toMb.setTotalBudget(newTotalTo);
                                            toMb.setRemainingBudget(newRemainingTo);
                                            mbDao.update(toMb);

                                            java.util.List<com.khanghv.campusexpense.data.model.BudgetAdjustment> toList = db.budgetAdjustmentDao().getByMonthlyBudget(toMb.getId());
                                            com.khanghv.campusexpense.data.model.BudgetAdjustment counterpart = null;
                                            for (com.khanghv.campusexpense.data.model.BudgetAdjustment a2 : toList) {
                                                if (a2.getAmount() == Math.abs(adj.getAmount()) && startsWithTransferFrom(a2.getNote()) && currentCategoryName != null && a2.getNote().endsWith(currentCategoryName)) {
                                                    counterpart = a2; break;
                                                }
                                            }
                                            if (counterpart != null) {
                                                db.budgetAdjustmentDao().deleteById(counterpart.getId());
                                            }
                                        }
                                    }
                                }
                            } else if (isTransferFrom) {
                                String fromName = extractFromNameFromNote(adj.getNote());
                                if (fromName != null) {
                                    java.util.List<com.khanghv.campusexpense.data.model.Category> cats = categoryDao.getAllByUser(currentUserId);
                                    Integer fromCatId = null;
                                    for (com.khanghv.campusexpense.data.model.Category c : cats) {
                                        if (fromName.equals(c.getName())) { fromCatId = c.getId(); break; }
                                    }
                                    if (fromCatId != null) {
                                        com.khanghv.campusexpense.data.model.MonthlyBudget fromMb = mbDao.getBudgetByCategoryUserMonth(currentUserId, fromCatId, currentMonth, currentYear);
                                        if (fromMb != null) {
                                            double newTotalFrom = fromMb.getTotalBudget() + Math.abs(adj.getAmount());
                                            long[] rangeFrom = getMonthRange(currentYear, currentMonth);
                                            Double spentFrom = expDao.getTotalExpensesByCategoryAndDateRange(currentUserId, fromMb.getCategoryId(), rangeFrom[0], rangeFrom[1]);
                                            double spentValFrom = spentFrom == null ? 0.0 : spentFrom;
                                            double newRemainingFrom = newTotalFrom - spentValFrom;
                                            if (newRemainingFrom < 0) newRemainingFrom = 0;
                                            fromMb.setTotalBudget(newTotalFrom);
                                            fromMb.setRemainingBudget(newRemainingFrom);
                                            mbDao.update(fromMb);

                                            java.util.List<com.khanghv.campusexpense.data.model.BudgetAdjustment> fromList = db.budgetAdjustmentDao().getByMonthlyBudget(fromMb.getId());
                                            com.khanghv.campusexpense.data.model.BudgetAdjustment counterpart = null;
                                            for (com.khanghv.campusexpense.data.model.BudgetAdjustment a2 : fromList) {
                                                if (a2.getAmount() == -Math.abs(adj.getAmount()) && startsWithTransferTo(a2.getNote()) && currentCategoryName != null && a2.getNote().endsWith(currentCategoryName)) {
                                                    counterpart = a2; break;
                                                }
                                            }
                                            if (counterpart != null) {
                                                db.budgetAdjustmentDao().deleteById(counterpart.getId());
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        db.budgetAdjustmentDao().deleteById(adj.getId());
                        java.util.List<com.khanghv.campusexpense.data.model.BudgetAdjustment> list = mb != null ? db.budgetAdjustmentDao().getByMonthlyBudget(mb.getId()) : new java.util.ArrayList<>();
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            try {
                                adapter.notifyDataSetChanged();
                            } catch (Exception ignored) {}
                            refreshBudgetList();
                        });
                    }).start();
                })
                .setNegativeButton(getString(R.string.cancel_label), null)
                .show();
    }

    private boolean startsWithTransferTo(String note) {
        if (note == null) return false;
        String pref1 = getString(R.string.transfer_to, "");
        return note.startsWith(pref1) || note.startsWith("Transfer to ") || note.startsWith("Chuyển tới ") || note.startsWith("Chuyển sang ");
    }
    private boolean startsWithTransferFrom(String note) {
        if (note == null) return false;
        String pref1 = getString(R.string.transfer_from, "");
        return note.startsWith(pref1) || note.startsWith("Transfer from ") || note.startsWith("Chuyển từ ");
    }
    private String extractTargetNameFromNote(String note) {
        if (note == null) return null;
        String[] prefixes = { getString(R.string.transfer_to, ""), "Transfer to ", "Chuyển tới ", "Chuyển sang "};
        for (String p : prefixes) { if (note.startsWith(p)) return note.substring(p.length()).trim(); }
        return null;
    }
    private String extractFromNameFromNote(String note) {
        if (note == null) return null;
        String[] prefixes = { getString(R.string.transfer_from, ""), "Transfer from ", "Chuyển từ "};
        for (String p : prefixes) { if (note.startsWith(p)) return note.substring(p.length()).trim(); }
        return null;
    }

    private long[] getMonthRange(int year, int month) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month - 1);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(java.util.Calendar.MONTH, 1);
        cal.add(java.util.Calendar.MILLISECOND, -1);
        long end = cal.getTimeInMillis();
        return new long[]{start, end};
    }

    private void showMonthYearPickerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker monthPicker = view.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = view.findViewById(R.id.yearPicker);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        String[] months = getResources().getStringArray(R.array.months_numbers);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(cal.get(java.util.Calendar.MONTH));
        yearPicker.setMinValue(year - 5);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(year);
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            currentMonth = monthPicker.getValue() + 1;
            currentYear = yearPicker.getValue();
            // ensure budgets exist for this month by carrying over previous month
            repository.ensureBudgetsForMonth(String.format(java.util.Locale.getDefault(), "%04d-%02d", currentYear, currentMonth), currentUserId);
            refreshBudgetList();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }

    private void refreshBudgetList() {
        // Cập nhật text cho nút Select Month
        View btnSelectMonth = getView() != null ? getView().findViewById(R.id.btnSelectMonth) : null;
        if (btnSelectMonth instanceof android.widget.Button) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(currentYear, currentMonth - 1, 1);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault());
            ((android.widget.Button) btnSelectMonth).setText(sdf.format(cal.getTime()));
        }

        budgetList.clear();
        categoryNames.clear();
        java.util.List<com.khanghv.campusexpense.data.model.MonthlyBudget> mbs = monthlyBudgetDao.getBudgetsByUserAndMonth(currentUserId, currentMonth, currentYear);
        for (com.khanghv.campusexpense.data.model.MonthlyBudget mb : mbs) {
            Budget b = new Budget();
            b.setId(mb.getId());
            b.setUserId(mb.getUserId());
            b.setCategoryId(mb.getCategoryId());
            b.setAmount(mb.getTotalBudget());
            b.setPeriod("Monthly");
            b.setCreatedAt(mb.getCreatedAt());
            budgetList.add(b);
            Category category = categoryDao.getById(b.getCategoryId());
            categoryNames.add(category == null ? "Unknown Category" : category.getName());
        }
        adapter.notifyDataSetChanged();
        if (budgetList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showCategoryFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_choose_category, null);
        RecyclerView rv = dialogView.findViewById(R.id.recyclerViewCategories);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        List<Category> cats = new ArrayList<>();
        Category allCat = new Category();
        allCat.setId(-1);
        allCat.setName(getString(R.string.all_categories));
        cats.add(allCat);
        cats.addAll(categoryDao.getAllByUser(currentUserId));

        final android.app.AlertDialog[] dialogPtr = new android.app.AlertDialog[1];
        com.khanghv.campusexpense.ui.adapters.CategorySelectionAdapter adapter = new com.khanghv.campusexpense.ui.adapters.CategorySelectionAdapter(cats, category -> {
            selectedCategoryId = category.getId();
            selectedCategoryName = category.getName();
            View btnFilterCategory = getView() != null ? getView().findViewById(R.id.btnFilterCategory) : null;
            if (btnFilterCategory instanceof android.widget.Button) {
                ((android.widget.Button) btnFilterCategory).setText(selectedCategoryName);
            }
            refreshBudgetList();
            if (dialogPtr[0] != null) dialogPtr[0].dismiss();
        });
        
        adapter.setSelectedCategoryId(selectedCategoryId);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        builder.setView(dialogView);
        dialogPtr[0] = builder.create();
        
        btnCancel.setOnClickListener(v -> dialogPtr[0].dismiss());
        if (dialogPtr[0].getWindow() != null) {
            dialogPtr[0].getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogPtr[0].show();
    }

    private void showDeleteDialog(Budget budget) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_budget))
                .setMessage(getString(R.string.confirm_delete_budget))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                        // delete related transactions and monthly budget record
                        expenseDao.deleteExpensesByBudgetId(budget.getId());
                        com.khanghv.campusexpense.data.model.MonthlyBudget mbToDelete = monthlyBudgetDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
                        if (mbToDelete != null) {
                            monthlyBudgetDao.delete(mbToDelete);
                        }
                        refreshBudgetList();
                        Toast.makeText(requireContext(), getString(R.string.budget_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showAddBudgetDialog() {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_add_categories_first), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        CurrencyManager.attachInputFormatter(amountInput);

        List<String> categoryNameList = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNameList.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNameList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        String[] periods = {getString(R.string.period_monthly), getString(R.string.period_weekly)};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            int categoryPosition = categorySpinner.getSelectedItemPosition();
            String amountStr = amountInput.getText().toString().trim();
            int periodPosition = periodSpinner.getSelectedItemPosition();
            double amount;
            if (TextUtils.isEmpty(amountStr)) {
                amount = 0.0;
            } else {
                double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
                amount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
                if (amount < 0) amount = 0.0;
            }
            Category selectedCategory = categoryList.get(categoryPosition);
            // For monthly budgets, ensure uniqueness per month/year
            com.khanghv.campusexpense.data.model.MonthlyBudget existingMb = monthlyBudgetDao.getBudgetByCategoryUserMonth(currentUserId, selectedCategory.getId(), currentMonth, currentYear);
            if (existingMb != null) {
                Toast.makeText(requireContext(), getString(R.string.budget_exists_selected_month), Toast.LENGTH_SHORT).show();
                return;
            }
            com.khanghv.campusexpense.data.model.MonthlyBudget mb = new com.khanghv.campusexpense.data.model.MonthlyBudget(currentUserId, selectedCategory.getId(), currentMonth, currentYear, amount, amount);
            monthlyBudgetDao.insert(mb);
            refreshBudgetList();
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.budget_added_success), Toast.LENGTH_SHORT).show();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditDialog(Budget budget) {
        categoryList.clear();
        categoryList.addAll(categoryDao.getAllByUser(currentUserId));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputLayout amountLayout = dialogView.findViewById(R.id.amountLayout);
        Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        if (amountLayout != null) {
            String symbol = CurrencyManager.getCurrencySymbol(requireContext());
            amountLayout.setHint(getString(R.string.amount_with_currency, symbol));
        }
        CurrencyManager.attachInputFormatter(amountInput);

        List<String> categoryNameList = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNameList.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNameList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        Category category = categoryDao.getById(budget.getCategoryId());
        if (category != null) {
            int categoryIndex = -1;
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId() == budget.getCategoryId()) {
                    categoryIndex = i;
                    break;
                }
            }
            if (categoryIndex >= 0) {
                categorySpinner.setSelection(categoryIndex);
            }
        }
        categorySpinner.setEnabled(false);

        amountInput.setText(CurrencyManager.formatEditableValue(requireContext(), budget.getAmount()));

        String[] periods = {getString(R.string.period_monthly), getString(R.string.period_weekly)};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);

        int periodIndex = -1;
        for (int i = 0; i < periods.length; i++) {
            if (periods[i].equals(budget.getPeriod())) {
                periodIndex = i;
                break;
            }
        }
        if (periodIndex >= 0) {
            periodSpinner.setSelection(periodIndex);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            int periodPosition = periodSpinner.getSelectedItemPosition();

            if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(requireContext(), getString(R.string.please_enter_amount), Toast.LENGTH_SHORT).show();
                return;
            }

            if (periodPosition < 0 || periodPosition >= periods.length) {
            Toast.makeText(requireContext(), getString(R.string.please_select_period), Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
                amount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), getString(R.string.amount_must_be_greater_than_zero), Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                return;
            }

            // Update corresponding monthly budget record for current month/year
            com.khanghv.campusexpense.data.model.MonthlyBudget mb = monthlyBudgetDao.getBudgetByCategoryUserMonth(currentUserId, budget.getCategoryId(), currentMonth, currentYear);
            if (mb != null) {
                mb.setTotalBudget(amount);
                mb.setRemainingBudget(amount);
                monthlyBudgetDao.update(mb);
            }
            refreshBudgetList();
            dialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.budget_updated_success), Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}


