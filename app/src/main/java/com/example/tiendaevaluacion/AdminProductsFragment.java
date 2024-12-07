package com.example.tiendaevaluacion;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.tiendaevaluacion.adapters.ProductAdapter;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.Product;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;

public class AdminProductsFragment extends Fragment implements ProductAdapter.OnProductDeleteListener {
    private DatabaseHelper dbHelper;
    private ProductAdapter adapter;
    private List<Product> productList;
    private List<Product> filteredList;
    private String currentQuery = "";
    private String currentType = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_products, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        productList = new ArrayList<>();
        filteredList = new ArrayList<>();

        RecyclerView recyclerView = view.findViewById(R.id.products_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProductAdapter(getContext(), filteredList, this);
        recyclerView.setAdapter(adapter);

        // Configurar SearchView
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.toLowerCase();
                filterProducts();
                return true;
            }
        });

        // Configurar Spinner
        Spinner filterSpinner = view.findViewById(R.id.filter_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.product_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        // Agregar "Todos" como primera opción
        List<String> types = new ArrayList<>();
        types.add("Todos");
        for (int i = 0; i < spinnerAdapter.getCount(); i++) {
            types.add(spinnerAdapter.getItem(i).toString());
        }
        ArrayAdapter<String> newAdapter = new ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            types
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(getContext().getResources().getColor(R.color.bright_red));
                text.setTypeface(ResourcesCompat.getFont(getContext(), R.font.metal_mania));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(getContext().getResources().getColor(R.color.bright_red));
                text.setTypeface(ResourcesCompat.getFont(getContext(), R.font.metal_mania));
                text.setPadding(32, 32, 32, 32);
                text.setTextSize(18);
                view.setBackgroundColor(getContext().getResources().getColor(R.color.black));
                return view;
            }
        };
        filterSpinner.setAdapter(newAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentType = position == 0 ? null : parent.getItemAtPosition(position).toString();
                filterProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentType = null;
                filterProducts();
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditProductActivity.class);
            startActivity(intent);
        });

        loadProducts();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        productList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            null,
            null,
            null,
            null,
            null,
            null
        );

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE));
            String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_URL));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
            int stock;

            if (type.equals("SHIRT")) {
                // Para poleras, obtener la suma del stock de todas las tallas
                String stockQuery = "SELECT SUM(stock) FROM " + DatabaseHelper.TABLE_SHIRT_SIZES + 
                                  " WHERE " + DatabaseHelper.COLUMN_PRODUCT_ID + " = ?";
                try (Cursor stockCursor = db.rawQuery(stockQuery, new String[]{String.valueOf(id)})) {
                    if (stockCursor.moveToFirst() && !stockCursor.isNull(0)) {
                        stock = stockCursor.getInt(0);
                    } else {
                        stock = 0;
                    }
                }
            } else {
                stock = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK));
            }

            Product product = new Product(id, name, description, price, imageUrl, stock, type);
            productList.add(product);
        }
        cursor.close();

        adapter.updateProducts(productList);
    }

    private void filterProducts() {
        filteredList.clear();
        
        for (Product product : productList) {
            boolean matchesType = currentType == null || product.getType().equals(currentType);
            boolean matchesQuery = currentQuery.isEmpty() || 
                                 product.getName().toLowerCase().contains(currentQuery) ||
                                 product.getDescription().toLowerCase().contains(currentQuery);
            
            if (matchesType && matchesQuery) {
                filteredList.add(product);
            }
        }
        
        adapter.updateProducts(filteredList);
    }

    @Override
    public void onDeleteProduct(Product product) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_PRODUCTS,
                DatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(product.getId())});
        loadProducts();
    }
} 