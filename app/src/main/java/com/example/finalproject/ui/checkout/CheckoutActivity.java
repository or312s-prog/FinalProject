package com.example.finalproject.ui.checkout;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.example.finalproject.ui.Payment.PaymentDetailsFragment;

import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private TextView txtTotalPrice;
    private RadioGroup paymentMethodsGroup;
    private Button btnConfirmPayment;
    private double totalPrice = 0.0;

    private View checkoutRoot;
    private View paymentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        txtTotalPrice = findViewById(R.id.txtTotalPrice);
        paymentMethodsGroup = findViewById(R.id.paymentMethodsGroup);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        Button btnBack = findViewById(R.id.btnBack);
        checkoutRoot = findViewById(R.id.checkout_root);
        paymentContainer = findViewById(R.id.payment_container);


        totalPrice = getIntent().getDoubleExtra("total_price", 0.0);
        txtTotalPrice.setText(String.format(Locale.getDefault(), "Total: ₪%.2f", totalPrice));


        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean showingPayment = getSupportFragmentManager().getBackStackEntryCount() > 0;
            checkoutRoot.setVisibility(showingPayment ? View.GONE : View.VISIBLE);
            paymentContainer.setVisibility(showingPayment ? View.VISIBLE : View.GONE);
        });
        btnBack.setOnClickListener(v -> finish());

        btnConfirmPayment.setOnClickListener(v -> {
            int selectedId = paymentMethodsGroup.getCheckedRadioButtonId();
            String paymentMethod = "";

            if (selectedId == R.id.rbCreditCard) {
                paymentMethod = "Visa card";
            }

            if (paymentMethod.isEmpty()) {
                Toast.makeText(this, "Please choose a payment method", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putDouble("totalPrice", totalPrice);
            args.putString("paymentMethod", paymentMethod);

            PaymentDetailsFragment fragment = new PaymentDetailsFragment();
            fragment.setArguments(args);

            checkoutRoot.setVisibility(View.GONE);
            paymentContainer.setVisibility(View.VISIBLE);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.payment_container, fragment)
                    .addToBackStack("payment")
                    .commit();
        });
    }
}
