package com.example.tiendaevaluacion.adapters;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.tiendaevaluacion.R;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.Product;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.squareup.picasso.Picasso;
import com.google.firebase.analytics.FirebaseAnalytics;

public class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.ViewHolder> {
    private List<Product> products;
    private Context context;
    private int userId;
    private DatabaseHelper dbHelper;

    public CatalogAdapter(Context context, List<Product> products, int userId) {
        this.context = context;
        this.products = products;
        this.userId = userId;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Registrar vista del producto
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(product.getId()));
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, product.getName());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "product_view");
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        holder.productName.setText(product.getName());
        holder.productDescription.setText(product.getDescription());
        holder.productPrice.setText(String.format(Locale.getDefault(), "$%.2f", product.getPrice()));
        
        // Mostrar stock y configurar botón
        if ("SHIRT".equals(product.getType())) {
            int totalStock = getTotalShirtStock(db, product.getId());
            holder.productStock.setText(String.format("Stock Total: %d", totalStock));
            holder.addToCartButton.setEnabled(totalStock > 0);
            
            // Cambiar color según stock
            if (totalStock <= 0) {
                holder.productStock.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                holder.addToCartButton.setAlpha(0.5f);
            } else {
                holder.productStock.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                holder.addToCartButton.setAlpha(1.0f);
            }
        } else {
            int stock = product.getStock();
            Log.d("CatalogAdapter", String.format("Disco: %s (ID: %d) - Stock: %d", 
                    product.getName(), product.getId(), stock));
            holder.productStock.setText(String.format("Stock: %d", stock));
        }

        // Cargar imagen
        try {
            String imageUrl = product.getImageUrl();
            Log.d("CatalogAdapter", "Loading image URL: " + imageUrl);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Uri imageUri = Uri.parse(imageUrl);
                
                // Registrar carga de imagen
                Bundle imageBundle = new Bundle();
                imageBundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(product.getId()));
                imageBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, product.getName());
                imageBundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
                FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, imageBundle);
                
                // Usar Glide con manejo específico según el tipo de URI
                if (ContentResolver.SCHEME_CONTENT.equals(imageUri.getScheme())) {
                    // Para URIs de contenido
                    Glide.with(context)
                        .load(imageUri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_product_image)
                        .error(R.drawable.default_product_image)
                        .into(holder.productImage);
                } else if (ContentResolver.SCHEME_FILE.equals(imageUri.getScheme())) {
                    // Para URIs de archivo
                    File file = new File(imageUri.getPath());
                    if (file.exists()) {
                        Glide.with(context)
                            .load(file)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.default_product_image)
                            .error(R.drawable.default_product_image)
                            .into(holder.productImage);
                    } else {
                        holder.productImage.setImageResource(R.drawable.default_product_image);
                    }
                } else {
                    // Para otros tipos de URI
                    Glide.with(context)
                        .load(imageUri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_product_image)
                        .error(R.drawable.default_product_image)
                        .into(holder.productImage);
                }
            } else {
                holder.productImage.setImageResource(R.drawable.default_product_image);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("CatalogAdapter", "Error loading image: " + e.getMessage());
            holder.productImage.setImageResource(R.drawable.default_product_image);
        }

        holder.addToCartButton.setOnClickListener(v -> {
            if ("SHIRT".equals(product.getType())) {
                // Mostrar diálogo de selección de talla para poleras
                showSizeSelectionDialog(product);
            } else {
                // Para otros productos, agregar directamente al carrito
                addToCart(product, null);
            }
        });

        db.close();
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        products = newProducts;
        notifyDataSetChanged();
    }

    private void showSizeSelectionDialog(Product product) {
        // Obtener tallas disponibles de la base de datos
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> availableSizes = new ArrayList<>();
        
        String query = "SELECT size, stock FROM shirt_sizes WHERE product_id = ? AND stock > 0";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(product.getId())})) {
            while (cursor.moveToNext()) {
                String size = cursor.getString(0);
                int stock = cursor.getInt(1);
                availableSizes.add(size + " (Stock: " + stock + ")");
            }
        }

        if (availableSizes.isEmpty()) {
            Toast.makeText(context, "No hay tallas disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] sizes = availableSizes.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Seleccionar Talla")
               .setItems(sizes, (dialog, which) -> {
                   String selectedSize = sizes[which].split(" ")[0]; // Obtener solo la talla sin el stock
                   addToCart(product, selectedSize);
               })
               .setNegativeButton("Cancelar", null)
               .show();
    }

    private void addToCart(Product product, String selectedSize) {
        Log.d("CatalogAdapter", "Adding to cart - Product: " + product.getName() + ", Size: " + selectedSize);
        
        // Verificar stock específico de la talla primero
        if ("SHIRT".equals(product.getType())) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String stockQuery = "SELECT stock FROM " + DatabaseHelper.TABLE_SHIRT_SIZES + 
                              " WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + " = ? AND " + 
                              DatabaseHelper.COLUMN_SHIRT_SIZE + " = ?";
            try (Cursor stockCursor = db.rawQuery(stockQuery, 
                    new String[]{String.valueOf(product.getId()), selectedSize})) {
                if (stockCursor.moveToFirst()) {
                    int availableStock = stockCursor.getInt(0);
                    Log.d("CatalogAdapter", "Available stock for size " + selectedSize + ": " + availableStock);

                    // Verificar cantidad actual en el carrito
                    String cartQuery = "SELECT " + DatabaseHelper.COLUMN_CART_QUANTITY + 
                                     " FROM " + DatabaseHelper.TABLE_CART +
                                     " WHERE " + DatabaseHelper.COLUMN_CART_USER_ID + "=? AND " +
                                     DatabaseHelper.COLUMN_CART_PRODUCT_ID + "=? AND " +
                                     DatabaseHelper.COLUMN_SIZE + "=?";
                    try (Cursor cartCursor = db.rawQuery(cartQuery, 
                            new String[]{String.valueOf(userId), String.valueOf(product.getId()), selectedSize})) {
                        int currentQty = 0;
                        if (cartCursor.moveToFirst()) {
                            currentQty = cartCursor.getInt(0);
                        }

                        if (currentQty >= availableStock) {
                            Toast.makeText(context, "No hay más stock disponible para esta talla", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                } else {
                    Toast.makeText(context, "Talla no disponible", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;

        try {
            // Verificar si ya existe el producto con esa talla en el carrito
            String query = "SELECT " + DatabaseHelper.COLUMN_CART_ID + ", " + 
                          DatabaseHelper.COLUMN_CART_QUANTITY +
                          " FROM " + DatabaseHelper.TABLE_CART +
                          " WHERE " + DatabaseHelper.COLUMN_CART_USER_ID + "=? AND " +
                          DatabaseHelper.COLUMN_CART_PRODUCT_ID + "=?";
            
            if ("SHIRT".equals(product.getType())) {
                query += " AND " + DatabaseHelper.COLUMN_SIZE + "=?";
            }
            
            String[] selectionArgs;
            if ("SHIRT".equals(product.getType())) {
                selectionArgs = new String[]{
                    String.valueOf(userId),
                    String.valueOf(product.getId()),
                    selectedSize
                };
            } else {
                selectionArgs = new String[]{
                    String.valueOf(userId),
                    String.valueOf(product.getId())
                };
            }

            Log.d("CatalogAdapter", "Query: " + query);
            Log.d("CatalogAdapter", "Args: " + String.join(", ", selectionArgs));

            // Primero intentar agregar la columna si no existe
            try {
                db.execSQL("ALTER TABLE " + DatabaseHelper.TABLE_CART + 
                          " ADD COLUMN " + DatabaseHelper.COLUMN_SIZE + " TEXT");
            } catch (Exception e) {
                // La columna ya existe, ignorar el error
                Log.d("CatalogAdapter", "Column size might already exist: " + e.getMessage());
            }

            cursor = db.rawQuery(query, selectionArgs);
            
            if (cursor != null && cursor.moveToFirst()) {
                // Actualizar cantidad existente
                int currentQty = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_QUANTITY));
                int cartId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_ID));

                // Ya no comparamos con product.getStock() aquí
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_CART_QUANTITY, currentQty + 1);
                
                int updated = db.update(DatabaseHelper.TABLE_CART,
                         values,
                         DatabaseHelper.COLUMN_CART_ID + "=?",
                         new String[]{String.valueOf(cartId)});
                
                Log.d("CatalogAdapter", "Updated existing cart item: " + updated + " rows affected");
                
                if (updated <= 0) {
                    throw new Exception("Failed to update cart item");
                }
            } else {
                // Agregar nuevo item
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_CART_USER_ID, userId);
                values.put(DatabaseHelper.COLUMN_CART_PRODUCT_ID, product.getId());
                values.put(DatabaseHelper.COLUMN_CART_QUANTITY, 1);
                if ("SHIRT".equals(product.getType())) {
                    values.put(DatabaseHelper.COLUMN_SIZE, selectedSize);
                }
                
                Log.d("CatalogAdapter", "Inserting new cart item with values: " + values.toString());
                long newId = db.insert(DatabaseHelper.TABLE_CART, null, values);
                Log.d("CatalogAdapter", "Inserted new cart item with ID: " + newId);
                
                if (newId == -1) {
                    throw new Exception("Failed to insert new cart item");
                }
            }

            Toast.makeText(context, "Producto agregado al carrito", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CatalogAdapter", "Error adding to cart: " + e.getMessage());
            Log.e("CatalogAdapter", "Stack trace: ", e);
            Toast.makeText(context, "Error al agregar al carrito: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private int getTotalShirtStock(SQLiteDatabase db, int productId) {
        int totalStock = 0;
        
        // Consulta más específica para obtener el stock por talla
        String query = "SELECT " + 
                      "ss." + DatabaseHelper.COLUMN_SHIRT_SIZE + ", " +
                      "ss." + DatabaseHelper.COLUMN_SHIRT_STOCK + " " +
                      "FROM " + DatabaseHelper.TABLE_SHIRT_SIZES + " ss " +
                      "WHERE ss." + DatabaseHelper.COLUMN_PRODUCT_ID + " = ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(productId)})) {
            while (cursor.moveToNext()) {
                String size = cursor.getString(0);
                int stockBySize = cursor.getInt(1);
                totalStock += stockBySize;
                
                // Log detallado del stock por talla
                Log.d("StockCheck", String.format("Polera ID: %d, Talla: %s, Stock: %d", 
                        productId, size, stockBySize));
            }
        } catch (Exception e) {
            Log.e("StockCheck", "Error getting shirt stock: " + e.getMessage());
            e.printStackTrace();
        }

        // Log del stock total
        Log.d("StockCheck", String.format("Polera ID: %d, Stock Total Final: %d", 
                productId, totalStock));
        return totalStock;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView productDescription;
        TextView productPrice;
        TextView productStock;
        ImageView productImage;
        Button addToCartButton;

        ViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productDescription = itemView.findViewById(R.id.product_description);
            productPrice = itemView.findViewById(R.id.product_price);
            productStock = itemView.findViewById(R.id.product_stock);
            productImage = itemView.findViewById(R.id.product_image);
            addToCartButton = itemView.findViewById(R.id.add_to_cart_button);
        }
    }
} 