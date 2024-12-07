package com.example.tiendaevaluacion;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.adapters.CatalogAdapter;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.Product;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import android.util.Log;

public class CatalogFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private CatalogAdapter adapter;
    private List<Product> productList;
    private List<Product> filteredList;
    private Spinner filterSpinner;
    private SearchView searchView;
    private static final String ALL_ITEMS = "Todos los productos";
    private String currentType = null;
    private String currentQuery = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_catalog, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        productList = new ArrayList<>();
        filteredList = new ArrayList<>();
        int userId = requireActivity().getIntent().getIntExtra("USER_ID", -1);

        // Configurar RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.catalog_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new CatalogAdapter(getContext(), filteredList, userId);
        recyclerView.setAdapter(adapter);

        // Configurar SearchView
        searchView = view.findViewById(R.id.search_view);
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
        filterSpinner = view.findViewById(R.id.filter_spinner);
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(ALL_ITEMS);
        filterOptions.add("Discos");
        filterOptions.add("Poleras");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filterOptions
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
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals(ALL_ITEMS)) {
                    currentType = null;
                } else if (selected.equals("Discos")) {
                    currentType = "DISC";
                } else if (selected.equals("Poleras")) {
                    currentType = "SHIRT";
                }
                filterProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadProducts();
        return view;
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
            int stock = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));

            Product product = new Product(id, name, description, price, imageUrl, stock, type);
            if (cursor.getColumnIndex(DatabaseHelper.COLUMN_SIZE) != -1) {
                product.setSize(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SIZE)));
            }
            productList.add(product);
        }
        cursor.close();

        filterProducts();
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
                Log.d("ProductFilter", "Added to filtered list - ID: " + product.getId() + 
                        ", Name: " + product.getName() + 
                        ", Stock: " + product.getStock());
            }
        }
        
        adapter.updateProducts(filteredList);
        adapter.notifyDataSetChanged();
    }

    public void refreshProducts() {
        // Limpiar las listas
        productList.clear();
        filteredList.clear();

        // Obtener los productos actualizados
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        try (Cursor cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            null,
            null,
            null,
            null,
            null,
            null)) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE));
                String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_URL));
                int stock = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));

                Product product = new Product(id, name, description, price, imageUrl, stock, type);
                productList.add(product);
                
                Log.d("CatalogRefresh", "Product ID: " + id + ", Name: " + name + ", Stock: " + stock);
            }
        }

        // Aplicar filtros actuales
        filterProducts();
        
        // Notificar al adaptador
        adapter.notifyDataSetChanged();
        
        Log.d("CatalogRefresh", "Products refreshed, total count: " + productList.size());
    }
} 