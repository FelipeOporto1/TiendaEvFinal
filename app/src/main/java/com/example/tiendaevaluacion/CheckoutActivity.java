package com.example.tiendaevaluacion;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.CartItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RadioGroup deliveryTypeGroup;
    private EditText addressInput;
    private TextView totalText;
    private int userId;
    private double total = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getIntExtra("USER_ID", -1);

        deliveryTypeGroup = findViewById(R.id.delivery_type_group);
        addressInput = findViewById(R.id.address_input);
        totalText = findViewById(R.id.total_text);
        Button confirmButton = findViewById(R.id.confirm_button);

        deliveryTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.delivery_radio) {
                addressInput.setVisibility(View.VISIBLE);
            } else {
                addressInput.setVisibility(View.GONE);
            }
        });

        confirmButton.setOnClickListener(v -> processOrder());

        calculateTotal();
    }

    private void calculateTotal() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT c.*, p.name, p.price FROM " + DatabaseHelper.TABLE_CART + " c "
                + "INNER JOIN " + DatabaseHelper.TABLE_PRODUCTS + " p "
                + "ON c." + DatabaseHelper.COLUMN_CART_PRODUCT_ID + " = p." + DatabaseHelper.COLUMN_ID
                + " WHERE c." + DatabaseHelper.COLUMN_CART_USER_ID + " = ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {
            total = 0;
            while (cursor.moveToNext()) {
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_QUANTITY));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE));
                total += quantity * price;
            }
        }

        totalText.setText(String.format("Total: $%.2f", total));
    }

    private void processOrder() {
        if (deliveryTypeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Por favor seleccione un tipo de entrega", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isDelivery = deliveryTypeGroup.getCheckedRadioButtonId() == R.id.delivery_radio;
        if (isDelivery && addressInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Por favor ingrese una dirección de entrega", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        
        try {
            // Verificar stock de todos los items antes de procesar
            try (Cursor stockCheckCursor = db.query(DatabaseHelper.TABLE_CART,
                    null,
                    DatabaseHelper.COLUMN_CART_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null, null, null)) {

                while (stockCheckCursor.moveToNext()) {
                    int productId = stockCheckCursor.getInt(stockCheckCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_PRODUCT_ID));
                    int quantity = stockCheckCursor.getInt(stockCheckCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_QUANTITY));
                    String size = stockCheckCursor.getString(stockCheckCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SIZE));

                    // Verificar si es una polera
                    Cursor productCursor = db.query(
                        DatabaseHelper.TABLE_PRODUCTS,
                        new String[]{
                            DatabaseHelper.COLUMN_TYPE,
                            DatabaseHelper.COLUMN_PRODUCT_NAME,
                            DatabaseHelper.COLUMN_STOCK
                        },
                        DatabaseHelper.COLUMN_ID + "=?",
                        new String[]{String.valueOf(productId)},
                        null, null, null
                    );

                    if (productCursor.moveToFirst()) {
                        String productType = productCursor.getString(0);
                        String productName = productCursor.getString(1);
                        
                        if ("SHIRT".equals(productType)) {
                            // Verificar stock de la talla específica
                            Cursor sizeStockCursor = db.query(
                                DatabaseHelper.TABLE_SHIRT_SIZES,
                                new String[]{DatabaseHelper.COLUMN_SHIRT_STOCK},
                                DatabaseHelper.COLUMN_PRODUCT_ID + "=? AND " + 
                                DatabaseHelper.COLUMN_SHIRT_SIZE + "=?",
                                new String[]{String.valueOf(productId), size},
                                null, null, null
                            );

                            if (sizeStockCursor.moveToFirst()) {
                                int availableStock = sizeStockCursor.getInt(0);
                                if (quantity > availableStock) {
                                    throw new RuntimeException("No hay stock suficiente para " + 
                                        productName + " talla " + size);
                                }
                            }
                            sizeStockCursor.close();
                        } else {
                            // Verificar stock general para otros productos
                            int availableStock = productCursor.getInt(2);
                            if (quantity > availableStock) {
                                throw new RuntimeException("No hay stock suficiente para " + productName);
                            }
                        }
                    }
                    productCursor.close();
                }
            }

            // Crear orden
            ContentValues orderValues = new ContentValues();
            orderValues.put(DatabaseHelper.COLUMN_USER_ID, userId);
            orderValues.put(DatabaseHelper.COLUMN_TOTAL_AMOUNT, total);
            orderValues.put(DatabaseHelper.COLUMN_STATUS, "PENDING");
            orderValues.put(DatabaseHelper.COLUMN_DELIVERY_TYPE, isDelivery ? "DELIVERY" : "PICKUP");
            orderValues.put(DatabaseHelper.COLUMN_ADDRESS, isDelivery ? addressInput.getText().toString() : "");
            orderValues.put(DatabaseHelper.COLUMN_ORDER_DATE, new Date().getTime());

            long orderId = db.insert(DatabaseHelper.TABLE_ORDERS, null, orderValues);

            // Mover items del carrito a la orden y actualizar stock
            try (Cursor cartCursor = db.query(DatabaseHelper.TABLE_CART,
                    null,
                    DatabaseHelper.COLUMN_CART_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null, null, null)) {

                while (cartCursor.moveToNext()) {
                    int productId = cartCursor.getInt(cartCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_PRODUCT_ID));
                    int quantity = cartCursor.getInt(cartCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_QUANTITY));
                    String size = cartCursor.getString(cartCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SIZE));

                    // Obtener el precio del producto
                    Cursor productCursor = db.query(
                        DatabaseHelper.TABLE_PRODUCTS,
                        new String[]{DatabaseHelper.COLUMN_PRICE},
                        DatabaseHelper.COLUMN_ID + "=?",
                        new String[]{String.valueOf(productId)},
                        null, null, null
                    );

                    if (productCursor.moveToFirst()) {
                        double price = productCursor.getDouble(0);
                        
                        // Insertar en order_items
                        ContentValues orderItemValues = new ContentValues();
                        orderItemValues.put(DatabaseHelper.COLUMN_ORDER_ID, orderId);
                        orderItemValues.put(DatabaseHelper.COLUMN_PRODUCT_ID, productId);
                        orderItemValues.put(DatabaseHelper.COLUMN_QUANTITY, quantity);
                        orderItemValues.put(DatabaseHelper.COLUMN_ITEM_PRICE, price);
                        if (size != null && !size.isEmpty()) {
                            orderItemValues.put(DatabaseHelper.COLUMN_SIZE, size);
                        }
                        db.insert(DatabaseHelper.TABLE_ORDER_ITEMS, null, orderItemValues);
                    }
                    productCursor.close();

                    // Actualizar stock
                    if (size != null && !size.isEmpty()) {
                        // Para poleras
                        db.execSQL("UPDATE " + DatabaseHelper.TABLE_SHIRT_SIZES 
                                + " SET " + DatabaseHelper.COLUMN_SHIRT_STOCK + " = " + DatabaseHelper.COLUMN_SHIRT_STOCK + " - ? "
                                + " WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + " = ? AND "
                                + DatabaseHelper.COLUMN_SHIRT_SIZE + " = ?",
                                new String[]{String.valueOf(quantity), String.valueOf(productId), size});
                    } else {
                        // Para otros productos
                        db.execSQL("UPDATE " + DatabaseHelper.TABLE_PRODUCTS 
                                + " SET " + DatabaseHelper.COLUMN_STOCK + " = " + DatabaseHelper.COLUMN_STOCK + " - ? "
                                + " WHERE " + DatabaseHelper.COLUMN_ID + " = ?",
                                new String[]{String.valueOf(quantity), String.valueOf(productId)});
                    }
                }
            }

            // Limpiar carrito
            db.delete(DatabaseHelper.TABLE_CART,
                    DatabaseHelper.COLUMN_CART_USER_ID + "=?",
                    new String[]{String.valueOf(userId)});

            db.setTransactionSuccessful();
            Toast.makeText(this, "Pedido realizado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }
} 