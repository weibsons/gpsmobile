package mobi.stos.gpsmobile.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import mobi.stos.gpsmobile.interfaces.LocationFutureCallback;
import mobi.stos.gpsmobile.util.Constants;

import static android.content.Context.MODE_PRIVATE;

public class DecodeLatLngAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = DecodeLatLngAsyncTask.class.getSimpleName();

    private final Context context;
    private final Location location;

    private LocationFutureCallback callback;

    public DecodeLatLngAsyncTask(Context context, Location location, LocationFutureCallback callback) {
        this.context = context;
        this.location = location;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                SharedPreferences.Editor editor = context.getSharedPreferences(Constants.TRACK_PREFERECES, MODE_PRIVATE).edit();
                editor.putString(Constants.Location.ADDRESS, address.getThoroughfare());
                editor.putString(Constants.Location.NUMBER, address.getSubThoroughfare());
                editor.putString(Constants.Location.NEIGHBORHOOD, address.getLocality() == null ? address.getSubLocality() : address.getLocality());
                editor.putString(Constants.Location.CITY, address.getSubAdminArea());
                editor.putString(Constants.Location.POSTAL_CODE, address.getPostalCode());
                editor.putString(Constants.Location.STATE, this.decodeShortNameState(address.getAdminArea()));
                for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    editor.putString(Constants.Location.FULL_ADDRESS, address.getAddressLine(i));
                    Log.v(TAG, address.getAddressLine(i));
                }
                editor.apply();


                if (callback != null) {
                    callback.onLocationAddressDecode(location);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decodeShortNameState(String state) {
        HashMap<String, String> estados = new HashMap<>();
        estados.put("Acre", "AC");
        estados.put("Alagoas", "AL");
        estados.put("Amapá", "AP");
        estados.put("Amazonas", "AM");
        estados.put("Bahia", "BA");
        estados.put("Ceará", "CE");
        estados.put("Distrito Federal", "DF");
        estados.put("Espírito Santo", "ES");
        estados.put("Goiás", "GO");
        estados.put("Maranhão", "MA");
        estados.put("Mato Grosso", "MT");
        estados.put("Mato Grosso do Sul", "MS");
        estados.put("Minas Gerais", "MG");
        estados.put("Pará", "PA");
        estados.put("Paraíba", "PB");
        estados.put("Paraná", "PR");
        estados.put("Pernambuco", "PE");
        estados.put("Piauí", "PI");
        estados.put("Rio de Janeiro", "RJ");
        estados.put("Rio Grande do Norte", "RN");
        estados.put("Rio Grande do Sul", "RS");
        estados.put("Rondônia", "RO");
        estados.put("Roraima", "RR");
        estados.put("Santa Catarina", "SC");
        estados.put("São Paulo", "SP");
        estados.put("Sergipe", "SE");
        estados.put("Tocantins", "TO");

        String found = estados.get(state);
        if (found == null || found.equals("")) {
            return state;
        } else {
            return found;
        }
    }

}