package com.kdm.wi_fip2p;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

	public static final String TAG = "Wi-Fi Direct";
	Button btnOnOff, btnDiscover, btnSend;
	ListView listView;
	TextView read_msg_box, connectionStatus;
	EditText writeMsg;

	WifiManager wifiManager;
	private WifiP2pManager wifiP2pManager;
	private WifiP2pManager.Channel channel;
	private BroadcastReceiver mReceiver;
	private final IntentFilter mIntentFilter = new IntentFilter();;

	List<WifiP2pDevice> peers = new ArrayList<>();
	String[] deviceNameArray;
	WifiP2pDevice[] deviceArray;

	ServerClass serverClass;
	ClientClass clientClass;
	SendReceive sendReceive;
	Socket socket;

	static final int MESSAGE_READ = 1;
	private final int SOCKET_PORT = 5001;
	private final static int SOCKET_TIMEOUT = 500;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		addIntentFilter();
		initialize();
		initButtonListener();
	}

	private void initButtonListener() {
		btnOnOff.setOnClickListener(view -> {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
				if(wifiManager.isWifiEnabled()) {
					wifiManager.setWifiEnabled(false);
					btnOnOff.setText(R.string.str_wifi_off);
				} else {
					wifiManager.setWifiEnabled(true);
					btnOnOff.setText(R.string.str_wifi_on);
				}
			} else {
//				startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS),1);
				startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI),1);
			}

		});

		btnDiscover.setOnClickListener(view -> wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "onSuccess");
				connectionStatus.setText(R.string.str_discovery_start);
			}

			@Override
			public void onFailure(int reasonCode) {
				connectionStatus.setText(R.string.str_discovery_fail + " : " + reasonCode);
			}
		}));

		listView.setOnItemClickListener((adapterView, view, i, l) -> {
			final WifiP2pDevice device = deviceArray[i];
			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = device.deviceAddress;

			wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
				@Override
				public void onSuccess() {
					connectionStatus.setText(R.string.str_connect_to);// + device.deviceName);
				}

				@Override
				public void onFailure(int i) {
					connectionStatus.setText(R.string.str_connect_not);
				}
			});
		});

		btnSend.setOnClickListener(view -> {
			String msg = writeMsg.getText().toString();
//			sendReceive.write(msg);
			new Thread(() -> send(msg)).start();
		});
	}

	public void send(String data) {
		try {
			Log.d(TAG, "Send data = " + data );
			OutputStream outputStream = socket.getOutputStream();
			byte[] bytes = data.getBytes();
			outputStream.write(bytes);
			outputStream.flush();
		} catch (Exception e) {
			Log.d(TAG, "send  e = " + e.getMessage() );
			e.printStackTrace();
		}

	}

	private void initialize() {
		btnOnOff = findViewById(R.id.onOff);
		btnDiscover = findViewById(R.id.discover);
		btnSend = findViewById(R.id.sendButton);

		listView = findViewById(R.id.peerListView);
		read_msg_box = findViewById(R.id.readMsg);
		connectionStatus = findViewById(R.id.connectionStatus);
		writeMsg = findViewById(R.id.writeMsg);

		wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = wifiP2pManager.initialize(this, getMainLooper(), null);

		mReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this);
	}

	private void checkPermission() {
		ActivityCompat.requestPermissions(MainActivity.this, new String[]{
				android.Manifest.permission.ACCESS_COARSE_LOCATION,
				android.Manifest.permission.ACCESS_FINE_LOCATION,
		}, 0);
	}

	@Override
	protected void onActivityResult(int reqCode, int resultCode, Intent intent) {
		super.onActivityResult(reqCode,resultCode, intent);
		Log.d(TAG, "onActivityResult  reqCode = " + reqCode + " , resultCode = " + resultCode);
		if(reqCode == 1) {
			if(wifiManager.isWifiEnabled()) {
				btnOnOff.setText(R.string.str_wifi_off);
				checkPermission();
			} else {
				btnOnOff.setText(R.string.str_wifi_on);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 0) {
			for(int i = 0; i < permissions.length; i++){
				if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
					Log.d(TAG, "Permission " + permissions[i] + " granted");
				}else{
					Log.d(TAG, "Permission " + permissions[i] + " denied");
				}
			}
		}
	}

	private void addIntentFilter() {
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	public WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
		@Override
		public void onPeersAvailable(WifiP2pDeviceList peerList) {
			if(!peerList.getDeviceList().equals(peers)) {
				peers.clear();
				peers.addAll(peerList.getDeviceList());

				deviceNameArray = new String[peerList.getDeviceList().size()];
				deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];

				int idx = 0;
				for(WifiP2pDevice device: peerList.getDeviceList()) {
					deviceNameArray[idx] = device.deviceName;
					Log.d(TAG, "deviceNameArray[" + idx + "] = " + deviceNameArray[idx] );
					deviceArray[idx] = device;
					idx++;
				}
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
			listView.setAdapter(adapter);

			if(peers.size() == 0) {
				Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
			}
		}
	};

	public WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
			final InetAddress groupOwnerAddr = wifiP2pInfo.groupOwnerAddress;

			if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
				connectionStatus.setText(R.string.str_host);
				serverClass = new ServerClass();
				serverClass.start();
			} else if (wifiP2pInfo.groupFormed) {
				connectionStatus.setText(R.string.str_client);
				clientClass = new ClientClass(groupOwnerAddr);
				clientClass.start();
			}
		}
	};

	public class ServerClass extends Thread {
		ServerSocket serverSocket;

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(SOCKET_PORT);
				socket = serverSocket.accept();
				Log.d(TAG, "ServerClass addr= " + socket.getLocalAddress() + " ." + socket.getPort());
//				sendReceive = new SendReceive(socket);
//				sendReceive.start();
			} catch (Exception e) {
				Log.d(TAG, "ServerClass Error");
				e.printStackTrace();
			}
		}
	}

	public class ClientClass extends Thread {
		Socket socket;
		String hostAdd;

		public ClientClass(InetAddress hostAddress) {
			hostAdd = hostAddress.getHostAddress();
			socket = new Socket();
			Log.d(TAG, "ClientClass hostAdd = " + hostAdd);
		}

		@Override
		public void run() {
			try {
				socket.connect(new InetSocketAddress(hostAdd, SOCKET_PORT), SOCKET_TIMEOUT);
				sendReceive = new SendReceive(socket);
				sendReceive.start();
				Log.d(TAG, "ClientClass !!!!!!");
			} catch (IOException e) {
				Log.d(TAG, "ClientClass Error");
				e.printStackTrace();
			}
		}
	}

	Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(@NonNull Message msg) {
			if (msg.what == MESSAGE_READ) {
				byte[] readBuff = (byte[]) msg.obj;
				String tmpMsg = new String(readBuff, 0, msg.arg1);
				Log.d(TAG, "handleMessage = " + tmpMsg );
				read_msg_box.setText(tmpMsg);
			}
			return true;
		}
	});

	private class SendReceive extends Thread {
		private final Socket socket;
		private InputStream inputStream;
		private OutputStream outputStream;

		public SendReceive(Socket skt) {
			socket = skt;
			try {
				Log.d(TAG, "SendReceive port = " + socket.getPort() );
				inputStream = socket.getInputStream();
				outputStream = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;

			while (socket != null) {
				try {
					bytes = inputStream.read(buffer);
					if(bytes > 0) {
						handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void write(String msg) {
			byte[] bytes = msg.getBytes();
			try {
				Log.d(TAG, "SendReceive write len = " + msg);
				outputStream.write(bytes);
				outputStream.flush();
			} catch (IOException e) {
				Log.d(TAG, "SendReceive e= " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
