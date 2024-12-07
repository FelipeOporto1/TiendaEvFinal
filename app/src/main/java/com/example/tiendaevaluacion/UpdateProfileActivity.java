package com.example.tiendaevaluacion;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tiendaevaluacion.database.DatabaseHelper;

public class UpdateProfileActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText currentPasswordInput;
    private EditText newUsernameInput;
    private EditText newPasswordInput;
    private EditText confirmPasswordInput;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        dbHelper = new DatabaseHelper(this);
        
        usernameInput = findViewById(R.id.username_input);
        currentPasswordInput = findViewById(R.id.current_password_input);
        newUsernameInput = findViewById(R.id.new_username_input);
        newPasswordInput = findViewById(R.id.new_password_input);
        confirmPasswordInput = findViewById(R.id.confirm_new_password_input);
        Button updateButton = findViewById(R.id.update_button);

        updateButton.setOnClickListener(v -> attemptUpdate());
    }

    private void attemptUpdate() {
        String currentUsername = usernameInput.getText().toString();
        String currentPassword = currentPasswordInput.getText().toString();
        String newUsername = newUsernameInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (currentUsername.isEmpty() || currentPassword.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese usuario y contraseña actuales", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Verificar credenciales actuales
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_USERNAME + "=? AND " + DatabaseHelper.COLUMN_PASSWORD + "=?",
                new String[]{currentUsername, currentPassword},
                null, null, null);

        if (!cursor.moveToFirst()) {
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            cursor.close();
            return;
        }
        int userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
        cursor.close();

        // Si se ingresó nuevo nombre de usuario, verificar que no exista
        if (!newUsername.isEmpty()) {
            cursor = db.query(DatabaseHelper.TABLE_USERS,
                    null,
                    DatabaseHelper.COLUMN_USERNAME + "=? AND " + DatabaseHelper.COLUMN_ID + "!=?",
                    new String[]{newUsername, String.valueOf(userId)},
                    null, null, null);
            
            if (cursor.moveToFirst()) {
                Toast.makeText(this, R.string.username_exists, Toast.LENGTH_SHORT).show();
                cursor.close();
                return;
            }
            cursor.close();
        }

        // Actualizar datos
        ContentValues values = new ContentValues();
        if (!newUsername.isEmpty()) {
            values.put(DatabaseHelper.COLUMN_USERNAME, newUsername);
        }
        if (!newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, R.string.passwords_not_match, Toast.LENGTH_SHORT).show();
                return;
            }
            values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);
        }

        if (values.size() > 0) {
            int result = db.update(DatabaseHelper.TABLE_USERS,
                    values,
                    DatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(userId)});

            if (result > 0) {
                Toast.makeText(this, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay cambios para actualizar", Toast.LENGTH_SHORT).show();
        }
    }
} 