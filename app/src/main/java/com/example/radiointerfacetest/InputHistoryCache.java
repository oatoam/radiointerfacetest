package com.example.radiointerfacetest;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InputHistoryCache {

    private final String TAG = InputHistoryCache.class.getName();
    private static InputHistoryCache instance;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "input_history";
    public static final String KEY_NUMBERS = "numbers";
    public static final String KEY_IP = "ipaddresses";
    public static final String KEY_SHELL = "shellcommands";

    private static final List<String> ALL_KEYS = List.of(
        KEY_NUMBERS, 
        KEY_IP,
        KEY_SHELL
    );

//    private Set<String> mCachedNumbers = new HashSet<>();
//    private Map<String, Set<String>> mCaches = new HashMap();

    private Map<String, LruCache<String, Boolean>> mLruCaches = new HashMap<>();

    private int LRU_CACHE_SIZE = 16 * 1024 * 1024;

    private InputHistoryCache(Context context) {

        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        for (String key : ALL_KEYS) {
//            Set<String> thisCache = new HashSet<>();
            LruCache<String, Boolean> thisLruCache = new LruCache<>(LRU_CACHE_SIZE);
            String pref = preferences.getString(key, "");
            String[] values = pref.isEmpty() ? new String[0] : pref.split(",");
            for (String value : values) {
//                thisCache.add(value);
                thisLruCache.put(value, false);
            }
//            mCaches.put(key, thisCache);
            mLruCaches.put(key, thisLruCache);
        }
//        Log.d(TAG, "init mCaches = " + mCaches);
        Log.d(TAG, "init mLruCaches = " + mLruCaches);
    }

    public static synchronized InputHistoryCache getInstance(Context context) {
        if (instance == null) {
            instance = new InputHistoryCache(context);
        }
        return instance;
    }

    public void add(String key, String value) {
        if (key == KEY_NUMBERS && value.startsWith("tel:")) {
            value = value.substring(4);
        }
//        Set<String> thisCache = mCaches.get(key);
        LruCache<String, Boolean> thisLruCache = mLruCaches.get(key);
//        if (thisCache == null) {
//            return;
//        }
//        thisCache.add(value);
//        Log.d(TAG, "cache " + this.mCaches.toString());
        if (thisLruCache == null) {
            return;
        }
        thisLruCache.put(value, false);

        Log.d(TAG, "lrucache " + this.mLruCaches.toString());

        save();

        for (Listener listener : mListeners) {
            listener.onChanged();
        }
    }

    private void save() {
        for (String key : ALL_KEYS) {
            StringBuilder sb = new StringBuilder();
//            Set<String> thisCache = mCaches.get(key);
            LruCache<String, Boolean> thisLruCache = mLruCaches.get(key);
            for (String value : thisLruCache.snapshot().keySet()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(value);
            }
            Log.d(TAG, "save: " + key + " : " + sb.toString());
            preferences.edit().putString(key, sb.toString()).apply();
        }
    }

    public List<String> getAll(String key) {
        List<String> result = new ArrayList<>();
//        Set<String> thisCache = mCaches.get(key);
        LruCache<String, Boolean> thisLruCache = mLruCaches.get(key);
        if (thisLruCache != null) {
            for (String value : thisLruCache.snapshot().keySet()) {
                result.add(value);
            }
        }
        Collections.reverse(result);
//        result.sort(null);
        Log.d(TAG, "getAll "+ key +" return " + result);
        return result;
    }

    public void clear(String key) {
        LruCache<String, Boolean> thisLruCache = mLruCaches.get(key);
        if (thisLruCache != null) {
            thisLruCache.evictAll();
        }

        Log.d(TAG, "lrucache " + this.mLruCaches.toString());

        save();

        for (Listener listener : mListeners) {
            listener.onChanged();
        }
    }

    public interface Listener {
        public void onChanged();
    }

    private Set<Listener> mListeners = new HashSet();

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }
}
