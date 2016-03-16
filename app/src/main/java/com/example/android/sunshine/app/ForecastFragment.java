package com.example.android.sunshine.app;

/**
 * Created by Rajarshi.Sharma on 3/6/2016.
 */

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    ArrayAdapter<String> forecastArrAdapter;


    public ForecastFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        String[] dataArray = {""};
        List weeklyForecast = new ArrayList<String>(Arrays.asList(dataArray));
        forecastArrAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, weeklyForecast);
        ListView forecastsListView = (ListView) rootView.findViewById(R.id.list_view_forecast);
        forecastsListView.setAdapter(forecastArrAdapter);

        forecastsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = forecastArrAdapter.getItem(position);
               /* Toast.makeText(getActivity(),forecast,Toast.LENGTH_SHORT).show();*/

                //intent
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(detailIntent);
            }

        });

        return rootView;
    }


    //when the  fragment lifecycle begins, indicate that an options menu is present to the main
    //activity
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //populating with api data
    }

    @Override
    public void onStart() {
        updateWeather();
        super.onStart();
    }

    // the menu from the fragment is inflated to the activity level menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    //the action to perform when a fragment menu item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateWeather();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String zipcode = prefs.getString(getString(R.string.pref_zipcode_key),
                getString(R.string.pref_zipcode_default_value));
        weatherTask.execute(zipcode);
    }

    /**
     * FetchWeather task is the async task to be used to refresh the screen
     */
    private class FetchWeatherTask extends AsyncTask<String, Void, List> {
        @Override
        protected List doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            String rawJsonForecastStr = null;
            BufferedReader reader = null;
            InputStream inputStream = null;
            URL url = null;
            //variable to contain the passed vZipcode to be checked
            String vZipcode;
            //return value
            List resultList = null;

            //check for params. if there are none then nothing is checked for
            if (params.length == 0) {
                return null;
            } else {
                vZipcode = params[0];
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String vUnits = prefs.getString(getString(R.string.pref_units_key),"imperial");

            //query parameter values
            String vCount = "7",
                    vMode = "JSON",
                    vAppID = BuildConfig.WEATHER_API_KEY;


            try {

                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?",
                        ZIP_PARAM = "zip",
                        COUNT_PARAM = "cnt",
                        MODE_PARAM = "vMode",
                        UNIT_PARAM = "units",
                        APPID_PARAM = "appid";
                Uri uri = Uri.parse(BASE_URL);

                uri = uri.buildUpon().appendQueryParameter(ZIP_PARAM, vZipcode)
                        .appendQueryParameter(COUNT_PARAM, vCount)
                        .appendQueryParameter(MODE_PARAM, vMode)
                        .appendQueryParameter(UNIT_PARAM, vUnits)
                        .appendQueryParameter(APPID_PARAM, vAppID).build();

                url = new URL(uri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                inputStream = urlConnection.getInputStream();
                StringBuffer strBuffer = new StringBuffer();
                if (inputStream == null) {
                    rawJsonForecastStr = null;
                } else {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        strBuffer.append(line + "\n");
                    }
                    if (strBuffer.length() == 0) {
                        rawJsonForecastStr = null;
                    } else {
                        rawJsonForecastStr = strBuffer.toString();
                    }
                }
                resultList = Arrays.asList(getWeatherDataFromJson(rawJsonForecastStr,7));

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error", e);
                rawJsonForecastStr = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                rawJsonForecastStr = null;

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error", e);
                rawJsonForecastStr = null;

            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
                try {
                    if (inputStream != null)
                        inputStream.close();
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error", e);
                }
            }

            return resultList;
        }
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(List result) {
            forecastArrAdapter.clear();
            forecastArrAdapter.addAll(result);
        }



        /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }



        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            return resultStrs;

        }
    }


}
