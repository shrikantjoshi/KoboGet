package org.joshihut.koboget;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SETTINGS_ACTIVITY = 0;
    private static final int REQUEST_CODE_SEARCH_PROJECTS_ACTIVITY = 1;
    private EditText textField;
    private CheckBox regexField;
    private CheckBox ignoreCaseField;
    private Spinner projectListField;
    private Spinner columnListField;
    private TableLayout tableLayout;
    private View paginationView;
    private Button prevButton;
    private Button nextButton;
    private TextView pageIndicator;
    private ProgressBar progressBar;
    private int currentPage = 0;

    private List<String> columns = new ArrayList<>();
    private String selectedColumn;

    private String koboUrl;
    private String koboApiToken;
    private String[] excludedKeysArray;
    private String[] includedColumnsArray;

    private ArrayAdapter<String> projectListFieldAdapter;
    private ArrayAdapter<String> columnListFieldAdapter;

    private JSONArray koboData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        getCurrentPreferences();
        getAllProjectsFromKobo();

        regexField = findViewById(R.id.regexCheckBox);
        ignoreCaseField = findViewById(R.id.ignoreCaseCheckBox);
        textField = findViewById(R.id.text_field);

        projectListField = findViewById(R.id.project_list_field);
        projectListFieldAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item);

        projectListFieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectListField.setAdapter(projectListFieldAdapter);

        columnListField = findViewById(R.id.column_list_field);
        columnListFieldAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item);

        columnListFieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        columnListField.setAdapter(columnListFieldAdapter);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDataFromKobo();
            }
        });

        // Set a selection listener on projects spinner
        projectListField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                getAllColumnsForProject(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Set a selection listener on projects spinner
        columnListField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedColumn = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        tableLayout = findViewById(R.id.table_layout);

        paginationView = findViewById(R.id.paginationView);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pageIndicator = findViewById(R.id.pageIndicator);

        // Set up click listeners for pagination controls
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    showData();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage < (koboData.length() - 1)) {
                    currentPage++;
                    showData();
                }
            }
        });

        paginationView.setVisibility(View.GONE);
    }

    private void getCurrentPreferences() {
        SharedPreferences sharedPrefs = getSharedPreferences("KoboGet", MODE_PRIVATE);

        koboUrl = sharedPrefs.getString(getString(R.string.pref_key_kobo_url), getString(R.string.kobo_url_default_value));
        koboApiToken = sharedPrefs.getString(getString(R.string.pref_key_kobo_api_token), getString(R.string.kobo_api_key_default_value));

        String excludedKeys = sharedPrefs.getString(getString(R.string.pref_key_kobo_excluded_keys),
                getString(R.string.kobo_excluded_keys_default_value));
        excludedKeysArray = excludedKeys.split(",");

        String includedColumns = sharedPrefs.getString(getString(R.string.pref_key_kobo_included_columns),
                getString(R.string.kobo_included_columns_default_value));
        includedColumnsArray = includedColumns.split(",");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    // Handle item click events
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS_ACTIVITY);
            return true;
        } else if (id == R.id.searchProjects) {
            Intent intent = new Intent(MainActivity.this, SearchProjectsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SEARCH_PROJECTS_ACTIVITY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Check the request code to see if it matches the request code that was passed to the startActivityForResult() method
        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            // Check the result code to see if the started activity returned successfully
            if (resultCode == RESULT_OK) {
                getCurrentPreferences();
            }
        }
    }

    private void updateProjectSpinnerWithNewData() {
        Map<String, String> projectMap = ProjectCache.getInstance().getProjectsMap();
        Log.d("Main", "kobo: Projects: " + projectMap.keySet().toString());
        List<String> projects = projectMap.keySet().stream()
                .map(p -> p.toString())
                .collect(Collectors.toList());

        Log.d("Main", "kobo: Project size: " + projectMap.size());

        projectListFieldAdapter.clear();
        projectListFieldAdapter.addAll(projects); // Add the new item to the adapter

        // Notify the adapter that the data set has changed
        projectListFieldAdapter.notifyDataSetChanged();
    }

    private void updateColumnSpinnerWithNewData() {
        Log.d("Main", "kobo: Columns: " + columns);
        Log.d("Main", "kobo: Colmns size: " + columns.size());

        columnListFieldAdapter.clear();
        columnListFieldAdapter.addAll(columns); // Add the new item to the adapter

        // Notify the adapter that the data set has changed
        columnListFieldAdapter.notifyDataSetChanged();
    }

    /*
    To get all metadat for all forms, the url is : https://kf.kobotoolbox.org/api/v2/asset_snapshots/
    The important fields in the json response are:
        results array => source => survey array => type, $xpath, label array
        results array => uid (has id that is used fro snapshot info), asset (id used for project data info), asset_version_id
     */
    private void getAllColumnsForProject(String projectName) {
        progressBar.setVisibility(View.VISIBLE);

        // Create a Volley request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        Log.d("Main", "kobo: getAllColumnsForProject selectedProject is : " + projectName);

        String projectUri = ProjectCache.getInstance().getProjectsMap().get(projectName);
        Log.d("Main", "kobo: getAllColumnsForProject selectedProjectUri is : " + projectUri);

        // Make a REST request to mywebsite.org
        String koboQueryUrl = koboUrl + "/api/v2/assets/" + projectUri + "/data.json?sort={\"start\":-1}&limit=1";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, koboQueryUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONObject personData;
                try {
                    personData = (JSONObject) response.getJSONArray("results").get(0);
                } catch (JSONException e) {
                    progressBar.setVisibility(View.GONE);
                    showError("Error in getting project columns from Kobo");
                    return;
                }

                columns = new ArrayList<>();
                for (Iterator keys = personData.keys(); keys.hasNext();) {
                    String key = (String) keys.next();
                    if (isValidColumn(key)) {
                        columns.add(key);
                    }
                }

                columns.stream().forEach(p ->
                        Log.d("Main", "kobo: getAllColumnsForProject Columns found : name=" + p));

                updateColumnSpinnerWithNewData();
                progressBar.setVisibility(View.GONE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                showError("Error in getting project columns from Kobo");
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

    private void getAllProjectsFromKobo() {
        progressBar.setVisibility(View.VISIBLE);

        // Create a Volley request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        String koboProjectsUrl = koboUrl + "/api/v2/assets.json";

        Log.d("Main", "kobo: apiToken is : " + koboApiToken);
        Log.d("Main", "kobo: koboProjectUrl=" + koboProjectsUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, koboProjectsUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray projectsData;
                try {
                    Log.d("Main", "kobo: Projects: getting results");
                    projectsData = (JSONArray) response.getJSONArray("results");
                } catch (JSONException e) {
                    Log.d("Main", "kobo: Projects: No data found for project results");
                    progressBar.setVisibility(View.GONE);
                    showError("Error in getting projects from Kobo");
                    return;
                }

                Log.d("Main", "kobo: Projects: looping through results");

                Map<String, String> projectMap = new HashMap();

                for (int i = 0; i < projectsData.length(); i++) {
                    try {
                        JSONObject project = projectsData.getJSONObject(i);
                        String deploymentStatus = (String) project.getString("deployment_status");
                        if (deploymentStatus.equalsIgnoreCase("deployed")) {
                            String uid = (String) project.getString("uid");
                            String name = (String) project.getString("name");
                            projectMap.put(name, uid);
                            Log.d("Main", "kobo: Projects: Adding to projects name=" + name + " uid=" + uid);
                        }
                    } catch (JSONException e) {
                        progressBar.setVisibility(View.GONE);
                        showError("Error in processing projects from Kobo");
                        throw new RuntimeException(e);
                    }
                }

                projectMap.entrySet().stream().forEach(p ->
                        Log.d("Main", "kobo: Projects found : name=" + p.getKey() + ", uid=" + p.getValue()));

                ProjectCache.getInstance().cacheProjectsMap(projectMap);

                updateProjectSpinnerWithNewData();
                progressBar.setVisibility(View.GONE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                showError("Error in getting projects from Kobo");
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
    private void getDataFromKobo() {
        // Create a Volley request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        String selectedProjectName;
        if (projectListField.getSelectedItem() == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        selectedProjectName = projectListField.getSelectedItem().toString();

        projectListField.getSelectedItem().toString();
        String koboSelectedProjectUri = ProjectCache.getInstance().getProjectsMap().get(selectedProjectName);
        boolean isRegex = regexField.isChecked();
        boolean isIgnoreCase = ignoreCaseField.isChecked();

        Log.d("Main", "kobo: getDataFromKobo apiToken is : " + koboApiToken);
        Log.d("Main", "kobo: getDataFromKobo koboSelectedProject is : " + koboSelectedProjectUri);
        Log.d("Main", "kobo: getDataFromKobo koboSelectedColumn is : " + selectedColumn);

        // Make a REST request to mywebsite.org
        String koboQueryUrl;

        String searchText = textField.getText().toString().trim();

        if (isIgnoreCase) {
            String caseInsensitiveSearchText = searchText.toLowerCase().chars()
                    .mapToObj(c -> (char) c)
                    .map(ch -> Character.isLetter(ch) ? "[" + ch + Character.toUpperCase(ch) + "]" : "" + ch)
                    .collect(Collectors.joining());
            Log.d("Main", "kobo: getDataFromKobo case insensitive search text is : " + caseInsensitiveSearchText);

            koboQueryUrl = koboUrl + "/api/v2/assets/" +
                    koboSelectedProjectUri +
                    "/data.json?query={\"" +
                    selectedColumn + "\" : " +
                    "{\"$regex\" : \"" + caseInsensitiveSearchText + "\"}" +
                    "}";
        } else if (isRegex) {
                koboQueryUrl = koboUrl + "/api/v2/assets/" +
                        koboSelectedProjectUri +
                        "/data.json?query={\"" +
                        selectedColumn + "\" : " +
                        "{\"$regex\" : \"" + searchText + "\"}" +
                        "}";
        } else {
            koboQueryUrl = koboUrl + "/api/v2/assets/" +
                    koboSelectedProjectUri +
                    "/data.json?query={\"" +
                    selectedColumn + "\" : " +
                    "\"" + searchText + "\"" +
                    "}";
        }

        Log.d("Main", "kobo: getDataFromKobo koboQueryUrl is : " + koboQueryUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, koboQueryUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Get the TableLayout element from the XML layout file
                tableLayout.removeAllViews();

                try {
                    koboData = response.getJSONArray("results");
                } catch (JSONException e) {
                    progressBar.setVisibility(View.GONE);
                    showError("No data found for " + selectedColumn+ " : " + searchText);
                    return;
                }

                Log.d("Main", "kobo: getDataFromKobo Got data rows=" + koboData.length());

                currentPage = 0;
                showData();
                progressBar.setVisibility(View.GONE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                showError(error.getMessage());
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

    private void showData() {
        JSONObject personData;
        try {
            personData = (JSONObject) koboData.get(currentPage);
        } catch (JSONException e) {
            showError("No data found for " + selectedColumn+ " : " + textField.getText());
            return;
        }

        boolean alternateColor = false;
        tableLayout.removeAllViews();
        for (Iterator keys = personData.keys(); keys.hasNext();) {
            String key = (String) keys.next();
            if (isValidKey(key)) {
                // Create a new TableRow element
                TableRow tableRow = null;
                try {
                    tableRow = addTableRow(key, personData.getString(key),
                            alternateColor ? Color.WHITE : Color.parseColor("#add8e6"));
                    alternateColor = !alternateColor;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Add the TableRow element to the TableLayout element
                tableLayout.addView(tableRow);
            }
        }

        pageIndicator.setText("Page: " + (currentPage + 1) + " of " + koboData.length());
        paginationView.setVisibility(View.VISIBLE);
    }

    private boolean isValidKey(String key) {
        for (String excludedKey : excludedKeysArray) {
            if (key.matches(excludedKey)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidColumn(String key) {
        for (String includedColumn : includedColumnsArray) {
            if (key.matches(includedColumn)) {
                return true;
            }
        }
        Log.d("Main", "isValidColumn: excluding column : " + key);
        return false;
    }

    private void showError(String message) {
        // Handle the error
        tableLayout = findViewById(R.id.table_layout);
        tableLayout.removeAllViews();
        paginationView.setVisibility(View.GONE);

        showErrorDialog(MainActivity.this, message);
    }

    private void showErrorDialog(Context context, String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setTitle("Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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