package info.ivicel.steam.slowdowng;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static android.text.TextUtils.isEmpty;

public class ServerEditActivity extends AppCompatActivity {
    private Boolean isNew;
    private EditText mNameEditText;
    private EditText mAddressEditText;
    private Button mSaveButton;
    private Button mDiscardButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_edit);
    
        mNameEditText = (EditText)findViewById(R.id.edit_server_name);
        mAddressEditText = (EditText)findViewById(R.id.edit_server_address);
        mSaveButton = (Button)findViewById(R.id.save_server);
        mDiscardButton = (Button)findViewById(R.id.discard_server);
        Intent intent = getIntent();
        isNew = intent.getBooleanExtra("new", false);
    
        
        if (!isNew) {
            mNameEditText.setText(intent.getStringExtra("name"));
            mAddressEditText.setText(intent.getStringExtra("address"));
        }
        
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mNameEditText.getText().toString();
                String address = mAddressEditText.getText().toString();
                if (!checkInputServer(name, address)) {
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra("new", isNew);
                intent.putExtra("name", name);
                intent.putExtra("address", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        
        mDiscardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        setResult(RESULT_CANCELED);
    }
    
    private boolean checkInputServer(String name, String address) {
        if (isEmpty(name) || isEmpty(address)) {
            Toast.makeText(this, "Server can't be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!"http".equalsIgnoreCase(address.substring(0, 4)) &&
                !"ws".equalsIgnoreCase(address.substring(0, 2))) {
            Log.d("EditActivity", "checkInputServer: " + address);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setPositiveButton("Ok", null)
                    .setTitle("Server address error")
                    .setMessage("Server address should be http[s]://example.com")
                    .setIcon(android.R.drawable.ic_dialog_alert);
            builder.show();
            return false;
        }
        return true;
    }
}
