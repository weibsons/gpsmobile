package mobi.stos.gpsmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import mobi.stos.gpsmobile.util.Constants;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText etUpdateInterval;
    private EditText etSmallestDisplacement;
    private EditText etEndPoint;

    private final long oneMinute = 1000 * 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initView();
    }

    private void initView() {
        SharedPreferences preferences = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE);

        etUpdateInterval = (EditText) findViewById(R.id.etUpdateInterval);
        etSmallestDisplacement = (EditText) findViewById(R.id.etSmallestDisplacement);
        etEndPoint = (EditText) findViewById(R.id.etEndPoint);

        long interval = preferences.getLong(Constants.UPDATE_INTERVAL_IN_MILLISECONDS, Constants.DEFAULT_UPDATE_INTERVAL_IN_MILLISECONDS);
        etUpdateInterval.setText(String.valueOf(interval / oneMinute));
        etSmallestDisplacement.setText(String.valueOf(preferences.getFloat(Constants.SMALLEST_DISPLACEMENT, Constants.DEFAULT_SMALLEST_DISPLACEMENT)));
        etEndPoint.setText(preferences.getString(Constants.END_POINT, ""));

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            SharedPreferences.Editor editor = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE).edit();
            long timeInMilliseconds = oneMinute * Long.parseLong(etUpdateInterval.getText().toString());
            editor.putLong(Constants.UPDATE_INTERVAL_IN_MILLISECONDS, timeInMilliseconds);
            editor.putFloat(Constants.SMALLEST_DISPLACEMENT, Float.valueOf(etSmallestDisplacement.getText().toString()));
            editor.putString(Constants.END_POINT, etEndPoint.getText().toString());
            editor.apply();
            Toast.makeText(SettingsActivity.this, R.string.track_preferences_saved, Toast.LENGTH_LONG).show();
            finish();
        });

        findViewById(R.id.btnRequestExample).setOnClickListener(v -> {
            CharSequence title = getResources().getText(R.string.share_with);

            StringBuilder text = new StringBuilder();
            text.append("POST for your end point:\n\n");
            text.append("{\n");
            text.append("\t\"latitude\" : -8.057838,\n");
            text.append("\t\"longitude\" : -34.882897,\n");
            text.append("\t\"speed\" : 0,\n");
            text.append("\t\"altitude\" : 30,\n");
            text.append("\t\"bearing\" : 180,\n");
            text.append("\t\"time\" : 1445751139779,\n");
            text.append("\t\"fixed\" : true,\n");
            text.append("\t\"street\" : \"Rua do Hospício\",\n");
            text.append("\t\"number\" : \"1124\",\n");
            text.append("\t\"neighborhood\" : \"Boa Vista\",\n");
            text.append("\t\"city\" : \"Recife\",\n");
            text.append("\t\"state\" : \"PE\",\n");
            text.append("\t\"imei\" : 1234567890123\n");
            text.append("}\n\n");
            text.append("Important:\n");
            text.append("speed = m/s\n");
            text.append("fixed = boolean\n");


            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, title));
        });

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

}
