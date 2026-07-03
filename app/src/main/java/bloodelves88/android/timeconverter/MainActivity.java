package bloodelves88.android.timeconverter;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private static final String DST_NOT_OBSERVED = "Daylight saving time is currently not observed. " +
            "Consider using %s instead";
    private static final String DST_OBSERVED = "Daylight saving time is currently being observed. " +
            "Consider using %s instead";
    private static final int SETTINGS_DONE = 1;
    private boolean is12HourFormat = true;
    private Calendar inputCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        inputCalendar.set(Calendar.MINUTE, 0);
        inputCalendar.set(Calendar.SECOND, 0);
        inputCalendar.set(Calendar.MILLISECOND, 0);

        updateUIUsingSettings();

        // Configure the source dropdown list such that it updates the converted time when changed
        Spinner sourceTimeZoneSpinner = (Spinner) findViewById(R.id.from_timezones_spinner);
        sourceTimeZoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                convertTime();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        // Configure the target dropdown list such that it updates the converted time when changed
        Spinner targetTimeZoneSpinner = (Spinner) findViewById(R.id.to_timezones_spinner);
        setDefaultTargetSpinnerSelection(targetTimeZoneSpinner);
        targetTimeZoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                convertTime();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        Button timeButton = (Button) findViewById(R.id.timeButton);
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        inputCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        inputCalendar.set(Calendar.MINUTE, minute);
                        updateInputButtons();
                        convertTime();
                    }
                }, inputCalendar.get(Calendar.HOUR_OF_DAY), inputCalendar.get(Calendar.MINUTE), !is12HourFormat).show();
            }
        });

        Button dateButton = (Button) findViewById(R.id.dateButton);
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        inputCalendar.set(Calendar.YEAR, year);
                        inputCalendar.set(Calendar.MONTH, month);
                        inputCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateInputButtons();
                        convertTime();
                    }
                }, inputCalendar.get(Calendar.YEAR), inputCalendar.get(Calendar.MONTH), inputCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        updateInputButtons();
    }

    private void updateInputButtons() {
        Button dateButton = (Button) findViewById(R.id.dateButton);
        Button timeButton = (Button) findViewById(R.id.timeButton);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        dateButton.setText(dateFormat.format(inputCalendar.getTime()));

        String timePattern = is12HourFormat ? "h:mm a" : "HH:mm";
        SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern, Locale.US);
        timeButton.setText(timeFormat.format(inputCalendar.getTime()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivityForResult(intent, SETTINGS_DONE);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_DONE) {
            updateUIUsingSettings();
            convertTime();
        }
    }

    private void updateUIUsingSettings() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Button dateButton = (Button) findViewById(R.id.dateButton);

        if (dateButton != null) {
            boolean isDatePickerShown = sharedPrefs.getBoolean(getString(R.string.date_picker_preference_key), true);
            if (isDatePickerShown) {
                dateButton.setVisibility(View.VISIBLE);
            } else {
                dateButton.setVisibility(View.GONE);
            }
        }

        String result = sharedPrefs.getString(getString(R.string.time_format_preference_key), "12 hour");
        if (result.equals("12 hour")) {
            is12HourFormat = true;
        } else {
            is12HourFormat = false;
        }
        updateInputButtons();
    }

    private void setDefaultTargetSpinnerSelection(Spinner targetTimeZoneSpinner) {
        TimeZone currentTimeZone = TimeZone.getDefault();

        if (currentTimeZone.getDisplayName(false, TimeZone.SHORT).equals("GMT-08:00")) {
            targetTimeZoneSpinner.setSelection(0);
        } else if (currentTimeZone.getDisplayName(false, TimeZone.SHORT).equals("GMT-07:00")) {
            targetTimeZoneSpinner.setSelection(1);
        } else if (currentTimeZone.getDisplayName(false, TimeZone.SHORT).equals("GMT-05:00")) {
            targetTimeZoneSpinner.setSelection(2);
        } else if (currentTimeZone.getDisplayName(false, TimeZone.SHORT).equals("GMT-04:00")) {
            targetTimeZoneSpinner.setSelection(3);
        } else if (currentTimeZone.getDisplayName(false, TimeZone.SHORT).equals("GMT+01:00")) {
            targetTimeZoneSpinner.setSelection(4);
        } else if (currentTimeZone.getDisplayName(false, TimeZone.SHORT).equals("GMT+02:00")) {
            targetTimeZoneSpinner.setSelection(5);
        } else {
            targetTimeZoneSpinner.setSelection(6);
        }
    }

    public void convertTime() {
        Calendar date = getInputDateAndTime();

        TimeZone sourceTimeZoneGMT = getSourceTimeZoneGMT();
        TimeZone sourceTimeZoneLocation = getSourceTimeZoneLocation();
        boolean isObservingDaylightSavings = sourceTimeZoneLocation.inDaylightTime(date.getTime());

        int sourceTimeOffset = getSourceTimeOffset(sourceTimeZoneGMT, date);
        int targetTimeOffset = getTargetTimeOffset();

        int adjustment = targetTimeOffset - sourceTimeOffset;
        date.add(Calendar.MILLISECOND, +adjustment);

        setDaylightSavingsText(isObservingDaylightSavings, sourceTimeZoneGMT);
        setTimeText(date);
        setCurrentTimeZoneText();
        setDateText(date);
    }

    private void setDaylightSavingsText(boolean isObservingDaylightSavings, TimeZone sourceTimeZone) {
        TextView daylightSavingsText = (TextView) findViewById(R.id.daylight_savings_warning);

        daylightSavingsText.setVisibility(View.VISIBLE);

        String shortName = sourceTimeZone.getDisplayName(false, TimeZone.SHORT);
        if (isObservingDaylightSavings == false) {
            if (shortName.equals("GMT-07:00")) {
                daylightSavingsText.setText(String.format(DST_NOT_OBSERVED, "PST (UTC-08:00)"));
            } else if (shortName.equals("GMT-04:00")) {
                daylightSavingsText.setText(String.format(DST_NOT_OBSERVED, "EST (UTC-05:00)"));
            } else if (shortName.equals("GMT+02:00")) {
                daylightSavingsText.setText(String.format(DST_NOT_OBSERVED, "CET (UTC+01:00)"));
            } else {
                daylightSavingsText.setVisibility(View.GONE);
            }
        } else if (isObservingDaylightSavings == true) {
            if (shortName.equals("GMT-08:00")) {
                daylightSavingsText.setText(String.format(DST_OBSERVED, "PDT (UTC-07:00)"));
            } else if (shortName.equals("GMT-05:00")) {
                daylightSavingsText.setText(String.format(DST_OBSERVED, "EDT (UTC-04:00)"));
            } else if (shortName.equals("GMT+01:00")) {
                daylightSavingsText.setText(String.format(DST_OBSERVED, "CEST (UTC+02:00)"));
            } else {
                daylightSavingsText.setVisibility(View.GONE);
            }
        }
    }

    private int getSourceTimeOffset(TimeZone sourceTimeZone, Calendar date) {
        return sourceTimeZone.getOffset(GregorianCalendar.AD,
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH),
                date.get(Calendar.DAY_OF_WEEK),
                date.get(Calendar.MILLISECOND));
    }

    private TimeZone getSourceTimeZoneGMT() {
        Spinner timeZoneSpinner = (Spinner) findViewById(R.id.from_timezones_spinner);
        long spinnerSelectionId = timeZoneSpinner.getSelectedItemId();

        return getTimeZoneGMT(spinnerSelectionId);
    }

    private TimeZone getSourceTimeZoneLocation() {
        Spinner timeZoneSpinner = (Spinner) findViewById(R.id.from_timezones_spinner);
        long spinnerSelectionId = timeZoneSpinner.getSelectedItemId();

        return getTimeZoneLocation(spinnerSelectionId);
    }

    private int getTargetTimeOffset() {
        Spinner timeZoneSpinner = (Spinner) findViewById(R.id.to_timezones_spinner);
        long spinnerSelectionId = timeZoneSpinner.getSelectedItemId();

        Calendar today = Calendar.getInstance();

        TimeZone targetTimeZone = getTimeZoneGMT(spinnerSelectionId);

        return targetTimeZone.getOffset(GregorianCalendar.AD, today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
                today.get(Calendar.DAY_OF_WEEK),
                today.get(Calendar.MILLISECOND));
    }

    private TimeZone getTimeZoneGMT(long spinnerSelectionId) {
        TimeZone timeZone;
        if (spinnerSelectionId == 0) {
            timeZone = TimeZone.getTimeZone("GMT-8:00");
        } else if (spinnerSelectionId == 1) {
            timeZone = TimeZone.getTimeZone("GMT-7:00");
        } else if (spinnerSelectionId == 2) {
            timeZone = TimeZone.getTimeZone("GMT-5:00");
        } else if (spinnerSelectionId == 3) {
            timeZone = TimeZone.getTimeZone("GMT-4:00");
        } else if (spinnerSelectionId == 4) {
            timeZone = TimeZone.getTimeZone("GMT+1:00");
        } else if (spinnerSelectionId == 5) {
            timeZone = TimeZone.getTimeZone("GMT+2:00");
        } else {
            timeZone = TimeZone.getTimeZone("GMT+8:00");
        }
        return timeZone;
    }

    private TimeZone getTimeZoneLocation(long spinnerSelectionId) {
        TimeZone timeZone;
        if (spinnerSelectionId == 0 || spinnerSelectionId == 1) {
            timeZone = TimeZone.getTimeZone("America/Los_Angeles");
        } else if (spinnerSelectionId == 2 || spinnerSelectionId == 3) {
            timeZone = TimeZone.getTimeZone("America/New_York");
        } else if (spinnerSelectionId == 4 || spinnerSelectionId == 5) {
            timeZone = TimeZone.getTimeZone("Europe/Brussels");
        } else {
            timeZone = TimeZone.getTimeZone("Asia/Singapore");
        }
        return timeZone;
    }

    private void setTimeText(Calendar date) {
        TextView convertedTimeText = (TextView) findViewById(R.id.converted_time);

        int hour = date.get(Calendar.HOUR);
        int minute = date.get(Calendar.MINUTE);

        if (is12HourFormat) {
            if (hour == 0) {
                hour = 12;
            }

            if (minute == 0) {
                convertedTimeText.setText(hour + ":" + minute + "0");
            } else if (minute < 10) {
                convertedTimeText.setText(hour + ":0" + minute);
            } else {
                convertedTimeText.setText(hour + ":" + minute);
            }

            if (date.get(Calendar.AM_PM) == Calendar.AM) {
                convertedTimeText.append(" am");
            } else {
                convertedTimeText.append(" pm");
            }
        } else {
            if (date.get(Calendar.AM_PM) == Calendar.PM) {
                hour += 12;
            }
            if (minute == 0) {
                convertedTimeText.setText(hour + ":" + minute + "0");
            } else if (minute < 10) {
                convertedTimeText.setText(hour + ":0" + minute);
            } else {
                convertedTimeText.setText(hour + ":" + minute);
            }
        }
    }

    private void setDateText(Calendar date) {
        TextView convertedDateText = (TextView) findViewById(R.id.converted_date);

        String month = "";
        int monthNumber = date.get(Calendar.MONTH);
        if (monthNumber == 0) {
            month = "Jan";
        } else if (monthNumber == 1) {
            month = "Feb";
        } else if (monthNumber == 2) {
            month = "Mar";
        } else if (monthNumber == 3) {
            month = "Apr";
        } else if (monthNumber == 4) {
            month = "May";
        } else if (monthNumber == 5) {
            month = "Jun";
        } else if (monthNumber == 6) {
            month = "Jul";
        } else if (monthNumber == 7) {
            month = "Aug";
        } else if (monthNumber == 8) {
            month = "Sep";
        } else if (monthNumber == 9) {
            month = "Oct";
        } else if (monthNumber == 10) {
            month = "Nov";
        } else if (monthNumber == 11) {
            month = "Dec";
        }

        convertedDateText.setText(date.get(Calendar.DATE) + " "
                + month + " " + date.get(Calendar.YEAR));
    }

    private void setCurrentTimeZoneText() {
        TextView currentTimeZone = (TextView) findViewById(R.id.current_timezone);
        TimeZone tz = TimeZone.getDefault();
        currentTimeZone.setText("(you are in the " + tz.getID() + " timezone, " +
                tz.getDisplayName(false, TimeZone.SHORT).replace("GMT", "UTC") + ")");
    }

    private Calendar getInputDateAndTime() {
        return (Calendar) inputCalendar.clone();
    }
}
