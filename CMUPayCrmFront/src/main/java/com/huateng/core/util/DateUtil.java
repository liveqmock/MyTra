package com.huateng.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 日期工具类
 * 
 * @author Gary
 * 
 */
public class DateUtil {
	public static String FORMAT_DATE = "yyyy-MM-dd";
	private static final String userName="<UserName>**********</UserName>";
	private static final String userId="<UserID>**********</UserID>";
	private static final String bankAcctID="<BankAcctID>************</BankAcctID>";

	private static final Logger logger = LoggerFactory.getLogger("DateUtil");
	/**
	 * 获取当前日期
	 * 
	 * @return 字符串格式
	 */
	public static String getCurrentDate() {
		SimpleDateFormat df = new SimpleDateFormat(FORMAT_DATE);
		return df.format(new Date());
	}

	/**
	 * 获取当然日期
	 * 
	 * @return 日期格式
	 */
	public static Date getNowDate() {
		return new Date();
	}

	/**
	 * 获取当前日期和时间
	 * 
	 * @return
	 */
	public static String getCurrentFullDate() {
		return DateUtil.getCurrentDate() + " " + TimeUtil.getCurrentTime();
	}

	/**
	 * 转换特殊字符
	 * @param xmlContent
	 * @return
	 */
	public static String paseLog(String xmlContent){	
		String xmlStr = xmlContent;
		try {
			if (StringUtils.isBlank(xmlContent))
				return xmlContent;
			
			StringBuffer xmlBody = new StringBuffer(xmlContent);
			if (xmlContent.indexOf("<UserName>") != -1) {
				xmlBody = new StringBuffer();
				String startXml = xmlContent.substring(0,
						xmlContent.indexOf("<UserName>"));
				xmlBody.append(startXml);
				xmlBody.append(userName);
				String endXml = xmlContent.substring(xmlContent
						.indexOf("</UserName>") + 11);
				xmlBody.append(endXml);
				xmlContent = xmlBody.toString();
			}
			if (xmlContent.indexOf("<BankAcctID>") != -1) {
				xmlBody = new StringBuffer();
				String startStr = xmlContent.substring(0,
						xmlContent.indexOf("<BankAcctID>"));
				xmlBody.append(startStr);
				xmlBody.append(bankAcctID);
				String endXml = xmlContent.substring(xmlContent
						.indexOf("</BankAcctID>") + 13);
				xmlBody.append(endXml);
				xmlContent = xmlBody.toString();
			}
			if (xmlContent.indexOf("<UserID>") != -1) {
				xmlBody = new StringBuffer();
				String startStr = xmlContent.substring(0,
						xmlContent.indexOf("<UserID>"));
				xmlBody.append(startStr);
				xmlBody.append(userId);
				String endXml = xmlContent.substring(xmlContent
						.indexOf("</UserID>") + 9);
				xmlBody.append(endXml);
				xmlContent = xmlBody.toString();
			}
		} catch (Exception e) {
			logger.error("网状网报文错误");
			return xmlStr;
		}
		return xmlContent;
	}
	public static void main(String[] args) {
		System.out.println(DateUtil.getCurrentDate());
	}
}
