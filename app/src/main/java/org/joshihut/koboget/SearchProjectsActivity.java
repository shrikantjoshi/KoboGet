package org.joshihut.koboget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.koboget.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class SearchProjectsActivity extends AppCompatActivity {
    private Spinner searchColumnListField;

    private EditText textBoxEditText;
    private Button searchButton;
    private TableLayout tableLayout;
    private ProgressBar progressBar;

    private ConcurrentHashMap<String, Integer> resultMap;
    private ArrayAdapter<String> searchColumnListFieldAdapter;
    private String koboUrl;
    private String koboApiToken;
    private String[] koboSearchColumns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_projects);

        getCurrentPreferences();

        textBoxEditText = findViewById(R.id.textBoxEditText);
        searchButton = findViewById(R.id.searchButton);
        progressBar = findViewById(R.id.searchProgressBar);

        searchColumnListField = findViewById(R.id.search_column_list_field);
        searchColumnListFieldAdapter = new ArrayAdapter<String>(SearchProjectsActivity.this,
                android.R.layout.simple_spinner_item);

        searchColumnListFieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchColumnListFieldAdapter.addAll(koboSearchColumns);
        searchColumnListField.setAdapter(searchColumnListFieldAdapter);

        tableLayout = findViewById(R.id.search_table_layout);

        // Set a listener for the search button click
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = textBoxEditText.getText().toString();

                Set<String> selectedProjectNames = ProjectCache.getInstance().getProjectsMap().keySet();
                String projectColumn = searchColumnListField.getSelectedItem().toString();

                progressBar.setVisibility(View.VISIBLE);

                selectedProjectNames.stream()
                        .forEach(p -> getDataFromKobo(p, projectColumn));

                // updateTable();
            }
        });

        // Show the "Up" button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        resultMap = new ConcurrentHashMap();
    }

    private void getCurrentPreferences() {
        SharedPreferences sharedPrefs = getSharedPreferences("KoboGet", MODE_PRIVATE);

        koboUrl = sharedPrefs.getString(getString(R.string.pref_key_kobo_url), getString(R.string.kobo_url_default_value));
        koboApiToken = sharedPrefs.getString(getString(R.string.pref_key_kobo_api_token), getString(R.string.kobo_api_key_default_value));

        String koboSearchColumnPrefs = sharedPrefs.getString(getString(R.string.pref_key_kobo_search_columns), getString(R.string.kobo_search_columns_default_value));
        koboSearchColumns = koboSearchColumnPrefs.split(",");
    }

    private void getDataFromKobo(String projectName, String projectColumnName) {
        // Create a Volley request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        String koboSelectedProjectUri = ProjectCache.getInstance().getProjectsMap().get(projectName);

        Log.d("Main", "kobo: getDataFromKobo apiToken is : " + koboApiToken);
        Log.d("Main", "kobo: getDataFromKobo koboSelectedProject is : " + koboSelectedProjectUri);

        // Make a REST request to mywebsite.org
        String koboQueryUrl;

        String searchText = textBoxEditText.getText().toString().trim();

        String caseInsensitiveSearchText = searchText.toLowerCase().chars()
                .mapToObj(c -> (char) c)
                .map(ch -> Character.isLetter(ch) ? "[" + ch + Character.toUpperCase(ch) + "]" : "" + ch)
                .collect(Collectors.joining());
        Log.d("Main", "kobo: getDataFromKobo case insensitive search text is : " + caseInsensitiveSearchText);

        koboQueryUrl = koboUrl + "/api/v2/assets/" +
                koboSelectedProjectUri +
                "/data.json?limit=1&query={\"" +
                projectColumnName + "\" : " +
                "{\"$regex\" : \"" + caseInsensitiveSearchText + "\"}" +
                "}";

        Log.d("Main", "kobo: getDataFromKobo koboQueryUrl is : " + koboQueryUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, koboQueryUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                int koboDataCount;

                try {
                    koboDataCount = response.getInt("count");
                } catch (JSONException e) {
                    return;
                }

                Log.d("Main", "kobo: getDataFromKobo Got data row count=" + koboDataCount);

                resultMap.put(projectName, koboDataCount);
                updateTable();

                // countDownLatch.countDown();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // countDownLatch.countDown();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // Add header params
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Token " + koboApiToken);
                return headers;
            }
        };

        // Add the request to the queue
        queue.add(jsonObjectRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        setResult(RESULT_OK, new Intent());
        finish();
        return true;
    }
    private void updateTable() {
        if (resultMap.size() < ProjectCache.getInstance().getProjectsMap().size()) {
            return;
        }

        Log.d("Main", "kobo: updateTable after await");

        tableLayout.removeAllViews();

        for(String key : resultMap.keySet()) {
            TableRow tableRow = addTableRow(key, String.valueOf(resultMap.get(key)),
                    (resultMap.get(key) == 0) ? Color.WHITE : Color.parseColor("#add8e6"));

            tableLayout.addView(tableRow);
        }

        progressBar.setVisibility(View.GONE);
    }

    @NonNull
    private TableRow addTableRow(String key, String value, int backgroundColor) {
        TableRow tableRow = new TableRow(tableLayout.getContext());
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(10, 10, 10,10);
        tableRow.setLayoutParams(layoutParams);

        // Add some child views to the TableRow element
        TextView tv1 = new TextView(getApplicationContext());
        int slashIndex = key.indexOf('/');
        tv1.setText(key.substring((slashIndex != -1) ? slashIndex+1 : 0));
        TooltipCompat.setTooltipText(tv1, key);
        tv1.setBackgroundColor(backgroundColor);
        tv1.setTextColor(Color.BLACK);
        tv1.setPadding(10, 10, 10, 10);
        TextView tv2 = new TextView(getApplicationContext());
        tv2.setText(value);
        tv2.setBackgroundColor(backgroundColor);
        tv2.setTextColor(Color.BLACK);
        tv2.setPadding(10, 10, 10, 10);

        tv1.setLayoutParams(new TableRow.LayoutParams(
                0, // Width set to 0 to make it take equal space
                TableRow.LayoutParams.MATCH_PARENT, // Height
                1f // Layout weight to distribute equal space
        ));

        tv2.setLayoutParams(new TableRow.LayoutParams(
                0, // Width set to 0 to make it take equal space
                TableRow.LayoutParams.MATCH_PARENT, // Height
                1f // Layout weight to distribute equal space
        ));

        tableRow.addView(tv1);
        tableRow.addView(tv2);
        return tableRow;
    }
}
