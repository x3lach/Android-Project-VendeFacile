package com.example.projetandroid.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.projetandroid.adapters.InvoiceItemAdapter;
import com.example.projetandroid.databinding.ActivityInvoiceSaleBinding;
import com.example.projetandroid.model.Invoice;
import com.example.projetandroid.model.InvoiceItem;
import com.example.projetandroid.model.Product;
import com.example.projetandroid.utils.LocationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceSaleActivity extends AppCompatActivity implements InvoiceItemAdapter.OnItemRemovedListener, LocationHelper.OnLocationReceivedListener {

    private ActivityInvoiceSaleBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<Product> productList;
    private ArrayAdapter<Product> productAdapter;
    private Invoice currentInvoice;
    private InvoiceItemAdapter invoiceItemAdapter;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private Product selectedProduct;
    private LocationHelper locationHelper;
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private String currentLocationAddress = "";
    private static final int PERMISSIONS_REQUEST_LOCATION = 99;
    private boolean locationCaptured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceSaleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        productList = new ArrayList<>();
        
        // Vérifier si nous sommes en mode édition
        boolean editMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        String invoiceId = getIntent().getStringExtra("INVOICE_ID");
        
        // Initialiser l'invoice (nouvelle ou existante)
        if (editMode && invoiceId != null) {
            // Mode édition - changer le titre
            binding.tvInvoiceTitle.setText("Modifier la Facture");
            // Changer le texte du bouton
            binding.btnSaveInvoice.setText("Enregistrer les modifications");
            // Charger la facture existante
            loadExistingInvoice(invoiceId);
        } else {
            currentInvoice = new Invoice();
        }

        // Configuration du RecyclerView et des adaptateurs...
        binding.recyclerViewInvoiceItems.setLayoutManager(new LinearLayoutManager(this));
        invoiceItemAdapter = new InvoiceItemAdapter(currentInvoice != null ? currentInvoice.getItems() : new ArrayList<>(), this);
        binding.recyclerViewInvoiceItems.setAdapter(invoiceItemAdapter);
        
        // Configurer le Spinner des produits
        productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productList);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerProducts.setAdapter(productAdapter);
        
        // Charger les produits
        loadProducts();
        
        // Configurer les listeners...
        binding.spinnerProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProduct = (Product) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedProduct = null;
            }
        });
        
        binding.btnAddProduct.setOnClickListener(v -> addProductToInvoice());
        binding.btnSaveInvoice.setOnClickListener(v -> saveInvoice());
        
        // Initialiser l'helper de localisation
        locationHelper = new LocationHelper(this);
        locationHelper.setOnLocationReceivedListener(this);
        
        // Ajouter un indicateur de localisation dans le layout
        TextView tvLocationStatus = new TextView(this);
        tvLocationStatus.setId(View.generateViewId());
        tvLocationStatus.setText("Capture de la localisation en cours...");
        tvLocationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        
        // Ajouter cette vue au layout
        ViewGroup layoutRoot = (ViewGroup) binding.getRoot();
        layoutRoot.addView(tvLocationStatus, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Vérifier et demander les permissions de localisation
        checkLocationPermission();
    }
    
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            // Vérifier si l'utilisateur a déjà refusé la permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, 
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                
                // Afficher une explication à l'utilisateur
                new AlertDialog.Builder(this)
                        .setTitle("Permissions de localisation requises")
                        .setMessage("Cette application a besoin de votre localisation pour enregistrer le point de vente.")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            //Demander la permission
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION},
                                    PERMISSIONS_REQUEST_LOCATION);
                        })
                        .create()
                        .show();
            } else {
                // Pas besoin d'explication, demander la permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            // La permission est déjà accordée, démarrer la localisation
            startLocationUpdates();
            return true;
        }
    }
    
    private void startLocationUpdates() {
        if (locationHelper != null) {
            locationHelper.startLocationUpdates();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
                startLocationUpdates();
            } else {
                // Permission refusée
                Toast.makeText(this, "La localisation ne sera pas enregistrée", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void loadProducts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference productsRef = mDatabase.child("users").child(userId).child("products");
        
        binding.progressBarInvoice.setVisibility(View.VISIBLE);
        
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                productList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                    }
                }
                productAdapter.notifyDataSetChanged();
                binding.progressBarInvoice.setVisibility(View.GONE);
                
                if (productList.isEmpty()) {
                    Toast.makeText(InvoiceSaleActivity.this, 
                            "Aucun produit trouvé. Veuillez en ajouter d'abord.", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(InvoiceSaleActivity.this, 
                        "Erreur de chargement des produits: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                binding.progressBarInvoice.setVisibility(View.GONE);
            }
        });
    }
    
    private void addProductToInvoice() {
        if (selectedProduct == null) {
            Toast.makeText(this, "Veuillez sélectionner un produit", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String quantityStr = binding.etProductQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            binding.etProductQuantity.setError("La quantité est requise");
            return;
        }
        
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                binding.etProductQuantity.setError("La quantité doit être positive");
                return;
            }
            
            // Vérifier si la quantité demandée est disponible
            if (quantity > selectedProduct.getQuantity()) {
                binding.etProductQuantity.setError("Seulement " + selectedProduct.getQuantity() + " disponibles");
                return;
            }
            
        } catch (NumberFormatException e) {
            binding.etProductQuantity.setError("Quantité invalide");
            return;
        }
        

        currentInvoice.addItem(selectedProduct, quantity);
        

        invoiceItemAdapter.updateItems(currentInvoice.getItems());
        binding.tvTotalAmount.setText(currencyFormat.format(currentInvoice.getTotalAmount()));
        

        binding.etProductQuantity.setText("");
    }
    
    @Override
    public void onItemRemoved(int position) {
        currentInvoice.removeItem(position);
        invoiceItemAdapter.updateItems(currentInvoice.getItems());
        binding.tvTotalAmount.setText(currencyFormat.format(currentInvoice.getTotalAmount()));
    }
    
    // Méthode pour charger une facture existante
    private void loadExistingInvoice(String invoiceId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userId = currentUser.getUid();
        
        binding.progressBarInvoice.setVisibility(View.VISIBLE);
        
        mDatabase.child("users").child(userId).child("invoices").child(invoiceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Invoice invoice = snapshot.getValue(Invoice.class);
                        if (invoice != null) {
                            currentInvoice = invoice;
                            // Afficher les données de la facture
                            binding.etClientName.setText(invoice.getClientName());
                            binding.etClientPhone.setText(invoice.getClientPhone()); // Afficher le numéro de téléphone
                            
                            // Mise à jour du récyclerView avec les produits de la facture
                            invoiceItemAdapter.updateItems(invoice.getItems());
                            
                            // Afficher le montant total
                            binding.tvTotalAmount.setText(currencyFormat.format(invoice.getTotalAmount()));
                        } else {
                            Toast.makeText(InvoiceSaleActivity.this, 
                                    "Impossible de charger la facture", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        binding.progressBarInvoice.setVisibility(View.GONE);
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBarInvoice.setVisibility(View.GONE);
                        Toast.makeText(InvoiceSaleActivity.this, 
                                "Erreur: " + error.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // Modifiez également la méthode saveInvoice pour gérer l'édition
    private void saveInvoice() {
        String clientName = binding.etClientName.getText().toString().trim();
        String clientPhone = binding.etClientPhone.getText().toString().trim(); // Récupérer le numéro de téléphone
        
        if (TextUtils.isEmpty(clientName)) {
            binding.etClientName.setError("Le nom du client est requis");
            return;
        }
        
        if (currentInvoice.getItems().isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins un produit à la facture", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String userId = currentUser.getUid();
        binding.progressBarInvoice.setVisibility(View.VISIBLE);
        
        // Si la facture existe déjà (mode édition), utiliser son ID
        String invoiceId = currentInvoice.getInvoiceId();
        if (invoiceId == null) {
            // Sinon, générer un nouvel ID pour une nouvelle facture
            invoiceId = mDatabase.child("users").child(userId).child("invoices").push().getKey();
            
            if (invoiceId == null) {
                Toast.makeText(this, "Échec de la création de l'ID de facture.", Toast.LENGTH_SHORT).show();
                binding.progressBarInvoice.setVisibility(View.GONE);
                return;
            }
            currentInvoice.setInvoiceId(invoiceId);
        }
        
        currentInvoice.setClientName(clientName);
        currentInvoice.setClientPhone(clientPhone); // Sauvegarder le numéro de téléphone
        currentInvoice.setTimestamp(System.currentTimeMillis());
        
        // Si la localisation n'est pas capturée, essayer une dernière fois
        if (!locationCaptured) {
            Toast.makeText(this, "Tentative de capture de la localisation...", Toast.LENGTH_SHORT).show();
            startLocationUpdates();
            // Continuer quand même avec l'enregistrement
        }
        
        // Ajouter les informations de localisation
        currentInvoice.setLatitude(currentLatitude);
        currentInvoice.setLongitude(currentLongitude);
        currentInvoice.setLocationAddress(currentLocationAddress);
        
        mDatabase.child("users").child(userId).child("invoices").child(invoiceId).setValue(currentInvoice)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Mettre à jour les stocks de produits
                        updateProductStock(userId);
                    } else {
                        binding.progressBarInvoice.setVisibility(View.GONE);
                        Toast.makeText(InvoiceSaleActivity.this, 
                                "Échec de l'enregistrement: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void updateProductStock(String userId) {

        for (InvoiceItem item : currentInvoice.getItems()) {
            final String productId = item.getProductId();
            final int soldQuantity = item.getQuantity();
            
            DatabaseReference productRef = mDatabase.child("users").child(userId).child("products").child(productId);
            productRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    Product product = task.getResult().getValue(Product.class);
                    if (product != null) {
                        int newQuantity = Math.max(0, product.getQuantity() - soldQuantity);
                        productRef.child("quantity").setValue(newQuantity);
                    }
                }
            });
        }
        

        binding.progressBarInvoice.setVisibility(View.GONE);
        Toast.makeText(this, "Facture enregistrée avec succès!", Toast.LENGTH_SHORT).show();
        

        currentInvoice = new Invoice();
        invoiceItemAdapter.updateItems(currentInvoice.getItems());
        binding.etClientName.setText("");
        binding.tvTotalAmount.setText(currencyFormat.format(0));
    }
    
    // Méthodes de l'interface OnLocationReceivedListener
    @Override
    public void onLocationReceived(double latitude, double longitude, String address) {
        currentLatitude = latitude;
        currentLongitude = longitude;
        currentLocationAddress = address;
        locationCaptured = true;
        
        // Mettre à jour l'interface utilisateur pour montrer que la localisation est capturée
        runOnUiThread(() -> {
            TextView tvLocationStatus = findViewById(binding.getRoot().getChildAt(
                    binding.getRoot().getChildCount() - 1).getId());
            if (tvLocationStatus != null) {
                tvLocationStatus.setText("Localisation capturée : " + address);
                tvLocationStatus.setTextColor(ContextCompat.getColor(InvoiceSaleActivity.this, 
                        android.R.color.holo_green_dark));
            }
        });
        
        Toast.makeText(this, "Localisation capturée avec succès", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onLocationError(String error) {
        // Mettre à jour l'interface utilisateur pour montrer l'erreur
        runOnUiThread(() -> {
            TextView tvLocationStatus = findViewById(binding.getRoot().getChildAt(
                    binding.getRoot().getChildCount() - 1).getId());
            if (tvLocationStatus != null) {
                tvLocationStatus.setText("Erreur de localisation : " + error);
                tvLocationStatus.setTextColor(ContextCompat.getColor(InvoiceSaleActivity.this, 
                        android.R.color.holo_red_dark));
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }

    // ... autres méthodes existantes ...
}