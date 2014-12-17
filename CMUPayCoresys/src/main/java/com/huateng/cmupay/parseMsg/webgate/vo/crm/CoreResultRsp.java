package com.huateng.cmupay.parseMsg.webgate.vo.crm;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * 核心返回结果
 * 
 * @author Gary
 * 
 */
public class CoreResultRsp {
	/**
	 * 后台通知URL
	 */
	@JsonProperty
	private String serverURL;
	/**
	 * 前台通知URL
	 */
	@JsonProperty
	private String backURL;
	/**
	 * 错误码
	 */
	@JsonProperty
	private String rspCode;
	/**
	 * 错误信息
	 */
	@JsonProperty
	private String rspInfo;

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public String getBackURL() {
		return backURL;
	}

	public void setBackURL(String backURL) {
		this.backURL = backURL;
	}

	public String getRspCode() {
		return rspCode;
	}

	public void setRspCode(String rspCode) {
		this.rspCode = rspCode;
	}

	public String getRspInfo() {
		return rspInfo;
	}

	public void setRspInfo(String rspInfo) {
		this.rspInfo = rspInfo;
	}

}
