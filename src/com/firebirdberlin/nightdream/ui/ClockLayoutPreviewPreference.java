package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.ui.ClockLayout;

public class ClockLayoutPreviewPreference extends Preference {
    private ClockLayout clockLayout = null;
    private View preferenceView = null;
    private Context context = null;

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        preferenceView = super.onCreateView(parent);

        View summary = preferenceView.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                layoutInflater.inflate(R.layout.clock_layout_preference, summaryParent2, true);

                clockLayout = (ClockLayout) summaryParent2.findViewById(R.id.clockLayout);
            }
        }

        return preferenceView;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());

        clockLayout.setTypeface(settings.typeface);
        clockLayout.setPrimaryColor(settings.clockColor);
        clockLayout.setSecondaryColor(settings.secondaryColor);

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.showDate(settings.showDate);
        clockLayout.showWeather(settings.showWeather);
        clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        WeatherEntry entry = getWeatherEntry(settings);
        clockLayout.update(entry);

        Utility utility = new Utility(getContext());
        Point size = utility.getDisplaySize();
        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayout(size.x - preferenceView.getPaddingLeft()
                                        - preferenceView.getPaddingRight(),
                                 config);
        clockLayout.invalidate();

    }

    private WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.weatherEntry;
        if ( entry.timestamp ==  -1L) {
            entry.setFakeData();
        }
        return entry;
    }
}