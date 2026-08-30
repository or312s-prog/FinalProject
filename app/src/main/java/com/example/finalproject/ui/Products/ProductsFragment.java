package com.example.finalproject.ui.Products;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class ProductsFragment extends Fragment {

    private DatabaseReference productsRef;

    private final ArrayList<Products> products = new ArrayList<>();
    private final ArrayList<Products> shownProducts = new ArrayList<>();

    private EditText etSearch;
    private Spinner spCategory;

    private ValueEventListener productsListener;

    private String selectedCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        productsRef = FirebaseDatabase.getInstance().getReference("products");
        View root = inflater.inflate(R.layout.fragment_products, container, false);

        etSearch = root.findViewById(R.id.etSearch);
        spCategory = root.findViewById(R.id.spCategory);

        ListView ls = root.findViewById(R.id.ls);

        final MyAdapter myAdapter = new MyAdapter(this, shownProducts);
        ls.setAdapter(myAdapter);

        setupSearchListener(myAdapter);
        setupCategorySpinner(myAdapter);

        fetchProductsFromFirebase(myAdapter);

        return root;
    }

    private void setupSearchListener(MyAdapter adapter) {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(adapter);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategorySpinner(MyAdapter adapter) {
        if (spCategory == null) return;

        ArrayList<String> cats = new ArrayList<>();
        cats.add("All");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cats
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(spinnerAdapter);

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = String.valueOf(parent.getItemAtPosition(position));
                applyFilter(adapter);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchProductsFromFirebase(final MyAdapter adapter) {

        if (productsListener != null) {
            productsRef.removeEventListener(productsListener);
        }

        productsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!isAdded() || getContext() == null) return;

                products.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Products p = snapshot.getValue(Products.class);
                    if (p == null) continue;

                    p.setProductKey(snapshot.getKey());
                    products.add(p);
                }

                rebuildCategories();
                applyFilter(adapter);
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e("FirebaseError", "Error fetching data", e.toException());
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        productsRef.addValueEventListener(productsListener);
    }

    private void rebuildCategories() {
        if (!isAdded() || getContext() == null || spCategory == null) return;

        ArrayList<String> cats = new ArrayList<>();
        cats.add("All");

        HashSet<String> set = new HashSet<>();
        for (Products p : products) {
            String c = p.getCategory();
            if (c != null) {
                c = c.trim();
                if (!c.isEmpty()) set.add(c);
            }
        }

        ArrayList<String> rest = new ArrayList<>(set);
        rest.sort(String.CASE_INSENSITIVE_ORDER);
        cats.addAll(rest);

        String keep = selectedCategory == null ? "All" : selectedCategory;

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_item,
                cats
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(spinnerAdapter);

        int idx = cats.indexOf(keep);
        if (idx < 0) idx = 0;
        spCategory.setSelection(idx);
    }

    private void applyFilter(MyAdapter adapter) {
        if (!isAdded() || getContext() == null) return;

        String q = "";
        if (etSearch != null && etSearch.getText() != null) {
            q = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        }

        String cat = selectedCategory == null ? "All" : selectedCategory.trim();

        shownProducts.clear();

        for (Products p : products) {
            String name = p.getNameProduct() == null ? "" : p.getNameProduct().toLowerCase(Locale.getDefault());
            String pc   = p.getCategory() == null ? "" : p.getCategory().trim();

            boolean matchesText = q.isEmpty() || name.contains(q);
            boolean matchesCat  = cat.equals("All") || pc.equalsIgnoreCase(cat);

            if (matchesText && matchesCat) {
                shownProducts.add(p);
            }
        }

        adapter.notifyDataSetChanged();
    }

    void addToCartAtomic(@NonNull String productKey, @NonNull String nameProduct, int unitPrice, int qty) {
        if (qty <= 0) {
            Toast.makeText(getContext(), "Quantity must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference productAmountRef = db.getReference("products").child(productKey).child("amount");
        DatabaseReference cartItemRef     = db.getReference("cart").child(productKey);

        productAmountRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer cur = currentData.getValue(Integer.class);
                if (cur == null) cur = 0;
                if (cur < qty) return Transaction.abort();
                currentData.setValue(cur - qty);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null || !committed) {
                    Toast.makeText(getContext(), "Not enough stock", Toast.LENGTH_SHORT).show();
                    return;
                }

                cartItemRef.child("quantity").runTransaction(new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData d) {
                        Integer cur = d.getValue(Integer.class);
                        if (cur == null) cur = 0;
                        d.setValue(cur + qty);
                        return Transaction.success(d);
                    }
                    @Override
                    public void onComplete(DatabaseError e, boolean committed, DataSnapshot s) {
                        if (e != null || !committed) {
                            productAmountRef.runTransaction(new Transaction.Handler() {
                                @NonNull @Override
                                public Transaction.Result doTransaction(@NonNull MutableData m) {
                                    Integer cur = m.getValue(Integer.class);
                                    if (cur == null) cur = 0;
                                    m.setValue(cur + qty);
                                    return Transaction.success(m);
                                }
                                @Override public void onComplete(DatabaseError e2, boolean c2, DataSnapshot s2) { }
                            });
                            Toast.makeText(getContext(), "Failed to add to cart", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        cartItemRef.child("productKey").setValue(productKey);
                        cartItemRef.child("nameProduct").setValue(nameProduct);
                        cartItemRef.child("unitPrice").setValue(unitPrice);

                        showAddToCartSuccessDialog();
                    }
                });
            }
        });
    }

    private void showAddToCartSuccessDialog() {
        if (!isAdded() || getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Item Added to Cart! 🛒")
                .setMessage("Would you like to go to your shopping cart or continue browsing products?")
                .setPositiveButton("Go to Cart", (dialog, which) -> {
                    try {
                        NavigationView navView = requireActivity().findViewById(R.id.nav_view);
                        if (navView != null) {
                            navView.getMenu().performIdentifierAction(R.id.nav_cart, 0);
                        }
                    } catch (Exception e) {
                        Log.e("NavigationError", "Failed to navigate to cart", e);
                    }
                })
                .setNegativeButton("Continue Shopping", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    static class MyAdapter extends BaseAdapter {
        private final ArrayList<Products> data;
        private final ProductsFragment host;

        MyAdapter(ProductsFragment host, ArrayList<Products> data) {
            this.host = host; this.data = data;
        }

        static class VH {
            TextView txtName, txtCategory, txtPrice, badgeLow, txtAmount, txtPromo;
            EditText etQuantity;
            Button btnAdd;
            ImageView imgProduct;
            Products boundProduct;
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            VH h;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_view, parent, false);
                h = new VH();
                h.txtName     = convertView.findViewById(R.id.txtNameProduct);
                h.txtCategory = convertView.findViewById(R.id.txtcategory);
                h.txtPrice    = convertView.findViewById(R.id.txtprice);
                h.badgeLow    = convertView.findViewById(R.id.badgeLowStock);
                h.etQuantity  = convertView.findViewById(R.id.etQuantity);
                h.btnAdd      = convertView.findViewById(R.id.btnAddToCart);
                h.txtAmount   = convertView.findViewById(R.id.txtAmount);
                h.imgProduct  = convertView.findViewById(R.id.imgProduct);
                h.txtPromo    = convertView.findViewById(R.id.txtPromo);

                convertView.setTag(h);

                h.btnAdd.setOnClickListener(v -> {
                    if (h.boundProduct == null) return;

                    String qStr = h.etQuantity.getText().toString().trim();
                    int qty = 0;
                    try { qty = Integer.parseInt(qStr); } catch (Exception ignore) {}

                    if (qty <= 0) {
                        Toast.makeText(v.getContext(), "Enter quantity > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int stock = h.boundProduct.getAmount();
                    if (qty > stock) {
                        Toast.makeText(v.getContext(), "Not enough in stock", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Promotion promo = h.boundProduct.getPromotion();
                    int effectivePrice = h.boundProduct.getPrice();
                    if (promo != null && promo.isActive() && promo.getDiscountPercent() > 0) {
                        effectivePrice = effectivePrice - (effectivePrice * promo.getDiscountPercent() / 100);
                    }

                    host.addToCartAtomic(
                            h.boundProduct.getProductKey(),
                            h.boundProduct.getNameProduct(),
                            effectivePrice,
                            qty
                    );
                });

            } else {
                h = (VH) convertView.getTag();
            }

            Products p = data.get(pos);
            h.boundProduct = p;

            int stock = p.getAmount();
            int minProduct = p.getMin_product();

            h.txtName.setText(p.getNameProduct());
            h.txtCategory.setText(p.getCategory());
            h.txtAmount.setText(" " + stock);


            Promotion promo = p.getPromotion();
            if (promo != null && promo.isActive()) {
                if (h.txtPromo != null) {
                    h.txtPromo.setVisibility(View.VISIBLE);
                    if (promo.getDiscountPercent() > 0) {
                        h.txtPromo.setText(promo.getDiscountPercent() + "% OFF!");
                    } else if (promo.getBuyQty() > 0 && promo.getFreeQty() > 0) {
                        h.txtPromo.setText(String.format(Locale.getDefault(), "Buy %d Get %d Free!", promo.getBuyQty(), promo.getFreeQty()));
                    } else {
                        h.txtPromo.setVisibility(View.GONE);
                    }
                }

                if (promo.getDiscountPercent() > 0) {
                    int originalPrice = p.getPrice();
                    int discountedPrice = originalPrice - (originalPrice * promo.getDiscountPercent() / 100);
                    h.txtPrice.setText(String.format(Locale.getDefault(), "₪%d (₪%d)", discountedPrice, originalPrice));
                } else {
                    h.txtPrice.setText(String.format(Locale.getDefault(), "₪%d", p.getPrice()));
                }
            } else {
                if (h.txtPromo != null) h.txtPromo.setVisibility(View.GONE);
                h.txtPrice.setText(String.format(Locale.getDefault(), "₪%d", p.getPrice()));
            }

            if (TextUtils.isEmpty(h.etQuantity.getText())) {
                h.etQuantity.setText("1");
            }

            String url = p.getImageUrl();
            if (!TextUtils.isEmpty(url)) {
                Glide.with(parent.getContext())
                        .load(url)
                        .placeholder(R.drawable.image_background)
                        .error(R.drawable.image_background)
                        .into(h.imgProduct);
            } else {
                h.imgProduct.setImageResource(R.drawable.image_background);
            }

            if (stock <= 0) {
                h.badgeLow.setText("Out of stock");
                h.badgeLow.setVisibility(View.VISIBLE);
                h.btnAdd.setEnabled(false);
                h.btnAdd.setAlpha(0.5f);

            } else if (stock <= minProduct) {
                h.badgeLow.setText("Low stock (min " + minProduct + ")");
                h.badgeLow.setVisibility(View.VISIBLE);
                h.btnAdd.setEnabled(true);
                h.btnAdd.setAlpha(1f);

            } else {
                h.badgeLow.setVisibility(View.GONE);
                h.btnAdd.setEnabled(true);
                h.btnAdd.setAlpha(1f);
            }

            return convertView;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) {
            productsRef.removeEventListener(productsListener);
            productsListener = null;
        }
    }
}