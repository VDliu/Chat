package com.sevele.ds.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.decryptstranger.R;
import com.sevele.ds.activity.ChatActivity;
import com.sevele.ds.activity.DecryptgameActivity;
import com.sevele.ds.activity.MainActivity;
import com.sevele.ds.adapter.PushAdapter;
import com.sevele.ds.app.DsAppManager;
import com.sevele.ds.app.DsApplication;
import com.sevele.ds.app.DsConstant;
import com.sevele.ds.parsers.BeanJsonConvert;
import com.sevele.ds.table.FriendTable;
import com.sevele.ds.table.MsgBeanTable;
import com.sevele.ds.table.PushMessageTable;
import com.sevele.ds.table.StrangerTable;
import com.sevele.ds.table.UserTable;

/**
 * @author:liu ge
 * @createTime:2015年3月19日
 * @descrption:消息界面 ,接受推送消息
 */
public class MessageFragment extends ListFragment {

	public static PushAdapter m_pushAdapter;

	public static List<PushMessageTable> m_msgs;

	private static Boolean isUpdatePushList = false;
	public static updageFriendPagerInterface m_intgerface;

	public interface updageFriendPagerInterface {
		public void addFriend(FriendTable ft);
	}

	@SuppressLint("UseValueOf")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isUpdatePushList == null) {
			isUpdatePushList = new Boolean(false);
		}
		m_msgs = new ArrayList<PushMessageTable>();
		List<PushMessageTable> pmts = DsApplication.db.getPushMsgs();
		if (pmts != null) {
			for (PushMessageTable pmt : pmts) {
				String strPmt = pmt.getPushContent();
				List<MsgBeanTable> ll = BeanJsonConvert.jsonToBeanListM(strPmt);
				pmt.setLists(ll);
				m_msgs.add(pmt);
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof updageFriendPagerInterface) {
			m_intgerface = (updageFriendPagerInterface) activity;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_message, container,
				false);
		m_pushAdapter = new PushAdapter(m_msgs, getActivity());
		setListAdapter(m_pushAdapter);
		if (m_pushAdapter != null) {
			m_pushAdapter.notifyDataSetChanged();
		}
		// 读取数据库中的推送消息
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				final int pos = arg2;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setTitle("是否删除该聊天")
						.setPositiveButton("是", new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								DsApplication.db.deletePushMsg(m_msgs.get(pos));
								m_msgs.remove(pos);
								m_pushAdapter.notifyDataSetChanged();

							}
						}).setNegativeButton("否", null);
				builder.create().show();
				return true;
			}
		});
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = null;
		// 让推送消息头上的数目消失
		v.findViewById(R.id.push_layout).setVisibility(View.GONE);
		TextView tv = (TextView) v.findViewById(R.id.messageCount);
		tv.setText("");
		// 跳转到聊天界面,或者是服务器推送的其他消息界面（目前只跳转到聊天界面）

		if (m_msgs.get(position).getPusherId() > 0) {
			PushMessageTable pp = m_msgs.get(position);
			if (pp.getLists().get(0).getMsgType() == DsConstant.HELP_FD_OK) {
				Toast.makeText(getActivity(), "夸张！！你已解密成功", Toast.LENGTH_LONG)
						.show();
				return;
			}
			if (pp.getLists().get(0).getMsgType() == DsConstant.GAME_HELP) {
				intent = new Intent(getActivity(), DecryptgameActivity.class);
			} else {
				intent = new Intent(getActivity(), ChatActivity.class);
			}
			DsApplication.tempObject.put("CurrentPushMsg", pp);//
			startActivity(intent);
		} else {
			// 其他界面
		}
	}

	public static class pushMessageBroadcast extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			PushMessageTable pushMessage = null;
			MsgBeanTable pMsg = null;
			FriendTable friend = null;
			// 如果当前mainActivity在前台运行,直接更新推送消息列表
			Activity activity = DsAppManager.getAppManager().currentActivity();

			String msg = intent.getStringExtra("pushMsg");
			pMsg = (MsgBeanTable) BeanJsonConvert.jsonToBean(msg,
					MsgBeanTable.class);
			if (pMsg.getSid() > 0) {
				// 找到发送消息好友的头像，路径
				friend = DsApplication.db.getFriendTable(pMsg.getSid());
				if (friend == null) {// 不是自己好友发送的信息
					return;
				}
			}

			if (pMsg.getMsgType() == DsConstant.ADD_FRIEND_MSG) {

				String uInfo = BeanJsonConvert.getJsonEntity(pMsg
						.getMsgContent());
				UserTable adder = BeanJsonConvert.jsonToBean(uInfo,
						UserTable.class);
				if (DsApplication.db.getFriendTable(adder.getId()) != null) {// 此人已经是好友了
					return;
				}
				pushMessage = new PushMessageTable();
				pushMessage.setPusherId(adder.getId());
				pushMessage.setfPicPath(adder.getUserHeadPicture());
				pushMessage.setPusherName(adder.getUserName());
				pMsg.setMsgContent("");
				pushMessage.setMsgs(1);
				pushMessage.getLists().add(pMsg);
				m_pushAdapter.addItemPushMsg(pushMessage);
				
			} else if (pMsg.getMsgType() == DsConstant.DES_OK) {
				pushMessage = new PushMessageTable();
				pushMessage.setMsgs(1);
				pushMessage.setPusherName(friend.getUserName());
				pushMessage.setPusherId(pMsg.getSid());
				pushMessage.getLists().add(pMsg);
				m_pushAdapter.addItemPushMsg(pushMessage);
				if (pMsg.getMsgType() == DsConstant.DES_OK) {
					String friendInfo = pMsg.getMsgContent();
					StrangerTable st = BeanJsonConvert.jsonToBean(friendInfo,
							StrangerTable.class);
					FriendTable fd = DsApplication.db.getFriendTable(st
							.getSid());
					if (fd != null) {
						return;
					}
					FriendTable ft = new FriendTable();
					ft.setSid(st.getSid());
					ft.setGameRank(st.getGameRank());
					ft.setUserAge(st.getUserAge());
					ft.setUserGender(st.getUserGender());
					if (st.getUserHeadPicture() != null) {
						ft.setUserHeadPicture(st.getUserHeadPicture());
					}
					ft.setUserHometown(st.getUserHometown());
					ft.setUserName(st.getUserName());
					DsApplication.db.writeFriendToDb(ft);
					m_intgerface.addFriend(ft);
				}
			} else if (pMsg.getMsgType() == DsConstant.GAME_HELP) {
				pushMessage = new PushMessageTable();
				pushMessage.setMsgs(1);
				pushMessage.setPusherName(friend.getUserName());
				if (friend.getUserHeadPicture() != null) {
					pushMessage.setfPicPath(friend.getUserHeadPicture());
				}
				pushMessage.setPusherId(pMsg.getSid());
				pushMessage.getLists().add(pMsg);
				m_pushAdapter.addItemPushMsg(pushMessage);

			} else {

				int exsitPusher = -1;
				if (pMsg.getSid() > 0) {
					// 如果发送该消息的不是第一次发送消息，将消息保存并且消息计数加1
					exsitPusher = isPusheredBefore(pMsg.getSid());

					if (exsitPusher != -2
							&& m_msgs.get(exsitPusher).getLists().get(0)
									.getMsgType() != DsConstant.HELP_FD_OK) {
						pushMessage = m_msgs.get(exsitPusher);
					} else {// 该好友以前没有推送过消息
						pushMessage = new PushMessageTable();
						pushMessage.setPusherId(pMsg.getSid());
						pushMessage.setfPicPath(friend.getUserHeadPicture());
						pushMessage.setPusherName(friend.getUserName());
						m_pushAdapter.addItemPushMsg(pushMessage);
					}
					pushMessage.getLists().add(pMsg);
					pushMessage.setMsgs(pushMessage.getMsgs() + 1);
				}
				pMsg.setIsUserMsg(DsConstant.MSG_COME);
				pMsg.putShowtype();
				pMsg.setLoadState(DsConstant.NO_PUSH_OR_LOAD);
				pMsg.setChatTime(System.currentTimeMillis());
				pMsg.setUser(DsApplication.user);
				pMsg.setFriend(friend);
				DsApplication.db.saveChatRecord(pMsg);
			}
			
			if (pushMessage != null) {
				pushMessage.setPushTime(System.currentTimeMillis());

				String strPushmsg = BeanJsonConvert.beanListToJson(pushMessage
						.getLists());
				pushMessage.setPushContent(strPushmsg);
				DsApplication.db.writePushMsg(pushMessage);
			}

			if (activity instanceof MainActivity) {
				m_pushAdapter.notifyDataSetChanged();
			} else {
				isUpdatePushList = true;
			}

		}

		private int isPusheredBefore(int pusherId) {
			for (int i = 0; i < m_msgs.size(); i++) {
				if (pusherId == m_msgs.get(i).getPusherId())
					return i;
			}
			return -2;
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		if (isUpdatePushList) {
			m_pushAdapter.notifyDataSetChanged();
			isUpdatePushList = false;
		}
	}

	private void release() {
		m_pushAdapter = null;
		m_msgs.clear();
		m_msgs = null;
		isUpdatePushList = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		release();
	}
}
