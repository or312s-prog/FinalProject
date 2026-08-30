package com.example.finalproject.ui.Payment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.navigation.Navigation;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finalproject.R;
import com.example.finalproject.User_Page;

public class OrderConfirmationFragment extends Fragment {

    private TextView txtConfirmationMessage;
    private Button btnGoHome;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_order_confirmation_activity, container, false);

        txtConfirmationMessage = view.findViewById(R.id.txtConfirmationMessage);
        btnGoHome = view.findViewById(R.id.btnGoHome);

        txtConfirmationMessage.setText("The order was placed successfully!");


        btnGoHome.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_home);
        });

        new Handler().postDelayed(() -> {
            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.nav_home);
            }
        }, 3000);

        return view;
    }
}

