package fr.ensimag.eazyboss;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.vision.barcode.Barcode;


public class MainActivity extends AppCompatActivity {
    private Button scanEtu;
    private Button scanProf;
    private Button scanCarte;

    private View.OnClickListener scanner = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // TODO : lancer le scan
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
                    scanEtu.setText(barcode.displayValue);
                } else {
                    scanEtu.setText("Aucun barcode detecté !");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanEtu = findViewById(R.id.button_scan_etu);
        scanProf = findViewById(R.id.button_scan_prof);
        scanCarte = findViewById(R.id.button_scan_carte);

        scanEtu.setOnClickListener(scanner);
        scanProf.setOnClickListener(scanner);
        scanCarte.setOnClickListener(scanner);
    }
}
