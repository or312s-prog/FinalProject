package com.example.finalproject.Admin.ui.add_product;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class add_product extends Fragment {

    private EditText etName, etCategory, etPrice, etAmount, etImageUrl;
    private Button btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_add_product, c, false);

        etName     = v.findViewById(R.id.etName);
        etCategory = v.findViewById(R.id.etCategory);
        etPrice    = v.findViewById(R.id.etPrice);
        etAmount   = v.findViewById(R.id.etAmount);
        etImageUrl = v.findViewById(R.id.etImageUrl);
        btnSave    = v.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(view -> saveProduct());
        return v;
    }

    private void saveProduct() {
        String name      = etName.getText().toString().trim();
        String category  = etCategory.getText().toString().trim();
        String priceStr  = etPrice.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String imageUrl  = etImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(category)
                || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(amountStr)) {
            toast("Fill all required fields");
            return;
        }

        final int price, amount;
        try {
            price  = Integer.parseInt(priceStr);
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            toast("Price/Amount must be numbers");
            return;
        }

        String key = safeKey(name);

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();


        DatabaseReference productsRef = root.child("products").child(key);

        int minProduct = 30;

        HashMap<String, Object> productData = new HashMap<>();
        productData.put("nameProduct", name);
        productData.put("category", category);
        productData.put("price", price);
        productData.put("amount", amount);
        productData.put("min_product", minProduct);

        if (!imageUrl.isEmpty()) {
            productData.put("imageUrl", imageUrl);
        }


        productsRef.setValue(productData)
                .addOnSuccessListener(u -> {


                    DatabaseReference supplierRef = root.child("supplier_orders").child(key);

                    HashMap<String, Object> supplyData = new HashMap<>();
                    supplyData.put("productKey",    key);
                    supplyData.put("nameProduct",   name);
                    supplyData.put("category",      category);
                    supplyData.put("price",         price);
                    supplyData.put("orderedAmount", amount);
                    supplyData.put("orderDate",     getTodayDate());
                    if (!imageUrl.isEmpty()) supplyData.put("imageUrl", imageUrl);

                    supplierRef.setValue(supplyData)
                            .addOnSuccessListener(a -> {
                                toast("Product saved & added to supplier orders");
                                clearForm();
                            })
                            .addOnFailureListener(e ->
                                    toast("Saved product, but failed to add supplier order: " + e.getMessage())
                            );

                })
                .addOnFailureListener(e -> toast("Failed: " + e.getMessage()));
    }


    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }

    private void clearForm() {
        etName.setText("");
        etCategory.setText("");
        etPrice.setText("");
        etAmount.setText("");
        etImageUrl.setText("");
    }

    private void toast(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
    }

    private static String safeKey(String raw) {
        return (raw == null ? "item" : raw).replaceAll("[.#$\\[\\]/]", "_");
    }
}
