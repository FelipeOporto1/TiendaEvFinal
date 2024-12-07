package com.example.tiendaevaluacion.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.tiendaevaluacion.AddEditProductActivity;
import com.example.tiendaevaluacion.R;
import com.example.tiendaevaluacion.models.Product;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<Product> products;
    private Context context;
    private OnProductDeleteListener deleteListener;

    public interface OnProductDeleteListener {
        void onDeleteProduct(Product product);
    }

    public ProductAdapter(Context context, List<Product> products, OnProductDeleteListener listener) {
        this.context = context;
        this.products = products;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.nameText.setText(product.getName());
        holder.descriptionText.setText(product.getDescription());
        holder.priceText.setText(String.format(Locale.getDefault(), "$%.2f", product.getPrice()));
        holder.stockText.setText(String.format(Locale.getDefault(), "Stock: %d", product.getStock()));

        // Cargar imagen
        try {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Uri imageUri = Uri.parse(product.getImageUrl());
                Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.default_product_image)
                    .error(R.drawable.default_product_image)
                    .into(holder.productImage);
            } else {
                holder.productImage.setImageResource(R.drawable.default_product_image);
            }
        } catch (Exception e) {
            holder.productImage.setImageResource(R.drawable.default_product_image);
        }

        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditProductActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            context.startActivity(intent);
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteProduct(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        products = newProducts;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView descriptionText;
        TextView priceText;
        TextView stockText;
        Button editButton;
        Button deleteButton;
        ImageView productImage;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.product_name);
            descriptionText = itemView.findViewById(R.id.product_description);
            priceText = itemView.findViewById(R.id.product_price);
            stockText = itemView.findViewById(R.id.product_stock);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            productImage = itemView.findViewById(R.id.product_image);
        }
    }
} 