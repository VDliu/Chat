package com.sevele.ds.ddpush;

import java.io.File;
import java.nio.ByteBuffer;

import org.ddpush.im.util.DateTimeUtil;
import org.ddpush.im.v1.client.appuser.Message;
import org.ddpush.im.v1.client.appuser.TCPClientBase;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import com.example.decryptstranger.R;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.sevele.ds.activity.ChatActivity;
import com.sevele.ds.activity.MainActivity;
import com.sevele.ds.app.DsAppManager;
import com.sevele.ds.app.DsApplication;
import com.sevele.ds.app.DsConstant;
import com.sevele.ds.http.HttpUrl;
import com.sevele.ds.parsers.BeanJsonConvert;
import com.sevele.ds.table.FriendTable;
import com.sevele.ds.table.MsgBeanTable;
import com.sevele.ds.table.UserTable;
import com.sevele.ds.utils.Util;

/**
 * 
 * @author:liu ge
 * @createTime:2015年5月22日
 * @descrption:应用与发送心跳包
 */

public class OnlineService extends Service {

	protected PendingIntent tickPendIntent;
	protected TickAlarmReceiver tickAlarmReceiver = new TickAlarmReceiver(); // 在被应用休眠被唤醒后，该广播启动ddush
																				// service
	WakeLock wakeLock;
	MyTcpClient myTcpClient; // 采用udp连接发送心跳包
	Notification n;
	MsgBeanTable msg; // 要发送的消息
	String str; // 收到的消息转化为字符串
	AlarmManager alarmMgr; // 用于每个几分中唤醒cpu 发送心跳包

	public class MyTcpClient extends TCPClientBase {

		public MyTcpClient(byte[] uuid, int appid, String serverAddr,
				int serverPort) throws Exception {
			super(uuid, appid, serverAddr, serverPort, 10);
		}

		// 判断网络
		@Override
		public boolean hasNetworkConnection() {
			return Util.hasNetwork(OnlineService.this);
		}

		@Override
		public void trySystemSleep() {
			tryReleaseWakeLock();
		}

		// 当推送来的时候在此处接收到推送消息
		@Override
		public void onPushMessage(Message message) {
			if (message == null) {
				return;
			}
			if (message.getData() == null || message.getData().length == 0) {
				return;
			}
			if (message.getCmd() == 16) {// 0x10 通用推送信息
				notifyUser(16, "解密陌生人", "时间：" + DateTimeUtil.getCurDateTime(),
						"收到通用推送信息");
			}
			if (message.getCmd() == 17) {// 0x11 分组推送信息
				long msg = ByteBuffer.wrap(message.getData(), 5, 8).getLong();
				notifyUser(17, "解密陌生人", "" + msg, "收到通用推送信息");
			}
			if (message.getCmd() == 32) {// 0x20 自定义推送信息
				str = null;
				try {
					str = new String(message.getData(), 5,
							message.getContentLength(), "UTF-8");
				} catch (Exception e) {
					str = Util.convert(message.getData(), 5,
							message.getContentLength());
				}
				// 如果当前的推送消息是给正在聊天的好友发送过来的，开启一个广播把消息传送给正在聊天的activity
				msg = BeanJsonConvert.jsonToBean(str, MsgBeanTable.class);

				FriendTable friend = DsApplication.db.getFriendTable(msg
						.getSid());
				if (friend == null
						&& msg.getMsgType() != DsConstant.ADD_FRIEND_MSG) {
					return;
				}

				notifyUser(32, "解密陌生人", "" + str, "收到自定义推送信息");

				// 如果当前的推送消息是给正在聊天的好友发送过来的，开启一个广播把消息传送给正在聊天的activity
				msg = BeanJsonConvert.jsonToBean(str, MsgBeanTable.class);

				if (msg.getMsgType() == DsConstant.ADD_FRIEND_MSG) {// 添加好友消息
					// 下载添加好友的人的头像 如果没有 默认一张图片
					HttpUtils http = new HttpUtils();
					final String addFriendMsg = BeanJsonConvert
							.getJsonEntity(msg.getMsgContent());
					final UserTable adder = BeanJsonConvert.jsonToBean(
							addFriendMsg, UserTable.class);// 添加人信息
					if (adder.getUserHeadPicture() == null) {// 如果该添加人头像为空，启动广播
						Intent intent1 = new Intent();
						intent1.setAction("android.com.sevele.ds.broadcast.addFriend");
						intent1.putExtra("ft", addFriendMsg);
						intent1.putExtra("pushMsg", str);
						OnlineService.this.sendBroadcast(intent1);
						return;
					}
					http.download(DsConstant.MY_QINIU + adder.userHeadPicture
							+ DsConstant.SMALL_PIC,
							DsConstant.IMG_ROOT + adder.getUserHeadPicture(),
							new RequestCallBack<File>() {
								@Override
								public void onSuccess(ResponseInfo<File> arg0) {
									Intent intent2 = new Intent();
									intent2.setAction("android.com.sevele.ds.broadcast.addFriend");
									intent2.putExtra("ft", addFriendMsg);
									intent2.putExtra("pushMsg", str);
									OnlineService.this.sendBroadcast(intent2);
								}

								@Override
								public void onFailure(HttpException arg0,
										String arg1) {
									Intent intent2 = new Intent();
									intent2.setAction("android.com.sevele.ds.broadcast.addFriend");
									intent2.putExtra("ft", addFriendMsg);
									intent2.putExtra("pushMsg", str);
									OnlineService.this.sendBroadcast(intent2);
								}

								@Override
								public void onStart() {
									super.onStart();
								}
							});
				} else if (msg.getMsgType() == DsConstant.GAME_HELP
						|| msg.getMsgType() == DsConstant.DES_OK) {// 请求好友帮助消息
					Intent intent = new Intent();
					intent.putExtra("pushMsg", str);
					intent.setAction("android.com.sevele.ds.broadcast.updatePushPage");
					sendBroadcast(intent);
				} else {// 聊天消息
					Intent intent = new Intent();
					intent.putExtra("pushMsg", str);
					if (!(DsAppManager.getAppManager().currentActivity() instanceof ChatActivity)
							|| DsApplication.currentFd == null) {
						// 如果此时的activity不是聊天界面中
						intent.setAction("android.com.sevele.ds.broadcast.updatePushPage");
					} else if (DsApplication.currentFd != null
							&& DsAppManager.getAppManager().currentActivity() instanceof ChatActivity) {
						if (DsApplication.currentFd.getSid() == msg.getSid())
							intent.setAction("android.com.sevele.ds.broadcast.updateChating");// 自定义action
					}
					sendBroadcast(intent);
				}
			}
		}
	}

