package com.example.finalproject.ui.Payment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.User_Page;
import com.example.finalproject.ui.Cart.CartItem;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentDetailsFragment extends Fragment {

    // UI
    private EditText edtName, edtAddress, edtPhone, edtCardNumber, edtExpiry, edtCVV;
    private TextView txtTotalPrice;
    private Button btnFinishOrder, btnBackToCart;

    // Order data
    private double totalPrice = 0.0;
    private String paymentMethod = "Visa card";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_payment_details_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtName = view.findViewById(R.id.edtName);
        edtAddress = view.findViewById(R.id.edtAddress);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtCardNumber = view.findViewById(R.id.edtCardNumber);
        edtExpiry = view.findViewById(R.id.edtExpiry);
        edtCVV = view.findViewById(R.id.edtCVV);
        txtTotalPrice = view.findViewById(R.id.txtTotalPrice);
        btnFinishOrder = view.findViewById(R.id.btnFinishOrder);
        btnBackToCart = view.findViewById(R.id.btnBackToCart);

        if (getArguments() != null) {
            totalPrice = getArguments().getDouble("totalPrice", 0.0);
            paymentMethod = getArguments().getString("paymentMethod", "Visa card");
        }

        txtTotalPrice.setText(String.format(Locale.getDefault(), "Total: ₪%.2f", totalPrice));

        btnBackToCart.setOnClickListener(v -> requireActivity().finish());

        btnFinishOrder.setOnClickListener(v -> {
            if (validateInputs()) {
                fetchCartThenPlaceOrder();
            }
        });
    }

    private boolean validateInputs() {

        if (TextUtils.isEmpty(edtName.getText())) {
            edtName.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(edtAddress.getText())) {
            edtAddress.setError("Required");
            return false;
        }
        if (!Patterns.PHONE.matcher(edtPhone.getText()).matches()) {
            edtPhone.setError("Invalid phone");
            return false;
        }
        if (!edtCardNumber.getText().toString().matches("\\d{16}")) {
            edtCardNumber.setError("16 digits required");
            return false;
        }
        if (!edtExpiry.getText().toString().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            edtExpiry.setError("MM/YY");
            return false;
        }
        if (!edtCVV.getText().toString().matches("\\d{3}")) {
            edtCVV.setError("3 digits");
            return false;
        }
        return true;
    }


    private void fetchCartThenPlaceOrder() {

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        root.child("cart").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                ArrayList<CartItem> items = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);

                    if (item != null && item.getQuantity() > 0) {
                        // Ensure productKey is set (fallback to node key)
                        if (item.getProductKey() == null) {
                            item.setProductKey(ds.getKey());
                        }
                        items.add(item);
                    }
                }

                if (items.isEmpty()) {
                    Toast.makeText(getContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveOrderThenUpdateAnalyticsThenClearCart(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to read cart", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void saveOrderThenUpdateAnalyticsThenClearCart(List<CartItem> items) {

        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        String orderId = ordersRef.push().getKey();
        if (orderId == null) {
            Toast.makeText(getContext(), "Failed to create order", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Order order = new Order(
                edtName.getText().toString().trim(),
                edtAddress.getText().toString().trim(),
                edtPhone.getText().toString().trim(),
                String.format(Locale.getDefault(), "%.2f", totalPrice),
                paymentMethod,
                maskCard(),
                edtExpiry.getText().toString(),
                date,
                items
        );

        ordersRef.child(orderId).setValue(order)
                .addOnSuccessListener(unused -> {

                    updateAnalyticsAfterOrder(items, totalPrice, () -> {


                        FirebaseDatabase db = FirebaseDatabase.getInstance();
                        db.getReference("cart").removeValue();
                        db.getReference("cart_total").child("amount").setValue(0);

                        Toast.makeText(getContext(),
                                "Order placed successfully!",
                                Toast.LENGTH_LONG).show();

                        goHomeAfterPayment();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to save order",
                                Toast.LENGTH_SHORT).show()
                );
    }


    private void updateAnalyticsAfterOrder(List<CartItem> items, double orderTotal, Runnable onDone) {

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        DatabaseReference analyticsRef = root.child("analytics");
        DatabaseReference productsRef = root.child("products");

        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        String yearKey  = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());

        Map<String, Integer> priceByProductKey = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(items.size());

        for (CartItem item : items) {

            String productKey = item.getProductKey();
            if (productKey == null) {
                if (pending.decrementAndGet() == 0) {
                    performAnalyticsWrites(analyticsRef, monthKey, yearKey, items, priceByProductKey, onDone);
                }
                continue;
            }

            productsRef.child(productKey).child("price")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer price = snapshot.getValue(Integer.class);
                            if (price == null) price = 0;
                            priceByProductKey.put(productKey, price);

                            if (pending.decrementAndGet() == 0) {
                                performAnalyticsWrites(analyticsRef, monthKey, yearKey, items, priceByProductKey, onDone);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // If price fetch fails, default to 0 for that product and continue
                            priceByProductKey.put(productKey, 0);

                            if (pending.decrementAndGet() == 0) {
                                performAnalyticsWrites(analyticsRef, monthKey, yearKey, items, priceByProductKey, onDone);
                            }
                        }
                    });
        }
    }


    private void performAnalyticsWrites(DatabaseReference analyticsRef,
                                        String monthKey,
                                        String yearKey,
                                        List<CartItem> items,
                                        Map<String, Integer> priceByProductKey,
                                        Runnable onDone) {


        incrementLong(analyticsRef.child("sales_by_month").child(monthKey).child("orders_count"), 1);
        incrementLong(analyticsRef.child("sales_by_year").child(yearKey).child("orders_count"), 1);

        for (CartItem item : items) {

            String productKey = item.getProductKey();
            if (productKey == null) continue;

            int qty = item.getQuantity();
            if (qty <= 0) continue;

            int price = priceByProductKey.containsKey(productKey) ? priceByProductKey.get(productKey) : 0;
            long revenue = (long) qty * (long) price;

            DatabaseReference monthProductRef = analyticsRef.child("sales_by_month")
                    .child(monthKey)
                    .child("products")
                    .child(productKey);

            DatabaseReference yearProductRef = analyticsRef.child("sales_by_year")
                    .child(yearKey)
                    .child("products")
                    .child(productKey);

            incrementLong(monthProductRef.child("qty_sold"), qty);
            incrementLong(monthProductRef.child("Totalsum"), revenue);

            incrementLong(yearProductRef.child("qty_sold"), qty);
            incrementLong(yearProductRef.child("Totalsum"), revenue);
        }

        if (onDone != null) onDone.run();
    }

    private void incrementLong(DatabaseReference ref, long delta) {
        ref.runTransaction(new Transaction.Handler() {

            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long current = currentData.getValue(Long.class);
                if (current == null) current = 0L;
                currentData.setValue(current + delta);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {

            }
        });
    }


    private void goHomeAfterPayment() {
        Intent intent = new Intent(requireActivity(), User_Page.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        requireActivity().finish();
    }


    private String maskCard() {
        String num = edtCardNumber.getText().toString();
        return "**** **** **** " + num.substring(12);
    }


    public static class Order {
        public String name, address, phone, total, paymentMethod,
                cardNumber, expiry, timestamp;
        public List<CartItem> items;

        public Order() {}

        public Order(String name, String address, String phone,
                     String total, String paymentMethod,
                     String cardNumber, String expiry,
                     String timestamp, List<CartItem> items) {

            this.name = name;
            this.address = address;
            this.phone = phone;
            this.total = total;
            this.paymentMethod = paymentMethod;
            this.cardNumber = cardNumber;
            this.expiry = expiry;
            this.timestamp = timestamp;
            this.items = items;
        }
    }
}
