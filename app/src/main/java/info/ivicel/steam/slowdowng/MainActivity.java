package info.ivicel.steam.slowdowng;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.List;
import okhttp3.Response;

import static android.text.TextUtils.isEmpty;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SERVER_UPDATE_CODE = 1;
    public static final int MESSAGE_LOGIN_FAILED = 1;
    public static final int MESSAGE_NEED_AUTHCODE = 2;
    private SecuritySharedPreference spref;
    private EditText userEditText;
    private EditText passwordEditText;
    private CheckBox checkBox;
    private List<Server> mServers;
    private SQLiteDatabase db;
    private ArrayAdapter<Server> adapter;
    private Server mCurrentServer;
    private int mPosition;
    private SocketController mController;
    private LoginConnectListener mLoginConnectListener = new LoginConnectListener();
    private Handler mHandler;
    private ProgressBar mProgressBar;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_server:
                addNewServer();
                break;
            case R.id.edit_server:
                if (mCurrentServer == null) {
                    addNewServer();
                } else {
                    updateServer();
                }
                break;
            case R.id.delete_server:
                deleteServer();
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        userEditText = (EditText)findViewById(R.id.username);
        passwordEditText = (EditText)findViewById(R.id.password);
        checkBox = (CheckBox)findViewById(R.id.remember_me);
        Button mainButton = (Button)findViewById(R.id.main_button);
        mProgressBar = (ProgressBar)findViewById(R.id.progressbar);
        spref = new SecuritySharedPreference(this, "user_info", Context.MODE_PRIVATE);
        
        
        String username = spref.getString("username", null);
        String password = spref.getString("password", null);
        if (username != null) {
            userEditText.setText(username);
        }
        if (password != null) {
            passwordEditText.setText(password);
            checkBox.setChecked(true);
        }
    
        initServerList();
        mainButton.setOnClickListener(new LoginClickListener());
        mController = SocketController.getInstance();
        mController.setOnLoginListener(mLoginConnectListener);
        mController.setOnConnectFailure(new SocketController.OnConnectFailure() {
            @Override
            public void onConnectFailure(Throwable t, Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                Toast.makeText(MainActivity.this, "connection failed", Toast.LENGTH_SHORT).show();
        
                    }
                });
            }
        });
        
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                dimissProgress();
                if (msg.what == MESSAGE_LOGIN_FAILED) {
                    Toast.makeText(MainActivity.this, "Login failed, " + msg.obj,
                            Toast.LENGTH_SHORT).show();
                } else if (msg.what == MESSAGE_NEED_AUTHCODE) {
                    showAuthCodeDialog();
                }
                return true;
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SERVER_UPDATE_CODE:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    String address = data.getStringExtra("address");
                    if (data.getBooleanExtra("new", false)) {
                        Server server = new Server();
                        server.setName(name);
                        server.setAddress(address);
                        server = DatabaseUtils.insertServer(db, server);
                        if (server != null) {
                            mServers.add(server);
                        }
                    } else {
                        Server server = mServers.get(mPosition);
                        server.setName(name);
                        server.setAddress(address);
                        mCurrentServer = server;
                        DatabaseUtils.updateServer(db, mCurrentServer);
                    }
                    adapter.notifyDataSetChanged();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private void addNewServer() {
        Intent intent = new Intent(this, ServerEditActivity.class);
        intent.putExtra("new", true);
        startActivityForResult(intent, SERVER_UPDATE_CODE);
    }
    
    private void updateServer() {
        Intent i = new Intent(this, ServerEditActivity.class);
        i.putExtra("name", mCurrentServer.getName());
        i.putExtra("address", mCurrentServer.getAddress());
        startActivityForResult(i, SERVER_UPDATE_CODE);
    }
    
    private void deleteServer() {
        if (mCurrentServer == null) {
            return;
        }
        DatabaseUtils.removeServer(db, mCurrentServer);
        mServers.remove(mPosition);
        try {
            mCurrentServer = mServers.get(mPosition);
        } catch (IndexOutOfBoundsException e) {
            if (mServers.size() > 0) {
                mPosition = mServers.size() - 1;
                mCurrentServer = mServers.get(mPosition);
            } else {
                mCurrentServer = null;
            }
        }
        if (mCurrentServer != null) {
            saveCurrentServer();
        }
        adapter.notifyDataSetChanged();
    }
    
    private void initServerList() {
        ServerDatabase database = new ServerDatabase(this, "db.sqlite", null, 1);
        db = database.getWritableDatabase();
        mServers = DatabaseUtils.queryAllServer(db);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mServers);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        Spinner serverSpinner = (Spinner)findViewById(R.id.server_name);
        serverSpinner.setAdapter(adapter);
        serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentServer = mServers.get(position);
                mPosition = position;
                saveCurrentServer();
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
        resumeCurrentServer(serverSpinner);
    }
    
    private class LoginClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String username = userEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            boolean isRememberMe = checkBox.isChecked();
            
            if (isEmpty(username)) {
                Toast.makeText(MainActivity.this, R.string.username_empty,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isEmpty(password)) {
                Toast.makeText(MainActivity.this, R.string.password_empty,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences.Editor editor = spref.edit();
            if (isRememberMe) {
                editor.putString("username", username);
                editor.putString("password", password);
                editor.apply();
            } else {
                editor.clear();
            }
            if (mCurrentServer != null) {
                if (!mController.isConnected()) {
                    mLoginConnectListener.setUsername(username);
                    mLoginConnectListener.setPassword(password);
                    mController.initConnection(mCurrentServer.getAddress());
                    showProgress();
                    Log.d(TAG, "onClick: initial connection " + mCurrentServer.getAddress());
                }
            } else {
                Toast.makeText(MainActivity.this, "empty server", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private class LoginConnectListener implements SocketController.OnLoginListener {
        private String username;
        private String password;
        
        @Override
        public void onConnect(JSONObject object) {
            mController.login(username, password);
        }
    
        @Override
        public void onLogin(final JSONObject object) {
            String result = object.optString("result");
            if ("failed".equalsIgnoreCase(result)) {
                Log.d(TAG, "onLogin: failed");
                Message msg = mHandler.obtainMessage(1);
                msg.obj = object.optString("message");
                msg.sendToTarget();
                mController.close();
            } else if ("success".equalsIgnoreCase(result)) {
                Intent intent = new Intent(MainActivity.this, RedeemCodeActivity.class);
                startActivity(intent);
                finish();
            }
        }
    
        @Override
        public void onAuthCode(JSONObject object) {
            Message msg = mHandler.obtainMessage(MESSAGE_NEED_AUTHCODE);
            msg.sendToTarget();
            Log.d(TAG, "onAuthCode: ");
        }
    
        public void setUsername(String username) {
            this.username = username;
        }
    
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    private void showAuthCodeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Please input your Steam Guard Authcode")
                .setView(R.layout.authcode)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog alertDialog = (AlertDialog)dialog;
                        EditText authcodeText = (EditText)alertDialog.findViewById(R.id.authcode);
                        String authcode = authcodeText.getText().toString().trim();
                        if (isEmpty(authcode)) {
                            Toast.makeText(MainActivity.this, "you should input authcode",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            mController.sendAuthCode(authcode);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mController.close();
                        Toast.makeText(MainActivity.this, "Login failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false);
        builder.show();
    }
    
    private void saveCurrentServer() {
        SharedPreferences pref = getSharedPreferences("current_server", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("server_address", mCurrentServer.getAddress());
        editor.putInt("server_position", mPosition);
        editor.apply();
    }
    
    private void resumeCurrentServer(Spinner spinner) {
        SharedPreferences pref = getSharedPreferences("current_server", MODE_PRIVATE);
        mPosition = pref.getInt("server_position", -1);
        try {
            mCurrentServer = mServers.get(mPosition);
            spinner.setSelection(mPosition);
        } catch (IndexOutOfBoundsException e) {
            // pass
        }
    }
    
    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
    }
    
    private void dimissProgress() {
        mProgressBar.setVisibility(View.GONE);
    }
}
