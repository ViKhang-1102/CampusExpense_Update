package com.khanghv.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.database.ExpenseDao;
import com.khanghv.campusexpense.data.database.PaymentDao;
import com.khanghv.campusexpense.data.model.Category;
import com.khanghv.campusexpense.data.model.Expense;
import com.khanghv.campusexpense.data.model.Payment;
import com.khanghv.campusexpense.ui.payment.CategoryPaymentAdapter;
import com.khanghv.campusexpense.util.CurrencyManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddPayment;
    private TextView emptyView;
    private TextView tvSubtitle;
    private TabLayout tabLayout;

    private PaymentDao paymentDao;
    private ExpenseDao expenseDao;
    private CategoryDao categoryDao;
    private int currentUserId;
    private List<Payment> allPayments;

    // 0: By Category, 1: By Date
    private int currentTab = 1;

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewPayments);
        emptyView = view.findViewById(R.id.emptyPaymentsView);
        fabAddPayment = view.findViewById(R.id.fabAddPayment);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tabLayout = view.findViewById(R.id.tabLayout);

        currentUserId = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("userId", -1);
        AppDatabase db = AppDatabase.getInstance(requireContext());
        paymentDao = db.paymentDao();
        expenseDao = db.expenseDao();
        categoryDao = db.categoryDao();
        allPayments = new ArrayList<>();

        setupTabs();

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        fabAddPayment.setOnClickListener(v -> showAddPaymentDialog());
        refreshData();
        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_by_category)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_by_date)));

        // Default selection
        TabLayout.Tab tab = tabLayout.getTabAt(currentTab);
        if (tab != null) tab.select();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshData();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        new Thread(() -> {
            if (getContext() == null) return;
            List<Payment> payments = paymentDao.getAllByUser(currentUserId);
            List<Category> categories = new ArrayList<>();
            if (currentTab == 0) {
                categories = categoryDao.getAllByUser(currentUserId);
            }

            List<Category> finalCategories = categories;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                allPayments.clear();
                allPayments.addAll(payments);

                if (currentTab == 0) {
                    refreshCategoryData(finalCategories);
                } else {
                    refreshDateData();
                }
                if (getActivity() instanceof com.khanghv.campusexpense.MainActivity) {
                    ((com.khanghv.campusexpense.MainActivity) getActivity()).updatePaymentBadge();
                }
            });
        }).start();
    }

    private void refreshCategoryData(List<Category> categories) {
        Map<Integer, List<Payment>> categoryMap = new HashMap<>();
        Map<Integer, Double> categoryTotal = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();

        // Get all categories to ensure we have names
        for (Category c : categories) {
            categoryNames.put(c.getId(), c.getName());
        }

        double grandTotal = 0;
        int count = 0;

        for (Payment p : allPayments) {
            // Only count Pending payments for "Total" amount usually? 
            // Or total of all payments? ExpenseFragment shows total expenses.
            // Payments are usually debts. Let's show total amount of payments (Pending + Paid).
            // But maybe user wants to see Pending amount specifically?
            // "By Category" -> "Total: $X".
            // Let's stick to total amount of all items in the list.
            
            if (!categoryMap.containsKey(p.getCategoryId())) {
                categoryMap.put(p.getCategoryId(), new ArrayList<>());
                categoryTotal.put(p.getCategoryId(), 0.0);
            }
            categoryMap.get(p.getCategoryId()).add(p);
            categoryTotal.put(p.getCategoryId(), categoryTotal.get(p.getCategoryId()) + p.getAmount());
            
            grandTotal += p.getAmount();
            count++;
        }

        List<CategoryPaymentAdapter.CategoryPaymentItem> items = new ArrayList<>();
        for (Map.Entry<Integer, List<Payment>> entry : categoryMap.entrySet()) {
            int catId = entry.getKey();
            String name = categoryNames.getOrDefault(catId, getString(R.string.unknown_category));
            items.add(new CategoryPaymentAdapter.CategoryPaymentItem(
                    catId,
                    name,
                    categoryTotal.get(catId),
                    entry.getValue().size()
            ));
        }

        CategoryPaymentAdapter adapter = new CategoryPaymentAdapter(items, item -> {
            // Handle click: show dialog with payments for this category
            showCategoryPaymentsDialog(item.categoryId, item.categoryName, categoryMap.get(item.categoryId));
        });

        recyclerView.setAdapter(adapter);
        
        // Update subtitle
        String totalStr = CurrencyManager.formatDisplayCurrency(requireContext(), grandTotal);
        tvSubtitle.setText(String.format("%s: %s | %s: %d", getString(R.string.total), totalStr, getString(R.string.count), count));
        
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshDateData() {
        PaymentAdapter adapter = new PaymentAdapter(allPayments, this::refreshData);
        recyclerView.setAdapter(adapter);
        
        double grandTotal = 0;
        for (Payment p : allPayments) grandTotal += p.getAmount();
        
        String totalStr = CurrencyManager.formatDisplayCurrency(requireContext(), grandTotal);
        tvSubtitle.setText(String.format("%s: %s | %s: %d", getString(R.string.total), totalStr, getString(R.string.count), allPayments.size()));
        
        emptyView.setVisibility(allPayments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showCategoryPaymentsDialog(int categoryId, String categoryName, List<Payment> payments) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment_list, null);
        
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(getString(R.string.expense_title, categoryName));
        
        RecyclerView rv = view.findViewById(R.id.recyclerViewPayments);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        PaymentAdapter adapter = new PaymentAdapter(payments, () -> {
            // Update main data
            refreshData();
            // Update dialog data
            List<Payment> updatedList = new ArrayList<>();
            for (Payment p : allPayments) {
                if (p.getCategoryId() == categoryId) {
                    updatedList.add(p);
                }
            }
            // We need to cast or access the adapter to update it.
            // Since we are inside the lambda, we need a final reference or similar.
            // But we can't reference 'adapter' before it's created.
            // So we need to handle this.
            // Let's use a method in the adapter or setup the callback after creation?
            // Or use a wrapper.
        });
        rv.setAdapter(adapter);
        
        // Fix for recursive reference: set the listener AFTER creation
        adapter.setListener(() -> {
            refreshData();
            List<Payment> updatedList = new ArrayList<>();
            for (Payment p : allPayments) {
                if (p.getCategoryId() == categoryId) {
                    updatedList.add(p);
                }
            }
            adapter.updateList(updatedList);
        });
        
        Button btnClose = view.findViewById(R.id.btnClose);
        
        builder.setView(view);
        AlertDialog dialog = builder.create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showAddPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etNote = dialogView.findViewById(R.id.etNote);
        TextView tvAmountPreview = dialogView.findViewById(R.id.tvAmountPreview);
        Button btnDate = dialogView.findViewById(R.id.btnDate);
        Spinner spCategory = dialogView.findViewById(R.id.spCategory);
        Calendar cal = Calendar.getInstance();
        long[] selectedDate = {cal.getTimeInMillis()};
        int[] selectedTime = {cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)};
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        btnDate.setText(getString(R.string.select_date));

        btnDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (view, year, month, day) -> {
                        cal.set(year, month, day, 0, 0, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        selectedDate[0] = cal.getTimeInMillis();
                        btnDate.setText(df.format(cal.getTime()));
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        com.khanghv.campusexpense.util.CurrencyManager.attachInputFormatter(etAmount);
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String raw = s.toString().trim();
                if (raw.isEmpty()) {
                    tvAmountPreview.setText("");
                    return;
                }
                try {
                    double displayVal = com.khanghv.campusexpense.util.CurrencyManager.parseDisplayAmount(requireContext(), raw);
                    java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(com.khanghv.campusexpense.util.CurrencyManager.getCurrencyLocale(requireContext()));
                    if (com.khanghv.campusexpense.util.CurrencyManager.getDisplayCurrency(requireContext()) == com.khanghv.campusexpense.util.CurrencyManager.CurrencyType.VND) {
                        nf.setMaximumFractionDigits(0);
                        String symbol = com.khanghv.campusexpense.util.CurrencyManager.getCurrencySymbol(requireContext());
                        tvAmountPreview.setText(nf.format(displayVal) + symbol + " (VND)");
                    } else {
                        nf.setMaximumFractionDigits(2);
                        String symbol = com.khanghv.campusexpense.util.CurrencyManager.getCurrencySymbol(requireContext());
                        tvAmountPreview.setText(nf.format(displayVal) + " " + symbol);
                    }
                } catch (Exception e) {
                    tvAmountPreview.setText("");
                }
            }
        });

        List<Category> categories = new ArrayList<>();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        new Thread(() -> {
            List<Category> dbCategories = categoryDao.getAllByUser(currentUserId);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                categories.addAll(dbCategories);
                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.getName());
                categoryAdapter.addAll(names);
                categoryAdapter.notifyDataSetChanged();
            });
        }).start();

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            if (TextUtils.isEmpty(amountStr) || spCategory.getSelectedItemPosition() < 0) {
                 Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
            double amount = CurrencyManager.toBaseCurrency(requireContext(), displayAmount);
            Category cat = categories.get(spCategory.getSelectedItemPosition());
            Payment p = new Payment();
            p.setUserId(currentUserId);
            p.setCategoryId(cat.getId());
            p.setName(cat.getName());
            p.setAmount(amount);
            p.setNote(note);
            p.setDate(selectedDate[0]);
            p.setTimeMinutes(selectedTime[0]);
            p.setStatus("Pending");
            p.setCreatedAt(System.currentTimeMillis());
            long id = paymentDao.insert(p);
            // scheduleAlarm((int) id, p.getDate(), p.getTimeMinutes()); // Notifications removed
            refreshData(); 
            dialog.dismiss();
        });
        dialog.show();
    }

    private void scheduleAlarm(int paymentId, long dateMillis, int timeMin) {
        android.app.AlarmManager am = (android.app.AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);
        int hour = timeMin / 60;
        int minute = timeMin % 60;
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long trigger = cal.getTimeInMillis();
        android.content.Intent intent = new android.content.Intent(requireContext(), com.khanghv.campusexpense.notifications.PaymentAlarmReceiver.class);
        intent.putExtra("payment_id", paymentId);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(requireContext(), paymentId, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 31 ? android.app.PendingIntent.FLAG_MUTABLE : 0));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
        } else {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }

    class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {
        private List<Payment> list;
        private Runnable listener;

        PaymentAdapter(List<Payment> list, Runnable listener) {
            this.list = list;
            this.listener = listener;
        }

        public void setListener(Runnable listener) {
            this.listener = listener;
        }

        public void updateList(List<Payment> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Payment p = list.get(position);
            holder.tvPaymentName.setText(p.getName());
            holder.tvPaymentAmount.setText(CurrencyManager.formatDisplayCurrency(holder.itemView.getContext(), p.getAmount()));
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(p.getDate());
            int hour = p.getTimeMinutes() / 60;
            int minute = p.getTimeMinutes() % 60;
            holder.tvPaymentDateTime.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d • %02d:%02d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR), hour, minute));
            holder.tvPaymentStatus.setText(p.getStatus());
            long now = System.currentTimeMillis();
            long due = p.getDate() + p.getTimeMinutes() * 60000L;
            if ("Paid".equals(p.getStatus())) {
                holder.tvPaymentStatus.setTextColor(0xFF10B981); // Success Green
            } else if ("Pending".equals(p.getStatus())) {
                if (now > due) {
                    holder.tvPaymentStatus.setTextColor(0xFFEF4444); // Error Red
                } else {
                    holder.tvPaymentStatus.setTextColor(0xFFF59E0B); // Warning Amber
                }
            } else {
                holder.tvPaymentStatus.setTextColor(0xFF757575);
            }
            String note = p.getNote();
            if (note == null || note.trim().isEmpty()) {
                holder.tvPaymentNote.setVisibility(View.GONE);
            } else {
                holder.tvPaymentNote.setVisibility(View.VISIBLE);
                holder.tvPaymentNote.setText(note);
            }
            holder.btnPay.setVisibility("Pending".equals(p.getStatus()) ? View.VISIBLE : View.GONE);
            holder.btnPay.setOnClickListener(v -> {
                Expense e = new Expense(currentUserId, p.getCategoryId(), p.getAmount(), p.getNote(), p.getDate());
                e.setCreatedAt(System.currentTimeMillis());
                long expenseId = expenseDao.insert(e);
                p.setStatus("Paid");
                p.setLinkedExpenseId((int) expenseId);
                paymentDao.update(p);
                if (listener != null) listener.run();
            });
            holder.btnEdit.setOnClickListener(v -> showEditPaymentDialog(p));
            holder.btnDelete.setOnClickListener(v -> {
                if (p.getLinkedExpenseId() != null) {
                    Expense linked = expenseDao.getExpenseById(p.getLinkedExpenseId());
                    if (linked != null) expenseDao.delete(linked);
                }
                paymentDao.delete(p);
                if (listener != null) listener.run();
            });
        }
        @Override
        public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPaymentName, tvPaymentAmount, tvPaymentDateTime, tvPaymentStatus, tvPaymentNote;
            android.widget.ImageButton btnPay, btnEdit, btnDelete;
            ViewHolder(View itemView) {
                super(itemView);
                tvPaymentName = itemView.findViewById(R.id.tvPaymentName);
                tvPaymentAmount = itemView.findViewById(R.id.tvPaymentAmount);
                tvPaymentDateTime = itemView.findViewById(R.id.tvPaymentDateTime);
                tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
                tvPaymentNote = itemView.findViewById(R.id.tvPaymentNote);
                btnPay = itemView.findViewById(R.id.btnPay);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    private void showEditPaymentDialog(Payment p) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null);
            EditText etAmount = dialogView.findViewById(R.id.etAmount);
            EditText etNote = dialogView.findViewById(R.id.etNote);
            TextView tvAmountPreview = dialogView.findViewById(R.id.tvAmountPreview);
            Button btnDate = dialogView.findViewById(R.id.btnDate);
            Spinner spCategory = dialogView.findViewById(R.id.spCategory);
            
            etAmount.setText(CurrencyManager.formatEditableValue(requireContext(), p.getAmount()));
            etNote.setText(p.getNote() == null ? "" : p.getNote());
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(p.getDate());
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            btnDate.setText(df.format(cal.getTime()));
            
            long[] selectedDate = {p.getDate()};
            int[] selectedTime = {p.getTimeMinutes()};
            
            btnDate.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(selectedDate[0]);
                DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                        (view, year, month, day) -> {
                            c.set(year, month, day, 0, 0, 0);
                            c.set(Calendar.MILLISECOND, 0);
                            selectedDate[0] = c.getTimeInMillis();
                            btnDate.setText(df.format(c.getTime()));
                        },
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            });
            com.khanghv.campusexpense.util.CurrencyManager.attachInputFormatter(etAmount);
            etAmount.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    String raw = s.toString().trim();
                    if (raw.isEmpty()) {
                        tvAmountPreview.setText("");
                        return;
                    }
                    try {
                        double displayVal = com.khanghv.campusexpense.util.CurrencyManager.parseDisplayAmount(requireContext(), raw);
                        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(com.khanghv.campusexpense.util.CurrencyManager.getCurrencyLocale(requireContext()));
                        if (com.khanghv.campusexpense.util.CurrencyManager.getDisplayCurrency(requireContext()) == com.khanghv.campusexpense.util.CurrencyManager.CurrencyType.VND) {
                            nf.setMaximumFractionDigits(0);
                            String symbol = com.khanghv.campusexpense.util.CurrencyManager.getCurrencySymbol(requireContext());
                            tvAmountPreview.setText(nf.format(displayVal) + symbol + " (VND)");
                        } else {
                            nf.setMaximumFractionDigits(2);
                            String symbol = com.khanghv.campusexpense.util.CurrencyManager.getCurrencySymbol(requireContext());
                            tvAmountPreview.setText(nf.format(displayVal) + " " + symbol);
                        }
                    } catch (Exception e) {
                        tvAmountPreview.setText("");
                    }
                }
            });

            List<Category> categories = new ArrayList<>();
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(categoryAdapter);
            
            new Thread(() -> {
                List<Category> dbCategories = categoryDao.getAllByUser(currentUserId);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    categories.addAll(dbCategories);
                    List<String> names = new ArrayList<>();
                    for (Category c : categories) names.add(c.getName());
                    categoryAdapter.addAll(names);
                    categoryAdapter.notifyDataSetChanged();
                    
                    int idx = 0;
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getId() == p.getCategoryId()) { 
                            idx = i; 
                            break; 
                        }
                    }
                    if (idx < categories.size()) spCategory.setSelection(idx);
                });
            }).start();
            
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            Button btnSave = dialogView.findViewById(R.id.btnSave);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnSave.setOnClickListener(v -> {
                try {
                    String amountStr = etAmount.getText().toString().trim();
                    if (TextUtils.isEmpty(amountStr)) {
                        Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double displayAmount = CurrencyManager.parseDisplayAmount(requireContext(), amountStr);
                    p.setAmount(CurrencyManager.toBaseCurrency(requireContext(), displayAmount));
                    p.setNote(etNote.getText().toString().trim());
                    p.setDate(selectedDate[0]);
                    p.setTimeMinutes(selectedTime[0]);
                    if (spCategory.getSelectedItemPosition() >= 0 && spCategory.getSelectedItemPosition() < categories.size()) {
                        p.setCategoryId(categories.get(spCategory.getSelectedItemPosition()).getId());
                        p.setName(categories.get(spCategory.getSelectedItemPosition()).getName());
                    }
                    
                    paymentDao.update(p);
                    // scheduleAlarm(p.getId(), p.getDate(), p.getTimeMinutes()); // Notifications removed
                    
                    if (p.getLinkedExpenseId() != null) {
                        Expense linked = expenseDao.getExpenseById(p.getLinkedExpenseId());
                        if (linked != null) {
                            linked.setAmount(p.getAmount());
                            linked.setCategoryId(p.getCategoryId());
                            linked.setDescription(p.getNote());
                            linked.setDate(p.getDate());
                            expenseDao.update(linked);
                        }
                    }
                    refreshData();
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error saving payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error opening edit dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
