package com.example.android.sunshine.app;

/**
 * Created by Rajarshi.Sharma on 3/6/2016.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


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
        FetchWeatherTask weatherTask = new FetchWeatherTask(getContext(), forecastArrAdapter);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String zipcode = prefs.getString(getString(R.string.pref_zipcode_key),
                getString(R.string.pref_zipcode_default_value));
        weatherTask.execute(zipcode);
    }


}
