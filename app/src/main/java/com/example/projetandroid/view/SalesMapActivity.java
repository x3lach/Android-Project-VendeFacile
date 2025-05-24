package com.example.projetandroid.view;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projetandroid.databinding.ActivitySalesMapBinding;
import com.example.projetandroid.model.Invoice;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalesMapActivity extends AppCompatActivity {

    private ActivitySalesMapBinding binding;
    private MapView mapView;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configuration d'OpenStreetMap - AVANT d'inflater la vue
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        
        // IMPORTANT: Configurer le dossier de cache pour éviter les erreurs de suppression
        File osmdroidDir = new File(ctx.getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidDir);
        File tileCache = new File(osmdroidDir, "tiles");
        tileCache.mkdirs();
        Configuration.getInstance().setOsmdroidTileCache(tileCache);
        
        binding = ActivitySalesMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Configurer la carte OpenStreetMap
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        
        // Configuration initiale de la carte (zoom et position)
        mapView.getController().setZoom(5.0); // Zoom initial
        GeoPoint startPoint = new GeoPoint(46.603354, 1.888334); // Centre de la France
        mapView.getController().setCenter(startPoint);
        
        // Charger les points de vente
        loadSalesLocations();
    }

    private void loadSalesLocations() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference invoicesRef = mDatabase.child("users").child(userId).child("invoices");
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        invoicesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasLocations = false;
                mapView.getOverlays().clear();  // Effacer les marqueurs existants
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Invoice invoice = snapshot.getValue(Invoice.class);
                    if (invoice != null && invoice.getLatitude() != 0 && invoice.getLongitude() != 0) {
                        // Créer un marqueur pour chaque point de vente
                        Marker marker = new Marker(mapView);
                        GeoPoint position = new GeoPoint(invoice.getLatitude(), invoice.getLongitude());
                        marker.setPosition(position);
                        
                        // Créer un titre avec information client et date
                        String title = "Client: " + invoice.getClientName();
                        String snippet = "Date: " + dateFormat.format(new Date(invoice.getTimestamp())) 
                                + "\nMontant: " + String.format("%.2f €", invoice.getTotalAmount());
                        
                        marker.setTitle(title);
                        marker.setSnippet(snippet);
                        mapView.getOverlays().add(marker);
                        
                        // Centrer la carte sur le premier point
                        if (!hasLocations) {
                            mapView.getController().setCenter(position);
                            mapView.getController().setZoom(15.0);
                            hasLocations = true;
                        }
                    }
                }
                
                mapView.invalidate(); // Rafraîchir l'affichage de la carte
                binding.progressBar.setVisibility(View.GONE);
                
                if (!hasLocations) {
                    Toast.makeText(SalesMapActivity.this, 
                            "Aucun point de vente avec localisation trouvé", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(SalesMapActivity.this, 
                        "Erreur de chargement: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}