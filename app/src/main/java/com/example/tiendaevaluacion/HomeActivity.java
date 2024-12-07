package com.example.tiendaevaluacion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.Menu;
import android.view.MenuItem;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private boolean isAdmin;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "onCreate called");

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        userId = getIntent().getIntExtra("USER_ID", -1);

        Log.d(TAG, "isAdmin: " + isAdmin);
        Log.d(TAG, "userId: " + userId);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Configurar menú según el tipo de usuario
        if (isAdmin) {
            bottomNav.inflateMenu(R.menu.bottom_navigation_admin);
            loadFragment(new AdminProductsFragment()); // Cargar fragmento inicial
        } else {
            bottomNav.inflateMenu(R.menu.bottom_navigation_client);
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CatalogFragment(), "catalog_fragment")
                .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (isAdmin) {
                if (itemId == R.id.nav_products) {
                    selectedFragment = new AdminProductsFragment();
                } else if (itemId == R.id.nav_orders) {
                    selectedFragment = new AdminOrdersFragment();
                }
            } else {
                if (itemId == R.id.nav_catalog) {
                    selectedFragment = new CatalogFragment();
                    getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment, "catalog_fragment")
                        .commit();
                    return true;
                } else if (itemId == R.id.nav_cart) {
                    selectedFragment = new CartFragment();
                } else if (itemId == R.id.nav_orders) {
                    selectedFragment = new OrdersFragment();
                }
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        // Pasar el userId al fragmento
        Bundle args = new Bundle();
        args.putInt("USER_ID", userId);
        fragment.setArguments(args);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, 
                    fragment instanceof CatalogFragment ? "catalog_fragment" : null)
            .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.confirm_logout)
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Volver a MainActivity
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshCatalog() {
        // Buscar el CatalogFragment y actualizarlo
        CatalogFragment catalogFragment = (CatalogFragment) getSupportFragmentManager()
                .findFragmentByTag("catalog_fragment");  // Asegúrate de que este tag coincida con el usado al agregar el fragmento
            
        if (catalogFragment != null) {
            Log.d("HomeActivity", "Refreshing catalog...");
            catalogFragment.refreshProducts();
        } else {
            Log.e("HomeActivity", "CatalogFragment not found!");
            // Si no se encuentra el fragmento, intentar buscarlo sin tag
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment instanceof CatalogFragment) {
                    Log.d("HomeActivity", "Found CatalogFragment without tag");
                    ((CatalogFragment) fragment).refreshProducts();
                    break;
                }
            }
        }
    }
} 