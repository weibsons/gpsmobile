package mobi.stos.gpsmobile.util;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;


public class Util {

    /**
     * Função retorna distância em metros a partir de uma latitude e longitude
     * de inicio e uma latitude e longitude de destino.
     *
     * @param orglat double Latitude de início
     * @param orglon double Longitude de início
     * @param destlat double Latitude de destino
     * @param destlon double Longitude de destino
     * @return double distância em metros entre as posições geográficas.
     */
    public static double haversine(double orglat, double orglon, double destlat, double destlon) {
        orglat = orglat * Math.PI / 180;
        orglon = orglon * Math.PI / 180;
        destlat = destlat * Math.PI / 180;
        destlon = destlon * Math.PI / 180;

        double raioterra = 6378140; // METROS
        double dlat = destlat - orglat;
        double dlon = destlon - orglon;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(orglat) * Math.cos(destlat) * Math.pow(Math.sin(dlon / 2), 2);
        double distancia = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return raioterra * distancia;
    }

    @Nullable
    public static String getImei(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                return telephonyManager.getDeviceId();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
