package com.example.tiendaevaluacion.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.OrderDetailActivity;
import com.example.tiendaevaluacion.R;
import com.example.tiendaevaluacion.models.Order;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import androidx.core.content.res.ResourcesCompat;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.ViewHolder> {
    private List<Order> orders;
    private Context context;
    private SimpleDateFormat dateFormat;
    private OrderStatusListener listener;
    private static final String[] STATUS_OPTIONS = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED"};

    public interface OrderStatusListener {
        void onStatusChanged(Order order, String newStatus);
    }

    public AdminOrderAdapter(Context context, List<Order> orders, OrderStatusListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.orderIdText.setText(String.format("Pedido #%d", order.getId()));
        holder.dateText.setText(dateFormat.format(order.getOrderDate()));
        holder.customerText.setText("Cliente: " + order.getCustomerName());
        holder.totalText.setText(String.format(Locale.getDefault(), "$%.2f", order.getTotalAmount()));
        
        String deliveryInfo = order.getDeliveryType().equals("DELIVERY") ?
                "Entrega a domicilio: " + order.getAddress() :
                "Retiro en tienda";
        holder.deliveryText.setText(deliveryInfo);

        // Configurar spinner de estado con el nuevo estilo
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            STATUS_OPTIONS
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(context.getResources().getColor(R.color.bright_red));
                text.setTypeface(ResourcesCompat.getFont(context, R.font.metal_mania));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(context.getResources().getColor(R.color.bright_red));
                text.setTypeface(ResourcesCompat.getFont(context, R.font.metal_mania));
                view.setBackgroundColor(context.getResources().getColor(R.color.black));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.statusSpinner.setAdapter(adapter);

        int statusPosition = Arrays.asList(STATUS_OPTIONS).indexOf(order.getStatus());
        if (statusPosition != -1) {
            holder.statusSpinner.setSelection(statusPosition);
        }

        holder.statusSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String newStatus = STATUS_OPTIONS[position];
                if (!newStatus.equals(order.getStatus())) {
                    listener.onStatusChanged(order, newStatus);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Agregar click listener para ver detalles
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.getId());
            context.startActivity(intent);
        });
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
        TextView customerText;
        Spinner statusSpinner;
        TextView totalText;
        TextView deliveryText;

        ViewHolder(View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            dateText = itemView.findViewById(R.id.date_text);
            customerText = itemView.findViewById(R.id.customer_text);
            statusSpinner = itemView.findViewById(R.id.status_spinner);
            totalText = itemView.findViewById(R.id.total_text);
            deliveryText = itemView.findViewById(R.id.delivery_text);
        }
    }
} 