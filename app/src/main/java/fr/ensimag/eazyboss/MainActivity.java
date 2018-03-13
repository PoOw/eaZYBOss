package fr.ensimag.eazyboss;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    Button scanEtu;
    Button scanProf;
    Button scanCarte;
    Button send;
    Button currentButton;
    RequestQueue queue;
    public static final String TAG = "cancel"; // We will use this tag to cancel our request

    private View.OnClickListener scanner = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // currentButton is updated
            currentButton = (Button)view;
            Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
            startActivityForResult(intent, 0); // We launch the scan
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // We get the barcode that the scan activity sent
                    Barcode barcode = data.getParcelableExtra("barcode");
                    // We display the value of the barcode in the current button
                    currentButton.setText(barcode.displayValue);
                } else {
                    currentButton.setText("Aucun barcode detecté !");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(getApplicationContext());
        scanEtu = findViewById(R.id.button_scan_etu);
        scanProf = findViewById(R.id.button_scan_prof);
        scanCarte = findViewById(R.id.button_scan_carte);
        send = findViewById(R.id.button_send);

        scanEtu.setOnClickListener(scanner);
        scanProf.setOnClickListener(scanner);
        scanCarte.setOnClickListener(scanner);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Vérifier que toutes les infos sont présentes
                sendingPostRequest();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // We want to cancel all our http request
        if (queue != null) {
            queue.cancelAll(TAG);
        }
    }

    /**
     * Send a request to the server using POST method
     * this request contains information on the student, the zybo card and the professor
     * We're using Volley
     */
    private void sendingPostRequest() {
        String url = "https://eazyboss.glitch.me/";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Envoi effectué avec succès", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Manage the error
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Echec de la requête http", Toast.LENGTH_LONG);
                toast.show();
            }
        }) {
            // Here we add the parameters of our POST method
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("etudiant", scanEtu.getText().toString());
                params.put("carte", scanCarte.getText().toString());
                params.put("prof", scanProf.getText().toString());
                return params;
            }
        };

        stringRequest.setTag(TAG);

        // Adding the request to queue
        queue.add(stringRequest);
    }
}
