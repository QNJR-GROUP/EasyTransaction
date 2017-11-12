package com.yiqiniu.easytrans.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.springframework.util.StringUtils;

public class ObjectDigestUtil {
	
	private ObjectDigestUtil(){
	}
	
	private final static String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7",
           "8", "9", "a", "b", "c", "d", "e", "f"};

   
//   private static Logger LOG = LoggerFactory.getLogger(MD5.class);
   
   /**
    * 转换字节数组为16进制字串
    * @param b 字节数组
    * @return 16进制字串
    */
   private static String byteArrayToHexString(byte[] b) {
       StringBuilder resultSb = new StringBuilder();
       for (byte aB : b) {
           resultSb.append(byteToHexString(aB));
       }
       return resultSb.toString();
   }

   /**
    * 转换byte到16进制
    * @param b 要转换的byte
    * @return 16进制格式
    */
   private static String byteToHexString(byte b) {
       int n = b;
       if (n < 0) {
           n = 256 + n;
       }
       int d1 = n / 16;
       int d2 = n % 16;
       return hexDigits[d1] + hexDigits[d2];
   }
	
	public static String getObjectMD5(Object obj) {
		return MD5Encode(getObjectString(obj));
	}

	/**
	 * MD5编码
	 * 
	 * @param origin
	 *            原始字符串
	 * @return 经过MD5加密之后的结果
	 */
	public static String MD5Encode(String origin) {
		String resultString = null;
		try {
			resultString = origin;
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(resultString.getBytes("UTF-8"));
			resultString = byteArrayToHexString(md.digest());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultString;
	}

	/**
	 * 将Object按照key1=value1&key2=value2的形式拼接起来，程序不递归。Value需为基本类型，
	 * 或者其toString方法能表示其实际的值
	 * 
	 * @param o
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getObjectString(Object o) {

		if (isBasicType(o)) {
			return o.toString();
		} else if (o.getClass().isArray()) {
			return getArrayValue((Object[]) o);
		} else if (o instanceof Iterable) {
			Iterable itrPropertie = (Iterable) o;
			return getItrValue(itrPropertie);
		} else if (o instanceof Map) {
			return getOrderedMapString((Map) o);
		} else {
			return getComlpexObjectOrderedString(o);
		}
	}

	private static String getComlpexObjectOrderedString(Object o) {
		ArrayList<String> list = new ArrayList<String>();
		try {
			// LOG.error("检测：" + JSONUtil.toJson(o));
			PropertyDescriptor[] propertyDescriptors = Introspector
					.getBeanInfo(o.getClass(), Object.class)
					.getPropertyDescriptors();
			for (PropertyDescriptor f : propertyDescriptors) {

				Object propertie = f.getReadMethod().invoke(o);
				if (propertie != null) {
					String objectString = getObjectString(propertie);
					if (!StringUtils.isEmpty(objectString)) {
						list.add(f.getName() + "=" + objectString + "&");
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		int size = list.size();
		String[] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append(arrayToSort[i]);
		}

		return "{" + sb.toString() + "}";
	}

	@SuppressWarnings("rawtypes")
	private static String getItrValue(Iterable propertie) {

		StringBuilder result = new StringBuilder();
		result.append("[");

		Iterator iterator = propertie.iterator();
		while (iterator.hasNext()) {
			Object next = iterator.next();
			result.append(getObjectString(next) + ",");
		}
		result.append("]");

		return result.toString();
	}

	private static String getArrayValue(Object[] propertie) {
		StringBuilder result = new StringBuilder();
		result.append("[");
		for (Object o : propertie) {
			result.append(getObjectString(o) + ",");
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * 将Object按照key1=value1&key2=value2的形式拼接起来，程序不递归。Value需为基本类型，
	 * 或者其toString方法能表示其实际的值
	 * 
	 * @param o
	 * @return
	 */
	private static String getOrderedMapString(Map<Object, Object> map) {
		ArrayList<String> list = new ArrayList<String>();
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			if (entry.getValue() != null) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				String objectString = getObjectString(value);
				if (!StringUtils.isEmpty(objectString)) {
					list.add(getObjectString(key) + "=" + objectString + "&");
				}

			}
		}
		int size = list.size();
		String[] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append(arrayToSort[i]);
		}
		return "{" + sb.toString() + "}";
	}

	private static boolean isBasicType(Object o) {
		if (o instanceof String) {
			return true;
		}
		if (o instanceof Integer) {
			return true;
		}
		if (o instanceof Long) {
			return true;
		}
		if (o instanceof Float) {
			return true;
		}
		if (o instanceof Double) {
			return true;
		}
		if (o instanceof Byte) {
			return true;
		}
		if (o instanceof Boolean) {
			return true;
		}
		if (o instanceof Enum) {
			return true;
		}
		if (o instanceof BigDecimal) {
			return true;
		}
		return false;
	}

}
