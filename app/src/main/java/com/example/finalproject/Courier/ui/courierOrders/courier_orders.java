package com.example.finalproject.Courier.ui.courierOrders;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.ui.Orders.Orders;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class courier_orders extends Fragment {

    private ListView listView;
    private ArrayList<Orders> ordersList = new ArrayList<>();
    private DatabaseReference ordersRef;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_courier_orders, container, false);

        listView = view.findViewById(R.id.listCourierOrders);
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        listView.setAdapter(new CourierOrdersAdapter());
        loadOrders();

        return view;
    }

    private void loadOrders() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                ordersList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {

                    String id = s.getKey();

                    String orderDate = s.child("timestamp").getValue(String.class);
                    if (orderDate == null) orderDate = "";

                    String totalAmountStr = "0";
                    Object totalObj = s.child("total").getValue();
                    if (totalObj instanceof Number) {
                        totalAmountStr = String.valueOf(((Number) totalObj).intValue());
                    } else if (totalObj instanceof String) {
                        totalAmountStr = (String) totalObj;
                    }

                    String deliveryDate = s.child("deliveryDate").getValue(String.class);
                    if (deliveryDate == null) deliveryDate = "Not set";

                    String deliveryTime = s.child("deliveryTime").getValue(String.class);
                    if (deliveryTime == null) deliveryTime = "Not set";

                    String status = s.child("status").getValue(String.class);
                    if (status == null) status = "PAID";

                    if ("DELIVERED".equals(status)) {
                        continue;
                    }

                    ordersList.add(
                            new Orders(
                                    id,
                                    orderDate,
                                    totalAmountStr,
                                    deliveryDate,
                                    deliveryTime,
                                    status
                            )
                    );
                }

                ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    class CourierOrdersAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ordersList.size();
        }

        @Override
        public Object getItem(int position) {
            return ordersList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.row_courier_order, parent, false);
            }

            Orders o = ordersList.get(position);

            TextView txtOrderDate = convertView.findViewById(R.id.txtCourierOrderDate);
            TextView txtOrderTotal = convertView.findViewById(R.id.txtCourierOrderTotal);
            TextView txtDeliveryDate = convertView.findViewById(R.id.txtCourierDeliveryDate);
            TextView txtDeliveryTime = convertView.findViewById(R.id.txtCourierDeliveryTime);
            Button btnTakeDelivery = convertView.findViewById(R.id.btnCourierTakeDelivery);

            txtOrderDate.setText("Order date: " + o.getOrderDate());
            txtOrderTotal.setText("Total: ₪" + o.getTotalAmount());
            txtDeliveryDate.setText("Delivery date: " + o.getDeliveryDate());
            txtDeliveryTime.setText("Delivery time: " + o.getDeliveryTime());

            if ("SENT_TO_COURIER".equals(o.getStatus())) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date today = sdf.parse(sdf.format(new Date()));
                    Date delivery = sdf.parse(o.getDeliveryDate());

                    if (today != null && delivery != null && today.equals(delivery)) {
                        btnTakeDelivery.setVisibility(View.VISIBLE);
                        btnTakeDelivery.setOnClickListener(v -> {
                            FirebaseDatabase.getInstance()
                                    .getReference("orders")
                                    .child(o.getOrderId())
                                    .child("status")
                                    .setValue("ON_THE_WAY");

                            Toast.makeText(getContext(),
                                    "Delivery started", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        btnTakeDelivery.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    btnTakeDelivery.setVisibility(View.GONE);
                }
            } else {
                btnTakeDelivery.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}