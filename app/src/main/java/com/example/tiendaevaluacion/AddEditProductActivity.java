package com.example.tiendaevaluacion;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.example.tiendaevaluacion.database.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddEditProductActivity extends AppCompatActivity {
    private EditText nameInput, descriptionInput, priceInput, stockInput, sizeInput;
    private Spinner typeSpinner;
    private ImageView productImage;
    private Uri selectedImageUri;
    private DatabaseHelper dbHelper;
    private int productId = -1;
    private Map<String, Integer> selectedSizes;
    private Button manageSizesButton;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            }
        });

    private final ActivityResultLauncher<String> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleImageResult(uri);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        dbHelper = new DatabaseHelper(this);
        selectedSizes = new HashMap<>();
        
        nameInput = findViewById(R.id.product_name_input);
        descriptionInput = findViewById(R.id.product_description_input);
        priceInput = findViewById(R.id.product_price_input);
        stockInput = findViewById(R.id.product_stock_input);
        sizeInput = findViewById(R.id.product_size_input);
        typeSpinner = findViewById(R.id.product_type_spinner);
        productImage = findViewById(R.id.product_image);
        Button selectImageButton = findViewById(R.id.select_image_button);
        Button saveButton = findViewById(R.id.save_button);
        manageSizesButton = findViewById(R.id.manage_sizes_button);

        // Configurar spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
            this,
            android.R.layout.simple_spinner_item,
            getResources().getStringArray(R.array.product_types)
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(getResources().getColor(R.color.bright_red));
                text.setTypeface(ResourcesCompat.getFont(getContext(), R.font.metal_mania));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(getResources().getColor(R.color.bright_red));
                text.setTypeface(ResourcesCompat.getFont(getContext(), R.font.metal_mania));
                view.setBackgroundColor(getResources().getColor(R.color.black));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Mostrar/ocultar campo de talla según el tipo seleccionado
        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals("SHIRT")) {
                    sizeInput.setVisibility(View.GONE);
                    stockInput.setVisibility(View.GONE);  // Ocultar stock general
                    manageSizesButton.setVisibility(View.VISIBLE);
                } else {
                    sizeInput.setVisibility(View.GONE);
                    stockInput.setVisibility(View.VISIBLE);  // Mostrar stock general
                    manageSizesButton.setVisibility(View.GONE);
                    selectedSizes.clear();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                sizeInput.setVisibility(View.GONE);
                stockInput.setVisibility(View.VISIBLE);  // Mostrar stock general
                manageSizesButton.setVisibility(View.GONE);
                selectedSizes.clear();
            }
        });

        selectImageButton.setOnClickListener(v -> checkPermissionAndPickImage());
        saveButton.setOnClickListener(v -> saveProduct());

        // Verificar si estamos editando un producto existente
        productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        if (productId != -1) {
            setTitle(R.string.edit_product);
            loadProduct();
        } else {
            setTitle(R.string.new_product);
        }

        // Agregar botón para gestionar tallas
        manageSizesButton.setOnClickListener(v -> showSizesDialog());
        manageSizesButton.setVisibility(View.GONE); // Oculto por defecto
    }

    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void saveProduct() {
        try {
            String name = nameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            double price = Double.parseDouble(priceInput.getText().toString());
            String type = typeSpinner.getSelectedItem().toString();
            
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar stock según el tipo
            int stock = 0;
            if (!type.equals("SHIRT")) {
                stock = Integer.parseInt(stockInput.getText().toString());
            } else if (selectedSizes.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese el stock por talla", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_PRODUCT_NAME, name);
            values.put(DatabaseHelper.COLUMN_DESCRIPTION, description);
            values.put(DatabaseHelper.COLUMN_PRICE, price);
            values.put(DatabaseHelper.COLUMN_STOCK, stock);  // Será 0 para poleras
            values.put(DatabaseHelper.COLUMN_TYPE, type);
            
            if (selectedImageUri != null) {
                values.put(DatabaseHelper.COLUMN_IMAGE_URL, selectedImageUri.toString());
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long result;
            
            if (productId == -1) {
                result = db.insert(DatabaseHelper.TABLE_PRODUCTS, null, values);
                if (result != -1) {
                    productId = (int) result;  // Guardar el ID del producto nuevo
                    Toast.makeText(this, R.string.product_saved, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                result = db.update(DatabaseHelper.TABLE_PRODUCTS, values,
                        DatabaseHelper.COLUMN_ID + "=?",
                        new String[]{String.valueOf(productId)});
                if (result > 0) {
                    Toast.makeText(this, R.string.product_updated, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.update_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Guardar stock por talla para poleras
            if (type.equals("SHIRT") && !selectedSizes.isEmpty()) {
                // Primero eliminar stocks anteriores si es una actualización
                db.delete(DatabaseHelper.TABLE_SHIRT_SIZES, 
                         DatabaseHelper.COLUMN_PRODUCT_ID + "=?", 
                         new String[]{String.valueOf(productId)});
                
                // Luego insertar los nuevos stocks
                ContentValues sizeValues = new ContentValues();
                for (Map.Entry<String, Integer> entry : selectedSizes.entrySet()) {
                    sizeValues.clear();
                    sizeValues.put(DatabaseHelper.COLUMN_PRODUCT_ID, productId);
                    sizeValues.put(DatabaseHelper.COLUMN_SHIRT_SIZE, entry.getKey());
                    sizeValues.put(DatabaseHelper.COLUMN_SHIRT_STOCK, entry.getValue());
                    long insertResult = db.insert(DatabaseHelper.TABLE_SHIRT_SIZES, null, sizeValues);
                    Log.d("AddEditProduct", "Inserted size " + entry.getKey() + 
                        " with stock " + entry.getValue() + ", result: " + insertResult);
                }
            }

            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProduct() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        try (Cursor cursor = db.query(DatabaseHelper.TABLE_PRODUCTS,
                null,
                DatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(productId)},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                nameInput.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)));
                descriptionInput.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)));
                priceInput.setText(String.valueOf(cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE))));
                
                String type = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
                
                // Configurar campos según el tipo
                if (type.equals("SHIRT")) {
                    stockInput.setVisibility(View.GONE);
                    manageSizesButton.setVisibility(View.VISIBLE);
                    
                    // Cargar stock por tallas
                    String sizeQuery = "SELECT size, stock FROM shirt_sizes WHERE product_id = ?";
                    try (Cursor sizeCursor = db.rawQuery(sizeQuery, new String[]{String.valueOf(productId)})) {
                        while (sizeCursor.moveToNext()) {
                            String size = sizeCursor.getString(0);
                            int stock = sizeCursor.getInt(1);
                            selectedSizes.put(size, stock);
                        }
                    }
                } else {
                    stockInput.setVisibility(View.VISIBLE);
                    manageSizesButton.setVisibility(View.GONE);
                    stockInput.setText(String.valueOf(cursor.getInt(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK))));
                }
                
                // Seleccionar tipo en spinner
                ArrayAdapter adapter = (ArrayAdapter) typeSpinner.getAdapter();
                int position = adapter.getPosition(type);
                if (position >= 0) {
                    typeSpinner.setSelection(position);
                }

                // Cargar imagen
                String imageUrl = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_URL));
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    selectedImageUri = Uri.parse(imageUrl);
                    try {
                        Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.default_product_image)
                            .error(R.drawable.default_product_image)
                            .into(productImage);
                    } catch (Exception e) {
                        productImage.setImageResource(R.drawable.default_product_image);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar el producto", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleImageResult(Uri uri) {
        try {
            // Crear directorio para imágenes si no existe
            File imagesDir = new File(getFilesDir(), "product_images");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            // Crear archivo para la nueva imagen
            String fileName = "product_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(imagesDir, fileName);

            // Copiar la imagen seleccionada
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(destFile)) {
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            // Guardar la ruta de la imagen
            selectedImageUri = Uri.fromFile(destFile);
            
            // Mostrar la imagen
            Glide.with(this)
                .load(destFile)
                .into(productImage);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSizesDialog() {
        String[] availableSizes = {"S", "M", "L", "XL"};
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manage_sizes, null);
        LinearLayout sizesContainer = dialogView.findViewById(R.id.sizes_container);

        for (String size : availableSizes) {
            View sizeView = getLayoutInflater().inflate(R.layout.item_size_stock, null);
            TextView sizeText = sizeView.findViewById(R.id.size_text);
            EditText stockInput = sizeView.findViewById(R.id.stock_input);
            
            sizeText.setText(size);
            stockInput.setText(String.valueOf(selectedSizes.getOrDefault(size, 0)));
            
            sizesContainer.addView(sizeView);
        }

        new AlertDialog.Builder(this)
            .setTitle("Gestionar Stock por Talla")
            .setView(dialogView)
            .setPositiveButton("Guardar", (dialog, which) -> {
                selectedSizes.clear();
                for (int i = 0; i < sizesContainer.getChildCount(); i++) {
                    View sizeView = sizesContainer.getChildAt(i);
                    TextView sizeText = sizeView.findViewById(R.id.size_text);
                    EditText stockInput = sizeView.findViewById(R.id.stock_input);
                    
                    try {
                        int stock = Integer.parseInt(stockInput.getText().toString());
                        if (stock > 0) {
                            selectedSizes.put(sizeText.getText().toString(), stock);
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar valores inválidos
                    }
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
}