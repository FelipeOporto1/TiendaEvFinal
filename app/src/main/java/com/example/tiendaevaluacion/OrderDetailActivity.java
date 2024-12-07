package com.example.tiendaevaluacion;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.adapters.OrderItemAdapter;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.OrderItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int orderId;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        dbHelper = new DatabaseHelper(this);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        orderId = getIntent().getIntExtra("ORDER_ID", -1);

        if (orderId == -1) {
            Toast.makeText(this, "Error al cargar el pedido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadOrderDetails();

        // Agregar botón de cancelar/devolver
        Button cancelButton = findViewById(R.id.cancel_button);
        String status = getIntent().getStringExtra("STATUS");
        if (status != null) {
            if (status.equals("PENDING")) {
                cancelButton.setText(R.string.cancel_order_title);
                cancelButton.setVisibility(View.VISIBLE);
            } else if (status.equals("DELIVERED")) {
                cancelButton.setText(R.string.return_order_title);
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                cancelButton.setVisibility(View.GONE);
            }

            cancelButton.setOnClickListener(v -> {
                String action = status.equals("PENDING") ? "cancelar" : "devolver";
                new AlertDialog.Builder(this)
                    .setTitle("Confirmar acción")
                    .setMessage("¿Está seguro que desea " + action + " este pedido?")
                    .setPositiveButton("Sí", (dialog, which) -> processCancelation())
                    .setNegativeButton("No", null)
                    .show();
            });
        } else {
            cancelButton.setVisibility(View.GONE);
        }
    }

    private void loadOrderDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<OrderItem> items = new ArrayList<>();

        // Cargar detalles del pedido
        try (Cursor orderCursor = db.query(
                DatabaseHelper.TABLE_ORDERS,
                null,
                DatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(orderId)},
                null, null, null)) {

            if (orderCursor != null && orderCursor.moveToFirst()) {
                // Mostrar ID del pedido
                TextView orderIdText = findViewById(R.id.order_id_text);
                orderIdText.setText("Pedido #" + orderId);

                // Mostrar fecha
                long dateLong = orderCursor.getLong(orderCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ORDER_DATE));
                TextView dateText = findViewById(R.id.date_text);
                dateText.setText("Fecha: " + dateFormat.format(new Date(dateLong)));

                // Mostrar estado
                String status = orderCursor.getString(orderCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS));
                TextView statusText = findViewById(R.id.status_text);
                statusText.setText("Estado: " + getStatusText(status));

                // Mostrar tipo de entrega y dirección
                String deliveryType = orderCursor.getString(orderCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_TYPE));
                String address = orderCursor.getString(orderCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS));
                TextView deliveryText = findViewById(R.id.delivery_text);
                if (deliveryType.equals("DELIVERY")) {
                    deliveryText.setText("Entrega a domicilio\nDirección: " + address);
                } else {
                    deliveryText.setText("Retiro en tienda");
                }

                // Mostrar total
                double total = orderCursor.getDouble(orderCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_AMOUNT));
                TextView totalText = findViewById(R.id.total_text);
                totalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", total));
            } else {
                Toast.makeText(this, "No se encontró el pedido", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        // Cargar items del pedido
        String query = "SELECT oi.*, p.name FROM " + DatabaseHelper.TABLE_ORDER_ITEMS + " oi "
                + "INNER JOIN " + DatabaseHelper.TABLE_PRODUCTS + " p "
                + "ON oi." + DatabaseHelper.COLUMN_PRODUCT_ID + " = p." + DatabaseHelper.COLUMN_ID
                + " WHERE oi." + DatabaseHelper.COLUMN_ORDER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(orderId)})) {
            while (cursor.moveToNext()) {
                OrderItem item = new OrderItem(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUANTITY)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_PRICE))
                );
                item.setProductName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)));
                items.add(item);
            }
        }

        // Configurar RecyclerView
        RecyclerView recyclerView = findViewById(R.id.items_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        OrderItemAdapter adapter = new OrderItemAdapter(items);
        recyclerView.setAdapter(adapter);
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

    private void processCancelation() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Obtener los items del pedido
            String query = "SELECT oi.*, p.name FROM " + DatabaseHelper.TABLE_ORDER_ITEMS + " oi "
                    + "INNER JOIN " + DatabaseHelper.TABLE_PRODUCTS + " p "
                    + "ON oi." + DatabaseHelper.COLUMN_PRODUCT_ID + " = p." + DatabaseHelper.COLUMN_ID
                    + " WHERE oi." + DatabaseHelper.COLUMN_ORDER_ID + " = ?";

            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(orderId)})) {
                while (cursor.moveToNext()) {
                    int productId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_ID));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUANTITY));

                    // 2. Devolver el stock
                    db.execSQL("UPDATE " + DatabaseHelper.TABLE_PRODUCTS 
                            + " SET " + DatabaseHelper.COLUMN_STOCK + " = " + DatabaseHelper.COLUMN_STOCK + " + ? "
                            + " WHERE " + DatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{String.valueOf(quantity), String.valueOf(productId)});
                }
            }

            // 3. Actualizar estado del pedido
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_STATUS, "CANCELLED");
            db.update(DatabaseHelper.TABLE_ORDERS,
                    values,
                    DatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(orderId)});

            db.setTransactionSuccessful();
            Toast.makeText(this, "Pedido cancelado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar la cancelación", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }
} 