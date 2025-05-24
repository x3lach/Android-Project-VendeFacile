package com.example.projetandroid.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projetandroid.R;
import com.example.projetandroid.model.Invoice;
import com.example.projetandroid.model.InvoiceItem;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder> {

    private List<Invoice> invoiceList;
    private OnInvoiceClickListener listener;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnInvoiceClickListener {
        void onInvoiceClick(Invoice invoice);
    }

    public InvoiceAdapter(List<Invoice> invoiceList, OnInvoiceClickListener listener) {
        this.invoiceList = invoiceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new InvoiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        Invoice invoice = invoiceList.get(position);
        
        // Vérifier s'il s'agit d'un groupe
        if (invoice.isGroup()) {
            // C'est un groupe de factures du même client
            holder.tvInvoiceId.setText("Client fidèle");
            
            // Afficher la date la plus récente
            holder.tvInvoiceDate.setText(dateFormat.format(new Date(invoice.getTimestamp())));
            
            // Afficher le nom du client
            holder.tvClientName.setText("Client: " + invoice.getClientName());
            
            // Add location information if available
            if (invoice.getLatitude() != 0 && invoice.getLongitude() != 0) {
                String locationText = "Position: " + String.format("%.5f, %.5f", invoice.getLatitude(), invoice.getLongitude());
                holder.tvLocationInfo.setText(locationText);
                holder.tvLocationInfo.setVisibility(View.VISIBLE);
            } else {
                holder.tvLocationInfo.setVisibility(View.GONE);
            }
            
            // Afficher le nombre de factures dans ce groupe
            List<String> summaries = new ArrayList<>();
            summaries.add("Total de " + invoice.getGroupedInvoices().size() + " factures");
            summaries.add("Dernier achat: " + dateFormat.format(
                    new Date(invoice.getGroupedInvoices().get(0).getTimestamp())));
            summaries.add("Total cumulé: " + currencyFormat.format(invoice.getTotalAmount()));
            
            // Display products from the most recent invoice in the group
            if (!invoice.getGroupedInvoices().isEmpty() && invoice.getGroupedInvoices().get(0).getItems() != null) {
                List<InvoiceItem> items = invoice.getGroupedInvoices().get(0).getItems();
                int itemCount = Math.min(items.size(), 3);
                for (int i = 0; i < itemCount; i++) {
                    summaries.add("- " + items.get(i).getQuantity() + " × " + items.get(i).getProductName());
                }
                
                // Si plus de 3 produits, ajouter indication
                if (items.size() > 3) {
                    summaries.add("- ... et " + (items.size() - 3) + " autres");
                }
            }
            
            holder.tvProductsSummary.setText(TextUtils.join("\n", summaries));
            holder.tvTotalAmount.setText(currencyFormat.format(invoice.getTotalAmount()));
            
        } else {
            // Traitement normal pour une facture individuelle
            // Afficher l'ID de facture court
            holder.tvInvoiceId.setText(getShortId(invoice.getInvoiceId()));
            
            // Afficher la date
            holder.tvInvoiceDate.setText(dateFormat.format(new Date(invoice.getTimestamp())));
            
            // Afficher le nom du client
            holder.tvClientName.setText("Client: " + invoice.getClientName());
            
            // Add location information if available
            if (invoice.getLatitude() != 0 && invoice.getLongitude() != 0) {
                String locationText = "Position: " + String.format("%.5f, %.5f", invoice.getLatitude(), invoice.getLongitude());
                holder.tvLocationInfo.setText(locationText);
                holder.tvLocationInfo.setVisibility(View.VISIBLE);
            } else {
                holder.tvLocationInfo.setVisibility(View.GONE);
            }
            
            // Make sure to display products for individual invoices
            List<String> productSummaries = new ArrayList<>();
            if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
                int itemCount = Math.min(invoice.getItems().size(), 3);
                for (int i = 0; i < itemCount; i++) {
                    productSummaries.add("- " + invoice.getItems().get(i).getQuantity() + " × " 
                            + invoice.getItems().get(i).getProductName());
                }
                
                // Si plus de 3 produits, ajouter indication
                if (invoice.getItems().size() > 3) {
                    productSummaries.add("- ... et " + (invoice.getItems().size() - 3) + " autres");
                }
                
                holder.tvProductsSummary.setText(TextUtils.join("\n", productSummaries));
            } else {
                holder.tvProductsSummary.setText("Aucun produit");
            }
            
            holder.tvTotalAmount.setText(currencyFormat.format(invoice.getTotalAmount()));
        }
        
        // Before calling bind, make sure the button is visible if it exists
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setVisibility(View.VISIBLE);
        }
        
        holder.bind(invoice, listener);
    }

    @Override
    public int getItemCount() {
        return invoiceList.size();
    }
    
    public void updateInvoices(List<Invoice> newInvoiceList) {
        this.invoiceList = newInvoiceList;
        notifyDataSetChanged();
    }
    
    // Méthode utilitaire pour raccourcir l'ID de la facture
    private String getShortId(String id) {
        if (id != null && id.length() > 6) {
            return id.substring(id.length() - 6);
        }
        return id;
    }

    static class InvoiceViewHolder extends RecyclerView.ViewHolder {
        TextView tvInvoiceId, tvInvoiceDate, tvClientName, tvProductsSummary, tvTotalAmount, tvLocationInfo;
        Button btnViewDetails;
        
        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInvoiceId = itemView.findViewById(R.id.tv_invoice_id);
            tvInvoiceDate = itemView.findViewById(R.id.tv_invoice_date);
            tvClientName = itemView.findViewById(R.id.tv_client_name);
            tvProductsSummary = itemView.findViewById(R.id.tv_products_summary);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            tvLocationInfo = itemView.findViewById(R.id.tv_location_info);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);  // Initialize the button
        }
        
        public void bind(Invoice invoice, OnInvoiceClickListener listener) {
            // Keep the existing item click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInvoiceClick(invoice);
                }
            });
            
            // Add click listener to the details button if it exists
            if (btnViewDetails != null) {
                btnViewDetails.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onInvoiceClick(invoice);
                    }
                });
            }
        }
    }
}