package com.way.wifi;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.netfeige.broadcast.WTBroadcast;
import com.netfeige.display.ui.GifView;
import com.netfeige.display.ui.WTSearchAnimationFrameLayout;
import com.netfeige.wt.WifiAdmin;

public class WTActivity extends Activity implements WTBroadcast.EventHandler {
	public static final int m_nWTSearchTimeOut = 0;// 搜索超时
	public static final int m_nWTScanResult = 1;// 搜索到wifi返回结果
	public static final int m_nWTConnectResult = 2;// 连接上wifi热点
	public static final int m_nCreateAPResult = 3;// 创建热点结果
	public static final int m_nUserResult = 4;// 用户上线人数更新命令(待定)
	public static final int m_nWTConnected = 5;// 点击连接后断开wifi，3.5秒后刷新adapter
	public static final String PACKAGE_NAME = "com.way.wifi";
	public static final String FIRST_OPEN_KEY = "version";
	public static final String WIFI_AP_HEADER = "way_";
	public static final String WIFI_AP_PASSWORD ="way12345";
	
	private WTSearchAnimationFrameLayout m_FrameLWTSearchAnimation;
	private LinearLayout m_LinearLDialog;
	private LinearLayout m_LinearLIntroduction;
	private Button m_btnBack;
	private Button m_btnCancelDialog;
	private Button m_btnConfirmDialog;
	private Button m_btnCreateWT;
	private Button m_btnSearchWT;
	private GifView m_gifRadar;
	private boolean m_isFirstOpen = true;
	private LinearLayout m_linearLCreateAP;
	private ListView m_listVWT;
	ArrayList<ScanResult> m_listWifi = new ArrayList();
	private ProgressBar m_progBarCreatingAP;
	private TextView m_textVContentDialog;
	private TextView m_textVPromptAP;
	private TextView m_textVWTPrompt;
	private WifiAdmin m_wiFiAdmin;
	private CreateAPProcess m_createAPProcess;
	private WTSearchProcess m_wtSearchProcess;
	private int wTOperateEnum = WTOperateEnum.NOTHING;
	private WTAdapter m_wTAdapter;

