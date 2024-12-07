package com.example.tiendaevaluacion.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.OrderDetailActivity;
import com.example.tiendaevaluacion.R;
import com.example.tiendaevaluacion.models.Order;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orders;
    private Context context;
    private SimpleDateFormat dateFormat;
    private OrderClickListener listener;

    public interface OrderClickListener {
        void onOrderClick(Order order);
        void onOrderCancel(Order order);
    }

    public OrderAdapter(Context context, List<Order> orders, OrderClickListener listener) {
        this.context = context;
        this.orders = orders;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.orderIdText.setText(String.format("Pedido #%d", order.getId()));
        holder.dateText.setText(dateFormat.format(order.getOrderDate()));
        holder.statusText.setText(getStatusText(order.getStatus()));
        holder.totalText.setText(String.format(Locale.getDefault(), "$%.2f", order.getTotalAmount()));
        
        String deliveryInfo = order.getDeliveryType().equals("DELIVERY") ?
                "Entrega a domicilio: " + order.getAddress() :
                "Retiro en tienda";
        holder.deliveryText.setText(deliveryInfo);

        if ("PENDING".equals(order.getStatus())) {
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                    .setTitle(R.string.cancel_order_title)
                    .setMessage(R.string.confirm_cancel_order)
                    .setPositiveButton("Sí", (dialog, which) -> {
                        if (listener != null) {
                            listener.onOrderCancel(order);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            });
        } else {
            holder.cancelButton.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Pendiente";
            case "PROCESSING": return "En proceso";
            case "SHIPPED": return "Enviado";
            case "DELIVERED": return "Entregado";
            default: return status;
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<Order> newOrders) {
        orders = newOrders;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText;
        TextView dateText;
        TextView statusText;
        TextView totalText;
        TextView deliveryText;
        Button cancelButton;

        ViewHolder(View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            dateText = itemView.findViewById(R.id.date_text);
            statusText = itemView.findViewById(R.id.status_text);
            totalText = itemView.findViewById(R.id.total_text);
            deliveryText = itemView.findViewById(R.id.delivery_text);
            cancelButton = itemView.findViewById(R.id.cancel_order_button);
        }
    }
} 