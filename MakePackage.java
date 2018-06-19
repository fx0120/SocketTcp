package com.bsj.company.net;

import java.io.UnsupportedEncodingException;

public class MakePackage {
	/**
	 * 
	 * @param from
	 *            指令号
	 * @param content
	 *            发送的内容
	 * @return 打包好的字节数据
	 */
	public byte[] generalPackage(int from, String content) {
		byte[] msgByte = null;
		try {
			msgByte = content.getBytes("GBK");
		} catch (Exception e) {
			return null;
		}
		byte[] result = new byte[msgByte.length + 19];
		// 包头
		result[0] = 0x2A;
		result[1] = 0x2B;
		// 包长
		result[2] = (byte) (result.length / 255);
		result[3] = (byte) (result.length % 255);
		// 来源
		result[4] = (byte) 0x91;
		// 指令
		result[5] = (byte) 0xF0;
		result[6] = (byte) from;
		// 用户ID
		result[7] = 0x00;
		result[8] = 0x00;
		result[9] = 0x00;
		result[10] = 0x00;
		// 消息格式
		result[11] = 0x00;
		// 总包数
		result[12] = 0x00;
		result[13] = 0x01;
		// 当前包数
		result[14] = 0x00;
		result[15] = 0x01;
		System.arraycopy(msgByte, 0, result, 16, msgByte.length);
		result[result.length - 3] = getXorValue(result, 0, result.length - 3);
		result[result.length - 2] = 0x2B;
		result[result.length - 1] = 0x2A;
		return result;
	}
	

	// 计算检验值
	private byte getXorValue(byte[] buff, int offset, int count) {
		int xorValue = buff[offset] & 0xFF;
		for (int i = 1; i < count; i++) {
			xorValue ^= ((int) buff[offset + i]) & 0xFF;
		}
		return (byte) (xorValue & 0xFF);
	}

	// 解包
	public String parseBytes(byte[] result) {
		String restult = null;
		byte[] buffer = null;
		byte[] buff = null;
		for (int i = 0; i < result.length;) {
			int length = new FormaterBytes().byteToInteger(result[i + 2], result[i + 3]);
			if (buffer == null) {
				buffer = new byte[length - 19];
				System.arraycopy(result, i + 16, buffer, 0, buffer.length);
			} else {
				buff = buffer;
				buffer = new byte[buffer.length + length - 19];
				System.arraycopy(buff, 0, buffer, 0, buff.length);
				System.arraycopy(result, i + 16, buffer, buff.length, length - 19);
			}
			i = i + length;
		}
		try {
			restult = new String(buffer, "GBK");
		} catch (UnsupportedEncodingException e) {
		}
		return restult;
	}
	
	// 解包
	public Object parseObjectBytes(byte[] result) {
		Object restult = null;
		byte[] buffer = null;
		byte[] buff = null;
		for (int i = 0; i < result.length;) {
			int length = new FormaterBytes().byteToInteger(result[i + 2], result[i + 3]);
			if (buffer == null) {
				buffer = new byte[length - 19];
				System.arraycopy(result, i + 16, buffer, 0, buffer.length);
			} else {
				buff = buffer;
				buffer = new byte[buffer.length + length - 19];
				System.arraycopy(buff, 0, buffer, 0, buff.length);
				System.arraycopy(result, i + 16, buffer, buff.length, length - 19);
			}
			i = i + length;
		}
		try {
			restult = new String(buffer, "GBK");
		} catch (UnsupportedEncodingException e) {
		}
		return restult;
	}
}
