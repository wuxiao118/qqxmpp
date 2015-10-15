package com.zyxb.qqxmpp.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import com.zyxb.qqxmpp.engine.XMPPEngine3;
import com.zyxb.qqxmpp.util.Const;
import com.zyxb.qqxmpp.util.Logger;
import com.zyxb.qqxmpp.util.SharedPreferencesUtils;

public class ChatService extends Service {
	private static final String TAG = "ChatService";
	private ChatService mService;
	private XMPPEngine3 mEngine;

	public static final String USER_LOCAL_ADD_COMPLETE = "com.zyxb.qqxmpp.USER_LOCAL_ADD_COMPLETE";
	public static final String LOGIN = "com.zyxb.qqxmpp.SERVER_LOGIN";
	public static final String LOGOUT = "com.zyxb.qqxmpp.SERVER_LOGOUT";
	public static final String CHAT_SERVICE_CLOSE = "com.zyxb.qqxmpp.CHAT_SERVICE_CLOSE";

	//数据改变
	public static final String USER_DATA_CHANGED = "com.zyxb.qqxmpp.USER_DATA_CHANGED";
	public static final String MESSAGE_DATA_CHANGED = "com.zyxb.qqxmpp.MESSAGE_DATA_CHANGED";

	public static final int SERVER_CONNECTED_USER_LOGIN = 1;
	public static final int SERVER_CONNECTED_USER_REJECTED = 2;
	public static final int SERVER_CONNECTED_USER_LOGOUT = 12;

	// 连接
	private ConnectReceiver connReceiver;
	private boolean isLogin = false;

	// 登陆用户信息保存完成
	private UserSaveReceiver userSaveReceiver;

	// 关闭自己
	private CloseReceiver closeReceiver;

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d(TAG, "chat service created");
		mService = this;
		// mEngine = XMPPEngine3.getInstance(mService);
		// mEngine = new XMPPEngine3(mService);
		// mEngine = XMPPEngine3.getmEngine();

		// 连接receiver
		connReceiver = new ConnectReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectService.LOGIN_SERVER_CONNECTED);
		filter.addAction(ConnectService.LOGIN_SERVER_DISCONNECTED);
		registerReceiver(connReceiver, filter);

		// 注册用户添加完成receiver,添加完成后可启动线程添加好友
		userSaveReceiver = new UserSaveReceiver();
		IntentFilter userSaveFilter = new IntentFilter();
		userSaveFilter.addAction(USER_LOCAL_ADD_COMPLETE);
		registerReceiver(userSaveReceiver, userSaveFilter);

		// 关闭
		closeReceiver = new CloseReceiver();
		IntentFilter closeFilter = new IntentFilter();
		closeFilter.addAction(CHAT_SERVICE_CLOSE);
		registerReceiver(closeReceiver, closeFilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(connReceiver);
		unregisterReceiver(userSaveReceiver);
		unregisterReceiver(closeReceiver);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new MyBinder();
	}

	private class MyBinder extends Binder implements IChatService {
		@Override
		public ChatService getService() {
			return mService;
		}
	}

	// 设置engine
	public void setmEngine(XMPPEngine3 mEngine) {
		this.mEngine = mEngine;
	}

	public XMPPEngine3 getmEngine() {
		if (mEngine == null) {
			mEngine = XMPPEngine3.getEngine();
		}

		return mEngine;
	}

	/**
	 * 登陆
	 *
	 * @return
	 */
	public void login(String account, String pwd, String ressource) {
		getmEngine();

		Logger.d(TAG, "user login:" + account + "," + ressource);
		Intent intent = new Intent(LOGIN);
		boolean isLogin = mEngine.login(account, pwd, ressource);
		if (isLogin) {
			intent.putExtra("reason", SERVER_CONNECTED_USER_LOGIN);
		} else {
			intent.putExtra("reason", SERVER_CONNECTED_USER_REJECTED);
		}

		sendBroadcast(intent);
		// return mEngine.login(account, pwd, ressource);
	}

	/**
	 * 注销
	 *
	 * @return
	 */
	public boolean logout() {
		getmEngine();

		return mEngine.logout();
	}

	/**
	 * 发送消息
	 *
	 * @param toJid
	 * @param msg
	 */
	public void sendMessage(String account, String toJid, String msg) {
		getmEngine();

		mEngine.sendMessage(account, toJid, msg);
	}

	/**
	 * 网络连接成功后自动登录,网络断开,自动注销
	 *
	 * @author Administrator
	 *
	 */
	private class ConnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(ConnectService.LOGIN_SERVER_CONNECTED)) {
				Logger.d(TAG, "用户自动登录");

				// 获取用户名密码
				String username = SharedPreferencesUtils.getString(mService,
						Const.SP_USERNAME, "");
				String pwd = SharedPreferencesUtils.getString(mService,
						Const.SP_PWD, "");
				String ressource = SharedPreferencesUtils.getString(mService,
						Const.XMPP_RESSOURCE, "qqxmpp");

				if (username.equals("") || pwd.equals("")) {
					// 用户名或密码为空,不连接
					return;
				}

				login(username.split("@")[0], pwd, ressource);
				isLogin = true;
			} else if (action.equals(ConnectService.LOGIN_SERVER_DISCONNECTED)) {
				Logger.d(TAG, "用户自动注销");
				// 停止消息队列
				//mEngine.stopMessageQueue();

				if (isLogin) {
					logout();
					isLogin = false;
				}
			}
		}
	}

	/**
	 * 登陆用户信息保存完成,启动好友和更新线程,更新数据库
	 *
	 * @author Administrator
	 *
	 */
	private class UserSaveReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Logger.d(TAG, "login user added.");
			// 开启线程,添加数据
			//mEngine.startMessageQueue();
		}

	}

	private class CloseReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			mService.stopSelf();
		}
	}
}
