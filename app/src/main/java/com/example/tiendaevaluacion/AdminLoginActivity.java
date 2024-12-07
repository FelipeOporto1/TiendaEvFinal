package com.example.tiendaevaluacion;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tiendaevaluacion.database.DatabaseHelper;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        dbHelper = new DatabaseHelper(this);
        
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        Button loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> attemptAdminLogin());
    }

    private void attemptAdminLogin() {
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_ID},
                DatabaseHelper.COLUMN_USERNAME + "=? AND " + 
                DatabaseHelper.COLUMN_PASSWORD + "=? AND " +
                DatabaseHelper.COLUMN_IS_ADMIN + "=1",
                new String[]{username, password},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                Intent intent = new Intent(AdminLoginActivity.this, HomeActivity.class);
                intent.putExtra("IS_ADMIN", true);
                intent.putExtra("USER_ID", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Credenciales de administrador incorrectas", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 