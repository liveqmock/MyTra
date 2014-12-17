package com.huateng.cmupay.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.commons.lang.StringUtils;
import org.springframework.jms.core.MessageCreator;
import com.huateng.cmupay.action.IBaseAction;
import com.huateng.cmupay.constant.CommonConstant;
import com.huateng.cmupay.constant.RspCodeConstant;
import com.huateng.cmupay.controller.cache.OrgMapTransCache;
import com.huateng.cmupay.exception.AppBizException;
import com.huateng.cmupay.parseMsg.reflect.handle.BaseMsgVo;
import com.huateng.cmupay.parseMsg.webgate.vo.crm.CoreResultRsp;
import com.huateng.toolbox.json.JacksonUtils;

/**
 * @author cmt
 * @version 创建时间：2013-3-11 下午3:15:30 类说明
 */
public class WebGateMessageListener extends AbsMessageListener {

	private Map<String, IBaseAction<Object, Object>> transactionMap;

	public void setTransactionMap(
			Map<String, IBaseAction<Object, Object>> transactionMap) {
		this.transactionMap = transactionMap;
	}

	@Override
	public void onMessage(Message message) {
		Object resMsg = null;
		String orderId="";
		Map<String, Object> beanPropertyMap = null;
		try {
			// 接受到文字消息
			if (message instanceof TextMessage) {
				/********** 接收参数 ***********/
				final Destination replyToDest = message.getJMSReplyTo();// 回复queue
				if (replyToDest == null) {
					throw new Exception("replyToDest is null");
				}
				final String senderid = message.getStringProperty("senderid");
				final String seq = message.getStringProperty("reqTxnSeq");// 回复流水
				if (seq == null || "".equals(seq)) {
					throw new Exception("mqSeq is null");
				}
				String param = ((TextMessage) message).getText();// 串参数
				if (param == null || "".equals(param)) {
					throw new Exception("TextMessage is null");
				}
				try {
                	beanPropertyMap = JacksonUtils.json2Map(param, false);
                	if(beanPropertyMap!=null)
                	    orderId = (String) (beanPropertyMap.get("OrderID")==null?beanPropertyMap.get("SessionID"):beanPropertyMap.get("OrderID"));
                	log.info("接收到网关发起的报文转换成功,交易流水:{}",new Object[] { orderId });
                	logger.info("接收到网关发起的报文转换成功,交易流水:{}",new Object[] { orderId });
				} catch (Exception e) {
					log.error("转换网关报文异常:",e);
					log.error("获取到的参数:{}",param );
					logger.error("转换网关报文异常:", e);
					logger.error("获取到的参数:{}", param);
					CoreResultRsp resMsgb = new CoreResultRsp();
					resMsgb.setRspCode(RspCodeConstant.Gate.GATE_9999.getValue());
					resMsgb.setRspInfo(RspCodeConstant.Gate.GATE_9999.getDesc());
					resMsgb.setBackURL("");
					resMsgb.setServerURL("");
					resMsg=resMsgb;
				}
				final String transCode = (String) beanPropertyMap.get("TransCode");
				if (StringUtils.equals(transCode,CommonConstant.TransCode.GatePay.getValue())
						|| StringUtils.equals(transCode,CommonConstant.TransCode.GateSign.getValue())) {
					resMsg=checkTransCodeStatus(beanPropertyMap);
				}
				if (resMsg == null) {
					resMsg = processMsg(beanPropertyMap, param);
				}

				/************ JMS交易处理结果消息回复 **************/
				final String rtnJson = JacksonUtils.bean2Json(resMsg);// 返回结果
				if (replyToDest != null) {
					template.send(replyToDest, new MessageCreator() {
						public Message createMessage(Session session)
								throws JMSException {
							Message msg = session.createTextMessage(rtnJson);
							msg.setStringProperty("senderid", senderid);
							msg.setStringProperty("reqTxnSeq", seq);
							return msg;
						}
					});
					logger.info("订单号{}返回给支付网关的应答报文:{}",orderId, rtnJson);
				}
			}
		} catch (Exception e) {
			log.error("支付网关交易发生错误", e);
			log.error("订单号:{}", orderId);
			logger.error("支付网关交易发生错误",e);
			logger.error("订单号:{}", orderId);
		}
	}

