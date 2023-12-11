package org.joshihut.koboget;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.koboget.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d("Main", "onCreatePreferences-rootkey: " + rootKey);
        setPreferencesFromResource(R.xml.preferences, rootKey);

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("KoboGet", MODE_PRIVATE);

        EditTextPreference koboUrlPref = findPreference(getString(R.string.pref_key_kobo_url));
        if (koboUrlPref != null) {
            koboUrlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Save the new value to SharedPreferences
                    Log.d("Main", "onPreferenceChange-pref_key_kobo_url: " + newValue.toString());

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.pref_key_kobo_url), newValue.toString());
                    editor.commit();
                    return true;
                }
            });
        }

        EditTextPreference koboApiTokenPref = findPreference(getString(R.string.pref_key_kobo_api_token));
        if (koboApiTokenPref != null) {
            koboApiTokenPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Save the new value to SharedPreferences
                    Log.d("Main", "onPreferenceChange-pref_key_kobo_api_token: " + newValue.toString());

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.pref_key_kobo_api_token), newValue.toString());
                    editor.commit();
                    return true;
                }
            });
        }

        EditTextPreference koboExcludedKeysPref = findPreference(getString(R.string.pref_key_kobo_excluded_keys));
        if (koboExcludedKeysPref != null) {
            koboExcludedKeysPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Save the new value to SharedPreferences
                    Log.d("Main", "onPreferenceChange-pref_key_kobo_excluded_keys: " + newValue.toString());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.pref_key_kobo_excluded_keys), newValue.toString());
                    editor.commit();
                    return true;
                }
            });
        }

        EditTextPreference koboIncludedColumnsPref = findPreference(getString(R.string.pref_key_kobo_included_columns));
        if (koboIncludedColumnsPref != null) {
            koboIncludedColumnsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Save the new value to SharedPreferences
                    Log.d("Main", "onPreferenceChange-pref_key_kobo_included_columns: " + newValue.toString());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.pref_key_kobo_included_columns), newValue.toString());
                    editor.commit();
                    return true;
                }
            });
        }

        EditTextPreference koboSearchColumnsPref = findPreference(getString(R.string.pref_key_kobo_search_columns));
        if (koboSearchColumnsPref != null) {
            koboSearchColumnsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Save the new value to SharedPreferences
                    Log.d("Main", "onPreferenceChange-pref_key_kobo_search_columns: " + newValue.toString());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.pref_key_kobo_search_columns), newValue.toString());
                    editor.commit();
                    return true;
                }
            });
        }
    }
}
