package com.example.projetandroid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projetandroid.databinding.ActivityMainBinding;
import com.example.projetandroid.view.LoginActivity;
import com.example.projetandroid.view.AddProductActivity;
import com.example.projetandroid.view.SalesMapActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize view binding
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            
            // Check if user is signed in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // Not signed in, launch the Login activity
                Log.d(TAG, "No user logged in, redirecting to LoginActivity");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            
            Toast.makeText(this, "Bienvenue " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
            
            // Set up buttons only if we're not redirecting
            setupButtons();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupButtons() {
        // Check if binding is initialized
        if (binding != null) {
            // Sales section buttons
            setupSalesButtons();
            
            // Inventory section button
            setupInventoryButton();
            
            // Logout button
            setupLogoutButton();
        } else {
            Log.e(TAG, "Binding is null in setupButtons");
        }
    }
    
    private void setupSalesButtons() {
        // New Sale button
        View btnNewSale = binding.cardSales.findViewById(R.id.btnNewSale);
        if (btnNewSale != null) {
            btnNewSale.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, 
                            Class.forName("com.example.projetandroid.view.InvoiceSaleActivity"));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching InvoiceSaleActivity", e);
                    Toast.makeText(MainActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // History button
        View btnHistory = binding.cardSales.findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, 
                            Class.forName("com.example.projetandroid.view.HistoryActivity"));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching HistoryActivity", e);
                    Toast.makeText(MainActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Sales Map button
        View btnSalesMap = binding.cardSales.findViewById(R.id.btnSalesMap);
        if (btnSalesMap != null) {
            btnSalesMap.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, SalesMapActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching SalesMapActivity", e);
                    Toast.makeText(MainActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void setupInventoryButton() {
        // Manage Products button
        View btnManageProducts = binding.cardInventory.findViewById(R.id.btnManageProducts);
        if (btnManageProducts != null) {
            btnManageProducts.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, 
                            Class.forName("com.example.projetandroid.view.AddProductActivity"));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching AddProductActivity", e);
                    Toast.makeText(MainActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void setupLogoutButton() {
        // Logout button
        View btnLogout = binding.cardAccount.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                try {
                    mAuth.signOut();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error during logout", e);
                    Toast.makeText(MainActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
