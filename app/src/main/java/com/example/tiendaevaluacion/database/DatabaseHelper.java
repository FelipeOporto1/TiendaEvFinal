package com.example.tiendaevaluacion.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tienda.db";
    private static final int DATABASE_VERSION = 3;

    // Definición de tablas
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_ORDER_ITEMS = "order_items";
    public static final String TABLE_CART = "cart";
    public static final String TABLE_SHIRT_SIZES = "shirt_sizes";

    // Columnas comunes
    public static final String COLUMN_ID = "id";
    
    // Columnas tabla users
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_IS_ADMIN = "is_admin";

    // Columnas tabla products
    public static final String COLUMN_PRODUCT_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_STOCK = "stock";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_IMAGE_URL = "image_url";

    // Columnas tabla orders
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_ORDER_DATE = "order_date";
    public static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DELIVERY_TYPE = "delivery_type";
    public static final String COLUMN_ADDRESS = "address";

    // Columnas tabla order_items
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_PRODUCT_ID = "product_id";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_PRICE = "item_price";

    // Columnas tabla cart
    public static final String COLUMN_CART_ID = "id";
    public static final String COLUMN_CART_USER_ID = "user_id";
    public static final String COLUMN_CART_PRODUCT_ID = "product_id";
    public static final String COLUMN_CART_QUANTITY = "quantity";

    // Columnas tabla shirt_sizes
    public static final String COLUMN_SHIRT_SIZE = "size";
    public static final String COLUMN_SHIRT_STOCK = "stock";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla users
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                + COLUMN_PASSWORD + " TEXT NOT NULL,"
                + COLUMN_IS_ADMIN + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Crear tabla products
        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PRODUCT_NAME + " TEXT NOT NULL,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_PRICE + " REAL NOT NULL,"
                + COLUMN_STOCK + " INTEGER NOT NULL,"
                + COLUMN_SIZE + " TEXT,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_IMAGE_URL + " TEXT"
                + ")";
        db.execSQL(CREATE_PRODUCTS_TABLE);

        // Crear tabla orders
        String CREATE_ORDERS_TABLE = "CREATE TABLE " + TABLE_ORDERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER NOT NULL,"
                + COLUMN_ORDER_DATE + " INTEGER NOT NULL,"
                + COLUMN_TOTAL_AMOUNT + " REAL NOT NULL,"
                + COLUMN_STATUS + " TEXT NOT NULL,"
                + COLUMN_DELIVERY_TYPE + " TEXT NOT NULL,"
                + COLUMN_ADDRESS + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_ORDERS_TABLE);

        // Crear tabla order_items con columna size
        String CREATE_ORDER_ITEMS_TABLE = "CREATE TABLE " + TABLE_ORDER_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ORDER_ID + " INTEGER NOT NULL,"
                + COLUMN_PRODUCT_ID + " INTEGER NOT NULL,"
                + COLUMN_QUANTITY + " INTEGER NOT NULL,"
                + COLUMN_ITEM_PRICE + " REAL NOT NULL,"
                + COLUMN_SIZE + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_ORDER_ID + ") REFERENCES " + TABLE_ORDERS + "(" + COLUMN_ID + "),"
                + "FOREIGN KEY(" + COLUMN_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_ORDER_ITEMS_TABLE);

        // Crear tabla cart
        String CREATE_CART_TABLE = "CREATE TABLE " + TABLE_CART + "("
                + COLUMN_CART_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CART_USER_ID + " INTEGER NOT NULL,"
                + COLUMN_CART_PRODUCT_ID + " INTEGER NOT NULL,"
                + COLUMN_CART_QUANTITY + " INTEGER NOT NULL,"
                + COLUMN_SIZE + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_CART_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "),"
                + "FOREIGN KEY(" + COLUMN_CART_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_CART_TABLE);

        // Crear usuario administrador por defecto
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, "admin");
        values.put(COLUMN_PASSWORD, "admin123");
        values.put(COLUMN_IS_ADMIN, 1);
        db.insert(TABLE_USERS, null, values);

        // Crear tabla de stock por talla
        String CREATE_SHIRT_SIZES_TABLE = "CREATE TABLE " + TABLE_SHIRT_SIZES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PRODUCT_ID + " INTEGER NOT NULL,"
                + COLUMN_SHIRT_SIZE + " TEXT NOT NULL,"
                + COLUMN_SHIRT_STOCK + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + COLUMN_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_SHIRT_SIZES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Agregar columna size a la tabla cart si no existe
            try {
                db.execSQL("ALTER TABLE " + TABLE_CART + 
                          " ADD COLUMN " + COLUMN_SIZE + " TEXT DEFAULT ''");
            } catch (Exception e) {
                // La columna ya existe o hay otro error
                e.printStackTrace();
            }
        }
        
        if (oldVersion < 3) {
            // Crear tabla shirt_sizes si no existe
            try {
                String CREATE_SHIRT_SIZES_TABLE = "CREATE TABLE " + TABLE_SHIRT_SIZES + "("
                        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COLUMN_PRODUCT_ID + " INTEGER NOT NULL,"
                        + COLUMN_SHIRT_SIZE + " TEXT NOT NULL,"
                        + COLUMN_SHIRT_STOCK + " INTEGER NOT NULL,"
                        + "FOREIGN KEY(" + COLUMN_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_ID + ")"
                        + ")";
                db.execSQL(CREATE_SHIRT_SIZES_TABLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Agregar columna size a order_items si no existe
        try {
            db.execSQL("ALTER TABLE " + TABLE_ORDER_ITEMS + 
                      " ADD COLUMN " + COLUMN_SIZE + " TEXT");
        } catch (Exception e) {
            // La columna ya existe, ignorar el error
        }
    }
} 