package com.example.tiendaevaluacion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ClientOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_options);

        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);
        Button updateButton = findViewById(R.id.update_button);
        Button deleteButton = findViewById(R.id.delete_button);

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClientOptionsActivity.this, ClientLoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClientOptionsActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        updateButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClientOptionsActivity.this, UpdateProfileActivity.class);
            startActivity(intent);
        });

        deleteButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClientOptionsActivity.this, DeleteAccountActivity.class);
            startActivity(intent);
        });
    }
} 