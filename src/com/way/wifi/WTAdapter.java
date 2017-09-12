package com.way.wifi;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.netfeige.wt.WifiAdmin;

public class WTAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private List<ScanResult> mList;
	private Context mContext;

	public WTAdapter(Context context, List<ScanResult> list) {
		// TODO Auto-generated constructor stub
		this.mContext = context;
		this.mList = list;
		this.mInflater = LayoutInflater.from(context);
	}

	// 新加的一个函数，用来更新数据
	public void setData(List<ScanResult> list) {
		//Log.i("way", "mList length = "+list.size());
		//mList.clear();
		this.mList = list;
		Log.i("way", "mList length = "+mList.size());
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		final ScanResult localScanResult = mList.get(position);
		final WifiAdmin wifiAdmin = WifiAdmin.getInstance(mContext);
		final ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.wtitem, null);
			viewHolder.textVName = ((TextView) convertView
					.findViewById(R.id.name_text_wtitem));
			viewHolder.textConnect = ((TextView) convertView
					.findViewById(R.id.connect_text_wtitem));
			viewHolder.linearLConnectOk = ((LinearLayout) convertView
					.findViewById(R.id.connect_ok_layout_wtitem));
			viewHolder.progressBConnecting = ((ProgressBar) convertView
					.findViewById(R.id.connecting_progressBar_wtitem));
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		// 点击连接处理事件
		viewHolder.textConnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				WifiConfiguration localWifiConfiguration = wifiAdmin
						.createWifiInfo(localScanResult.SSID, WTActivity.WIFI_AP_PASSWORD, 3,
								"wt");
				wifiAdmin.addNetwork(localWifiConfiguration);
				viewHolder.textConnect.setVisibility(View.GONE);
				viewHolder.progressBConnecting.setVisibility(View.VISIBLE);
				viewHolder.linearLConnectOk.setVisibility(View.GONE);
				Handler localHandler = ((WTActivity) mContext).handler;
				localHandler.sendEmptyMessageDelayed(
						WTActivity.m_nWTConnected, 3500L);
			}
		});
		// 点击断开处理事件
		viewHolder.linearLConnectOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				wifiAdmin
						.disconnectWifi(wifiAdmin.getWifiInfo().getNetworkId());
				viewHolder.textConnect.setVisibility(View.GONE);
				viewHolder.progressBConnecting.setVisibility(View.VISIBLE);
				viewHolder.linearLConnectOk.setVisibility(View.GONE);
				Handler localHandler = ((WTActivity) mContext).handler;
				localHandler.sendEmptyMessageDelayed(
						WTActivity.m_nWTConnected, 3500L);
			}
		});

		viewHolder.textConnect.setVisibility(View.GONE);
		viewHolder.progressBConnecting.setVisibility(View.GONE);
		viewHolder.linearLConnectOk.setVisibility(View.GONE);
		viewHolder.textVName.setText(localScanResult.SSID);
		WifiInfo localWifiInfo = WifiAdmin.getInstance(mContext).getWifiInfo();// 正连接的wifi信息
		if (localWifiInfo != null) {
			try {
				if ((localWifiInfo.getSSID() != null)
						&& (localWifiInfo.getSSID()
								.equals(localScanResult.SSID))) {
					viewHolder.linearLConnectOk.setVisibility(View.VISIBLE);
					// if (!((MainActivity)mContext).m_strSargetSSID
					// .equals("")) {
					// ((MainActivity) mContext).m_strSargetSSID = "";
					// // Public_Tools.showToast(getContext(), getContext()
					// // .getString(2131165314), 3000);
					// Handler localHandler = ((MainActivity) mContext).handler;
					// // ((MainActivity) getContext()).getClass();
					// localHandler.sendEmptyMessageDelayed(
					// MainActivity.m_nWTConnected, 3500L);
					// }
					return convertView;
				}
				// else if (localScanResult.SSID
				// .equals(((MainActivity) mContext).m_strSargetSSID)) {
				// viewHolder.progressBConnecting.setVisibility(View.VISIBLE);
				// return convertView;
				// }
			} catch (NullPointerException localNullPointerException) {
				localNullPointerException.printStackTrace();
				return convertView;
			}
			viewHolder.textConnect.setVisibility(View.VISIBLE);
		}

		return convertView;
	}

	public final class ViewHolder {
		public LinearLayout linearLConnectOk;
		public ProgressBar progressBConnecting;
		public TextView textConnect;
		public TextView textVName;

		public ViewHolder() {
		}
	}
}
