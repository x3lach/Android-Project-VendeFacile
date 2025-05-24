package com.example.projetandroid.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.projetandroid.adapters.InvoiceAdapter;
import com.example.projetandroid.databinding.ActivityHistoryBinding;
import com.example.projetandroid.model.Invoice;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity implements InvoiceAdapter.OnInvoiceClickListener {

    private ActivityHistoryBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<Invoice> allInvoiceList;  // All invoices retrieved from Firebase
    private List<Invoice> filteredInvoiceList;  // Filtered invoices to display
    private InvoiceAdapter invoiceAdapter;
    
    // Format pour les montants
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);  // Ajoutez cette ligne
    
    // Filter variables
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private boolean hasStartDateFilter = false;
    private boolean hasEndDateFilter = false;
    private String clientFilter = "";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        allInvoiceList = new ArrayList<>();
        filteredInvoiceList = new ArrayList<>();
        
        // Initialize calendars
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        
        // Setup RecyclerView
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        invoiceAdapter = new InvoiceAdapter(filteredInvoiceList, this);
        binding.recyclerViewHistory.setAdapter(invoiceAdapter);
        
        setupFilterButtons();
        
        // Load all invoices initially
        loadInvoices();
    }
    
    private void setupFilterButtons() {
        // Date pickers setup
        binding.btnStartDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        startDateCalendar.set(Calendar.YEAR, year);
                        startDateCalendar.set(Calendar.MONTH, month);
                        startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        startDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
                        startDateCalendar.set(Calendar.MINUTE, 0);
                        startDateCalendar.set(Calendar.SECOND, 0);
                        hasStartDateFilter = true;
                        
                        // Update button text
                        binding.btnStartDate.setText(dateFormat.format(startDateCalendar.getTime()));
                    },
                    startDateCalendar.get(Calendar.YEAR),
                    startDateCalendar.get(Calendar.MONTH),
                    startDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        binding.btnEndDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        endDateCalendar.set(Calendar.YEAR, year);
                        endDateCalendar.set(Calendar.MONTH, month);
                        endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        endDateCalendar.set(Calendar.HOUR_OF_DAY, 23);
                        endDateCalendar.set(Calendar.MINUTE, 59);
                        endDateCalendar.set(Calendar.SECOND, 59);
                        hasEndDateFilter = true;
                        
                        // Update button text
                        binding.btnEndDate.setText(dateFormat.format(endDateCalendar.getTime()));
                    },
                    endDateCalendar.get(Calendar.YEAR),
                    endDateCalendar.get(Calendar.MONTH),
                    endDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Apply filters button
        binding.btnApplyFilters.setOnClickListener(v -> {
            applyFilters();
        });
        
        // Reset filters button
        binding.btnResetFilters.setOnClickListener(v -> {
            resetFilters();
        });
    }
    
    private void resetFilters() {
        // Reset date filters
        hasStartDateFilter = false;
        hasEndDateFilter = false;
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        binding.btnStartDate.setText("Date début");
        binding.btnEndDate.setText("Date fin");
        
        // Reset client filter
        binding.etClientFilter.setText("");
        clientFilter = "";
        
        // Show all invoices
        filteredInvoiceList.clear();
        filteredInvoiceList.addAll(allInvoiceList);
        invoiceAdapter.updateInvoices(filteredInvoiceList);
        
        // Show/hide no results message
        if (filteredInvoiceList.isEmpty()) {
            binding.tvNoInvoices.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoInvoices.setVisibility(View.GONE);
        }
    }
    
    private void applyFilters() {
        // Get client filter
        clientFilter = binding.etClientFilter.getText().toString().trim().toLowerCase();
        
        // Filter the invoices
        filteredInvoiceList.clear();
        
        for (Invoice invoice : allInvoiceList) {
            boolean matchesDateFilter = true;
            boolean matchesClientFilter = true;
            
            // Check date filters
            if (hasStartDateFilter && invoice.getTimestamp() < startDateCalendar.getTimeInMillis()) {
                matchesDateFilter = false;
            }
            
            if (hasEndDateFilter && invoice.getTimestamp() > endDateCalendar.getTimeInMillis()) {
                matchesDateFilter = false;
            }
            
            // Check client filter
            if (!TextUtils.isEmpty(clientFilter) && 
                (invoice.getClientName() == null || 
                 !invoice.getClientName().toLowerCase().contains(clientFilter))) {
                matchesClientFilter = false;
            }
            
            // Add invoice if it matches all filters
            if (matchesDateFilter && matchesClientFilter) {
                filteredInvoiceList.add(invoice);
            }
        }
        
        // Update the adapter with filtered results
        invoiceAdapter.updateInvoices(filteredInvoiceList);
        
        // Show message if no results
        if (filteredInvoiceList.isEmpty()) {
            binding.tvNoInvoices.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoInvoices.setVisibility(View.GONE);
        }
    }
    
    private void loadInvoices() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference invoicesRef = mDatabase.child("users").child(userId).child("invoices");
        
        // Show loading indicator
        binding.progressBarHistory.setVisibility(View.VISIBLE);
        
        // Query invoices ordered by timestamp 
        Query query = invoicesRef.orderByChild("timestamp");
        
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allInvoiceList.clear();
                
                // Map pour regrouper les factures par client (clé = nom+téléphone)
                Map<String, List<Invoice>> clientInvoicesMap = new HashMap<>();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Invoice invoice = snapshot.getValue(Invoice.class);
                    if (invoice != null) {
                        // Créer une clé unique basée sur le nom et le téléphone du client
                        String clientKey = invoice.getClientName() + "_" + 
                                           (invoice.getClientPhone() != null ? invoice.getClientPhone() : "");
                        
                        // Ajouter la facture au groupe correspondant au client
                        if (!clientInvoicesMap.containsKey(clientKey)) {
                            clientInvoicesMap.put(clientKey, new ArrayList<>());
                        }
                        clientInvoicesMap.get(clientKey).add(invoice);
                    }
                }
                
                // Créer une facture "groupe" pour chaque client
                for (Map.Entry<String, List<Invoice>> entry : clientInvoicesMap.entrySet()) {
                    List<Invoice> clientInvoices = entry.getValue();
                    
                    // Si le client a plusieurs factures, on crée une facture "groupe"
                    if (clientInvoices.size() > 1) {
                        Invoice groupInvoice = new Invoice();
                        groupInvoice.setClientName(clientInvoices.get(0).getClientName());
                        groupInvoice.setClientPhone(clientInvoices.get(0).getClientPhone());
                        groupInvoice.setInvoiceId("groupe_" + entry.getKey()); // ID spécial pour groupe
                        groupInvoice.setTimestamp(System.currentTimeMillis()); // Date actuelle
                        
                        // Marquer cette facture comme un groupe
                        groupInvoice.setGroupedInvoices(clientInvoices);
                        
                        // Calculer le total pour toutes les factures de ce client
                        double totalForClient = 0;
                        for (Invoice inv : clientInvoices) {
                            totalForClient += inv.getTotalAmount();
                        }
                        groupInvoice.setTotalAmount(totalForClient);
                        
                        allInvoiceList.add(groupInvoice);
                    } else {
                        // Si le client n'a qu'une facture, on l'ajoute normalement
                        allInvoiceList.add(clientInvoices.get(0));
                    }
                }
                
                // Trier les factures (les plus récentes d'abord)
                Collections.sort(allInvoiceList, new Comparator<Invoice>() {
                    @Override
                    public int compare(Invoice i1, Invoice i2) {
                        return Long.compare(i2.getTimestamp(), i1.getTimestamp());
                    }
                });
                
                // Reset filters to show all invoices
                resetFilters();
                
                binding.progressBarHistory.setVisibility(View.GONE);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                binding.progressBarHistory.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, 
                        "Erreur de chargement: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onInvoiceClick(Invoice invoice) {
        if (invoice.isGroup()) {
            // Pour un groupe, afficher une boîte de dialogue avec la liste des factures
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Factures de " + invoice.getClientName());
            
            // Créer un adapter pour la liste
            final List<Invoice> groupInvoices = invoice.getGroupedInvoices();
            String[] invoiceDates = new String[groupInvoices.size()];
            
            for (int i = 0; i < groupInvoices.size(); i++) {
                Invoice inv = groupInvoices.get(i);
                // Add location information to each item in the dialog
                String locationInfo = "";
                if (inv.getLatitude() != 0 && inv.getLongitude() != 0) {
                    locationInfo = String.format("\nPosition: %.5f, %.5f", inv.getLatitude(), inv.getLongitude());
                }
                invoiceDates[i] = dateFormat.format(new Date(inv.getTimestamp())) + 
                                 " - " + currencyFormat.format(inv.getTotalAmount()) + locationInfo;
            }
            
            builder.setItems(invoiceDates, (dialog, which) -> {
                // Quand une facture est sélectionnée, ouvrir son détail
                Intent intent = new Intent(this, InvoiceDetailActivity.class);
                intent.putExtra("INVOICE_ID", groupInvoices.get(which).getInvoiceId());
                startActivity(intent);
            });
            
            builder.setNegativeButton("Fermer", null);
            builder.show();
        } else {
            // Pour une facture normale, comportement habituel
            Intent intent = new Intent(this, InvoiceDetailActivity.class);
            intent.putExtra("INVOICE_ID", invoice.getInvoiceId());
            startActivity(intent);
        }
    }
}