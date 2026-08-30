package com.example.finalproject.Admin.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.ui.Products.Products;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class SalesInsightsFragment extends Fragment {

    // UI
    private Spinner spPeriod, spKey;
    private ListView listView;

    // Data
    private final ArrayList<InsightItem> insights = new ArrayList<>();
    private InsightsAdapter adapter;

    // Firebase
    private DatabaseReference productsRef;
    private DatabaseReference analyticsRef;

    // Period keys
    private final ArrayList<String> monthKeys = new ArrayList<>();
    private final ArrayList<String> yearKeys = new ArrayList<>();

    private ArrayAdapter<String> periodAdapter;
    private ArrayAdapter<String> keyAdapter;
    private boolean isInitialLoad = true;


    private int maxSoldInPeriod = -1;


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sales_insights, container, false);

        spPeriod = v.findViewById(R.id.spPeriod);
        spKey = v.findViewById(R.id.spKey);
        listView = v.findViewById(R.id.listInsights);

        adapter = new InsightsAdapter();
        listView.setAdapter(adapter);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        productsRef = db.getReference("products");
        analyticsRef = db.getReference("analytics");

        setupPeriodSpinner();
        setupKeySpinner();

        loadAvailableMonthKeys();
        loadAvailableYearKeys();

        setupSpinnerListeners();

        return v;
    }


    private void setupPeriodSpinner() {
        ArrayList<String> periods = new ArrayList<>();
        periods.add("Month");
        periods.add("Year");

        periodAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                periods
        );
        spPeriod.setAdapter(periodAdapter);
        spPeriod.setSelection(0); // default Month
    }

    private void setupKeySpinner() {
        keyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new ArrayList<>()
        );
        spKey.setAdapter(keyAdapter);
    }

    private void setupSpinnerListeners() {

        spPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) {
                    updateKeySpinner(monthKeys);
                } else {
                    updateKeySpinner(yearKeys);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        spKey.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedKey = (String) spKey.getSelectedItem();
                if (selectedKey == null) return;

                if (spPeriod.getSelectedItemPosition() == 0) {
                    loadMonthInsightsForKey(selectedKey);
                } else {
                    loadYearInsightsForKey(selectedKey);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void updateKeySpinner(ArrayList<String> keys) {
        keyAdapter.clear();
        keyAdapter.addAll(keys);
        keyAdapter.notifyDataSetChanged();

        if (keys.isEmpty()) {
            clearList();

            if (!isInitialLoad) {
                showToast("No data keys found");
            }
            return;
        }

        isInitialLoad = false;

        spKey.setSelection(keys.size() - 1);
    }



    private void loadAvailableMonthKeys() {
        analyticsRef.child("sales_by_month")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        monthKeys.clear();

                        for (DataSnapshot m : snapshot.getChildren()) {
                            if (m.getKey() != null) monthKeys.add(m.getKey());
                        }

                        if (spPeriod.getSelectedItemPosition() == 0) {
                            updateKeySpinner(monthKeys);
                        }

                        isInitialLoad = false;
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        isInitialLoad = false;
                    }
                });
    }


    private void loadAvailableYearKeys() {
        analyticsRef.child("sales_by_year")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        yearKeys.clear();

                        for (DataSnapshot y : snapshot.getChildren()) {
                            if (y.getKey() != null) yearKeys.add(y.getKey());
                        }

                        if (spPeriod.getSelectedItemPosition() == 1) {
                            updateKeySpinner(yearKeys);
                        }

                        isInitialLoad = false;
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        isInitialLoad = false;
                    }
                });
    }



    private void loadMonthInsightsForKey(@NonNull String monthKey) {
        analyticsRef.child("sales_by_month")
                .child(monthKey)
                .child("products")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        clearList();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            showToast("No product analytics for month " + monthKey);
                            return;
                        }

                        maxSoldInPeriod = findMaxSold(snapshot);

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String analyticsProductKey = s.getKey();
                            Long sold = s.child("qty_sold").getValue(Long.class);
                            if (sold == null) sold = 0L;
                            loadProductDetailsFlexible(analyticsProductKey, sold.intValue());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Month read error: " + error.getMessage());
                    }
                });
    }

    private void loadYearInsightsForKey(@NonNull String yearKey) {
        analyticsRef.child("sales_by_year")
                .child(yearKey)
                .child("products")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        clearList();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            showToast("No product analytics for year " + yearKey);
                            return;
                        }

                        maxSoldInPeriod = findMaxSold(snapshot);

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String analyticsProductKey = s.getKey();
                            Long sold = s.child("qty_sold").getValue(Long.class);
                            if (sold == null) sold = 0L;
                            loadProductDetailsFlexible(analyticsProductKey, sold.intValue());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Year read error: " + error.getMessage());
                    }
                });
    }

    private int findMaxSold(@NonNull DataSnapshot productsNode) {
        int max = -1;
        for (DataSnapshot s : productsNode.getChildren()) {
            Long sold = s.child("qty_sold").getValue(Long.class);
            int soldQty = (sold == null) ? 0 : sold.intValue();
            if (soldQty > max) max = soldQty;
        }
        return max;
    }


    private void loadProductDetailsFlexible(@NonNull String analyticsProductKey, int soldQty) {

        productsRef.child(analyticsProductKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {

                        Products p = s.getValue(Products.class);
                        if (p != null) {
                            addInsightRow(analyticsProductKey, p, soldQty);
                            return;
                        }

                        // Fallback by nameProduct (if products keys are not the same as analytics keys)
                        Query q = productsRef.orderByChild("nameProduct").equalTo(analyticsProductKey);
                        q.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot snap) {

                                if (!snap.exists()) return;

                                DataSnapshot first = snap.getChildren().iterator().next();
                                Products pp = first.getValue(Products.class);

                                if (pp != null) {
                                    addInsightRow(first.getKey(), pp, soldQty);
                                }
                            }

                            @Override public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }



    private void addInsightRow(String productKey, Products p, int soldQty) {

        int stock = p.getAmount();
        int price = p.getPrice();

        if (soldQty == maxSoldInPeriod && soldQty > 0) {
            insights.add(new InsightItem(
                    productKey,
                    p.getNameProduct(),
                    "🏆 Top seller",
                    "Keep price / consider raising slightly",
                    soldQty,
                    stock,
                    price,
                    InsightItem.TYPE_TOP
            ));
        } else if (soldQty == 0) {
            insights.add(new InsightItem(
                    productKey,
                    p.getNameProduct(),
                    "No sales",
                    "Strong discount (20%) or remove",
                    soldQty,
                    stock,
                    price,
                    InsightItem.TYPE_DEAD
            ));
        } else if (soldQty < 15) {
            insights.add(new InsightItem(
                    productKey,
                    p.getNameProduct(),
                    "Below 15 sales",
                    "Recommend discount (10%)",
                    soldQty,
                    stock,
                    price,
                    InsightItem.TYPE_LOW_15
            ));
        } else {
            insights.add(new InsightItem(
                    productKey,
                    p.getNameProduct(),
                    "✅ OK",
                    "No action needed",
                    soldQty,
                    stock,
                    price,
                    InsightItem.TYPE_OK
            ));
        }

        adapter.notifyDataSetChanged();
    }


    private void applyDiscount(@NonNull InsightItem it) {

        // DEAD => 20% discount, LOW_15 => 10% discount
        double factor = (it.type == InsightItem.TYPE_DEAD) ? 0.8 : 0.9;
        int newPrice = (int) Math.max(1, Math.round(it.price * factor));

        productsRef.child(it.productKey)
                .child("price")
                .setValue(newPrice);

        showToast("Price updated to ₪" + newPrice);
    }


    private void clearList() {
        insights.clear();
        adapter.notifyDataSetChanged();
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }


    private class InsightsAdapter extends BaseAdapter {

        @Override public int getCount() { return insights.size(); }
        @Override public Object getItem(int i) { return insights.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_sales_insight, parent, false);
            }

            InsightItem it = insights.get(pos);

            TextView name   = convertView.findViewById(R.id.txtProductName);
            TextView reason = convertView.findViewById(R.id.txtReason);
            TextView action = convertView.findViewById(R.id.txtAction);
            TextView stats  = convertView.findViewById(R.id.txtStats);
            Button btnDisc  = convertView.findViewById(R.id.btnDiscount);

            name.setText(it.name);
            reason.setText(it.reason);
            action.setText(it.action);
            stats.setText(
                    "Sold: " + it.sold +
                            " | Stock: " + it.stock +
                            " | Price: ₪" + it.price
            );

            action.setTextColor(0xFF444444);
            btnDisc.setVisibility(View.GONE);

            switch (it.type) {

                case InsightItem.TYPE_TOP:
                    action.setTextColor(0xFF1565C0);
                    break;

                case InsightItem.TYPE_OK:
                    action.setTextColor(0xFF2E7D32);
                    break;

                case InsightItem.TYPE_LOW_15:
                    action.setTextColor(0xFFE65100);
                    btnDisc.setVisibility(View.VISIBLE);
                    btnDisc.setText("⬇️ Discount 10%");
                    btnDisc.setOnClickListener(v -> applyDiscount(it));
                    break;

                case InsightItem.TYPE_DEAD:
                    action.setTextColor(0xFFC62828);
                    btnDisc.setVisibility(View.VISIBLE);
                    btnDisc.setText("⬇️ Discount 20%");
                    btnDisc.setOnClickListener(v -> applyDiscount(it));
                    break;
            }

            return convertView;
        }


    }


    static class InsightItem {

        static final int TYPE_TOP     = 1;
        static final int TYPE_DEAD    = 2;
        static final int TYPE_LOW_15  = 3;
        static final int TYPE_OK      = 4;

        String productKey;
        String name;
        String reason;
        String action;
        int sold;
        int stock;
        int price;
        int type;

        InsightItem(String pk, String n, String r, String a,
                    int sold, int stock, int price, int type) {
            this.productKey = pk;
            this.name = n;
            this.reason = r;
            this.action = a;
            this.sold = sold;
            this.stock = stock;
            this.price = price;
            this.type = type;
        }
    }
}
