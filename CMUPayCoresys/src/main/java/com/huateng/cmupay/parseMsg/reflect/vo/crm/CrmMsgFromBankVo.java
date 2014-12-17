package com.huateng.cmupay.parseMsg.reflect.vo.crm;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import com.huateng.cmupay.parseMsg.reflect.handle.BaseMsgVo;
import com.huateng.cmupay.parseMsg.reflect.handle.CustomAnnotation;
import com.huateng.cmupay.parseMsg.reflect.handle.ParentAnnotation;

/**
 * @author cmt
 * @version 创建时间：2013-11-21 下午6:12:57
 * 银行端发起的交易，由核心发往省前置时，用到的数据VO,类似于CrmMsgVo，
 * 只是SNReserve的power不同
 */
public class CrmMsgFromBankVo extends BaseMsgVo {
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.Version")
	@NotNull(message = "version不能为空")
	@Pattern(regexp = "0100", message = "version格式不正确")
	private String version;// 报文版本号
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.TestFlag")
	@NotNull(message = "testFlag不能为空")
	@Pattern(regexp = "[0-1]", message = "testFlag格式不正确")
	private String testFlag;// 测试标记 0非测试交易 1测试交易
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.BIPType.BIPCode")
	@NotNull(message = "bIPCode不能为空")
	// BIP1A152
	@Pattern(regexp = "BIP1A1[0-9]{2}", message = "bIPCode格式不正确")
	private String BIPCode;// 业务功能代码
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.BIPType.ActivityCode")
	@NotNull(message = "activityCode不能为空")
	// T1000159
	@Pattern(regexp = "T10001[0-9]{2}", message = "activityCode格式不正确")
	private String activityCode;// 交易代码
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.BIPType.ActionCode")
	@NotNull(message = "actionCode不能为空")
	@Pattern(regexp = "0", message = "actionCode格式不正确")
	private String actionCode;// 交易动作代码 0请求 1应答
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.RoutingInfo.OrigDomain")
	@NotNull(message = "origDomain不能为空")
	@Pattern(regexp = "BOSS", message = "origDomain格式不正确")
	private String origDomain;// 发起方应用域代码
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.RoutingInfo.RouteType")
	@NotNull(message = "routeType不能为空")
	@Pattern(regexp = "0[0-1]", message = "routeType格式不正确")
	private String routeType;// 路由类型
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.RoutingInfo.Routing.HomeDomain")
	@NotNull(message = "homeDomain不能为空")
	@Pattern(regexp = "UPSS", message = "homeDomain格式不正确")
	private String homeDomain;// 归属方应用域代码
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.RoutingInfo.Routing.RouteValue")
	@NotNull(message = "routeValue不能为空")
	@Pattern(regexp = "997", message = "routeValue格式不正确")
	private String routeValue;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.TransInfo.SessionID")
	@NotNull(message = "sessionID不能为空")
	@Pattern(regexp = "[0-9,A-Z,a-z]{1,30}", message = "sessionID格式不正确")
	private String sessionID;// 业务流水号
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.TransInfo.TransIDO")
	@NotNull(message = "transIDO不能为空")
	@Pattern(regexp = "[0-9,A-Z,a-z]{1,30}", message = "transIDO格式不正确")
	private String transIDO;// 发起方交易流水号
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.TransInfo.TransIDOTime")
	@NotNull(message = "transIDOTime不能为空")
	@Pattern(regexp = "(([0-9]{4}(0[13578]|10|12)(0[1-9]|[12][0-9]|3[0-1]))"
			+ "|([0-9]{4}(0[469]|11)(0[1-9]|[12][0-9]|30))"
			+ "|([0-9]{4}02(0[1-9]|[12][0-9])))"
			+ "(0[0-9]|1[0-9]|2[0-3])([0-5][0-9][0-5][0-9])", message = "transIDOTime格式不正确")
	private String transIDOTime;// 处理时间 发起方发起请求的时间
	// 约束 ?
	@CustomAnnotation(path = "InterBOSS.TransInfo.TransIDH")
	@Pattern(regexp = "[0-9,A-Z,a-z]{1,30}", message = "transIDH格式不正确")
	private String transIDH;// 落地方交易流水号
	// 约束 ?
	@CustomAnnotation(path = "InterBOSS.TransInfo.TransIDHTime")
	@Pattern(regexp = "[0-9]{14}", message = "transIDHTime格式不正确")
	private String transIDHTime;// 落地方处理请求的时间
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.TransIDC", power = '0')
	@NotNull(message = "transIDC不能为空")
	@Pattern(regexp = "[0-9,A-Z,a-z]{1,60}", message = "transIDC格式不正确")
	private String transIDC;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.ConvID", power = '0')
	@NotNull(message = "convID不能为空")
	@Length(min = 1, max = 60, message = "convID格式不正确")
	private String convID;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.CutOffDay", power = '0')
	@NotNull(message = "cutOffDay不能为空")
	@Pattern(regexp = "([0-9]{4}(0[13578]|10|12)(0[1-9]|[12][0-9]|3[0-1]))"
			+ "|([0-9]{4}(0[469]|11)(0[1-9]|[12][0-9]|30))"
			+ "|([0-9]{4}02(0[1-9]|[12][0-9]))", message = "cutOffDay格式不正确")
	private String cutOffDay;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.OSNTime", power = '0')
	@NotNull(message = "OSNTime不能为空")
	@Pattern(regexp = "[0-9]{14}", message = "OSNTime格式不正确")
	private String OSNTime;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.OSNDUNS", power = '0')
	@NotNull(message = "oSNDUNS不能为空")
	@Pattern(regexp = "[0-9]{4}", message = "oSNDUNS格式不正确")
	private String OSNDUNS;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.HSNDUNS", power = '0')
	@NotNull(message = "HSNDUNS不能为空")
	@Pattern(regexp = "[0-9]{4}", message = "HSNDUNS格式不正确")
	private String HSNDUNS;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.MsgSender", power = '0')
	@NotNull(message = "msgSender不能为空")
	@Pattern(regexp = "[0-9]{4}", message = "msgSender格式不正确")
	private String msgSender;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.SNReserve.MsgReceiver", power = '0')
	@NotNull(message = "msgReceiver不能为空")
	@Pattern(regexp = "[0-9]{4}", message = "msgReceiver格式不正确")
	private String msgReceiver;
	// 约束 ?
	@CustomAnnotation(path = "InterBOSS.SNReserve.Priority", power = '0')
	@Pattern(regexp = "[0-9]{1,2}", message = "priority格式不正确")
	private String priority;
	// 约束 ?
	@CustomAnnotation(path = "InterBOSS.SNReserve.ServiceLevel", power = '0')
	@Pattern(regexp = "[0-9]{1,2}", message = "serviceLevel格式不正确")
	private String serviceLevel;
	// 约束 ?
	@CustomAnnotation(path = "InterBOSS.SNReserve.SvcContType", power = '0')
	@Pattern(regexp = "[0-9]{2}", message = "svcContType格式不正确")
	private String svcContType;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.Response.RspType")
	@Pattern(regexp = "[0-9]", message = "rspType格式不正确")
	private String rspType;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.Response.RspCode")
	@Pattern(regexp = "[0-9]{4}", message = "rspCode格式不正确")
	private String rspCode;
	// 约束 1
	@CustomAnnotation(path = "InterBOSS.Response.RspDesc")
	@Length(max = 128, message = "RspDesc格式不正确")
	private String rspDesc;