	public OnlineService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.setTickAlarm();
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"OnlineService");
		resetClient();
	}

	@Override
	public int onStartCommand(Intent param, int flags, int startId) {
		if (param == null) {
			return START_STICKY;
		}
		String cmd = param.getStringExtra("CMD");
		if (cmd == null) {
			cmd = "";
		}
		if (cmd.equals("TICK")) {
			if (wakeLock != null && wakeLock.isHeld() == false) {
				wakeLock.acquire();
			}
		}
		if (cmd.equals("RESET")) {
			if (wakeLock != null && wakeLock.isHeld() == false) {
				wakeLock.acquire();
			}
			resetClient();
		}
		if (cmd.equals("TOAST")) {
			String text = param.getStringExtra("TEXT");
			if (text != null && text.trim().length() != 0) {
				Toast.makeText(this, text, Toast.LENGTH_LONG).show();
			}
		}
		if (cmd.equals("EXIT")) {
			stopSelf();
			if(myTcpClient!=null){
				myTcpClient.stop();
			}
			return START_NOT_STICKY;
		}

		return START_STICKY;
	}

	protected void resetClient() {
		String serverIp = HttpUrl.DD_SERVER_IP;
		Integer serverPort = HttpUrl.SERVER_PORT;
		Integer pushPort = HttpUrl.PUSH_PORT;
		if (DsApplication.user == null) {
			stopSelf();
			return;
		}
		String userName = DsApplication.user.getId() + "";
		if (serverIp == null || serverIp.trim().length() == 0
				|| serverPort == null || pushPort == null || userName == null
				|| userName.trim().length() == 0) {
			return;
		}
		if (this.myTcpClient != null) {
			try {
				myTcpClient.stop();
			} catch (Exception e) {
			}
		}
		try {
			myTcpClient = new MyTcpClient(Util.md5Byte(userName), 1, serverIp,
					serverPort);
			myTcpClient.setHeartbeatInterval(50);
			myTcpClient.start();
		} catch (Exception e) {
			Toast.makeText(this.getApplicationContext(),
					"操作失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	protected void tryReleaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld() == true) {
			wakeLock.release();
		}
	}

	protected void setTickAlarm() {
		alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, TickAlarmReceiver.class);
		int requestCode = 0;
		tickPendIntent = PendingIntent.getBroadcast(this, requestCode, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		// 小米2s的MIUI操作系统，目前最短广播间隔为5分钟，少于5分钟的alarm会等到5分钟再触发！2014-04-28
		long triggerAtTime = System.currentTimeMillis();
		int interval = 300 * 1000;
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval,
				tickPendIntent);
	}

	protected void cancelTickAlarm() {
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(tickPendIntent);
	}

	protected void notifyRunning() {
		NotificationManager notificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		n = new Notification();
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_ONE_SHOT);
		n.contentIntent = pi;
		n.setLatestEventInfo(this, "DDPushDemoUDP", "正在运行", pi);
		// n.defaults = Notification.DEFAULT_ALL;
		// n.flags |= Notification.FLAG_SHOW_LIGHTS;
		// n.flags |= Notification.FLAG_AUTO_CANCEL;
		n.flags |= Notification.FLAG_ONGOING_EVENT;
		n.flags |= Notification.FLAG_NO_CLEAR;
		// n.iconLevel = 5;

		n.icon = R.drawable.ic_launcher;
		n.when = System.currentTimeMillis();
		n.tickerText = "DDPushDemoUDP正在运行";
		notificationManager.notify(0, n);
	}

	protected void cancelNotifyRunning() {
		NotificationManager notificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}

	public void notifyUser(int id, String title, String content,
			String tickerText) {
		NotificationManager notificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification();
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_ONE_SHOT);
		n.contentIntent = pi;
		n.setLatestEventInfo(this, title, "您有新的消息", pi);
		n.defaults = Notification.DEFAULT_ALL;
		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.flags |= Notification.FLAG_AUTO_CANCEL;

		n.icon = R.drawable.app_icon;
		n.when = System.currentTimeMillis();
		n.tickerText = tickerText;
		notificationManager.notify(id, n);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.cancelTickAlarm();
		cancelNotifyRunning();
		this.tryReleaseWakeLock();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
