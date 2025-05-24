package com.example.projetandroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projetandroid.R;
import com.example.projetandroid.model.InvoiceItem;

import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class InvoiceItemAdapter extends RecyclerView.Adapter<InvoiceItemAdapter.InvoiceItemViewHolder> {
    
    private List<InvoiceItem> items;
    private OnItemRemovedListener listener;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    
    public interface OnItemRemovedListener {
        void onItemRemoved(int position);
    }
    
    public InvoiceItemAdapter(List<InvoiceItem> items, OnItemRemovedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvoiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_product, parent, false);
        return new InvoiceItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceItemViewHolder holder, int position) {
        InvoiceItem item = items.get(position);
        holder.tvProductName.setText(item.getProductName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvUnitPrice.setText(currencyFormat.format(item.getUnitPrice()));
        holder.tvSubtotal.setText(currencyFormat.format(item.getSubtotal()));
        
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemRemoved(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<InvoiceItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class InvoiceItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvQuantity, tvUnitPrice, tvSubtotal;
        ImageButton btnRemove;
        
        public InvoiceItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnRemove = itemView.findViewById(R.id.btnRemoveItem);
        }
    }
}