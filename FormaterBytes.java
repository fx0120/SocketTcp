package com.bsj.company.net;

import java.io.UnsupportedEncodingException;

public class FormaterBytes {

	// 转化为16进制字符串
		public String toHexString(byte data[]) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < data.length; i++) {
				int n = ((int) data[i]) & 0xFF;
				if (n > 15) {
					sb.append((Integer.toHexString(n)) + " ");
				} else {
					sb.append("0" + (Integer.toHexString(n)) + " ");
				}
			}
			return sb.toString();
		}

		// 2字节
		public int byteToInt(byte a, byte b) {
			String resultA, resultB;
			int n = ((int) a) & 0xFF;
			if (n > 15) {
				resultA = (Integer.toHexString(n));
			} else {
				resultA = "0" + (Integer.toHexString(n));
			}
			n = ((int) b) & 0xFF;
			if (n > 15) {
				resultB = (Integer.toHexString(n));
			} else {
				resultB = "0" + (Integer.toHexString(n));
			}
			return Integer.parseInt(resultA + resultB, 16);
		}

		public int byteToInteger(int index0, int index1) {
			int hi = index0 & 0xff;
			int low = index1 & 0xff;
			return hi * 256 + low;
		}

		public int byteToInteger(int index0, int index1, int index2) {
			index0 = index0 << 16;
			index1 = index1 << 8;
			return (index0 | index1 | index2);
		}

		public int byteToInteger(int index0, int index1, int index2, int index3) {
			index0 = index0 << 24;
			index1 = index1 << 16;
			index2 = index2 << 8;
			return (index0 | index1 | index2 | index3);
		}

		// 按位数补起"0"
		public String appendZero(int length, String content) {
			int lengthByte = 0;
			try {
				lengthByte = content.getBytes("GBK").length;
			} catch (UnsupportedEncodingException e) {
			}
			String result = lengthByte + "";
			for (int i = 0; i < length - (lengthByte + "").length(); i++) {
				result = "0" + result;
			}
			return result;
		}

		// 按位数补起"0"
		public String appendZero(int length, String content, int pkgLength) {
			String result = "";
			try {
				int lengthByte = content.getBytes("GBK").length + pkgLength;
				result = lengthByte + "";
				for (int i = 0; i < length - (lengthByte + "").length(); i++) {
					result = "0" + result;
				}
			} catch (UnsupportedEncodingException e) {
			}
			return result;
		}
	
}
