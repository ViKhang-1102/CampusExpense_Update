package com.khanghv.campusexpense.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.CategoryDao;
import com.khanghv.campusexpense.data.model.Category;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionFragment extends Fragment {
    private CategoryDao categoryDao;
    private SharedPreferences prefs;
    private int currentUserId;
    private View btnFilter;

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);
        prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", -1);
        categoryDao = AppDatabase.getInstance(requireContext()).categoryDao();
        btnFilter = view.findViewById(R.id.btnOpenFilter);
        if (btnFilter != null) btnFilter.setOnClickListener(v -> showFilterDialog());
        updateFilterButtonText();
        return view;
    }

    private void showFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_filter, null);
        android.widget.ListView listView = dialogView.findViewById(R.id.listCategories);
        android.widget.NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        List<Category> categories = categoryDao.getAllByUser(currentUserId);
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.all_categories));
        for (Category c : categories) names.add(c.getName());
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice, names);
        listView.setAdapter(adapter);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);

        SharedPreferences fprefs = requireContext().getSharedPreferences("transaction_filter_prefs", Context.MODE_PRIVATE);
        int savedCatId = fprefs.getInt("filter_category_id", -1);
        int defaultIndex = 0;
        if (savedCatId != -1) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == savedCatId) {
                    defaultIndex = i + 1;
                    break;
                }
            }
        }
        listView.setItemChecked(defaultIndex, true);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        String[] months = getResources().getStringArray(R.array.months_numbers);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        int savedMonth = fprefs.getInt("filter_month", cal.get(Calendar.MONTH) + 1);
        int savedYear = fprefs.getInt("filter_year", year);
        monthPicker.setValue(savedMonth - 1);
        yearPicker.setMinValue(year - 5);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(savedYear);

        builder.setView(dialogView);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            int selIndex = listView.getCheckedItemPosition();
            int selMonth = monthPicker.getValue() + 1;
            int selYear = yearPicker.getValue();
            int selCatId = -1;
            String selCatName = getString(R.string.all_categories);
            if (selIndex > 0) {
                Category c = categories.get(selIndex - 1);
                selCatId = c.getId();
                selCatName = c.getName();
            }
            fprefs.edit()
                    .putInt("filter_month", selMonth)
                    .putInt("filter_year", selYear)
                    .putInt("filter_category_id", selCatId)
                    .putString("filter_category_name", selCatName)
                    .apply();
            updateFilterButtonText();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }

    private void updateFilterButtonText() {
        if (!(btnFilter instanceof com.google.android.material.button.MaterialButton)) return;
        SharedPreferences fprefs = requireContext().getSharedPreferences("transaction_filter_prefs", Context.MODE_PRIVATE);
        int m = fprefs.getInt("filter_month", Calendar.getInstance().get(Calendar.MONTH) + 1);
        int y = fprefs.getInt("filter_year", Calendar.getInstance().get(Calendar.YEAR));
        String cat = fprefs.getString("filter_category_name", getString(R.string.all_categories));
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String text = sdf.format(c.getTime()) + " • " + cat;
        ((com.google.android.material.button.MaterialButton) btnFilter).setText(text);
    }
}
