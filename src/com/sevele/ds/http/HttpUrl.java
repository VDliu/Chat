package com.sevele.ds.http;

/**
 * @author:liu ge
 * @createTime:2015年4月10日
 * @descrption:Url
 */
public class HttpUrl {

	private static final String PROTOCOL = "http://";
	public static final String SERVER_IP = "121.42.50.102";
	public static final String DD_SERVER_IP ="121.42.50.102";
	public static final int SERVER_PORT = 9966;
	
	public static final int PUSH_PORT = 9999;
	private static final String HOST = SERVER_IP + ":8080"; // private static
															// final String
															// HOST="10.116.0.21:8080";

	private static final String PROJECT = "/sevenElevenTeam";
	private static final String BASIC_URL = PROTOCOL + HOST + PROJECT;

	private static final String LOGIN_PATH = "/userLogin"; // 登录
	private static final String REGIST = "/userRegist"; // 注册
	private static final String UPLOAD_HEADPIC = "/userRegist/upLoadpic"; // 上传图片
	private static final String USER_POSITION = "/userPosition"; // 摇一摇
	private static final String ADD_FRIEND = "/addFriend"; // 添加好友
	private static final String GET_FRIEND = "/getFriend"; // 服务器上获取好友
	private static final String GET_HEADPIC_TOKEN = "/getToken/getHeadpicUploadToken"; // 服务器上传头像token
	private static final String GET_COMMON_TOKEN = "/getToken/getCommonUploadFileToken"; // 服务器上获取一般文件Token
	private static final String GET_USERINFO = "/getUserInfo"; // 获取用户个人信息

	private static final String PUSHMSG = "/pushMsg"; // 获取用户个人信息
	private static final String DELETE_FRIEND = "/deleteFriend"; // 获取用户个人信息

	private static final String UPDATE_USER_INFO = "/updateUserInfo"; // 更改用户个人信息

	public static final String LOGIN_URL = BASIC_URL + LOGIN_PATH;
	public static final String REGIST_URL = BASIC_URL + REGIST;
	public static final String UPLOADHEADPIC_URL = BASIC_URL + UPLOAD_HEADPIC;
	public static final String POSITION_URL = BASIC_URL + USER_POSITION;
	public static final String ADDFRIEND_URL = BASIC_URL + ADD_FRIEND;
	public static final String GETFRIEND_URL = BASIC_URL + GET_FRIEND;

	public static final String GET_HEADPIC_TOKEN_URL = BASIC_URL
			+ GET_HEADPIC_TOKEN;
	public static final String GET_COMMON_TOKEN_URL = BASIC_URL
			+ GET_COMMON_TOKEN;
	public static final String GET_USERINFO_URL = BASIC_URL + GET_USERINFO;
	public static final String PUSHMSG_URL = BASIC_URL + PUSHMSG;

	public static final String DELETEFRIEND_URL = BASIC_URL + DELETE_FRIEND;
	public static final String UPDATE_USERINFO_URL = BASIC_URL
			+ UPDATE_USER_INFO;

}
