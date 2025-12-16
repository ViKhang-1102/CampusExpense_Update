package com.khanghv.campusexpense.util;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gọi API tỷ giá ngoại tệ đơn giản bằng HttpURLConnection.
 */
public final class CurrencyRateService {

    private static final String RATE_URL = "https://open.er-api.com/v6/latest/USD";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private CurrencyRateService() {}

    public interface RateCallback {
        void onSuccess(double usdToVndRate);
        void onError(Exception exception);
    }

    public static void fetchUsdToVndRate(RateCallback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(RATE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Rate API error: " + responseCode);
                }

                InputStream inputStream = connection.getInputStream();
                String response = readStream(inputStream);
                JSONObject jsonObject = new JSONObject(response);
                JSONObject rates = jsonObject.getJSONObject("rates");
                double usdToVnd = rates.getDouble("VND");
                postSuccess(callback, usdToVnd);
            } catch (Exception e) {
                postError(callback, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static void postSuccess(RateCallback callback, double rate) {
        if (callback == null) {
            return;
        }
        MAIN_HANDLER.post(() -> callback.onSuccess(rate));
    }

    private static void postError(RateCallback callback, Exception exception) {
        if (callback == null) {
            return;
        }
        MAIN_HANDLER.post(() -> callback.onError(exception));
    }

    private static String readStream(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }
}

