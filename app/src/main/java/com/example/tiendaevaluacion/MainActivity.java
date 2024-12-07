package com.example.tiendaevaluacion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Activity started");

        // Inicializa Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Evento personalizado de inicio de la app
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "app_start");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        Button adminButton = findViewById(R.id.admin_button);
        Button clientButton = findViewById(R.id.client_button);

        adminButton.setOnClickListener(v -> {
            // Registrar evento de click en botón admin
            logButtonClick("admin_button", "Admin Login");
            Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        clientButton.setOnClickListener(v -> {
            // Registrar evento de click en botón cliente
            logButtonClick("client_button", "Client Options");
            Intent intent = new Intent(MainActivity.this, ClientOptionsActivity.class);
            startActivity(intent);
        });
    }

    // Método auxiliar para registrar clicks en botones
    private void logButtonClick(String buttonId, String buttonName) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, buttonId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, buttonName);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }
}