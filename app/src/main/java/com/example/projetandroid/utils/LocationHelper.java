package com.example.projetandroid.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private final Context context;
    private OnLocationReceivedListener listener;
    
    public interface OnLocationReceivedListener {
        void onLocationReceived(double latitude, double longitude, String address);
        void onLocationError(String error);
    }
    
    public LocationHelper(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        createLocationRequest();
    }
    
    public void setOnLocationReceivedListener(OnLocationReceivedListener listener) {
        this.listener = listener;
    }
    
    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
                
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    if (listener != null) {
                        listener.onLocationError("Impossible d'obtenir la localisation");
                    }
                    return;
                }
                
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    
                    // Obtenir l'adresse
                    getAddressFromLocation(latitude, longitude);
                    
                    // Arrêter les mises à jour une fois qu'on a obtenu une position
                    stopLocationUpdates();
                    break;
                }
            }
        };
    }
    
    private void getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();
                
                // Ajouter les éléments de l'adresse s'ils existent
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressString.append(address.getAddressLine(i)).append(", ");
                }
                
                // Supprimer la dernière virgule
                if (addressString.length() > 2) {
                    addressString.setLength(addressString.length() - 2);
                }
                
                if (listener != null) {
                    listener.onLocationReceived(latitude, longitude, addressString.toString());
                }
            } else {
                if (listener != null) {
                    listener.onLocationReceived(latitude, longitude, "Adresse inconnue");
                }
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onLocationReceived(latitude, longitude, "Erreur de géocodage: " + e.getMessage());
            }
        }
    }
    
    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                
            // Demander les permissions si on est dans une activité
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
            
            if (listener != null) {
                listener.onLocationError("Permissions de localisation non accordées");
            }
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback, Looper.getMainLooper());
        
        // Essayer d'obtenir la dernière position connue
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getAddressFromLocation(location.getLatitude(), location.getLongitude());
                // Ne pas arrêter les mises à jour maintenant, laissons le callback le faire
            }
        }).addOnFailureListener(e -> {
            if (listener != null) {
                listener.onLocationError("Erreur: " + e.getMessage());
            }
        });
    }
    
    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}