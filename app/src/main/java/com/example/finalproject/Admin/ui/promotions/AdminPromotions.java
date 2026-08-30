package com.example.finalproject.Admin.ui.promotions;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.ui.Products.Products;
import com.example.finalproject.ui.Products.Promotion;
import com.google.firebase.database.*;

import java.util.*;

public class AdminPromotions extends Fragment {

    private EditText etSearch;
    private ListView listView;
    private Button btnCategoryPromo;

    private final ArrayList<Products> all = new ArrayList<>();
    private final ArrayList<Products> filtered = new ArrayList<>();
    private BaseAdapter adapter;

    private DatabaseReference productsRef;
    private ValueEventListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_admin_promotions, container, false);

        etSearch = v.findViewById(R.id.etSearch);
        listView = v.findViewById(R.id.listProducts);
        btnCategoryPromo = v.findViewById(R.id.btnCategoryPromo);

        productsRef = FirebaseDatabase.getInstance().getReference("products");

        adapter = new BaseAdapter() {
            @Override public int getCount() { return filtered.size(); }
            @Override public Object getItem(int i) { return filtered.get(i); }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                if (cv == null) {
                    cv = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.row_admin_product, parent, false);
                }

                Products p = filtered.get(pos);

                TextView txtName = cv.findViewById(R.id.txtName);
                TextView txtSub  = cv.findViewById(R.id.txtSub);

                txtName.setText(p.getNameProduct());

                String promoText = "No promo";
                Promotion promo = p.getPromotion();
                if (promo != null && promo.isActive()) {
                    if (promo.getDiscountPercent() > 0) {
                        promoText = promo.getDiscountPercent() + "% OFF";
                    } else if (promo.getBuyQty() > 0 && promo.getFreeQty() > 0) {
                        promoText = "Buy " + promo.getBuyQty() + " Get " + promo.getFreeQty() + " FREE";
                    }
                }

                txtSub.setText("₪" + p.getPrice() + " | Category: " + p.getCategory() + " | " + promoText);
                return cv;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Products p = filtered.get(position);
            openSingleProductPromoDialog(p);
        });

        btnCategoryPromo.setOnClickListener(view -> openCategoryDiscountDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        startListen();
        return v;
    }

    private void startListen() {
        if (listener != null) productsRef.removeEventListener(listener);

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                all.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Products p = s.getValue(Products.class);
                    if (p == null) continue;
                    p.setProductKey(s.getKey());
                    all.add(p);
                }

                applyFilter(etSearch.getText() == null ? "" : etSearch.getText().toString());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };

        productsRef.addValueEventListener(listener);
    }

    private void applyFilter(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.getDefault());

        filtered.clear();
        if (query.isEmpty()) {
            filtered.addAll(all);
        } else {
            for (Products p : all) {
                String name = p.getNameProduct() == null ? "" : p.getNameProduct().toLowerCase(Locale.getDefault());
                String cat  = p.getCategory() == null ? "" : p.getCategory().toLowerCase(Locale.getDefault());
                if (name.contains(query) || cat.contains(query)) filtered.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openCategoryDiscountDialog() {
        if (!isAdded() || getContext() == null) return;

        Set<String> categoriesSet = new HashSet<>();
        for (Products p : all) {
            if (p.getCategory() != null && !p.getCategory().trim().isEmpty()) {
                categoriesSet.add(p.getCategory().trim());
            }
        }

        if (categoriesSet.isEmpty()) {
            Toast.makeText(getContext(), "No categories found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> categoryList = new ArrayList<>(categoriesSet);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        TextView txtSelectCat = new TextView(getContext());
        txtSelectCat.setText("Select Category:");
        txtSelectCat.setTextSize(16);
        layout.addView(txtSelectCat);

        Spinner spCategories = new Spinner(getContext());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, categoryList);
        spCategories.setAdapter(spinnerAdapter);
        layout.addView(spCategories);

        TextView txtPercent = new TextView(getContext());
        txtPercent.setText("\nDiscount Percentage (%):");
        txtPercent.setTextSize(16);
        layout.addView(txtPercent);

        EditText etPercent = new EditText(getContext());
        etPercent.setHint("e.g. 20");
        etPercent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPercent);

        new AlertDialog.Builder(getContext())
                .setTitle("Category Discount")
                .setView(layout)
                .setPositiveButton("Apply Discount", (d, which) -> {
                    String selectedCat = spCategories.getSelectedItem().toString();
                    int percent = parseIntSafe(etPercent.getText());

                    if (percent <= 0 || percent > 100) {
                        Toast.makeText(getContext(), "Enter percentage between 1 and 100", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Promotion categoryPromo = new Promotion(0, 0, true, percent);


                    for (Products p : all) {
                        if (selectedCat.equalsIgnoreCase(p.getCategory())) {
                            productsRef.child(p.getProductKey()).child("promotion").setValue(categoryPromo);
                        }
                    }
                    Toast.makeText(getContext(), percent + "% discount applied to " + selectedCat, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Remove Category Promo", (d, which) -> {
                    String selectedCat = spCategories.getSelectedItem().toString();
                    for (Products p : all) {
                        if (selectedCat.equalsIgnoreCase(p.getCategory())) {
                            productsRef.child(p.getProductKey()).child("promotion").removeValue();
                        }
                    }
                    Toast.makeText(getContext(), "Promotion removed from " + selectedCat, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSingleProductPromoDialog(Products p) {
        if (!isAdded() || getContext() == null) return;

        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_promotion, null, false);
        EditText etBuy = dv.findViewById(R.id.etBuyQty);
        EditText etFree = dv.findViewById(R.id.etFreeQty);
        EditText etDiscount = dv.findViewById(R.id.etDiscountPercent);
        Switch swActive = dv.findViewById(R.id.swActive);

        Promotion promo = p.getPromotion();
        if (promo != null) {
            etBuy.setText(promo.getBuyQty() > 0 ? String.valueOf(promo.getBuyQty()) : "");
            etFree.setText(promo.getFreeQty() > 0 ? String.valueOf(promo.getFreeQty()) : "");
            etDiscount.setText(promo.getDiscountPercent() > 0 ? String.valueOf(promo.getDiscountPercent()) : "");
            swActive.setChecked(promo.isActive());
        } else {
            swActive.setChecked(true);
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Product Promo: " + p.getNameProduct())
                .setView(dv)
                .setPositiveButton("Save", (d, which) -> {
                    int buy = parseIntSafe(etBuy.getText());
                    int free = parseIntSafe(etFree.getText());
                    int discount = parseIntSafe(etDiscount.getText());
                    boolean active = swActive.isChecked();

                    if ((buy <= 0 || free <= 0) && discount <= 0) {
                        Toast.makeText(getContext(), "Enter Buy/Free OR Discount %", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Promotion newPromo = new Promotion(buy, free, active, discount);
                    productsRef.child(p.getProductKey()).child("promotion").setValue(newPromo);
                    Toast.makeText(getContext(), "Promotion saved for " + p.getNameProduct(), Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Remove Promo", (d, which) -> {
                    productsRef.child(p.getProductKey()).child("promotion").removeValue();
                    Toast.makeText(getContext(), "Promotion removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int parseIntSafe(@Nullable Editable e) {
        try { return Integer.parseInt(e == null ? "" : e.toString().trim()); }
        catch (Exception ex) { return 0; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) {
            productsRef.removeEventListener(listener);
            listener = null;
        }
    }
}