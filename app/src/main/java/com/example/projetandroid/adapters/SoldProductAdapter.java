package com.example.projetandroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projetandroid.R;
import com.example.projetandroid.model.SoldProduct;

import java.util.List;
import java.util.Locale;

public class SoldProductAdapter extends RecyclerView.Adapter<SoldProductAdapter.SoldProductViewHolder> {

    private List<SoldProduct> products;

    public SoldProductAdapter(List<SoldProduct> products) {
        this.products = products;
    }

    public void updateProducts(List<SoldProduct> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SoldProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sold_product, parent, false);
        return new SoldProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SoldProductViewHolder holder, int position) {
        SoldProduct product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class SoldProductViewHolder extends RecyclerView.ViewHolder {
        private TextView tvProductName, tvProductQuantity, tvProductPrice, tvProductSubtotal;

        public SoldProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvSoldProductName);
            tvProductQuantity = itemView.findViewById(R.id.tvSoldProductQuantity);
            tvProductPrice = itemView.findViewById(R.id.tvSoldProductPrice);
            tvProductSubtotal = itemView.findViewById(R.id.tvSoldProductSubtotal);
        }

        public void bind(SoldProduct product) {
            tvProductName.setText(product.getName());
            tvProductQuantity.setText(String.format(Locale.getDefault(), "Qté: %d", product.getQuantity()));
            tvProductPrice.setText(String.format(Locale.getDefault(), "%.2f €/u", product.getPrice()));
            
            double subtotal = product.getPrice() * product.getQuantity();
            tvProductSubtotal.setText(String.format(Locale.getDefault(), "%.2f €", subtotal));
        }
    }
}