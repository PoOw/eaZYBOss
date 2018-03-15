package fr.ensimag.eazyboss;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
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
    boolean etuOk = false;
    boolean profOk = false;
    boolean carteOk = false;
    Button scanEtu;
    Button scanProf;
    Button scanCarte;
    Button send;
    Button currentButton;
    RadioButton empruntButton;
    RadioButton retourButton;
    RequestQueue queue;
    public static final String TAG = "cancel"; // We will use this tag to cancel our request
    final private int MY_PERMISSIONS_REQUEST_CAMERA = 42;

    /**
     * OnClickListener for our 3 Buttons
     * Start the scan and wait for the result
     */
    private View.OnClickListener scanner = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // currentButton is updated
            currentButton = (Button)view;
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Access camera permission is not granted, we will ask for it
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
                startActivityForResult(intent, 0); // We launch the scan
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (data != null) {
                // We get the barcode that the scan activity sent
                Barcode barcode = data.getParcelableExtra("barcode");
                // We display the value of the barcode in the current button
                currentButton.setText(barcode.displayValue);
                // We need to set the boolean value associated to the Button to true
                if (currentButton == scanEtu) {
                    etuOk = true;
                } else if (currentButton == scanProf) {
                    profOk = true;
                } else {
                    carteOk = true;
                }
            } else {
                currentButton.setText("Aucun barcode detecté !");
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
        empruntButton = findViewById(R.id.button_emprunt);
        retourButton = findViewById(R.id.button_retour);

        scanEtu.setOnClickListener(scanner);
        scanProf.setOnClickListener(scanner);
        scanCarte.setOnClickListener(scanner);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Vérifier que toutes les infos sont présentes
                if (!(empruntButton.isChecked() || retourButton.isChecked())) {
                    Toast.makeText(getApplicationContext(),
                            "Veuillez sélectionner un mode !", Toast.LENGTH_SHORT).show();
                } else if (!(etuOk && carteOk && profOk)) {
                    Toast.makeText(getApplicationContext(),
                            "Veuillez effectuer les 3 scans avant d'envoyer la requête !", Toast.LENGTH_SHORT).show();
                } else {
                    sendingPostRequest();
                }
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentButton.performClick();
                }
            }
        }
    }

    /**
     * Send a request to the server using POST method
     * this request contains information on the student, the zybo card and the professor
     * We're using Volley
     */
    private void sendingPostRequest() {
        String url = "https://eazyboss.glitch.me/ajout";
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
                if (empruntButton.isChecked()) {
                    params.put("emprunt", "true");
                } else {
                    params.put("emprunt", "false");
                }
                return params;
            }
        };

        stringRequest.setTag(TAG);

        // Adding the request to queue
        queue.add(stringRequest);
    }
}
