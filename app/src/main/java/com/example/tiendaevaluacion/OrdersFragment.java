package com.example.tiendaevaluacion;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.adapters.OrderAdapter;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.Order;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrdersFragment extends Fragment implements OrderAdapter.OrderClickListener {
    private DatabaseHelper dbHelper;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private TextView emptyView;
    private int userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        orderList = new ArrayList<>();
        userId = requireActivity().getIntent().getIntExtra("USER_ID", -1);

        emptyView = view.findViewById(R.id.empty_view);
        RecyclerView recyclerView = view.findViewById(R.id.orders_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(getContext(), orderList, this);
        recyclerView.setAdapter(adapter);

        loadOrders();
        return view;
    }

    private void loadOrders() {
        orderList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT o.*, u.username FROM " + DatabaseHelper.TABLE_ORDERS + " o "
                + "INNER JOIN " + DatabaseHelper.TABLE_USERS + " u "
                + "ON o." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID
                + " WHERE o." + DatabaseHelper.COLUMN_USER_ID + " = ? "
                + "ORDER BY " + DatabaseHelper.COLUMN_ORDER_DATE + " DESC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {
            while (cursor.moveToNext()) {
                Order order = new Order(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                    userId,
                    new ArrayList<>(),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS)),
                    new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ORDER_DATE)))
                );
                orderList.add(order);
            }
        }

        adapter.updateOrders(orderList);
        emptyView.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(getActivity(), OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        intent.putExtra("STATUS", order.getStatus());
        startActivity(intent);
    }

    @Override
    public void onOrderCancel(Order order) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Obtener los items del pedido
            String query = "SELECT " +
                          "oi." + DatabaseHelper.COLUMN_PRODUCT_ID + ", " +
                          "oi." + DatabaseHelper.COLUMN_QUANTITY + ", " +
                          "p." + DatabaseHelper.COLUMN_TYPE + " " +
                          "FROM " + DatabaseHelper.TABLE_ORDER_ITEMS + " oi " +
                          "INNER JOIN " + DatabaseHelper.TABLE_PRODUCTS + " p " +
                          "ON oi." + DatabaseHelper.COLUMN_PRODUCT_ID + " = p." + DatabaseHelper.COLUMN_ID + " " +
                          "WHERE oi." + DatabaseHelper.COLUMN_ORDER_ID + " = ?";

            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(order.getId())})) {
                while (cursor.moveToNext()) {
                    int productId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_ID));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUANTITY));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));

                    if ("SHIRT".equals(type)) {
                        // Para poleras, obtener la talla del pedido
                        String sizeQuery = "SELECT oi." + DatabaseHelper.COLUMN_SIZE +
                                          " FROM " + DatabaseHelper.TABLE_ORDER_ITEMS + " oi " +
                                          "WHERE oi." + DatabaseHelper.COLUMN_ORDER_ID + " = ? AND " +
                                          "oi." + DatabaseHelper.COLUMN_PRODUCT_ID + " = ?";

                        Log.d("StockUpdate", "Ejecutando consulta para obtener talla: " + sizeQuery);
                        Log.d("StockUpdate", "Parámetros: orderId=" + order.getId() + ", productId=" + productId);
                        
                        try (Cursor sizesCursor = db.rawQuery(sizeQuery, 
                                new String[]{String.valueOf(order.getId()), String.valueOf(productId)})) {
                            if (sizesCursor.moveToFirst()) {
                                String size = sizesCursor.getString(0);
                                Log.d("StockUpdate", "Talla encontrada: " + size);
                                
                                if (size != null && !size.isEmpty()) {
                                    // Verificar stock antes de la actualización
                                    String verifyQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SHIRT_SIZES +
                                                       " WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + " = ? AND " +
                                                       DatabaseHelper.COLUMN_SHIRT_SIZE + " = ?";
                                    
                                    try (Cursor verifySize = db.rawQuery(verifyQuery, 
                                            new String[]{String.valueOf(productId), size})) {
                                        if (verifySize.moveToFirst() && verifySize.getInt(0) > 0) {
                                            // La talla existe, proceder con la actualización
                                            String updateQuery = "UPDATE " + DatabaseHelper.TABLE_SHIRT_SIZES +
                                                    " SET " + DatabaseHelper.COLUMN_SHIRT_STOCK + " = " +
                                                    DatabaseHelper.COLUMN_SHIRT_STOCK + " + ? " +
                                                    "WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + " = ? AND " +
                                                    DatabaseHelper.COLUMN_SHIRT_SIZE + " = ?";
                                            
                                            Log.d("StockUpdate", "Ejecutando actualización: " + updateQuery);
                                            Log.d("StockUpdate", String.format("Parámetros: quantity=%d, productId=%d, size=%s",
                                                    quantity, productId, size));
                                            
                                            db.execSQL(updateQuery, new String[]{
                                                String.valueOf(quantity),
                                                String.valueOf(productId),
                                                size
                                            });
                                            
                                            // Verificar la actualización
                                            String checkQuery = "SELECT " + DatabaseHelper.COLUMN_SHIRT_STOCK +
                                                              " FROM " + DatabaseHelper.TABLE_SHIRT_SIZES +
                                                              " WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + " = ? AND " +
                                                              DatabaseHelper.COLUMN_SHIRT_SIZE + " = ?";
                                            
                                            try (Cursor checkStock = db.rawQuery(checkQuery, 
                                                    new String[]{String.valueOf(productId), size})) {
                                                if (checkStock.moveToFirst()) {
                                                    int newStock = checkStock.getInt(0);
                                                    Log.d("StockUpdate", String.format(
                                                        "Stock actualizado - Polera ID: %d, Talla: %s, Nuevo Stock: %d",
                                                        productId, size, newStock));
                                                }
                                            }
                                        } else {
                                            Log.e("StockUpdate", "No se encontró la talla " + size + 
                                                    " para la polera ID: " + productId);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Para discos, actualizar stock directamente
                        Log.d("StockUpdate", String.format("Actualizando stock de disco - ID: %d, Stock actual antes: %d, Cantidad a devolver: %d",
                                productId, getCurrentStock(db, productId), quantity));

                        // Actualizar stock
                        ContentValues values = new ContentValues();
                        values.put(DatabaseHelper.COLUMN_STOCK, 
                                  String.format("(%s + %d)", DatabaseHelper.COLUMN_STOCK, quantity));
                        
                        int updated = db.update(DatabaseHelper.TABLE_PRODUCTS,
                                values,
                                DatabaseHelper.COLUMN_ID + "=?",
                                new String[]{String.valueOf(productId)});

                        // Verificar actualización
                        if (updated > 0) {
                            Log.d("StockUpdate", String.format("Stock actualizado para disco ID: %d, Nuevo stock: %d",
                                    productId, getCurrentStock(db, productId)));
                        } else {
                            Log.e("StockUpdate", "Error al actualizar stock del disco ID: " + productId);
                        }
                    }
                }
            }

            // 2. Actualizar estado del pedido
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_STATUS, "CANCELLED");
            db.update(DatabaseHelper.TABLE_ORDERS,
                    values,
                    DatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(order.getId())});

            db.setTransactionSuccessful();
            Toast.makeText(getContext(), "Pedido cancelado exitosamente", Toast.LENGTH_SHORT).show();
            
            // 3. Notificar cambios
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).refreshCatalog();
            }
            loadOrders();
        } catch (Exception e) {
            Log.e("OrderCancel", "Error: " + e.getMessage());
            Toast.makeText(getContext(), "Error al cancelar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }

    // Método auxiliar para obtener stock actual
    private int getCurrentStock(SQLiteDatabase db, int productId) {
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            new String[]{DatabaseHelper.COLUMN_STOCK},
            DatabaseHelper.COLUMN_ID + "=?",
            new String[]{String.valueOf(productId)},
            null, null, null
        );
        
        int stock = 0;
        if (cursor.moveToFirst()) {
            stock = cursor.getInt(0);
        }
        cursor.close();
        return stock;
    }
} 