package com.example.tiendaevaluacion;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tiendaevaluacion.adapters.AdminOrderAdapter;
import com.example.tiendaevaluacion.database.DatabaseHelper;
import com.example.tiendaevaluacion.models.Order;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdminOrdersFragment extends Fragment implements AdminOrderAdapter.OrderStatusListener {
    private DatabaseHelper dbHelper;
    private AdminOrderAdapter adapter;
    private List<Order> orderList;
    private TextView emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        orderList = new ArrayList<>();

        emptyView = view.findViewById(R.id.empty_view);
        RecyclerView recyclerView = view.findViewById(R.id.orders_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminOrderAdapter(getContext(), orderList, this);
        recyclerView.setAdapter(adapter);

        loadOrders();
        return view;
    }

    private void loadOrders() {
        orderList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT o.*, u.username FROM " + DatabaseHelper.TABLE_ORDERS + " o " +
                "INNER JOIN " + DatabaseHelper.TABLE_USERS + " u " +
                "ON o." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID +
                " ORDER BY " + DatabaseHelper.COLUMN_ORDER_DATE + " DESC";
        
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            Order order = new Order(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)),
                new ArrayList<>(),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_AMOUNT)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELIVERY_TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS)),
                new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ORDER_DATE)))
            );
            orderList.add(order);
        }
        cursor.close();

        adapter.updateOrders(orderList);
        emptyView.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStatusChanged(Order order, String newStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.TABLE_ORDERS +
                " SET " + DatabaseHelper.COLUMN_STATUS + " = ?" +
                " WHERE " + DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{newStatus, String.valueOf(order.getId())});
        loadOrders();
    }
} 