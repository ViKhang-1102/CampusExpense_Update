package com.khanghv.campusexpense.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Chịu trách nhiệm quản lý đơn vị tiền hiển thị và chuyển đổi từ VND (đơn vị gốc trong DB).
 */
public final class CurrencyManager {

    private static final String PREF_NAME = "settings_prefs";
    private static final String KEY_DISPLAY_CURRENCY = "display_currency";
    private static final String KEY_RATE_BITS = "usd_vnd_rate_bits";
    private static final String KEY_RATE_UPDATED_AT = "usd_vnd_rate_updated_at";
    private static final long CACHE_DURATION_MS = 12 * 60 * 60 * 1000L; // 12 giờ
    private static final double DEFAULT_USD_TO_VND = 24500d;

    private CurrencyManager() {}

    public enum CurrencyType {
        USD, VND
    }

    public interface RateUpdateListener {
        void onSuccess(double rate);
        void onError(Exception exception);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static CurrencyType getDisplayCurrency(Context context) {
        String stored = getPrefs(context).getString(KEY_DISPLAY_CURRENCY, CurrencyType.VND.name());
        try {
            return CurrencyType.valueOf(stored);
        } catch (IllegalArgumentException exception) {
            return CurrencyType.VND;
        }
    }

    public static void setDisplayCurrency(Context context, CurrencyType currencyType) {
        getPrefs(context).edit()
                .putString(KEY_DISPLAY_CURRENCY, currencyType.name())
                .apply();
    }

    public static double getStoredUsdToVndRate(Context context) {
        long bits = getPrefs(context)
                .getLong(KEY_RATE_BITS, Double.doubleToRawLongBits(DEFAULT_USD_TO_VND));
        return Double.longBitsToDouble(bits);
    }

    public static void saveUsdToVndRate(Context context, double rate) {
        getPrefs(context).edit()
                .putLong(KEY_RATE_BITS, Double.doubleToRawLongBits(rate))
                .putLong(KEY_RATE_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void refreshRateIfNeeded(Context context, boolean force, RateUpdateListener listener) {
        SharedPreferences prefs = getPrefs(context);
        long lastUpdated = prefs.getLong(KEY_RATE_UPDATED_AT, 0);
        long now = System.currentTimeMillis();

        if (!force && (now - lastUpdated) < CACHE_DURATION_MS) {
            if (listener != null) {
                listener.onSuccess(getStoredUsdToVndRate(context));
            }
            return;
        }

        CurrencyRateService.fetchUsdToVndRate(new CurrencyRateService.RateCallback() {
            @Override
            public void onSuccess(double usdToVndRate) {
                saveUsdToVndRate(context, usdToVndRate);
                if (listener != null) {
                    listener.onSuccess(usdToVndRate);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (listener != null) {
                    listener.onError(exception);
                }
            }
        });
    }

    public static double toBaseCurrency(Context context, double displayAmount) {
        if (getDisplayCurrency(context) == CurrencyType.VND) {
            return displayAmount;
        }
        return displayAmount * getStoredUsdToVndRate(context);
    }

    public static double fromBaseCurrency(Context context, double baseAmount) {
        if (getDisplayCurrency(context) == CurrencyType.VND) {
            return baseAmount;
        }
        double rate = getStoredUsdToVndRate(context);
        if (rate == 0) {
            return baseAmount;
        }
        return baseAmount / rate;
    }

    public static double parseDisplayAmount(String raw) throws NumberFormatException {
        String normalized = raw != null ? raw.trim() : "";
        if (normalized.isEmpty()) {
            throw new NumberFormatException("Empty amount");
        }
        normalized = normalized.replace(" ", "");
        if (normalized.contains(",")) {
            if (normalized.contains(".")) {
                normalized = normalized.replace(",", "");
            } else {
                normalized = normalized.replace(",", ".");
            }
        }
        return Double.parseDouble(normalized);
    }

    public static double parseDisplayAmount(android.content.Context context, String raw) throws NumberFormatException {
        String input = raw != null ? raw.trim() : "";
        if (input.isEmpty()) throw new NumberFormatException("Empty amount");
        CurrencyType type = getDisplayCurrency(context);
        if (type == CurrencyType.VND) {
            String normalized = input.replace(".", "").replace(",", "");
            return Double.parseDouble(normalized);
        } else {
            try {
                java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
                nf.setGroupingUsed(true);
                nf.setMaximumFractionDigits(2);
                nf.setMinimumFractionDigits(0);
                Number n = nf.parse(input);
                return n.doubleValue();
            } catch (Exception e) {
                return parseDisplayAmount(input);
            }
        }
    }

    public static String formatDisplayCurrency(Context context, double baseAmount) {
        double displayAmount = fromBaseCurrency(context, baseAmount);
        NumberFormat format = NumberFormat.getCurrencyInstance(getCurrencyLocale(context));
        if (getDisplayCurrency(context) == CurrencyType.VND) {
            format.setMaximumFractionDigits(0);
        } else {
            format.setMaximumFractionDigits(2);
        }
        return format.format(displayAmount);
    }

    public static String formatEditableValue(Context context, double baseAmount) {
        double displayAmount = fromBaseCurrency(context, baseAmount);
        NumberFormat nf = NumberFormat.getNumberInstance(getCurrencyLocale(context));
        nf.setGroupingUsed(true);
        if (getDisplayCurrency(context) == CurrencyType.VND) {
            nf.setMaximumFractionDigits(0);
        } else {
            nf.setMaximumFractionDigits(2);
        }
        return nf.format(displayAmount);
    }

    public static Locale getCurrencyLocale(Context context) {
        return getDisplayCurrency(context) == CurrencyType.USD ?
                Locale.US :
                new Locale("vi", "VN");
    }

    public static String getCurrencySymbol(Context context) {
        NumberFormat format = NumberFormat.getCurrencyInstance(getCurrencyLocale(context));
        return format.getCurrency().getSymbol(getCurrencyLocale(context));
    }

    public static void attachInputFormatter(android.widget.EditText editText) {
        if (editText == null) return;
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            private boolean selfChange = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (selfChange) return;
                String raw = s.toString();
                if (raw.isEmpty()) return;
                android.content.Context ctx = editText.getContext();
                try {
                    double val = parseDisplayAmount(ctx, raw);
                    java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(getCurrencyLocale(ctx));
                    if (getDisplayCurrency(ctx) == CurrencyType.VND) {
                        nf.setMaximumFractionDigits(0);
                    } else {
                        nf.setMaximumFractionDigits(2);
                    }
                    String formatted = nf.format(val);
                    selfChange = true;
                    int cursor = formatted.length();
                    editText.setText(formatted);
                    editText.setSelection(Math.min(cursor, formatted.length()));
                } catch (Exception ignored) {
                } finally {
                    selfChange = false;
                }
            }
        };
        editText.addTextChangedListener(watcher);
    }
}

