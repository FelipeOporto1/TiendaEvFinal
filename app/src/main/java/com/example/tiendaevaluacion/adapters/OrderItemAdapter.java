package com.example.tiendaevaluacion.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.R;
import com.example.tiendaevaluacion.models.OrderItem;
import java.util.List;
import java.util.Locale;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {
    private List<OrderItem> items;

    public OrderItemAdapter(List<OrderItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = items.get(position);
        holder.nameText.setText(item.getProductName());
        holder.quantityText.setText("Cantidad: " + item.getQuantity());
        holder.priceText.setText(String.format(Locale.getDefault(), "$%.2f", item.getPrice()));
        holder.totalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", item.getTotal()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView quantityText;
        TextView priceText;
        TextView totalText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.product_name);
            quantityText = itemView.findViewById(R.id.quantity_text);
            priceText = itemView.findViewById(R.id.price_text);
            totalText = itemView.findViewById(R.id.total_text);
        }
    }
} 