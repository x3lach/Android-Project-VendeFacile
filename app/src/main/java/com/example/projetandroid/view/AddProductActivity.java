// filepath: app/src/main/java/com/example/projetandroid/view/AddProductActivity.java
package com.example.projetandroid.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.projetandroid.adapters.ProductAdapter;
import com.example.projetandroid.databinding.ActivityAddProductBinding;
import com.example.projetandroid.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddProductActivity extends AppCompatActivity implements ProductAdapter.OnProductActionListener {

    private ActivityAddProductBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<Product> productList;
    private ProductAdapter productAdapter;
    private String editingProductId = null; // Pour suivre quel produit est en cours d'édition

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        productList = new ArrayList<>();

        // Setup RecyclerView
        binding.recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(productList, this);
        binding.recyclerViewProducts.setAdapter(productAdapter);

        // Load existing products
        loadProducts();

        binding.btnSaveProduct.setOnClickListener(v -> saveProduct());
        
        // Changez le texte du bouton en fonction du mode
        updateSaveButtonText();
    }

    private void loadProducts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference productsRef = mDatabase.child("users").child(userId).child("products");

        binding.progressBarAddProduct.setVisibility(View.VISIBLE);
        
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
                productAdapter.updateProductList(productList);
                binding.progressBarAddProduct.setVisibility(View.GONE);
                
                if (productList.isEmpty()) {
                    Toast.makeText(AddProductActivity.this, 
                            "Aucun produit trouvé. Ajoutez votre premier produit.", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AddProductActivity.this, 
                        "Erreur de chargement des produits: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                binding.progressBarAddProduct.setVisibility(View.GONE);
            }
        });
    }

    private void saveProduct() {
        String productName = binding.etNewProductName.getText().toString().trim();
        String productPriceStr = binding.etNewProductPrice.getText().toString().trim();
        String productQuantityStr = binding.etNewProductQuantity.getText().toString().trim();

        if (TextUtils.isEmpty(productName)) {
            binding.etNewProductName.setError("Le nom du produit est requis");
            return;
        }

        double productPrice = 0.0;
        if (TextUtils.isEmpty(productPriceStr)) {
            binding.etNewProductPrice.setError("Le prix est requis");
            return;
        }

        try {
            productPrice = Double.parseDouble(productPriceStr);
            if (productPrice < 0) {
                binding.etNewProductPrice.setError("Le prix doit être positif");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etNewProductPrice.setError("Prix invalide");
            return;
        }

        int productQuantity = 0;
        if (TextUtils.isEmpty(productQuantityStr)) {
            binding.etNewProductQuantity.setError("La quantité est requise");
            return;
        }

        try {
            productQuantity = Integer.parseInt(productQuantityStr);
            if (productQuantity < 0) {
                binding.etNewProductQuantity.setError("La quantité doit être positive");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etNewProductQuantity.setError("Quantité invalide");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_LONG).show();
            return;
        }
        String userId = currentUser.getUid();

        binding.progressBarAddProduct.setVisibility(View.VISIBLE);

        // Déterminer s'il s'agit d'un nouveau produit ou d'une modification
        String productId;
        if (editingProductId != null) {
            productId = editingProductId;
        } else {
            productId = mDatabase.child("users").child(userId).child("products").push().getKey();
            if (productId == null) {
                Toast.makeText(this, "Échec de la création de l'ID du produit.", Toast.LENGTH_SHORT).show();
                binding.progressBarAddProduct.setVisibility(View.GONE);
                return;
            }
        }

        Product product = new Product(productId, productName, productPrice, productQuantity);

        mDatabase.child("users").child(userId).child("products").child(productId).setValue(product)
                .addOnCompleteListener(task -> {
                    binding.progressBarAddProduct.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        String message = (editingProductId != null) ? "Produit mis à jour !" : "Produit enregistré !";
                        Toast.makeText(AddProductActivity.this, message, Toast.LENGTH_SHORT).show();
                        
                        // Réinitialiser le formulaire et le mode d'édition
                        binding.etNewProductName.setText("");
                        binding.etNewProductPrice.setText("");
                        binding.etNewProductQuantity.setText("");
                        editingProductId = null;
                        updateSaveButtonText();
                    } else {
                        Toast.makeText(AddProductActivity.this, "Échec de l'opération: " + 
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    // Implémentation de l'interface OnProductActionListener
    @Override
    public void onEditProduct(Product product) {
        // Remplir les champs avec les valeurs du produit
        binding.etNewProductName.setText(product.getName());
        binding.etNewProductPrice.setText(String.valueOf(product.getPrice()));
        binding.etNewProductQuantity.setText(String.valueOf(product.getQuantity()));
        
        // Mettre à jour le mode d'édition et le texte du bouton
        editingProductId = product.getProductId();
        updateSaveButtonText();
        
        // Solution 1: Supprimer cette ligne si le défilement n'est pas nécessaire
        // Solution 2: Si vous avez un NestedScrollView ou un autre conteneur avec défilement:
        // binding.nestedScrollView.smoothScrollTo(0, 0);
        // Solution 3: Faire défiler la vue principale vers le haut
        binding.getRoot().scrollTo(0, 0);
    }
    
    @Override
    public void onDeleteProduct(Product product) {
        // Demander confirmation avant suppression
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le produit")
                .setMessage("Êtes-vous sûr de vouloir supprimer " + product.getName() + " ?")
                .setPositiveButton("Supprimer", (dialog, which) -> deleteProduct(product))
                .setNegativeButton("Annuler", null)
                .show();
    }
    
    private void deleteProduct(Product product) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        binding.progressBarAddProduct.setVisibility(View.VISIBLE);
        
        mDatabase.child("users").child(userId).child("products").child(product.getProductId())
                .removeValue()
                .addOnCompleteListener(task -> {
                    binding.progressBarAddProduct.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(AddProductActivity.this, 
                                "Produit supprimé !", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AddProductActivity.this, 
                                "Échec de la suppression: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void updateSaveButtonText() {
        if (editingProductId != null) {
            binding.btnSaveProduct.setText("Modifier le produit");
            // Option d'annuler l'édition
            binding.btnCancelEdit.setVisibility(View.VISIBLE);
            binding.btnCancelEdit.setOnClickListener(v -> {
                // Réinitialiser le formulaire et le mode d'édition
                binding.etNewProductName.setText("");
                binding.etNewProductPrice.setText("");
                binding.etNewProductQuantity.setText("");
                editingProductId = null;
                updateSaveButtonText();
            });
        } else {
            binding.btnSaveProduct.setText("Enregistrer Produit");
            binding.btnCancelEdit.setVisibility(View.GONE);
        }
    }
}