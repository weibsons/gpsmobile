package mobi.stos.gpsmobile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mobi.stos.gpsmobile.bean.Position;
import mobi.stos.gpsmobile.bo.PositionBo;
import mobi.stos.gpsmobile.service.TrackService;
import mobi.stos.gpsmobile.util.Constants;
import mobi.stos.gpsmobile.util.Util;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_ACCESS_FINE_LOCATION =  0;
    private static final int PERMISSIONS_READ_PHONE_STATE = 1;
    private BroadcastReceiver receiver;

    private InterstitialAd mInterstitialAd;

    private AlertDialog dialog;
    private TextView tvAtivarLocalizacao;
    private ToggleButton btnGps;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAltitude;
    private TextView tvAccuracy;
    private TextView tvSpeed;
    private TextView tvBearing;
    private TextView tvUpdateAt;
    private TextView tvAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initView();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateView();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("mobi.stos.gpsmobile.MainActivity");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences.Editor editor = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE).edit();
        editor.putBoolean(MainActivity.class.getSimpleName(), true);
        editor.apply();

        if (btnGps != null) {
            updateUserInfoLocationToggle(Util.isMyServiceRunning(this, TrackService.class));
        }
        updateView();
        checkReadPhoneState();
    }

    @Override
    protected void onPause() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        SharedPreferences.Editor editor = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE).edit();
        editor.putBoolean(MainActivity.class.getSimpleName(), false);
        editor.apply();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    startActivity(new Intent(this, SettingsActivity.class));
                }
                break;
            case R.id.action_about:
                try {
                    final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    String message = (String) getText(R.string.message_about);
                    message = String.format(message, pInfo.versionName);

                    dialog = new AlertDialog.Builder(this).create();
                    dialog.setTitle(R.string.action_about);
                    dialog.setMessage(message);
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.rating), (dialog, which) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pInfo.packageName)));
                        dialog.dismiss();
                    });
                    dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getText(R.string.close), (dialog, which) -> dialog.dismiss());
                    dialog.show();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService(new Intent(MainActivity.this, TrackService.class));
                    updateUserInfoLocationToggle(true);
                } else {
                    dialog = new AlertDialog.Builder(this).create();
                    dialog.setMessage(getString(R.string.why_we_need_permission_location));
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    btnGps.setChecked(false);
                }
            }
        }
    }

    private void checkReadPhoneState() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
                dialog = new AlertDialog.Builder(MainActivity.this).create();
                dialog.setTitle(R.string.revoke_decision);
                dialog.setMessage(getString(R.string.why_we_need_permission_read_phone_state));
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.i_dont_care), (dialog, which) -> dialog.dismiss());
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_READ_PHONE_STATE);
                });
                dialog.show();
                btnGps.setChecked(false);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_READ_PHONE_STATE);
            }
        }
    }

    private void updateUserInfoLocationToggle(boolean active) {
        if (active) {
            tvAtivarLocalizacao.setText(R.string.location_atived);
            btnGps.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue_500));
            btnGps.setChecked(true);
        } else {
            tvAtivarLocalizacao.setText(R.string.location_not_actived);
            btnGps.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red_500));
            btnGps.setChecked(false);
        }
    }

    private void initView() {
        tvAtivarLocalizacao = findViewById(R.id.tvAtivarLocalizacao);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude =  findViewById(R.id.tvLongitude);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvAltitude = findViewById(R.id.tvAltitude);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvBearing = findViewById(R.id.tvBearing);
        tvUpdateAt = findViewById(R.id.tvUpdateAt);
        tvAddress = findViewById(R.id.tvAddress);

        updateView();

        btnGps = findViewById(R.id.btnGps);
        btnGps.setOnClickListener(v -> {
            SharedPreferences preferences = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE);
            String url = preferences.getString(Constants.END_POINT, null);
            if (!TextUtils.isEmpty(url)) {
                startServiceLocationUpdates();
            } else {
                updateUserInfoLocationToggle(false);
                Toast.makeText(this, R.string.define_endpoint, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnFollow).setOnClickListener(v -> {
            try {
                double lat = Double.parseDouble(tvLatitude.getText().toString());
                double lng = Double.parseDouble(tvLongitude.getText().toString());
                Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.cant_display_position_on_map, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startServiceLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGPSEnabled && !isNetworkEnabled) {
            dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle(R.string.dialog_config_location_title);
            dialog.setMessage(getText(R.string.dialog_config_location_msg));
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dialog_config_location_positive), (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_config_location_negative), (dialog, which) -> dialog.dismiss());
            dialog.show();
            btnGps.setChecked(false);
        } else {
            if (btnGps.isChecked()) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.setTitle(R.string.revoke_decision);
                        dialog.setMessage(getString(R.string.why_we_need_permission_location));
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.i_dont_care), (dialog, which) -> dialog.dismiss());
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok), (dialog, which) -> {
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
                        });
                        dialog.show();
                        btnGps.setChecked(false);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
                    }
                } else {
                    startService(new Intent(this, TrackService.class));
                    updateUserInfoLocationToggle(true);
                }
            } else {
                stopService(new Intent(this, TrackService.class));
                updateUserInfoLocationToggle(false);
            }
        }
    }

    private void updateView() {
        PositionBo positionBo = new PositionBo(this);
        Position position = positionBo.getNewest();
        if (position != null) {
            tvLatitude.setText(String.valueOf(position.getLatitude()));
            tvLongitude.setText(String.valueOf(position.getLongitude()));
            tvAccuracy.setText(new DecimalFormat("0").format(position.getAccuracy()) + "m");
            tvAltitude.setText(new DecimalFormat("0").format(position.getAltitude()));
            tvSpeed.setText(new DecimalFormat("0").format(position.getSpeed() * 3.6) + "km/h");

            String cardinalPoint = "";
            if (position.getBearing() == 0 || position.getBearing() == 360) {
                cardinalPoint = getString(R.string.north);
            } else if (position.getBearing() == 90) {
                cardinalPoint = getString(R.string.east);
            } else if (position.getBearing() == 180) {
                cardinalPoint = getString(R.string.south);
            } else if (position.getBearing() == 270) {
                cardinalPoint = getString(R.string.west);
            }

            tvBearing.setText(String.valueOf(position.getBearing()));
            tvUpdateAt.setText( new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(position.getTime())));
            StringBuilder address = new StringBuilder();
            if (position.getAccuracy() <= 30) {
                address.append(position.getLogradouro());
                if (!TextUtils.isEmpty(position.getNumero())) {
                    address.append(", ").append(position.getNumero()).append(", ");
                }
            }
            address.append(position.getBairro()).append(", ").append(position.getCidade()).append(", ").append(position.getEstado());
            tvAddress.setText(address);
        }

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstical_ad_unit_id));
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.v(TAG, "The instestical is load");
            }

            @Override
            public void onAdClosed() {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        mInterstitialAd.loadAd(adRequestBuilder.build());
    }

}
