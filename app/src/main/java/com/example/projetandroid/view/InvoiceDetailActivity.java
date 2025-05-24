package com.example.projetandroid.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.projetandroid.adapters.SoldProductAdapter;
import com.example.projetandroid.databinding.ActivityInvoiceDetailBinding;
import com.example.projetandroid.model.Invoice;
import com.example.projetandroid.model.InvoiceItem;
import com.example.projetandroid.model.SoldProduct;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceDetailActivity extends AppCompatActivity {

    private ActivityInvoiceDetailBinding binding;
    private String invoiceId;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private SoldProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get the invoice ID from the intent
        invoiceId = getIntent().getStringExtra("INVOICE_ID");
        
        // Add debug logging
        Log.d("InvoiceDetail", "Looking for invoice with ID: " + invoiceId);
        
        if (invoiceId == null) {
            Toast.makeText(this, "Erreur: Impossible de charger la facture", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Reference to the Firebase database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        
        // Check if the path is correct - this might be the issue
        // Some apps use a different structure like "users/{userId}/invoices/{invoiceId}"
        DatabaseReference invoiceRef = database.getReference("invoices").child(invoiceId);
        
        // Try one of these alternatives if the current path isn't working:

        // If your invoices are stored per user:
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        invoiceRef = database.getReference("users").child(userId).child("invoices").child(invoiceId);

        // Or if you have a different top-level collection:
        // invoiceRef = database.getReference("sales").child("invoices").child(invoiceId);

        // Or directly at the root:
        // invoiceRef = database.getReference().child(invoiceId);
        
        // Add debug listener to see if the path exists
        invoiceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("InvoiceDetail", "Data exists: " + dataSnapshot.exists());
                if (dataSnapshot.exists()) {
                    Log.d("InvoiceDetail", "Data value: " + dataSnapshot.getValue().toString());
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("InvoiceDetail", "Error checking if data exists", databaseError.toException());
            }
        });
        
        // Load the invoice data
        invoiceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Invoice invoice = dataSnapshot.getValue(Invoice.class);
                    if (invoice != null) {
                        Log.d("InvoiceDetail", "Invoice loaded successfully");
                        displayInvoiceDetails(invoice);
                        
                        // Add this to display the products
                        if (invoice.getItems() != null) {
                            displayProducts(invoice.getItems());
                        } else {
                            Log.e("InvoiceDetail", "Invoice items list is null");
                            binding.tvProductsList.setText("Aucun produit dans cette facture.");
                        }
                    } else {
                        Log.e("InvoiceDetail", "Invoice is null after conversion");
                        Toast.makeText(InvoiceDetailActivity.this, "Facture introuvable", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("InvoiceDetail", "Error converting data to Invoice", e);
                    Toast.makeText(InvoiceDetailActivity.this, "Erreur de conversion: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(InvoiceDetailActivity.this, "Erreur: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayInvoiceDetails(Invoice invoice) {
        // Display basic invoice info
        binding.tvClientInfo.setText("Client: " + invoice.getClientName());
        
        // Display phone number if available
        if (invoice.getClientPhone() != null && !invoice.getClientPhone().isEmpty()) {
            binding.tvClientPhone.setText("Téléphone: " + invoice.getClientPhone());
            binding.tvClientPhone.setVisibility(View.VISIBLE);
        } else {
            binding.tvClientPhone.setVisibility(View.GONE);
        }
        
        // Display location information (latitude & longitude)
        if (invoice.getLatitude() != 0 || invoice.getLongitude() != 0) {
            String locationText = String.format(Locale.getDefault(), 
                "Position: %.5f, %.5f", invoice.getLatitude(), invoice.getLongitude());
            
            // Add address if available
            if (invoice.getLocationAddress() != null && !invoice.getLocationAddress().isEmpty()) {
                locationText += "\nAdresse: " + invoice.getLocationAddress();
            }
            
            binding.tvLocation.setText(locationText);
            binding.tvLocation.setVisibility(View.VISIBLE);
        } else {
            binding.tvLocation.setVisibility(View.GONE);
        }
        
        // Display date
        binding.tvDate.setText("Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(invoice.getTimestamp())));
                
        // Display total
        binding.tvTotal.setText("Total: " + String.format(Locale.getDefault(), "%.2f €", invoice.getTotalAmount()));
    }

    // Add this new method to display products
    private void displayProducts(List<InvoiceItem> items) {
        if (items == null || items.isEmpty()) {
            binding.tvProductsList.setText("Aucun produit dans cette facture.");
            return;
        }
        
        // Build the product list text
        StringBuilder productsText = new StringBuilder();
        for (InvoiceItem item : items) {
            productsText.append("• ")
                       .append(item.getQuantity())
                       .append(" × ")
                       .append(item.getProductName())
                       .append(" - ")
                       .append(String.format(Locale.getDefault(), "%.2f €", item.getPrice() * item.getQuantity()))
                       .append("\n");
        }
        
        // Set the text to the TextView
        binding.tvProductsList.setText(productsText.toString());
    }

    private void editInvoice() {
        // Rediriger vers l'activité de vente avec les informations de la facture pour modification
        Intent intent = new Intent(this, InvoiceSaleActivity.class);
        intent.putExtra("INVOICE_ID", invoiceId);
        intent.putExtra("EDIT_MODE", true);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteInvoice() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la facture")
                .setMessage("Êtes-vous sûr de vouloir supprimer cette facture ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> deleteInvoice())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteInvoice() {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference invoiceRef = mDatabase.child("users").child(userId).child("invoices").child(invoiceId);

        invoiceRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Facture supprimée avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}