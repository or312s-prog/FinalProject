package com.example.finalproject.ui.Orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class OrdersFragment extends Fragment {

    private ListView ordersListView;
    private ArrayList<Orders> ordersList;
    private OrdersAdapter adapter;
    private DatabaseReference ordersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_orders, container, false);

        // Views
        ordersListView = root.findViewById(R.id.ordersListView);

        // Data
        ordersList = new ArrayList<>();
        adapter = new OrdersAdapter(ordersList);
        ordersListView.setAdapter(adapter);

        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        loadOrdersFromFirebase();

        return root;
    }

    private void loadOrdersFromFirebase() {
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



                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }



    class OrdersAdapter extends BaseAdapter {

        private final ArrayList<Orders> data;
        private final DatabaseReference ordersRef =
                FirebaseDatabase.getInstance().getReference("orders");

        OrdersAdapter(ArrayList<Orders> data) {
            this.data = data;
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_order, parent, false);
            }

            Orders order = data.get(position);

            TextView txtOrderDate = convertView.findViewById(R.id.txtOrderDate);
            TextView txtOrderTotal = convertView.findViewById(R.id.txtOrderTotal);
            TextView txtDeliveryDate = convertView.findViewById(R.id.txtDeliveryDate);
            TextView txtDeliveryTime = convertView.findViewById(R.id.txtDeliveryTime);
            TextView txtStatus = convertView.findViewById(R.id.txtOrderStatus);
            Button btnConfirm = convertView.findViewById(R.id.btnConfirmDelivery);

            txtOrderDate.setText("Order date: " + order.getOrderDate());
            txtOrderTotal.setText("Total: ₪" + order.getTotalAmount());
            txtDeliveryDate.setText("Delivery date: " + order.getDeliveryDate());
            txtDeliveryTime.setText("Delivery time: " + order.getDeliveryTime());
            txtStatus.setText("Status: " + order.getStatus());

            if ("ON_THE_WAY".equals(order.getStatus())) {
                btnConfirm.setVisibility(View.VISIBLE);
                btnConfirm.setOnClickListener(v -> {
                    ordersRef.child(order.getOrderId())
                            .child("status")
                            .setValue("DELIVERED");

                    Toast.makeText(parent.getContext(),
                            "Delivery confirmed ✔",
                            Toast.LENGTH_SHORT).show();
                });
            } else {
                btnConfirm.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

}