	@ParentAnnotation
	@CustomAnnotation(path = "InterBOSS.SvcCont")
	private Object body;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getTestFlag() {
		return testFlag;
	}

	public void setTestFlag(String testFlag) {
		this.testFlag = testFlag;
	}

	public String getBIPCode() {
		return BIPCode;
	}

	public void setBIPCode(String bIPCode) {
		BIPCode = bIPCode;
	}

	public String getActivityCode() {
		return activityCode;
	}

	public void setActivityCode(String activityCode) {
		this.activityCode = activityCode;
	}

	public String getActionCode() {
		return actionCode;
	}

	public void setActionCode(String actionCode) {
		this.actionCode = actionCode;
	}

	public String getOrigDomain() {
		return origDomain;
	}

	public void setOrigDomain(String origDomain) {
		this.origDomain = origDomain;
	}

	public String getRouteType() {
		return routeType;
	}

	public void setRouteType(String routeType) {
		this.routeType = routeType;
	}

	public String getHomeDomain() {
		return homeDomain;
	}

	public void setHomeDomain(String homeDomain) {
		this.homeDomain = homeDomain;
	}

	public String getRouteValue() {
		return routeValue;
	}

	public void setRouteValue(String routeValue) {
		this.routeValue = routeValue;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getTransIDO() {
		return transIDO;
	}

	public void setTransIDO(String transIDO) {
		this.transIDO = transIDO;
	}

	public String getTransIDOTime() {
		return transIDOTime;
	}

	public void setTransIDOTime(String transIDOTime) {
		this.transIDOTime = transIDOTime;
	}

	public String getTransIDH() {
		return transIDH;
	}

	public void setTransIDH(String transIDH) {
		this.transIDH = transIDH;
	}

	public String getTransIDHTime() {
		return transIDHTime;
	}

	public void setTransIDHTime(String transIDHTime) {
		this.transIDHTime = transIDHTime;
	}

	public String getTransIDC() {
		return transIDC;
	}

	public void setTransIDC(String transIDC) {
		this.transIDC = transIDC;
	}

	public String getConvID() {
		return convID;
	}

	public void setConvID(String convID) {
		this.convID = convID;
	}

	public String getCutOffDay() {
		return cutOffDay;
	}

	public void setCutOffDay(String cutOffDay) {
		this.cutOffDay = cutOffDay;
	}

	public String getOSNTime() {
		return OSNTime;
	}

	public void setOSNTime(String oSNTime) {
		OSNTime = oSNTime;
	}

	public String getOSNDUNS() {
		return OSNDUNS;
	}

	public void setOSNDUNS(String oSNDUNS) {
		OSNDUNS = oSNDUNS;
	}

	public String getHSNDUNS() {
		return HSNDUNS;
	}

	public void setHSNDUNS(String hSNDUNS) {
		HSNDUNS = hSNDUNS;
	}

	public String getMsgSender() {
		return msgSender;
	}

	public void setMsgSender(String msgSender) {
		this.msgSender = msgSender;
	}

	public String getMsgReceiver() {
		return msgReceiver;
	}

	public void setMsgReceiver(String msgReceiver) {
		this.msgReceiver = msgReceiver;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getServiceLevel() {
		return serviceLevel;
	}

	public void setServiceLevel(String serviceLevel) {
		this.serviceLevel = serviceLevel;
	}

	public String getSvcContType() {
		return svcContType;
	}

	public void setSvcContType(String svcContType) {
		this.svcContType = svcContType;
	}

	public String getRspType() {
		return rspType;
	}

	public void setRspType(String rspType) {
		this.rspType = rspType;
	}

	public String getRspCode() {
		return rspCode;
	}

	public void setRspCode(String rspCode) {
		this.rspCode = rspCode;
	}

	public String getRspDesc() {
		return rspDesc;
	}

	public void setRspDesc(String rspDesc) {
		this.rspDesc = rspDesc;
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}

	public CrmMsgFromBankVo() {
		super();
	}

	/**
	 * 通过CrmMsgVo对象来构造CrmMsgFromBankVo对象
	 * 注意setObject的副作用
	 * @param crmMsgVo
	 */
	public CrmMsgFromBankVo(CrmMsgVo crmMsgVo) {
		super();
		setVersion(crmMsgVo.getVersion());
		setTestFlag(crmMsgVo.getTestFlag());
		setBIPCode(crmMsgVo.getBIPCode());
		setActivityCode(crmMsgVo.getActivityCode());
		setActionCode(crmMsgVo.getActionCode());
		setOrigDomain(crmMsgVo.getOrigDomain());
		setRouteType(crmMsgVo.getRouteType());
		setHomeDomain(crmMsgVo.getHomeDomain());
		setRouteValue(crmMsgVo.getRouteValue());
		setSessionID(crmMsgVo.getSessionID());
		setTransIDO(crmMsgVo.getTransIDO());
		setTransIDOTime(crmMsgVo.getTransIDOTime());
		setTransIDH(crmMsgVo.getTransIDH());
		setTransIDHTime(crmMsgVo.getTransIDHTime());
		setTransIDC(crmMsgVo.getTransIDC());
		setConvID(crmMsgVo.getConvID());
		setCutOffDay(crmMsgVo.getCutOffDay());
		setOSNTime(crmMsgVo.getOSNTime());
		setOSNDUNS(crmMsgVo.getOSNDUNS());
		setHSNDUNS(crmMsgVo.getHSNDUNS());
		setMsgSender(crmMsgVo.getMsgSender());
		setMsgReceiver(crmMsgVo.getMsgReceiver());
		setPriority(crmMsgVo.getPriority());
		setServiceLevel(crmMsgVo.getServiceLevel());
		setSvcContType(crmMsgVo.getSvcContType());
		setRspType(crmMsgVo.getRspType());
		setRspCode(crmMsgVo.getRspCode());
		setRspDesc(crmMsgVo.getRspDesc());
		setBody(crmMsgVo.getBody());
	}
}
