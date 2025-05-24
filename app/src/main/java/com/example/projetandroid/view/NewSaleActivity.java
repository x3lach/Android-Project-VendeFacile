package com.example.projetandroid.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projetandroid.databinding.ActivityNewSaleBinding;
import com.example.projetandroid.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewSaleActivity extends AppCompatActivity {

    private ActivityNewSaleBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<Product> productList;
    private ArrayAdapter<Product> productAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewSaleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        productList = new ArrayList<>();

        setupProductSpinner();
        loadProducts();

        binding.btnSaveSale.setOnClickListener(v -> {
            saveSale();
        });
    }

    private void setupProductSpinner() {
        productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productList);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerProductName.setAdapter(productAdapter);
    }

    private void loadProducts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        DatabaseReference productsRef = mDatabase.child("users").child(userId).child("products");

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
                if (productList.isEmpty()) {
                    Toast.makeText(NewSaleActivity.this, "Aucun produit trouvé. Veuillez en ajouter.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(NewSaleActivity.this, "Erreur de chargement des produits.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSale() {
        // Récupérer le produit sélectionné depuis le Spinner
        Product selectedProduct = (Product) binding.spinnerProductName.getSelectedItem();

        if (selectedProduct == null) {
            Toast.makeText(this, "Veuillez sélectionner un produit.", Toast.LENGTH_SHORT).show();
            return;
        }
        String productName = selectedProduct.getName();
        // String productId = selectedProduct.getProductId(); // Si vous avez besoin de l'ID

        // String productName = binding.etProductName.getText().toString().trim(); // Ancienne ligne
        String quantityStr = binding.etQuantity.getText().toString().trim();
        String clientName = binding.etClientName.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();

        // if (TextUtils.isEmpty(productName)) { // Cette vérification n'est plus nécessaire si un produit est toujours sélectionné
        //     // binding.etProductName.setError("Product name is required");
        //     // return;
        // }
        if (TextUtils.isEmpty(quantityStr)) {
            binding.etQuantity.setError("Quantity is required");
            return;
        }
        if (TextUtils.isEmpty(clientName)) {
            binding.etClientName.setError("Client name is required");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            binding.etPrice.setError("Price is required");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                binding.etQuantity.setError("Quantity must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etQuantity.setError("Invalid quantity");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                binding.etPrice.setError("Price must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etPrice.setError("Invalid price");
            return;
        }


        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in. Cannot save sale.", Toast.LENGTH_LONG).show();
            return;
        }
        String userId = currentUser.getUid();

        binding.progressBarNewSale.setVisibility(View.VISIBLE);

        // Create a unique ID for the sale
        String saleId = mDatabase.child("users").child(userId).child("sales").push().getKey();

        if (saleId == null) {
            Toast.makeText(this, "Failed to create sale ID.", Toast.LENGTH_SHORT).show();
            binding.progressBarNewSale.setVisibility(View.GONE);
            return;
        }

        Map<String, Object> saleValues = new HashMap<>();
        saleValues.put("productName", productName); // Utilise le nom du produit sélectionné
        // saleValues.put("productId", selectedProduct.getProductId()); // Optionnel: stocker l'ID du produit
        saleValues.put("quantity", quantity);
        saleValues.put("clientName", clientName);
        saleValues.put("price", price); // Vous pourriez vouloir utiliser selectedProduct.getPrice() si le prix est fixe
        saleValues.put("timestamp", System.currentTimeMillis());

        mDatabase.child("users").child(userId).child("sales").child(saleId).setValue(saleValues)
                .addOnCompleteListener(task -> {
                    binding.progressBarNewSale.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(NewSaleActivity.this, "Sale recorded successfully!", Toast.LENGTH_SHORT).show();
                        // Clear fields or navigate away
                        // binding.spinnerProductName.setSelection(0); // Réinitialiser le spinner si nécessaire
                        binding.etQuantity.setText("");
                        binding.etClientName.setText("");
                        binding.etPrice.setText("");
                    } else {
                        Toast.makeText(NewSaleActivity.this, "Failed to record sale: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}