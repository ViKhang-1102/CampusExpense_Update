package com.khanghv.campusexpense.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.khanghv.campusexpense.R;

public class TransactionFragment extends Fragment {
    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);
        View btn = view.findViewById(R.id.btnOpenFilter);
        if (btn != null) {
            btn.setOnClickListener(v -> showFilterDialog());
        }
        return view;
    }

    private void showFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View pickerView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker monthPicker = pickerView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = pickerView.findViewById(R.id.yearPicker);
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

        builder.setView(pickerView);
        builder.setPositiveButton(getString(R.string.apply_label), (d, which) -> {
            int selMonth = monthPicker.getValue() + 1;
            int selYear = yearPicker.getValue();
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("transaction_filter_prefs", android.content.Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt("filter_month", selMonth)
                    .putInt("filter_year", selYear)
                    .apply();
            android.widget.Toast.makeText(requireContext(), R.string.expense_updated, android.widget.Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(getString(R.string.cancel_label), null);
        builder.show();
    }
}
