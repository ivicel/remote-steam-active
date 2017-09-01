package info.ivicel.steam.slowdowng;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class ActivationActivity extends AppCompatActivity {
    private static final String TAG = "ActivationActivity";
    private static final int MESSAGE_REDEEM_RESULT = 1;
    private RecyclerView mRecyclerView;
    private List<ActivateResult> mResultList;
    private SocketController mController;
    private Handler mHandler;
    private ActivationAdapter mAdapter;
    private Map<String, String> mResultMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);
        mResultMap.put("NoDetail", "----");
        mResultMap.put("AlreadyPurchased", "已拥有");
        mResultMap.put("DuplicateActivationCode", "重复激活");
        mResultMap.put("BadActivationCode", "无效激活码");
        mResultMap.put("RateLimited", "次数上限");
        mResultMap.put("DoesNotOwnRequiredApp", "缺少主游戏");
        mResultMap.put("RestrictedCountry", "区域限制");
        mResultList = new ArrayList<>();
        mController = SocketController.getInstance();
        mController.setOnRedeemListener(new OnRedeemListener());
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == MESSAGE_REDEEM_RESULT) {
                    mResultList.add((ActivateResult)msg.obj);
                    mAdapter.notifyItemInserted(mResultList.size() - 1);
                    return true;
                }
                return false;
            }
        });
        mRecyclerView = (RecyclerView)findViewById(R.id.result_recyclerview);
        mAdapter = new ActivationAdapter(mResultList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(layoutManager);

        mController.setOnConnectFailure(new SocketController.OnConnectFailure() {
            @Override
            public void onConnectFailure(Throwable t, Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ActivationActivity.this, "connection failure",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    class ActivationAdapter extends RecyclerView.Adapter<ActivationAdapter.ViewHolder> {
        private List<ActivateResult> mResultList;
        private Context mContext;

        public ActivationAdapter(List<ActivateResult> resultList) {
            mResultList = resultList;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView activateKey;
            private TextView activateResult;
            private TextView activateDetail;
            private TextView subTextView;
            private LinearLayout mSubDetails;
            private View view4;

            public ViewHolder(View itemView) {
                super(itemView);
                activateKey = (TextView)itemView.findViewById(R.id.activate_item_key);
                activateResult = (TextView)itemView.findViewById(R.id.activate_item_result);
                activateDetail = (TextView)itemView.findViewById(R.id.activate_item_detail);
                subTextView = (TextView)itemView.findViewById(R.id.sub_text);
                mSubDetails = (LinearLayout)itemView.findViewById(R.id.sub_details);
                view4 = itemView.findViewById(R.id.view4);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ActivateResult activateResult = mResultList.get(position);
            holder.activateKey.setText(activateResult.getKey());
            holder.activateResult.setText(activateResult.getResult());
            holder.activateDetail.setText(mResultMap.get(activateResult.getDetails()));

            if (activateResult.getPackages().size() > 0) {
                holder.subTextView.setVisibility(View.VISIBLE);
                holder.view4.setVisibility(View.VISIBLE);
                holder.mSubDetails.setVisibility(View.VISIBLE);
                for (final ActivateResult.PackageDetail packageDetail : activateResult.getPackages()) {
                    View view = LayoutInflater.from(mContext).inflate(R.layout.sub_list_item,
                            holder.mSubDetails, false);
                    TextView subidTextView = (TextView)view.findViewById(R.id.activate_item_sub_id);
                    TextView subNameTextView =(TextView)view.findViewById(
                                R.id.activate_item_sub_name);
                    subidTextView.setText(packageDetail.getSubid());
                    subNameTextView.setText(packageDetail.getSubName());
                    subNameTextView.setClickable(true);
                    subNameTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Uri uri = Uri.parse("https://steamdb.info/sub/" +
                                    packageDetail.getSubid());
                            Intent i = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(i);
                        }
                    });
                    holder.mSubDetails.addView(view);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mResultList.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mContext = parent.getContext();
            View view = LayoutInflater.from(mContext).inflate(R.layout.activate_result_item, parent, false);
            return new ViewHolder(view);
        }
    }

    private class OnRedeemListener implements SocketController.OnRedeemListener {
        @Override
        public void onRedeem(JSONObject object) {
            ActivateResult result =  parseJSON(object);
            if (result != null) {
                Message msg = mHandler.obtainMessage(MESSAGE_REDEEM_RESULT);
                msg.obj = result;
                msg.sendToTarget();
            }
        }
    }

    private ActivateResult parseJSON(JSONObject object) {
        try {
            JSONObject detailJsonObject = object.getJSONObject("detail");
            ActivateResult activateResult = new ActivateResult();
            activateResult.setKey(detailJsonObject.getString("key"));
            activateResult.setResult(detailJsonObject.getString("result"));
            activateResult.setDetails(detailJsonObject.getString("details"));
            List<ActivateResult.PackageDetail> packageDetailList = new ArrayList<>();
            JSONObject o = detailJsonObject.getJSONObject("packages");
            Iterator<String> i =  o.keys();
            while (i.hasNext()) {
                String key = i.next();
                ActivateResult.PackageDetail packageDetail = activateResult.new PackageDetail();
                packageDetail.setSubid(key);
                packageDetail.setSubName(o.getString(key));
                packageDetailList.add(packageDetail);
            }
            Log.d(TAG, "parseJSON: size = " + packageDetailList.size());
            activateResult.setPackages(packageDetailList);
            return activateResult;
        } catch (JSONException e) {
            // e.printStackTrace();
        }
        return null;
    }
}
