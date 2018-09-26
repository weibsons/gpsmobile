package mobi.stos.gpsmobile.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.net.URL;
import java.util.Calendar;
import java.util.List;

import mobi.stos.gpsmobile.MainActivity;
import mobi.stos.gpsmobile.R;
import mobi.stos.gpsmobile.bean.Position;
import mobi.stos.gpsmobile.bo.PositionBo;
import mobi.stos.gpsmobile.task.DecodeLatLngAsyncTask;
import mobi.stos.gpsmobile.util.Constants;
import mobi.stos.gpsmobile.util.Util;
import mobi.stos.httplib.HttpAsync;
import mobi.stos.httplib.inter.FutureCallback;

public class TrackService extends Service implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static final String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
    private static final int NOTIFICATION_ID = 9;
    private final String TAG = this.getClass().getSimpleName();
    private float smallestDisplacement;
    private PositionBo positionBo;

    private GoogleApiClient googleApiClient;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    public TrackService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        iniciarTrack();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(TrackService.this, R.string.gps_disconected, Toast.LENGTH_SHORT).show();
        googleApiClient.disconnect();
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
        super.onDestroy();
    }

    private void iniciarTrack() {
        positionBo = new PositionBo(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(connectionResult -> Log.i(TAG, "GoogleApiClient connection has failed"))
                .build();
        googleApiClient.connect();
        showNotification();
    }

    public void onConnected(Bundle bundle) {
        Log.v(TAG, "onConnected");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            SharedPreferences preferences = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE);
            smallestDisplacement = preferences.getFloat(Constants.SMALLEST_DISPLACEMENT, Constants.DEFAULT_SMALLEST_DISPLACEMENT);

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(preferences.getLong(Constants.UPDATE_INTERVAL_IN_MILLISECONDS, Constants.DEFAULT_UPDATE_INTERVAL_IN_MILLISECONDS));
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setFastestInterval(Constants.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
            locationRequest.setSmallestDisplacement(smallestDisplacement);

            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    onLocationChanged(locationResult.getLastLocation());
                }
            }, Looper.myLooper());
            Log.v(TAG, "select position");
        } else {
            Log.v(TAG, "haven't location permission");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    public void onLocationChanged(Location location) {
        try {

            Log.v(TAG, "onLocationChanged");

            DecodeLatLngAsyncTask task = new DecodeLatLngAsyncTask(this, location, (local) -> {
                SharedPreferences preferences = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE);
                try {
                    if (preferences.getBoolean(MainActivity.class.getSimpleName(), false)) {
                        Intent intent = new Intent();
                        intent.setAction("mobi.stos.gpsmobile.MainActivity");
                        sendBroadcast(intent);
                    }
                } catch (Exception e) {
                    Log.v(TAG, "View não existe, tentou acessar uma view fechada");
                }

                Position entity = new Position();
                entity.setLatitude(local.getLatitude());
                entity.setLongitude(local.getLongitude());
                entity.setAltitude(local.getAltitude());
                entity.setSpeed(local.getSpeed());
                entity.setAccuracy(local.getAccuracy());
                entity.setBearing(local.getBearing());
                entity.setTime(local.getTime());
                entity.setLogradouro(preferences.getString(Constants.Location.ADDRESS, ""));
                entity.setNumero(preferences.getString(Constants.Location.NUMBER, ""));
                entity.setBairro(preferences.getString(Constants.Location.NEIGHBORHOOD, ""));
                entity.setCidade(preferences.getString(Constants.Location.CITY, ""));
                entity.setEstado(preferences.getString(Constants.Location.STATE, ""));

                Position older = positionBo.getNewest();
                if (older != null && older.getTime() > 0) {
                    Calendar cOlder = Calendar.getInstance();
                    cOlder.setTimeInMillis(older.getTime());

                    Calendar current = Calendar.getInstance();
                    current.setTimeInMillis(local.getTime());

                    double fixPoint = smallestDisplacement;
                    if (fixPoint < 30) {
                        fixPoint = 30;
                    }
                    if (cOlder.get(Calendar.DAY_OF_MONTH) != current.get(Calendar.DAY_OF_MONTH)) {
                        entity.setFixed(true);
                    } else {
                        if (Util.haversine(older.getLatitude(), older.getLongitude(), local.getLatitude(), local.getLongitude()) >= fixPoint) {
                            entity.setFixed(true);
                        } else {
                            entity.setFixed(false);
                        }
                    }
                } else {
                    entity.setFixed(true);
                }
                entity.setSync(false);
                positionBo.insert(entity);

                if (ContextCompat.checkSelfPermission(TrackService.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    sendGPSPosition();
                } else {
                    builder.setContentText(getText(R.string.sync_not_enable));
                }

            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PendingIntent createPendingIntent(String event, Class intentClass) {
        Intent intentHide = new Intent(this, intentClass);
        intentHide.putExtra("event", event);
        return PendingIntent.getService(this, (int) System.currentTimeMillis(), intentHide, 0);
    }

    private void setChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Channel Description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.setVibrationPattern(new long[]{0, 500, 500, 500});
            notificationChannel.enableVibration(true);
            notificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void showNotification( ) {
        this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        this.builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        this.setChannel(notificationManager);

        this.builder.setSmallIcon(R.drawable.ic_stat_gps_receiving);
        this.builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_stat_gps_receiving));
        this.builder.setAutoCancel(false);
        this.builder.setContentIntent(createPendingIntent("open", TrackIntentService.class));
        this.builder.setOngoing(true);
        this.builder.setContentTitle(getText(R.string.location_shared));
        this.builder.setContentText(getText(R.string.app_name));

        this.builder.addAction(R.drawable.ic_stat_gps_disconnected, getString(R.string.stop_location), createPendingIntent("stop", TrackIntentService.class));
        this.notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void sendGPSPosition() {
        try {
            Position entity = positionBo.getNewest();
            SharedPreferences preferences = getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE);
            String url = preferences.getString(Constants.END_POINT, null);
            if (!TextUtils.isEmpty(url)) {

                HttpAsync task = new HttpAsync(new URL(url));
                task.setExecucaoSerial(false);
                task.setDebug(true);
                task.addParam("latitude", entity.getLatitude());
                task.addParam("longitude", entity.getLongitude());
                task.addParam("speed", entity.getSpeed());
                task.addParam("altitude", entity.getAltitude());
                task.addParam("bearing", entity.getBearing());
                task.addParam("accuracy", entity.getAccuracy());
                task.addParam("time", entity.getTime());
                task.addParam("fixed", entity.isFixed());
                task.addParam("street", entity.getLogradouro());
                task.addParam("number", entity.getNumero());
                task.addParam("neighborhood", entity.getBairro());
                task.addParam("city", entity.getCidade());
                task.addParam("state", entity.getEstado());
                task.addParam("imei", Long.parseLong(Util.getImei(this)));
                task.post(new FutureCallback() {
                    @Override
                    public void onBeforeExecute() {

                    }

                    @Override
                    public void onAfterExecute() {

                    }

                    @Override
                    public void onSuccess(int responseCode, Object object) {
                        try {
                            if (responseCode == 200) {
                                positionBo.delete(entity);
                            }
                        } catch (Exception e) {
                            //
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {

                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
