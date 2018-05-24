package fr.ensimag.eazyboss;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private boolean etuOk = false;
    private boolean profOk = false;
    private boolean carteOk = false;
    private Button scanEtu;
    private Button scanProf;
    private Button scanCarte;
    private TextView resultEtu;
    private TextView resultProf;
    private TextView resultCarte;
    private Button send;
    private Button currentButton;
    private RadioButton empruntButton;
    private RadioButton retourButton;
    private RequestQueue queue;
    private ProgressBar spinner;
    private RelativeLayout choixDuree;
    private RadioGroup radioType;
    private RadioGroup radioDuree;
    private String tokenProf;
    private String loginProf;
    private String codeProf;
    private String selectedDuration;
    private Spinner durationSpinner;
    public static final String TAG = "cancel"; // We will use this tag to cancel our request
    final private String URL = "https://eazyboss.herokuapp.com/";
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
                String result = data.getStringExtra("barcode");
                // Here we get "zyboX" and we need to get X
                String value = result.substring(4);
                /*
                 * We display the value of the barcode in the current button
                 * unless for professor
                 * We also need to set the boolean value associated to the Button to true
                 */
                if (currentButton == scanEtu) {
                    resultEtu.setText(result);
                    etuOk = true;
                } else if (currentButton == scanProf) {
                    // call the authenticate function
                    codeProf = result;
                    authenticate(result);
                } else {
                    resultCarte.setText(value);
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
        resultCarte = findViewById(R.id.result_carte);
        resultEtu = findViewById(R.id.result_etu);
        resultProf = findViewById(R.id.result_prof);

        spinner=findViewById(R.id.progress_bar);
        spinner.setVisibility(View.GONE);

        choixDuree = findViewById(R.id.choix_duree);
        choixDuree.setVisibility(View.GONE);

        radioType = findViewById(R.id.radio_type);
        radioType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(findViewById(i).equals(empruntButton)) {
                    choixDuree.setVisibility(View.VISIBLE);
                } else {
                    choixDuree.setVisibility(View.GONE);
                }

            }
        });

        scanEtu.setOnClickListener(scanner);
        scanProf.setOnClickListener(scanner);
        scanCarte.setOnClickListener(scanner);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(empruntButton.isChecked() || retourButton.isChecked())) {
                    Toast.makeText(getApplicationContext(),
                            "Veuillez sélectionner un mode !", Toast.LENGTH_SHORT).show();
                } else if (!(etuOk && carteOk && profOk)) {
                    Toast.makeText(getApplicationContext(),
                            "Veuillez effectuer les 3 scans avant d'envoyer la requête !", Toast.LENGTH_SHORT).show();
                } else {
                    spinner.setVisibility(View.VISIBLE);
                    sendingPostRequest();
                }
            }
        });


        durationSpinner = findViewById(R.id.duration_spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.duration_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        durationSpinner.setAdapter(adapter);
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

    private void authenticate(final String codeProf) {
        String url = URL + "secret/appAuthenticate";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            tokenProf = jsonObject.getString("token");
                            loginProf = jsonObject.getString("login");
                            resultProf.setText(loginProf);
                            profOk = true;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Echec lors de l'authentification, veuillez réessayer", Toast.LENGTH_LONG);
                toast.show();
            }
        }) {
            // Here we add the parameters of our POST method : the code we just scanned
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("code", codeProf);
                return params;
            }
        };

        stringRequest.setTag(TAG);

        // Adding the request to queue
        queue.add(stringRequest);
    }

    /**
     * Send a request to the server using POST method
     * this request contains information on the student, the zybo card and the professor
     * We're using Volley
     */
    private void sendingPostRequest() {
        String url = URL + "secret/ajout";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        spinner.setVisibility(View.GONE);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Envoi effectué avec succès", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Manage the error
                spinner.setVisibility(View.GONE);
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Echec de la requête http", Toast.LENGTH_LONG);
                toast.show();
            }
        }) {
            // Here we add the parameters of our POST method
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("etudiant", resultEtu.getText().toString());
                params.put("carte", resultCarte.getText().toString());
                params.put("prof", codeProf);
                // We add to the parameters teacher's token, used to authenticate him
                params.put("token", tokenProf);
                params.put("duree", durationSpinner.getSelectedItem().toString());
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
