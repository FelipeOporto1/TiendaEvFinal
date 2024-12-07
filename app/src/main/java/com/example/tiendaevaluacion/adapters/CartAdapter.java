package com.example.tiendaevaluacion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.R;
import com.example.tiendaevaluacion.models.CartItem;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<CartItem> items;
    private Context context;
    private CartItemListener listener;

    public interface CartItemListener {
        void onQuantityChanged(CartItem item);
        void onItemDeleted(CartItem item);
    }

    public CartAdapter(Context context, List<CartItem> items, CartItemListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.nameText.setText(item.getProductName());
        holder.priceText.setText(String.format(Locale.getDefault(), "$%.2f", item.getPrice()));
        holder.quantityText.setText(String.valueOf(item.getQuantity()));
        holder.totalText.setText(String.format(Locale.getDefault(), "$%.2f", item.getTotal()));

        String size = item.getSize();
        if (size != null && !size.isEmpty()) {
            holder.sizeText.setVisibility(View.VISIBLE);
            holder.sizeText.setText("Talla: " + size);
        } else {
            holder.sizeText.setVisibility(View.GONE);
        }

        holder.increaseButton.setOnClickListener(v -> {
            item.setQuantity(item.getQuantity() + 1);
            notifyItemChanged(position);
            listener.onQuantityChanged(item);
        });

        holder.decreaseButton.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                notifyItemChanged(position);
                listener.onQuantityChanged(item);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            listener.onItemDeleted(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<CartItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView priceText;
        TextView quantityText;
        TextView totalText;
        TextView sizeText;
        ImageButton increaseButton;
        ImageButton decreaseButton;
        Button deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.product_name);
            priceText = itemView.findViewById(R.id.product_price);
            quantityText = itemView.findViewById(R.id.quantity_text);
            totalText = itemView.findViewById(R.id.total_text);
            sizeText = itemView.findViewById(R.id.size_text);
            increaseButton = itemView.findViewById(R.id.increase_button);
            decreaseButton = itemView.findViewById(R.id.decrease_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
} 