	/**
	 * 检查银行和省份是否状态是否正常
	 * 
	 * @param bankId
	 * @param areaId
	 * @return
	 */
	private CoreResultRsp checkTransCodeStatus(Map<String, Object> beanPropertyMap) {
		String areaId = "";
		CoreResultRsp resMsg = null;
		String bankId = (String) beanPropertyMap.get("BankID");
		String orderId = (String) (beanPropertyMap.get("OrderID")==null?beanPropertyMap.get("SessionID"):beanPropertyMap.get("OrderID"));
		final String transCode = (String) beanPropertyMap.get("TransCode");
		// 检查业务类型是签约还是支付
		if (StringUtils.equals(transCode,
				CommonConstant.TransCode.GatePay.getValue())) {
			areaId = (String) beanPropertyMap.get("MerID");
		} else if (StringUtils.equals(transCode,
				CommonConstant.TransCode.GateSign.getValue())) {
			areaId = (String) beanPropertyMap.get("OrigDomain");
		}
		logger.info("订单号: {},获取{}业务的省份{}", new Object[] { orderId,transCode, areaId });
		if (StringUtils.isBlank(bankId) || StringUtils.isBlank(areaId)) {
			resMsg = new CoreResultRsp();
			logger.info(" 订单号:{},bankId:{},areaId:{}",new Object[]{orderId,bankId,areaId});
			resMsg.setRspCode(RspCodeConstant.Crm.CRM_2A18.getValue());
			resMsg.setRspInfo(RspCodeConstant.Crm.CRM_2A18.getDesc());
			resMsg.setBackURL((String) beanPropertyMap.get("BackURL"));
			resMsg.setServerURL((String) beanPropertyMap.get("ServerURL"));
		}
		String isTransCode=offOrgTrans(areaId,bankId,transCode);
		if (StringUtils.isNotBlank(isTransCode)) {
			resMsg = new CoreResultRsp();
			logger.info("订单号:{},网关发起方{}机构没有此服务的权限,机构号:{}",new Object[]{orderId,areaId,bankId});
			log.error("订单号:{},网关发起方{}机构没有此服务的权限,机构号:{}",new Object[]{orderId,areaId,bankId});
			resMsg.setRspCode(RspCodeConstant.Crm.CRM_3A25.getValue());
			resMsg.setRspInfo(RspCodeConstant.Crm.CRM_3A25.getDesc());
			resMsg.setBackURL((String) beanPropertyMap.get("BackURL"));
			resMsg.setServerURL((String) beanPropertyMap.get("ServerURL"));
		}
		return resMsg;
	}

	
	/**
	 * 验证机构的状态及机构交易权限，校验顺序：发起方就够，外部机构，接收方机构，机构交易权限
	 * @param reqOrg 发起方机构代码
	 * @param rcvOrg 接收方机构代码
	 * @param thridOrg 外部机构代码
	 * @param transCode 内部交易代码
	 * @return  reqOrg or rcvOrg or thridOrg,if any org check failed
	 * 			transCode if org trans_code check failed
	 */
	protected String offOrgTrans(String reqOrg, String rcvOrg,
			String thridOrg, String transCode) {
		logger.debug(
				"start check org trans,reqOrg:{},rcvOrg:{},thridOrg{},transCode:{}",
				new Object[] { reqOrg, rcvOrg, thridOrg, transCode });
		List<String> orgList = new ArrayList<String>();
		if (!StringUtils.isBlank(reqOrg)) {
			orgList.add(reqOrg);
		}
		if (!StringUtils.isBlank(rcvOrg)) {
			orgList.add(rcvOrg);
		}
		if (!StringUtils.isBlank(thridOrg)) {
			orgList.add(thridOrg);
		}
		String offOrg = this.offOrg(orgList);
		if(!StringUtils.isBlank(offOrg)){
			logger.info("机构:{}交易:{}权限关闭",offOrg,transCode);
			log.info("机构:{}交易:{}权限关闭",offOrg,transCode);
			return new StringBuffer(offOrg).append(",").append(transCode).toString();
		}

		String o2o = OrgMapTransCache.getOrgMapTransCode(reqOrg, rcvOrg);
		if (o2o == null) {
			logger.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			log.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			return new StringBuffer(reqOrg).append(",").append(rcvOrg).append(",").append(transCode).toString();
		}
		String transListStr = o2o.trim();
		if (StringUtils.isBlank(transListStr)) {
			logger.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			log.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			return new StringBuffer(reqOrg).append(",").append(rcvOrg).append(",").append(transCode).toString();
		}
		String transArr[] = transListStr
				.split(CommonConstant.SpeSymbol.COMMA_MARK.getValue());
		if (transArr.length == 0) {
			logger.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			log.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			return new StringBuffer(reqOrg).append(",").append(rcvOrg).append(",").append(transCode).toString();
		}
		List<String> transListTrim = new ArrayList<String>();
		for (String s : transArr) {
			transListTrim.add(s.trim());
		}
		if (!transListTrim.contains(transCode)) {
			logger.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			log.info("机构:{},{}交易:{}权限关闭",new Object[]{reqOrg,rcvOrg,transCode});
			return new StringBuffer(reqOrg).append(",").append(rcvOrg).append(",").append(transCode).toString();
		}
		return null;
	}
	
	/**
	 * 验证机构的状态及机构交易权限，校验顺序：发起方就够，接收方机构，机构交易权限
	 * @param reqOrg 发起方机构代码
	 * @param rcvOrg 接收方机构代码
	 * @param transCode 内部交易代码
	 * @return  reqOrg or rcvOrg,if any org check failed
	 * 			transCode if org trans_code check failed
	 */
	protected String offOrgTrans(String reqOrg, String rcvOrg, String transCode) {
		return offOrgTrans(reqOrg,rcvOrg,null,transCode);
	}

	/**
	 * 交易处理
	 * 
	 * @param paramMap
	 * @param paramString
	 * @return
	 * @throws AppBizException
	 */
	private Object processMsg(Map<String, Object> paramMap, String paramString)
			throws AppBizException {
		final String transCode = (String) paramMap.get("TransCode");
		Object resMsg = null;
		IBaseAction<Object, Object> action = transactionMap.get(transCode);
		resMsg = action.execute(paramString);
		return resMsg;
	}

	@Override
	protected BaseMsgVo convertRtnMsgVo(BaseMsgVo vo) {
		// TODO Auto-generated method stub
		return null;
	}
}
