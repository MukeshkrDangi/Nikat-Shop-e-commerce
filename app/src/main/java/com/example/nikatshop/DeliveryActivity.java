package com.example.nikatshop;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class DeliveryActivity extends AppCompatActivity {


    public static List<CartItemModel> cartItemModelList;
    private RecyclerView deliveryRecyclerView;
    private Button changeORaddNewAddressBtn;
    public static final int SELECT_ADDRESS = 0;
    private TextView totalAmount;
    private TextView fullname;
    private String name, mobileNo;
    private TextView fullAddress;
    private TextView pincode;
    private Button continueBtn;
    private Dialog loadingDialog;
    private Dialog paymentMethodDialog;
    private ImageButton paytm, cod;
    private ConstraintLayout orderConfirmationLayout;
    private ImageButton continueShoppingBtn;
    private TextView orderId;
    private boolean successResponse = false;
    public static boolean fromCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Delivery");

        deliveryRecyclerView = findViewById(R.id.delivery_recyclerview);
        changeORaddNewAddressBtn = findViewById(R.id.change_or_add_address_btn);
        totalAmount = findViewById(R.id.total_cart_amount);
        fullname = findViewById(R.id.fullname);
        fullAddress = findViewById(R.id.address);
        pincode = findViewById(R.id.pincode);
        continueBtn = findViewById(R.id.cart_continue_btn);
        orderConfirmationLayout = findViewById(R.id.order_confirmation_layout);
        continueShoppingBtn = findViewById(R.id.continue_shopping_btn);
        orderId = findViewById(R.id.order_id);


        ////////////loading dialog
        loadingDialog = new Dialog(DeliveryActivity.this);
        loadingDialog.setContentView(R.layout.loading_progress_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.slider_background));
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ////////////loading dialog


        ////////////payment method dialog
        paymentMethodDialog = new Dialog(DeliveryActivity.this);
        paymentMethodDialog.setContentView(R.layout.payment_method);
        paymentMethodDialog.setCancelable(true);
        paymentMethodDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.slider_background));
        paymentMethodDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paytm = paymentMethodDialog.findViewById(R.id.paytm);
        cod = paymentMethodDialog.findViewById(R.id.cod_btn);
        ////////////loading dialog


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        deliveryRecyclerView.setLayoutManager(layoutManager);


        CartAdapter cartAdapter = new CartAdapter(cartItemModelList, totalAmount, false);
        deliveryRecyclerView.setAdapter(cartAdapter);
        cartAdapter.notifyDataSetChanged();

        changeORaddNewAddressBtn.setVisibility(View.VISIBLE);
        changeORaddNewAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myAddressesIntent = new Intent(DeliveryActivity.this, MyAddressesActivity.class);
                myAddressesIntent.putExtra("MODE", SELECT_ADDRESS);
                startActivity(myAddressesIntent);
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentMethodDialog.show();


            }
        });

        cod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent otpIntent = new Intent(DeliveryActivity.this, OTPverificationActivity.class);
                otpIntent.putExtra("mobileNo",mobileNo.substring(0,10));
                startActivity(otpIntent);
            }
        });

        paytm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentMethodDialog.show();
                loadingDialog.show();
                if (ContextCompat.checkSelfPermission(DeliveryActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DeliveryActivity.this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, 101);
                }

                String M_id = "lVVfpe33481329549741";
                String customer_id = FirebaseAuth.getInstance().getUid();
                String order_id = UUID.randomUUID().toString().substring(0, 28);
                String url = "https://nikatshop23.000webhostapp.com/paytm/generateChecksum.php";
                String callBackUrl = "https://pguat.paytm.com/paytmchecksum/paytmCallback.jsp";

                RequestQueue requestQueue = Volley.newRequestQueue(DeliveryActivity.this);

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.has("CHECKSUMHASH")) {
                                String CHECKSUMHASH = jsonObject.getString("CHECKSUMHASH");

                                PaytmPGService paytmPGService = PaytmPGService.getStagingService(url);
                                HashMap<String, String> paramMap = new HashMap<String, String>();
                                paramMap.put("MID", M_id);
                                paramMap.put("ORDER_ID", order_id);
                                paramMap.put("CUST_ID", customer_id);
                                paramMap.put("MOBILE_NO", "7777777777");
                                paramMap.put("EMAIL", "username@emailprovider.com");
                                paramMap.put("CHANNEL_ID", "WAP");
                                paramMap.put("TXN_AMOUNT", totalAmount.getText().toString().substring(3, totalAmount.getText().length() - 2));
                                paramMap.put("WEBSITE", "WEBSTAGING");
                                paramMap.put("INDUSTRY_TYPE_ID", "Retail");
                                paramMap.put("CALLBACK_URL", callBackUrl);
                                paramMap.put("CHECKSUMHASH", CHECKSUMHASH);

                                PaytmOrder order = new PaytmOrder(paramMap);

                                paytmPGService.initialize(order, null);
                                paytmPGService.startPaymentTransaction(DeliveryActivity.this, true, true, new PaytmPaymentTransactionCallback() {
                                    @Override
                                    public void onTransactionResponse(Bundle inResponse) {
                                        // Toast.makeText(getApplicationContext(), "Payment Transaction response " + inResponse.toString(), Toast.LENGTH_LONG).show();

                                        if (inResponse.getString("STATUS").equals("TXN_SUCCESS")) {

                                            successResponse = true;


                                            if (FireMainActivity.fireMainActivity != null) {
                                                FireMainActivity.fireMainActivity.finish();
                                                FireMainActivity.fireMainActivity = null;
                                                FireMainActivity.showCart = false;
                                            }

                                            if (ProductDetailsActivity.productDetailsActivity != null) {
                                                ProductDetailsActivity.productDetailsActivity.finish();
                                                ProductDetailsActivity.productDetailsActivity = null;
                                            }

                                            if (fromCart) {
                                                loadingDialog.show();

                                                Map<String, Object> updateCartList = new HashMap<>();
                                                long cartListSize = 0;
                                                List<Integer> indexList = new ArrayList<>();
                                                for (int x = 0; x < DBqueries.cartList.size(); x++) {
                                                    if (!cartItemModelList.get(x).isInStock()) {
                                                        updateCartList.put("product_ID_" + cartListSize, cartItemModelList.get(x).getProductID());
                                                        cartListSize++;
                                                    } else {
                                                        indexList.add(x);
                                                    }
                                                }
                                                updateCartList.put("list_size", cartListSize);

                                                FirebaseFirestore.getInstance().collection("USERS").document(FirebaseAuth.getInstance().getUid()).collection("USER_DATA").document("MY_CART")
                                                        .set(updateCartList).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            for (int x = 0; x < indexList.size(); x++) {
                                                                DBqueries.cartList.remove(indexList.get(x).intValue());
                                                                DBqueries.cartItemModelList.remove(indexList.get(x).intValue());
                                                                DBqueries.cartItemModelList.remove(DBqueries.cartItemModelList.size() - 1);
                                                            }

                                                        } else {
                                                            String error = task.getException().getMessage();
                                                            Toast.makeText(DeliveryActivity.this, "error", Toast.LENGTH_SHORT).show();
                                                        }
                                                        loadingDialog.dismiss();
                                                    }
                                                });

                                            }

                                            continueBtn.setEnabled(false);
                                            changeORaddNewAddressBtn.setEnabled(false);
                                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                                            orderId.setText("Order ID " + inResponse.getString("ORDERID"));
                                            orderConfirmationLayout.setVisibility(View.VISIBLE);
                                            continueShoppingBtn.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    finish();
                                                }
                                            });


                                        }

                                    }

                                    @Override
                                    public void networkNotAvailable() {

                                        Toast.makeText(getApplicationContext(), "Network connection error: Check your internet connectivity", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void clientAuthenticationFailed(String inErrorMessage) {
                                        Toast.makeText(getApplicationContext(), "Authentication failed: Server error" + inErrorMessage.toString(), Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void someUIErrorOccurred(String inErrorMessage) {
                                        Toast.makeText(getApplicationContext(), "UI Error " + inErrorMessage, Toast.LENGTH_LONG).show();


                                    }

                                    @Override
                                    public void onErrorLoadingWebPage(int iniErrorCode, String inErrorMessage, String inFailingUrl) {
                                        Toast.makeText(getApplicationContext(), "Unable to load webpage " + inErrorMessage.toString(), Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onBackPressedCancelTransaction() {
                                        Toast.makeText(getApplicationContext(), "Transaction cancelled", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onTransactionCancel(String inErrorMessage, Bundle inResponse) {
                                        Toast.makeText(getApplicationContext(), "Transaction Cancelled " + inResponse.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loadingDialog.dismiss();
                        Toast.makeText(DeliveryActivity.this, "something went worng", Toast.LENGTH_SHORT).show();
                    }
                }) {
                    @Nullable
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> paramMap = new HashMap<String, String>();
                        paramMap.put("MID", M_id);
                        paramMap.put("ORDER_ID", order_id);
                        paramMap.put("CUST_ID", customer_id);
                        paramMap.put("MOBILE_NO", "7777777777");
                        paramMap.put("EMAIL", "username@emailprovider.com");
                        paramMap.put("CHANNEL_ID", "WAP");
                        paramMap.put("TXN_AMOUNT", totalAmount.getText().toString().substring(3, totalAmount.getText().length() - 2));
                        paramMap.put("WEBSITE", "WEBSTAGING");
                        paramMap.put("INDUSTRY_TYPE_ID", "Retail");
                        paramMap.put("CALLBACK_URL", callBackUrl);
                        return paramMap;
                    }
                };

                requestQueue.add(stringRequest);

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        name = DBqueries.addressesModelList.get(DBqueries.selectedAddress).getFullName();
        mobileNo = DBqueries.addressesModelList.get(DBqueries.selectedAddress).getMobileNo();
        fullname.setText(name + " - " + mobileNo);
        fullAddress.setText(DBqueries.addressesModelList.get(DBqueries.selectedAddress).getAddress());
        pincode.setText(DBqueries.addressesModelList.get(DBqueries.selectedAddress).getPincode());

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        ///noabva
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        loadingDialog.dismiss();
    }

    @Override
    public void onBackPressed() {
        if (successResponse) {
            finish();
            return;
        }
        super.onBackPressed();
    }
}