	public Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case m_nWTSearchTimeOut:
				m_wtSearchProcess.stop();
				m_FrameLWTSearchAnimation.stopAnimation();
				m_listWifi.clear();
				m_textVWTPrompt.setVisibility(View.VISIBLE);
				m_textVWTPrompt.setText(R.string.wt_list_empty);
				break;
			case m_nWTScanResult:
//				m_wtSearchProcess.stop();
//				m_FrameLWTSearchAnimation.stopAnimation();
				m_listWifi.clear();
				int size = m_wiFiAdmin.mWifiManager.getScanResults().size();
				if (size > 0) {
					for (int i = 0; i < size; ++i) {
						ScanResult scanResult = m_wiFiAdmin.mWifiManager
								.getScanResults().get(i);
						// Log.i("way", "SSID = " + scanResult.SSID);
						if (scanResult.SSID.startsWith(WIFI_AP_HEADER)) {
							m_listWifi.add(scanResult);
						}
					}
					if (m_listWifi.size() > 0) {
						 m_wtSearchProcess.stop();
						 m_FrameLWTSearchAnimation.stopAnimation();
						m_textVWTPrompt.setVisibility(View.GONE);
						m_wTAdapter.setData(m_listWifi);
						m_wTAdapter.notifyDataSetChanged();
					} 
//					else {
//						m_textVWTPrompt.setVisibility(View.VISIBLE);
//						m_textVWTPrompt.setText(R.string.wt_list_empty);
//					}
//				} else {
//					m_textVWTPrompt.setVisibility(View.VISIBLE);
//					m_textVWTPrompt.setText(R.string.wt_list_empty);
				}
				break;
			case m_nWTConnectResult:
				m_wTAdapter.notifyDataSetChanged();
				break;
			case m_nCreateAPResult:
				m_createAPProcess.stop();
				m_progBarCreatingAP.setVisibility(View.GONE);
				if (((m_wiFiAdmin.getWifiApState() == 3) || (m_wiFiAdmin
						.getWifiApState() == 13))
						&& (m_wiFiAdmin.getApSSID().startsWith(WIFI_AP_HEADER))) {
					m_textVWTPrompt.setVisibility(View.GONE);
					m_linearLCreateAP.setVisibility(View.VISIBLE);
					m_btnCreateWT.setVisibility(View.VISIBLE);
					m_gifRadar.setVisibility(View.VISIBLE);
					m_btnCreateWT.setBackgroundResource(R.drawable.x_ap_close);
					m_textVPromptAP
							.setText(getString(R.string.pre_wt_connect_ok)
									+ getString(R.string.middle_wt_connect_ok)
									+ m_wiFiAdmin.getApSSID()
									+ getString(R.string.suf_wt_connect_ok));
				} else {
					m_btnCreateWT.setVisibility(View.VISIBLE);
					m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
					m_textVPromptAP.setText(R.string.create_ap_fail);
				}
				break;
			case m_nUserResult:
				// 更新用户上线人数，待定
				break;
			case m_nWTConnected:
				m_wTAdapter.notifyDataSetChanged();
				break;
			default:
				break;
			}
		}
	};

	public boolean getWifiApState() {
		try {
			WifiManager localWifiManager = (WifiManager) getSystemService("wifi");
			int i = ((Integer) localWifiManager.getClass()
					.getMethod("getWifiApState", new Class[0])
					.invoke(localWifiManager, new Object[0])).intValue();
			return (3 == i) || (13 == i);
		} catch (Exception localException) {
		}
		return false;
	}

	public boolean isWifiConnect() {
		boolean isConnect = true;
		if (!((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected())
			isConnect = false;
		return isConnect;
	}

	// 是否为第一次打开应用
	private boolean isFirstOpen() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(
					PACKAGE_NAME, 0);
			int currentVersion = info.versionCode;
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			int lastVersion = prefs.getInt(FIRST_OPEN_KEY, 0);
			if (currentVersion > lastVersion) {
				prefs.edit().putInt(FIRST_OPEN_KEY, currentVersion).commit();
				return true;
			}
		} catch (PackageManager.NameNotFoundException e) {
			Log.w("way", e);
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wt_main);
		m_wtSearchProcess = new WTSearchProcess();
		m_createAPProcess = new CreateAPProcess();
		m_wiFiAdmin = WifiAdmin.getInstance(this);
		initView();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (!m_isFirstOpen)
			init();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		WTBroadcast.ehList.remove(this);
	}

	// 初始化view
	private void initView() {
		m_isFirstOpen = isFirstOpen();
		m_LinearLIntroduction = ((LinearLayout) findViewById(R.id.introduction_layout_wt_main));// 介绍使用的view
		m_LinearLIntroduction.setOnClickListener(new OnClickListener() {
			public void onClick(View paramView) {
				m_LinearLIntroduction.setVisibility(View.GONE);
				init();
			}

		});

		m_btnBack = ((Button) findViewById(R.id.back_btn_wt_main));// 返回键
		m_btnBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		m_linearLCreateAP = ((LinearLayout) findViewById(R.id.create_ap_llayout_wt_main));// 创建热点的view
		m_progBarCreatingAP = ((ProgressBar) findViewById(R.id.creating_progressBar_wt_main));// 创建热点的进度条
		m_textVPromptAP = ((TextView) findViewById(R.id.prompt_ap_text_wt_main));
		m_btnSearchWT = ((Button) findViewById(R.id.search_btn_wt_main));// 搜索热点按钮
		m_btnSearchWT.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if (!m_wtSearchProcess.running) {// 如果搜索线程没有启动
					if (m_wiFiAdmin.getWifiApState() == 13
							|| m_wiFiAdmin.getWifiApState() == 3) {
						wTOperateEnum = WTOperateEnum.SEARCH;
						m_LinearLDialog.setVisibility(View.VISIBLE);
						m_textVContentDialog.setText(R.string.opened_ap_prompt);
						return;
					}
					if (!m_wiFiAdmin.mWifiManager.isWifiEnabled()) {// 如果wifi打开着的
						m_wiFiAdmin.OpenWifi();
					}
					m_textVWTPrompt.setVisibility(View.VISIBLE);
					m_textVWTPrompt.setText(R.string.wt_searching);
					m_linearLCreateAP.setVisibility(View.GONE);
					m_gifRadar.setVisibility(View.GONE);
					m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
					m_wiFiAdmin.startScan();
					m_wtSearchProcess.start();
					m_FrameLWTSearchAnimation.startAnimation();
				} else {
					//重新启动一下
					m_wtSearchProcess.stop();
					m_wiFiAdmin.startScan();
					m_wtSearchProcess.start();
				}
			}
		});
		m_btnCreateWT = ((Button) findViewById(R.id.create_btn_wt_main));// 创建热点的按钮
		m_btnCreateWT.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (m_wiFiAdmin.getWifiApState() == 4) {
					Toast.makeText(getApplicationContext(),
							R.string.not_create_ap, Toast.LENGTH_SHORT).show();
					return;
				}
				if (m_wiFiAdmin.mWifiManager.isWifiEnabled()) {
					wTOperateEnum = WTOperateEnum.CREATE;
					m_LinearLDialog.setVisibility(View.VISIBLE);
					m_textVContentDialog.setText(R.string.close_wifi_prompt);
					return;
				}

				if (((m_wiFiAdmin.getWifiApState() == 3) || (m_wiFiAdmin
						.getWifiApState() == 13))
						&& (!m_wiFiAdmin.getApSSID().startsWith(WIFI_AP_HEADER))) {
					wTOperateEnum = WTOperateEnum.CREATE;
					m_LinearLDialog.setVisibility(View.VISIBLE);
					m_textVContentDialog.setText(R.string.ap_used);
					return;
				}
				if (((m_wiFiAdmin.getWifiApState() == 3) || (m_wiFiAdmin
						.getWifiApState() == 13))
						&& (m_wiFiAdmin.getApSSID().startsWith(WIFI_AP_HEADER))) {
					wTOperateEnum = WTOperateEnum.CLOSE;
					m_LinearLDialog.setVisibility(View.VISIBLE);
					m_textVContentDialog.setText(R.string.close_ap_prompt);
					return;
				}
				if (m_wtSearchProcess.running) {
					m_wtSearchProcess.stop();
					m_FrameLWTSearchAnimation.stopAnimation();
				}
				m_wiFiAdmin.closeWifi();
				m_wiFiAdmin.createWiFiAP(
						m_wiFiAdmin.createWifiInfo(WIFI_AP_HEADER
								+ getLocalHostName(), WIFI_AP_PASSWORD, 3, "ap"),
						true);
				m_createAPProcess.start();
				m_listWifi.clear();
				m_wTAdapter.setData(m_listWifi);
				m_wTAdapter.notifyDataSetChanged();
				m_linearLCreateAP.setVisibility(View.VISIBLE);
				m_progBarCreatingAP.setVisibility(View.VISIBLE);
				m_btnCreateWT.setVisibility(View.GONE);
				m_textVWTPrompt.setVisibility(View.GONE);
				m_textVPromptAP.setText(getString(R.string.creating_ap));
			}
		});
		m_FrameLWTSearchAnimation = ((WTSearchAnimationFrameLayout) findViewById(R.id.search_animation_wt_main));// 搜索时的动画
		m_listVWT = ((ListView) findViewById(R.id.wt_list_wt_main));// 搜索到的热点listView
		m_wTAdapter = new WTAdapter(this, m_listWifi);
		m_listVWT.setAdapter(m_wTAdapter);

		m_textVWTPrompt = (TextView) findViewById(R.id.wt_prompt_wt_main);
		m_gifRadar = (GifView) findViewById(R.id.radar_gif_wt_main);

		m_LinearLDialog = (LinearLayout) findViewById(R.id.dialog_layout_wt_main);
		m_textVContentDialog = (TextView) findViewById(R.id.content_text_wtdialog);
		m_btnConfirmDialog = (Button) findViewById(R.id.confirm_btn_wtdialog);
		m_btnCancelDialog = (Button) findViewById(R.id.cancel_btn_wtdialog);
		// 弹出对话框的确认按钮事件
		m_btnConfirmDialog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				m_LinearLDialog.setVisibility(View.GONE);
				switch (wTOperateEnum) {
				case WTOperateEnum.CLOSE:
					m_textVWTPrompt.setVisibility(View.VISIBLE);
					m_textVWTPrompt.setText("");
					m_linearLCreateAP.setVisibility(View.GONE);
					m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
					m_gifRadar.setVisibility(View.GONE);
					m_wiFiAdmin.createWiFiAP(m_wiFiAdmin.createWifiInfo(
							m_wiFiAdmin.getApSSID(), "81028066", 3, "ap"),
							false);

					m_wiFiAdmin.OpenWifi();
					m_wtSearchProcess.start();
					m_wiFiAdmin.startScan();
					m_FrameLWTSearchAnimation.startAnimation();
					m_textVWTPrompt.setVisibility(View.VISIBLE);
					m_textVWTPrompt.setText(R.string.wt_searching);
					m_linearLCreateAP.setVisibility(View.GONE);
					m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
					break;
				case WTOperateEnum.CREATE:
					if (m_wtSearchProcess.running) {
						m_wtSearchProcess.stop();
						m_FrameLWTSearchAnimation.stopAnimation();
					}
					m_wiFiAdmin.closeWifi();
					m_wiFiAdmin.createWiFiAP(
							m_wiFiAdmin.createWifiInfo(WIFI_AP_HEADER
									+ getLocalHostName(), WIFI_AP_PASSWORD, 3, "ap"),
							true);
					m_createAPProcess.start();
					m_listWifi.clear();
					m_wTAdapter.setData(m_listWifi);
					m_wTAdapter.notifyDataSetChanged();
					m_linearLCreateAP.setVisibility(View.VISIBLE);
					m_progBarCreatingAP.setVisibility(View.VISIBLE);
					m_btnCreateWT.setVisibility(View.GONE);
					m_textVWTPrompt.setVisibility(View.GONE);
					m_textVPromptAP.setText(getString(R.string.creating_ap));
					break;
				case WTOperateEnum.SEARCH:
					m_textVWTPrompt.setVisibility(View.VISIBLE);
					m_textVWTPrompt.setText(R.string.wt_searching);
					m_linearLCreateAP.setVisibility(View.GONE);
					m_btnCreateWT.setVisibility(View.VISIBLE);
					m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
					m_gifRadar.setVisibility(View.GONE);
					if (m_createAPProcess.running)
						m_createAPProcess.stop();
					m_wiFiAdmin.createWiFiAP(m_wiFiAdmin.createWifiInfo(
							m_wiFiAdmin.getApSSID(), WIFI_AP_PASSWORD, 3, "ap"),
							false);
					m_wiFiAdmin.OpenWifi();
					m_wtSearchProcess.start();
					m_FrameLWTSearchAnimation.startAnimation();
					break;
				default:
					break;
				}

			}
		});
		// 弹出对话框取消按钮事件
		m_btnCancelDialog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				m_LinearLDialog.setVisibility(View.GONE);
			}
		});

		WTBroadcast.ehList.add(this);// 监听广播
		if (m_isFirstOpen)
			m_LinearLIntroduction.setVisibility(View.VISIBLE);
	}

	public String getLocalHostName() {
		String str1 = Build.BRAND;
		String str2 = Build.MODEL;
		if (-1 == str2.toUpperCase().indexOf(str1.toUpperCase()))
			str2 = str1 + "_" + str2;
		return str2;
	}

	// 初始化
	private void init() {
		// TODO Auto-generated method stub
		if ((this.m_wtSearchProcess.running)
				|| (this.m_createAPProcess.running))
			return;

		if (!isWifiConnect() && !getWifiApState()) {
			m_wiFiAdmin.OpenWifi();
			m_wtSearchProcess.start();
			m_wiFiAdmin.startScan();
			m_FrameLWTSearchAnimation.startAnimation();
			m_textVWTPrompt.setVisibility(View.VISIBLE);
			m_textVWTPrompt.setText(R.string.wt_searching);
			m_linearLCreateAP.setVisibility(View.GONE);
			m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
		}
		if (isWifiConnect()) {
			this.m_wiFiAdmin.startScan();
			this.m_wtSearchProcess.start();
			this.m_FrameLWTSearchAnimation.startAnimation();
			this.m_textVWTPrompt.setVisibility(0);
			this.m_textVWTPrompt.setText(R.string.wt_searching);
			this.m_linearLCreateAP.setVisibility(8);
			this.m_btnCreateWT.setBackgroundResource(R.drawable.x_wt_create);
			this.m_gifRadar.setVisibility(8);
			m_listWifi.clear();
			if (m_wiFiAdmin.mWifiManager.getScanResults() != null) {
				int result = m_wiFiAdmin.mWifiManager.getScanResults().size();
				int i = 0;
				for (i = 0; i < result; ++i) {
					if (m_wiFiAdmin.mWifiManager.getScanResults().get(i).SSID
							.startsWith(WIFI_AP_HEADER))
						m_listWifi.add(m_wiFiAdmin.mWifiManager
								.getScanResults().get(i));
				}
				Log.i("way", "wifi size:"
						+ m_wiFiAdmin.mWifiManager.getScanResults().size());
			}
			m_wTAdapter.setData(m_listWifi);
			m_wTAdapter.notifyDataSetChanged();

		}

		if (getWifiApState()) {
			if (m_wiFiAdmin.getApSSID().startsWith(WIFI_AP_HEADER)) {
				m_textVWTPrompt.setVisibility(View.GONE);
				m_linearLCreateAP.setVisibility(View.VISIBLE);
				m_progBarCreatingAP.setVisibility(View.GONE);
				m_btnCreateWT.setVisibility(View.VISIBLE);
				m_gifRadar.setVisibility(View.VISIBLE);
				m_btnCreateWT.setBackgroundResource(R.drawable.x_ap_close);
				m_textVPromptAP.setText(getString(R.string.pre_wt_connect_ok)
						+ getString(R.string.middle_wt_connect_ok)
						+ m_wiFiAdmin.getApSSID()
						+ getString(R.string.suf_wt_connect_ok));
			}
		}
	}

	@Override
	public void handleConnectChange() {
		// TODO Auto-generated method stub
		Message msg = handler.obtainMessage(m_nWTConnectResult);
		handler.sendMessage(msg);
//		 handler.sendEmptyMessageDelayed(m_nWTConnectResult, 2000L);
	}

	@Override
	public void scanResultsAvailable() {
		// TODO Auto-generated method stub
		Message msg = handler.obtainMessage(m_nWTScanResult);
		handler.sendMessage(msg);
//		handler.sendEmptyMessageDelayed(m_nWTScanResult, 2000L);
	}

	@Override
	public void wifiStatusNotification() {
		// TODO Auto-generated method stub
		m_wiFiAdmin.mWifiManager.getWifiState();
	}

	class CreateAPProcess implements Runnable {
		public boolean running = false;
		private long startTime = 0L;
		private Thread thread = null;

		CreateAPProcess() {
		}

		public void run() {
			while (true) {
				if (!this.running)
					return;
				if ((m_wiFiAdmin.getWifiApState() == 3)
						|| (m_wiFiAdmin.getWifiApState() == 13)
						|| (System.currentTimeMillis() - this.startTime >= 30000L)) {
					Message msg = handler.obtainMessage(m_nCreateAPResult);
					handler.sendMessage(msg);
				}
				try {
					Thread.sleep(5L);
				} catch (Exception localException) {
				}
			}
		}

		public void start() {
			try {
				thread = new Thread(this);
				running = true;
				startTime = System.currentTimeMillis();
				thread.start();
			} finally {
			}
		}

		public void stop() {
			try {
				this.running = false;
				this.thread = null;
				this.startTime = 0L;
			} finally {
			}
		}
	}

	class WTSearchProcess implements Runnable {
		public boolean running = false;
		private long startTime = 0L;
		private Thread thread = null;

		WTSearchProcess() {
		}

		public void run() {
			while (true) {
				if (!this.running)
					return;
				if (System.currentTimeMillis() - this.startTime >= 30000L) {
					// Message localMessage = Message.obtain(handler);
					// localMessage.what = 0;
					// localMessage.sendToTarget();
					Message msg = handler.obtainMessage(m_nWTSearchTimeOut);
					handler.sendMessage(msg);
				}
				try {
					Thread.sleep(10L);
				} catch (Exception localException) {
				}
			}
		}

		public void start() {
			try {
				this.thread = new Thread(this);
				this.running = true;
				this.startTime = System.currentTimeMillis();
				this.thread.start();
			} finally {
			}
		}

		public void stop() {
			try {
				this.running = false;
				this.thread = null;
				this.startTime = 0L;
			} finally {
			}
		}
	}
}
