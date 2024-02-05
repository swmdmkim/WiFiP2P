package com.kdm.wi_fip2p;

import static com.kdm.wi_fip2p.MainActivity.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
	private final WifiP2pManager mManager;
	private final WifiP2pManager.Channel mChannel;
	private final MainActivity mActivity;

	public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
		super();
		this.mManager = manager;
		this.mChannel = channel;
		this.mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				Toast.makeText(context, "Wi-Fi is ON", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context, "Wi-Fi is OFF", Toast.LENGTH_SHORT).show();
			}
		} else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			if(mManager != null) {
				Log.d(TAG, "onReceive() WIFI_P2P_PEERS_CHANGED_ACTION" );
				mManager.requestPeers(mChannel, mActivity.peerListListener);
			}
		} else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
			if(mManager == null) {
				return;
			}

			NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
			Log.d(TAG, "onReceive() WIFI_P2P_CONNECTION_CHANGED_ACTION connect = " + networkInfo.isConnected());
			if(networkInfo.isConnected()) {
				mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
			} else {
				mActivity.connectionStatus.setText(R.string.str_disconnect);
			}
		} else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
			Log.d(TAG, "onReceive() WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
		}
	}

}
