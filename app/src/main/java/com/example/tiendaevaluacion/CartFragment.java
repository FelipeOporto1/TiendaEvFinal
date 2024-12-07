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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.adapters.CartAdapter;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.CartItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment implements CartAdapter.CartItemListener {
    private DatabaseHelper dbHelper;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private TextView totalText;
    private TextView emptyView;
    private Button checkoutButton;
    private int userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        cartItems = new ArrayList<>();
        userId = requireActivity().getIntent().getIntExtra("USER_ID", -1);

        emptyView = view.findViewById(R.id.empty_view);

        RecyclerView recyclerView = view.findViewById(R.id.cart_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CartAdapter(getContext(), cartItems, this);
        recyclerView.setAdapter(adapter);

        totalText = view.findViewById(R.id.total_text);
        checkoutButton = view.findViewById(R.id.checkout_button);
        
        checkoutButton.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(getContext(), "El carrito está vacío", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), CheckoutActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        loadCartItems();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCartItems(); // Recargar items después de checkout
    }

    private void loadCartItems() {
        cartItems.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        try {
            String query = "SELECT c.*, p.name, p.price FROM " + DatabaseHelper.TABLE_CART + " c "
                    + "INNER JOIN " + DatabaseHelper.TABLE_PRODUCTS + " p "
                    + "ON c." + DatabaseHelper.COLUMN_CART_PRODUCT_ID + " = p." + DatabaseHelper.COLUMN_ID
                    + " WHERE c." + DatabaseHelper.COLUMN_CART_USER_ID + " = ?";
            
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

            while (cursor != null && cursor.moveToNext()) {
                CartItem item = new CartItem(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_ID)),
                    userId,
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_PRODUCT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_QUANTITY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SIZE))
                );
                cartItems.add(item);
            }
            
            if (cursor != null) {
                cursor.close();
            }

            adapter.updateItems(cartItems);
            updateTotal();
            
            // Mostrar/ocultar mensaje de carrito vacío
            if (cartItems.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                checkoutButton.setEnabled(false);
            } else {
                emptyView.setVisibility(View.GONE);
                checkoutButton.setEnabled(true);
            }
            
        } catch (Exception e) {
            Log.e("CartFragment", "Error loading cart items: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar el carrito", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotal();
        }
        totalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", total));
    }

    @Override
    public void onQuantityChanged(CartItem item) {
        updateQuantity(item, item.getQuantity());
        loadCartItems();  // Recargar para verificar stock
    }

    @Override
    public void onItemDeleted(CartItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_CART,
                DatabaseHelper.COLUMN_CART_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        loadCartItems();
    }

    private void updateQuantity(CartItem item, int newQuantity) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        if (newQuantity <= 0) {
            // Eliminar del carrito
            db.delete(DatabaseHelper.TABLE_CART,
                    DatabaseHelper.COLUMN_CART_ID + "=?",
                    new String[]{String.valueOf(item.getId())});
            loadCartItems();
            return;
        }

        // Verificar stock disponible
        checkStock(item, newQuantity);
    }

    private void checkStock(CartItem item, int newQuantity) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String stockQuery;
        String[] queryParams;

        // Verificar si es una polera
        Cursor productCursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            new String[]{DatabaseHelper.COLUMN_TYPE},
            DatabaseHelper.COLUMN_ID + "=?",
            new String[]{String.valueOf(item.getProductId())},
            null, null, null
        );

        if (productCursor.moveToFirst()) {
            String productType = productCursor.getString(0);
            if ("SHIRT".equals(productType)) {
                // Para poleras, verificar stock específico de la talla
                stockQuery = "SELECT " + DatabaseHelper.COLUMN_SHIRT_STOCK +
                            " FROM " + DatabaseHelper.TABLE_SHIRT_SIZES +
                            " WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + "=? AND " +
                            DatabaseHelper.COLUMN_SHIRT_SIZE + "=?";
                queryParams = new String[]{
                    String.valueOf(item.getProductId()),
                    item.getSize()
                };
            } else {
                // Para otros productos, verificar stock general
                stockQuery = "SELECT " + DatabaseHelper.COLUMN_STOCK +
                            " FROM " + DatabaseHelper.TABLE_PRODUCTS +
                            " WHERE " + DatabaseHelper.COLUMN_ID + "=?";
                queryParams = new String[]{String.valueOf(item.getProductId())};
            }

            try (Cursor cursor = db.rawQuery(stockQuery, queryParams)) {
                if (cursor.moveToFirst()) {
                    int availableStock = cursor.getInt(0);
                    if (newQuantity > availableStock) {
                        Toast.makeText(getContext(), 
                            "No hay suficiente stock disponible", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }
        productCursor.close();

        // Si llegamos aquí, hay stock suficiente
        updateCartItemQuantity(item, newQuantity);
    }

    private void updateCartItemQuantity(CartItem item, int newQuantity) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Actualizar cantidad
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_CART_QUANTITY, newQuantity);
        
        int result = db.update(DatabaseHelper.TABLE_CART,
                values,
                DatabaseHelper.COLUMN_CART_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        
        if (result > 0) {
            loadCartItems();
        }
    }
} 