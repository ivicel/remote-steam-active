package info.ivicel.steam.slowdowng;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;

import static android.text.TextUtils.isEmpty;

public class RedeemCodeActivity extends AppCompatActivity {
    private SocketController mController;
    private EditText codeEditText;
    private Button mRedeemButton;
    private List<String> mKeyList;
    private Pattern pattern = Pattern.compile("(?:[0-9A-Z]{5}-){2,4}[0-9A-Z]{5}");
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.redeem_code_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.redeem_logout:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem_code);
        
        mKeyList  = new ArrayList<>();
        mController = SocketController.getInstance();
        codeEditText = (EditText)findViewById(R.id.keys);
        mRedeemButton = (Button)findViewById(R.id.redeem_button);
        mRedeemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyText = codeEditText.getText().toString();
                if (isEmpty(keyText)) {
                    Toast.makeText(RedeemCodeActivity.this, "empty keys",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Matcher m = pattern.matcher(keyText);
                while (m.find()) {
                    mKeyList.add(m.group());
                }
                if (mKeyList.size() <= 0) {
                    Toast.makeText(RedeemCodeActivity.this, "keys error",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(RedeemCodeActivity.this, ActivationActivity.class);
                startActivity(i);
                mController.redeemCode(mKeyList);
                
            }
        });
    
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mKeyList.clear();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mController != null) {
            mController.close();
        }
    }
}
