package com.bsj.company.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.xutils.common.util.LogUtil;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SocketTCP extends Handler {
	private final String address;
	// 变量
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private ExecutorService executorService;
	private byte[] loginPkg;
	// 常量
	public static final int CreateSuccess = 0x00;
	public static final int CreateFail = 0x01;
	public static final int ReceiveDoing = 0x02;
	public static final int ReceiveDone = 0x03;
	public static final int ReceiveFail = 0x04;
	public static final int NetworkError = 0x05;

	// 接口
	private SocketResult socketResult;

	public SocketTCP(String address, SocketResult socketResult,String id) {
		this.address = address;
		this.socketResult = socketResult;
		this.executorService = Executors.newCachedThreadPool();
		try {
			String content = id;
			this.loginPkg = new MakePackage().generalPackage(0x01, content);
		} catch (Exception e) {
			Log.e("SocketTCP", "创建登录包失败", e);
		}
		// 创建连接
		initConnection();
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		if (socketResult != null) {
			socketResult.content(msg.what, (byte[]) msg.obj);
		}
	}

	// 创建连接
	private void initConnection() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					String[] addressDest = address.split(":");
					socket = new Socket(addressDest[0], Integer.parseInt(addressDest[1]));
					socket.setSoTimeout(30000);
					inputStream = socket.getInputStream();
					outputStream = socket.getOutputStream();
					if (loginPkg != null) {
						LogUtil.v(new FormaterBytes().toHexString(loginPkg));
						outputStream.write(loginPkg);
					}
				} catch (Exception e) {
					sendEmptyMessage(CreateFail);
				}
				byte[] buffer = new byte[8192];
				byte[] remains = null;
				while (socket != null && inputStream != null) {
					try {
						int length = inputStream.read(buffer);
						if (length == -1) {
							break;
						}
						byte[] temp = new byte[length];
						System.arraycopy(buffer, 0, temp, 0, length);
						if (remains != null && remains.length > 0) {
							// 合并上次没有接收完的包
							temp = mergePackage(remains, temp);
						}
						remains = handlePackage(temp);
						Thread.sleep(10);
					} catch (Exception e) {
						sendEmptyMessage(ReceiveFail);
						break;
					}
				}
			}
		});
	}

	// 解决粘包
	private final byte[] handlePackage(byte[] unHandledPkg) {
		/** 调用一次read，从Server收到的数据包(可能是半包、1个包、1.x、2.x....) */
		// 数据包长度
		int pkgLen = unHandledPkg.length;

		/** 一个完整数据包的长度 */
		FormaterBytes forBytes = new FormaterBytes();
		int completePkgLen = forBytes.byteToInt(unHandledPkg[2], unHandledPkg[3]);

		if (completePkgLen > pkgLen) {
			/** 当前收到的数据不到一个完整包，则直接返回，等待下一个包 */
			return unHandledPkg;
		} else if (completePkgLen == pkgLen) {
			/** 一个完整包 */
			// 登录包判断
			if ((unHandledPkg[5] & 0xFF) == 0xf1 && (unHandledPkg[6] & 0xFF) == 0x01) {
				if (new MakePackage().parseBytes(unHandledPkg).equals("1")) {
					sendEmptyMessage(CreateSuccess);
					return null;
				} else if (new MakePackage().parseBytes(unHandledPkg).equals("0")) {
					sendEmptyMessage(CreateFail);
					return null;
				}
			}

			//心跳包
			if((unHandledPkg[5] & 0xFF) == 0xf1 && (unHandledPkg[6] & 0xFF) == 0x00){
				Message msg = new Message();
				msg.what = ReceiveDone;
				msg.obj = unHandledPkg;
				sendMessage(msg);
			}

			// 实时数据包或者详情包
			if ((unHandledPkg[5] & 0xFF) == 0xf1 && (unHandledPkg[6] & 0xFF) == 0x02 || (unHandledPkg[6] & 0xFF) == 0x03|| (unHandledPkg[6] & 0xFF) == 0x04 
					|| (unHandledPkg[6] & 0xFF) == 0x05 || (unHandledPkg[6] & 0xFF) == 0x06) {
				Message msg = new Message();
				msg.what = ReceiveDone;
				msg.obj = unHandledPkg;
				sendMessage(msg);
			}
			return null;
		} else {
			/** 有多个包，那么就递归解析， */
			int pkgAmount = forBytes.byteToInt(unHandledPkg[12], unHandledPkg[13]);// 总包数
			int pkgCurrent = forBytes.byteToInt(unHandledPkg[14], unHandledPkg[15]);// 当前包序号
			boolean condition = (pkgCurrent >= pkgAmount);
			if (condition) {
				Message msg = new Message();
				msg.what = ReceiveDone;
				byte[] temp = new byte[completePkgLen];
				System.arraycopy(unHandledPkg, 0, temp, 0, completePkgLen);
				msg.obj = temp;
				sendMessage(msg);
				temp = null;
			}
			/** 截取除完整包后的剩余部分 */
			byte[] remain = getSubBytes(unHandledPkg, completePkgLen, pkgLen - completePkgLen);
			return handlePackage(remain);
		}
	}

	// src - 源数组。
	// srcPos - 源数组中的起始位置。
	// dest - 目标数组。
	// destPos - 目标数据中的起始位置。
	// length - 要复制的数组元素的数量。
	private byte[] getSubBytes(byte[] dataLength, int pkgLen, int pkgRemain) {
		byte[] release = new byte[pkgRemain];
		System.arraycopy(dataLength, pkgLen, release, 0, pkgRemain);
		return release;
	}

	// 合并粘包
	private byte[] mergePackage(byte[] release, byte[] temp) {
		byte[] temporary = new byte[release.length + temp.length];
		System.arraycopy(release, 0, temporary, 0, release.length);
		System.arraycopy(temp, 0, temporary, release.length, temp.length);
		return temporary;
	}

	/**
	 * @param 发送字节数据
	 * @return 返回发送结果
	 */
	public boolean sendPkg(final byte[] pkg) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (outputStream != null) {
						outputStream.write(pkg);
						outputStream.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		return true;
	}

	/**
	 * 关闭连接,置空回调接口
	 */
	public void closeSocket() {
		LogUtil.i("-----关闭TCP连接");
		try {
			// 接口置空,停止回调
			if (socket != null) {
				socket.close();
				socket = null;
			}
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
			if (outputStream != null) {
				outputStream.close();
				outputStream = null;
			}
		} catch (IOException e) {
			Log.e("closeSocket", "关闭连接失败", e);
		} catch (NullPointerException e) {
			Log.e("closeSocket", "关闭连接失败", e);
		}
	}
}
