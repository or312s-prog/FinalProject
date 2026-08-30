package com.example.finalproject.Admin.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finalproject.R;
import com.example.finalproject.ui.Products.Products;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class HomePageAdmin extends Fragment {

    private static final int LOW_STOCK_THRESHOLD = 30;

    private TextView txtLowStockCount;
    private TextView txtPendingDeliveriesCount;
    private LinearLayout panelLowStock, panelPendingDeliveries;

    private DatabaseReference productsRef, ordersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_page_admin, container, false);

        txtLowStockCount = view.findViewById(R.id.txtLowStockCount);
        txtPendingDeliveriesCount = view.findViewById(R.id.txtPendingDeliveriesCount);
        panelLowStock = view.findViewById(R.id.panelLowStock);
        panelPendingDeliveries = view.findViewById(R.id.panelPendingDeliveries);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        productsRef = db.getReference("products");
        ordersRef = db.getReference("orders");

        listenLowStockProducts();
        listenPendingDeliveries();

        panelLowStock.setOnClickListener(v -> openLowStockDialog());
        panelPendingDeliveries.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.nav_update_orders));

        return view;
    }

    private void listenLowStockProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot s : snapshot.getChildren()) {
                    Long amount = s.child("amount").getValue(Long.class);
                    if (amount != null && amount <= LOW_STOCK_THRESHOLD) count++;
                }
                txtLowStockCount.setText(count + " products");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void openLowStockDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_low_stock_products, null);

        ListView listView = dialogView.findViewById(R.id.listLowStock);
        Button btnGoUpdate = dialogView.findViewById(R.id.btnGoToUpdateProducts);

        ArrayList<Products> lowStockList = new ArrayList<>();
        LowStockAdapter adapter = new LowStockAdapter(lowStockList);
        listView.setAdapter(adapter);

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                lowStockList.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Products p = s.getValue(Products.class);
                    if (p != null && p.getAmount() <= LOW_STOCK_THRESHOLD) {
                        lowStockList.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setNegativeButton("Close", (d, w) -> d.dismiss())
                .create();

        btnGoUpdate.setOnClickListener(v -> {
            dialog.dismiss();
            NavHostFragment.findNavController(this)
                    .navigate(R.id.nav_update_product);
        });

        dialog.show();
    }



    private void listenPendingDeliveries() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pending = 0;
                for (DataSnapshot s : snapshot.getChildren()) {
                    String d = s.child("deliveryDate").getValue(String.class);
                    String t = s.child("deliveryTime").getValue(String.class);
                    if (TextUtils.isEmpty(d) || TextUtils.isEmpty(t)) pending++;
                }
                txtPendingDeliveriesCount.setText(pending + " orders");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    class LowStockAdapter extends BaseAdapter {
        ArrayList<Products> data;
        LowStockAdapter(ArrayList<Products> d) { data = d; }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View v, ViewGroup p) {
            if (v == null) {
                v = LayoutInflater.from(getContext())
                        .inflate(R.layout.row_low_stock_product, p, false);
            }
            TextView name = v.findViewById(R.id.txtProductName);
            TextView amount = v.findViewById(R.id.txtProductAmount);

            Products pr = data.get(i);
            name.setText(pr.getNameProduct());
            amount.setText("In stock: " + pr.getAmount());
            return v;
        }
    }
}
