package com.example.radiointerfacetest;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;

public class InputWithHistoryAndClear extends ConstraintLayout {

    private final static String TAG = "InputWithHistoryAndClear";
    private EditText editText;
    private Spinner historySpinner;
    private ImageButton clearButton;
    private InputHistoryCache historyCache;
    private String historyCacheKey;
    private ArrayAdapter<String> historyAdapter;

    public InputWithHistoryAndClear(Context context) {
        super(context);
        init(context, null);
    }

    public InputWithHistoryAndClear(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InputWithHistoryAndClear(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.input_with_history_and_clear, this, true);

        editText = findViewById(R.id.editText);
        historySpinner = findViewById(R.id.historySpinner);
        clearButton = findViewById(R.id.clearButton);

        historyCache = InputHistoryCache.getInstance(context);

        historyAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Return an empty view to hide the selected item text in the spinner itself
                return new View(getContext());
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView label = (TextView) super.getDropDownView(position, convertView, parent);
                label.setText(getItem(position));
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                return label;
            }
        };
        historySpinner.setAdapter(historyAdapter);

        clearButton.setOnClickListener(v -> editText.setText(""));
        clearButton.setOnLongClickListener(v -> {
            Dialog dialog = new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all "+historyCacheKey+" history?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        InputHistoryCache numberCache = InputHistoryCache.getInstance(getContext());
                        numberCache.clear(historyCacheKey);
                        Toast.makeText(getContext(), historyCacheKey + " cache cleared.", Toast.LENGTH_LONG).show();
                        editText.setText(""); // Clear the text in the component
                        // Ensure UI updates are on the main thread
                        post(() -> {
                            historyAdapter.clear();
                            historyAdapter.notifyDataSetChanged();
                        });
                    })
                    .setNegativeButton("No", null)
                    .create();
            dialog.show();
            return false;
        });

        historySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedItem = (String) parent.getItemAtPosition(position);
                    Log.d(TAG, "onItemSelected position " + position + ", item " + selectedItem);
                    editText.setText(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
                Log.d(TAG, "onNothingSelected ");
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearButton.setVisibility(s.length() > 0 ? VISIBLE : GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initially hide clear button if EditText is empty
        clearButton.setVisibility(editText.getText().length() > 0 ? VISIBLE : GONE);
    }

    public void setHistoryCacheKey(String key) {
        this.historyCacheKey = key;
        loadHistory();
    }

    public String getText() {
        return editText.getText().toString();
    }

    public void setText(String text) {
        editText.setText(text);
    }

    public void setHint(String hint) {
        editText.setHint(hint);
    }

    public void setInputType(int type) {
        editText.setInputType(type);
    }

    public void saveHistory() {
        String currentText = editText.getText().toString().trim();
        if (!currentText.isEmpty() && historyCacheKey != null) {
            historyCache.put(historyCacheKey, currentText);
            loadHistory(); // Reload history to show the newly added item
        }
    }

    private void loadHistory() {
        if (historyCacheKey != null) {
            List<String> history = historyCache.getAll(historyCacheKey);
            List<String> spinnerItems = new ArrayList<String>();
            Log.d(TAG, "loadHistory: history size = " + history.size());
            spinnerItems.addAll(history);

            // Ensure UI updates are on the main thread
            post(() -> {
                historyAdapter.clear();
                historyAdapter.addAll(spinnerItems);
                historyAdapter.notifyDataSetChanged();
            });
        }
    }
}