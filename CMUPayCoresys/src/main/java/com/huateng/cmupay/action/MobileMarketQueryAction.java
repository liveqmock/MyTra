package com.huateng.cmupay.action;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.huateng.cmupay.constant.CommonConstant;
import com.huateng.cmupay.constant.ExcConstant;
import com.huateng.cmupay.constant.RspCodeConstant;
import com.huateng.cmupay.controller.cache.BankErrorCodeCache;
import com.huateng.cmupay.controller.cache.ProvAreaCache;
import com.huateng.cmupay.exception.AppBizException;
import com.huateng.cmupay.exception.AppRTException;
import com.huateng.cmupay.jms.message.SendCrmJmsMessageImpl;
import com.huateng.cmupay.logFormat.MobileMarketMessageLogger;
import com.huateng.cmupay.logFormat.TmallMessageLogger;
import com.huateng.cmupay.models.ProvincePhoneNum;
import com.huateng.cmupay.models.UpayCsysTmallTxnLog;
import com.huateng.cmupay.models.UpayCsysTransCode;
import com.huateng.cmupay.models.UpayCsysTxnLog;
import com.huateng.cmupay.parseMsg.reflect.handle.CustomAnnotation;
import com.huateng.cmupay.parseMsg.reflect.handle.MsgHandle;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.BankMsgVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.BankTransQueryReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.BankTransQueryResVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopMsgReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopMsgResVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopMsgVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopQueryMsgReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopQueryMsgResVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMsgVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmTransQueryReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmTransQueryResVo;
import com.huateng.cmupay.utils.UUIDGenerator;
import com.huateng.toolbox.utils.StrUtil;
import com.huateng.toolbox.utils.StringUtil;
import com.huateng.toolbox.utils.DateUtil;


@Controller("mmarkertResultQueryAction")
@Scope("prototype")
public class MobileMarketQueryAction extends AbsBaseAction<MobileShopMsgVo, MobileShopMsgVo>{
	
	
	private MobileMarketMessageLogger marketOperLogger = MobileMarketMessageLogger.getLogger(this.getClass());
	private final Logger marketLogger = LoggerFactory.getLogger("MMARKET_FILE");
	@Autowired
	private SendCrmJmsMessageImpl sendCrmJmsMessage;
	@Override
	public MobileShopMsgVo execute(MobileShopMsgVo msgVo) throws AppBizException {
		marketLogger.debug("MarketTransResultQueryAction execute(Object) - start");
		// 请求报文
		MobileShopMsgVo reqMsg = msgVo;
		MobileShopQueryMsgReqVo reqBody = new MobileShopQueryMsgReqVo();
		MobileShopQueryMsgResVo resBody = new MobileShopQueryMsgResVo();
		String transIDH="";
		MobileShopMsgVo resMsg = reqMsg;
		UpayCsysTxnLog txnLog = new UpayCsysTxnLog();
		try {
			//请求报文xml转化为bean
			MsgHandle.unmarshaller(reqBody, (String) reqMsg.getBody());
			//请求报文体bean中加入请求报文体
			reqMsg.setBody(reqBody);
			//获取平台交易流水号
			transIDH = msgVo.getTxnSeq();
			//获取平台交易时间
			String transIDHTime = msgVo.getTxnTime();
			//平台交易数据库日切日期
			String intTxnDate = msgVo.getTxnDate();
			//交易流水表唯一流水号
			Long seqId = msgVo.getSeqId();
			
			//给相应报文头添加值
			resMsg.setRcvDate(transIDHTime.substring(0, 8));
			resMsg.setRcvDateTime(transIDHTime);
			resMsg.setRcvTransID(transIDH);
			resMsg.setActionCode(CommonConstant.ActionCode.Respone.getValue());

			//内部交易码
			UpayCsysTransCode transCode = msgVo.getTransCode();
			
			/**
			 * 说明:oriOrderID与oriReqTransID至少要有一个不为空,否则返回格式错误
			 * */
			String oriOrderID=reqBody.getOriOrderID();
			String oriReqTransID=reqBody.getOriReqTransID();
			String querytype=reqBody.getQueryType();
			if( (oriOrderID==null || !querytype.equals(RspCodeConstant.MarketQueryType.MARKETQUERYTYPE_02.getValue()) ) 
					&& (oriReqTransID==null || !querytype.equals(RspCodeConstant.MarketQueryType.MARKETQUERYTYPE_01.getValue())) ){
				resMsg.setRspCode(RspCodeConstant.Market.MARKET_014A04.getValue());
				resMsg.setRspDesc(RspCodeConstant.Market.MARKET_014A04.getDesc());
				marketOperLogger.error("报文格式错误,内部交易流水:{},发起方:{},oriOrderID 为空,oriReqTransID 为空", transIDH, reqMsg.getReqSys());
				return resMsg;
			}
			//添加至交易流水表
			initLog(txnLog, seqId, transIDH,intTxnDate,transIDHTime,reqMsg,transCode,resMsg,reqBody );
			
			//将 发起方交易日期<请求报文头>  添加至      银行日期<交易流水表>
			txnLog.setSettleDate(reqMsg.getReqDate());
			/**
			 * 判断是否重复交易
			 * */
			Map<String,Object> param1 =new HashMap<String,Object>();
			param1.put("reqTransId", reqMsg.getReqTransID());
			param1.put("reqDomain", reqMsg.getReqSys());
			UpayCsysTxnLog transLog_isExist=upayCsysTxnLogService.findObj(param1);
			if(transLog_isExist!=null){
				resMsg.setRspCode(RspCodeConstant.Market.MARKET_013A34.getValue());
				resMsg.setRspDesc(RspCodeConstant.Market.MARKET_013A34.getDesc());
				resMsg.setBody(resBody);
				return resMsg;
			}else{
				//添加到数据库 交易流水表
				upayCsysTxnLogService.add(txnLog);
			}
			/* 验证消息 */
			String checkrtn = validateModel(reqBody);
			if (!"".equals(StringUtil.toTrim(checkrtn))) {
				marketOperLogger.error("报文体校验失败:{},内部交易流水:{},发起方:{}", new Object[] {
						checkrtn, transIDH, reqMsg.getReqSys() });
				marketLogger.error("报文体校验失败:{},内部交易流水:{},发起方:{}", new Object[] {
						checkrtn, transIDH, reqMsg.getReqSys() });
				//发起方应答代码
				txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_014A04.getValue());
				//发起方应答描述
				txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_014A04.getDesc() + checkrtn);
				//状态
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				//最后修改时间
				txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
				//修改操作
				upayCsysTxnLogService.modify(txnLog);
				
				//应答/错误代码
				resMsg.setRspCode(RspCodeConstant.Market.MARKET_014A04.getValue());
				//
				resMsg.setRspDesc(RspCodeConstant.Market.MARKET_014A04.getDesc());
				//
				resMsg.setBody(resBody);
				return resMsg;
			}
			// 查询原交易及处理
			Map<String, Object> params = new HashMap<String, Object>();
			
			if(reqBody.getQueryType().equals(RspCodeConstant.MarketQueryType.MARKETQUERYTYPE_01.getValue())){
				params.put("reqTransId", oriReqTransID);
			}else{
				params.put("orderId", oriOrderID);
			}
			params.put("reqDomain", reqBody.getOriReqSys());
			params.put("oriReqDate", reqBody.getOriReqDate());
			UpayCsysTxnLog transLog = upayCsysTxnLogService.findObj(params);
			if (null == transLog) {
				resMsg.setRspCode(RspCodeConstant.Market.MARKET_014A05.getValue());
				resMsg.setRspDesc(RspCodeConstant.Market.MARKET_014A05.getDesc());
				resMsg.setBody(resBody);
				txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_014A05.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_014A05.getDesc());
				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				upayCsysTxnLogService.modify(txnLog);
				return resMsg;
			}
			txnLog.setIdValue(transLog.getIdValue());
			txnLog.setIdType(transLog.getIdType());
			txnLog.setIdProvince(transLog.getIdProvince());
			String forwardOrg = transLog.getRcvDomain();// 转发方机构代码
			if(StringUtils.isBlank(forwardOrg)){
				marketOperLogger.error("原交易未发送到省充值:{},内部交易流水:{},发起方:{}",
						new Object[] { reqMsg.getReqSys(), transIDH,reqMsg.getReqSys() });
				marketLogger.error("原交易未发送到省充值:{},内部交易流水:{},发起方:{}",
						new Object[] { reqMsg.getReqSys(), transIDH,reqMsg.getReqSys() });
				resMsg.setBody(resBody);
				return resMsg;
			}

			/** 报文头 */
			CrmMsgVo forwardMsg = new CrmMsgVo();
			forwardMsg.setTransCode(transCode);
			forwardMsg.setVersion(ExcConstant.CRM_VERSION);
			forwardMsg.setTestFlag(testFlag);
			forwardMsg.setBIPCode(CommonConstant.Bip.Bis18.getValue());
			forwardMsg
					.setActivityCode(CommonConstant.CrmTrans.Crm09.getValue());
			forwardMsg.setActionCode(CommonConstant.ActionCode.Requset
					.getValue());
			forwardMsg.setOrigDomain(CommonConstant.OrgDomain.UPSS.getValue());
			forwardMsg.setHomeDomain(CommonConstant.OrgDomain.BOSS.getValue());
			String routeType=CommonConstant.RouteType.RoutePhone.getValue();
			forwardMsg.setRouteType(routeType);
			if(routeType.equals("00")){
				forwardMsg.setRouteValue("997");
			}else if(routeType.equals("01")){
				forwardMsg.setRouteValue(transLog.getRcvRouteVal());
			}
			forwardMsg.setSessionID(transIDH); // 待确认
			forwardMsg.setTransIDO(transIDH);
			forwardMsg.setTransIDOTime(StrUtil.subString(transIDHTime, 0, 14));
			forwardMsg.setMsgSender(CommonConstant.BankOrgCode.CMCC.getValue());
			forwardMsg.setMsgReceiver(forwardOrg);//

			/** 报文体 */
			CrmTransQueryReqVo forwardBody = new CrmTransQueryReqVo();
			forwardBody.setOriActionDate(reqMsg.getReqDate());
			forwardBody.setOriReqSys(reqMsg.getReqSys());
			forwardBody.setOriTransactionID(reqMsg.getReqTransID());
			forwardBody.setOriActivityCode(reqMsg.getActivityCode());
			forwardMsg.setBody(forwardBody);
			
			
			// 更新交易流水
			txnLog.setReqActivityCode(forwardMsg.getActivityCode());
			txnLog.setReqBipCode(forwardMsg.getBIPCode());
			txnLog.setOriOrgId(forwardOrg);
			txnLog.setReqRouteType(forwardMsg.getRouteType());
			txnLog.setReqRouteVal(forwardMsg.getRouteValue());
			txnLog.setReqSessionId(forwardMsg.getSessionID());
			txnLog.setReqCnlType(msgVo.getReqChannel());
			txnLog.setReqTransId(reqMsg.getReqTransID());
			txnLog.setReqTranshTm(transIDHTime);
			txnLog.setReqTransDt(intTxnDate);
			txnLog.setReqOprId(forwardBody.getOriTransactionID());
			txnLog.setReqOprDt(forwardBody.getOriActionDate());
			txnLog.setReqOprTm(forwardMsg.getTransIDHTime());
			/*------4*/
			// 查询该交易的号码段属于移动还是联通电信的。
			ProvincePhoneNum provincePhoneNum = ProvAreaCache.getProvAreaByPrimary(transLog.getIdValue());
			//校验落地方机构权限
			String checkFlag = offOrgTrans(reqMsg.getReqSys(), forwardOrg, msgVo.getTransCode().getTransCode(),
					provincePhoneNum != null ? provincePhoneNum.getPhoneNumFlag() : CommonConstant.PhoneNumType.CHINA_MOBILE.getType());

			//校验落地方机构权限
			if (checkFlag == null) {
				marketLogger.debug("sendCrmJmsMessage.sendMsg(forwardMsg) - start,intTxnSeq:{}",new Object[]{transIDH});
				forwardMsg = sendCrmJmsMessage.sendMsg(forwardMsg);
				marketLogger.debug("sendCrmJmsMessage.sendMsg(forwardMsg) - end,intTxnSeq:{}",new Object[]{transIDH});
				txnLog.setReqTranshId(forwardMsg.getTransIDH());
				txnLog.setReqTransTm(forwardMsg.getTransIDHTime());
				txnLog.setReqTranshDt(StrUtil.subString(forwardMsg.getTransIDHTime(), 0, 8));
				/*------5*/
				CrmTransQueryResVo forwardRtBody = new CrmTransQueryResVo();
				if (forwardMsg.getBody() == null
						|| "".equals(forwardMsg.getBody().toString())
						|| "null".equalsIgnoreCase(forwardMsg.getBody()
								.toString())) {
					marketOperLogger.error("CRM返回报文体为空!内部交易流水:{},发起方:{},接收方:{},手机号:{}",new Object[] { transIDH,
							reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
					marketLogger.error("CRM返回报文体为空!内部交易流水:{},发起方:{},接收方:{},手机号:{}",new Object[] { transIDH,
							reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
					
					String errCode = RspCodeConstant.Bank.BANK_015A06.getValue();
					errCode = BankErrorCodeCache.getBankErrCode(errCode);
					
					txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
					txnLog.setChlRspCode(errCode);
					txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
					txnLog.setChlRspCode(forwardMsg.getRspCode());
					txnLog.setChlRspDesc(forwardMsg.getRspDesc());
					txnLog.setChlRspType(forwardMsg.getRspType());
					txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
					upayCsysTxnLogService.modify(txnLog);
					resMsg.setRspCode(errCode);
					resMsg.setRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
					resMsg.setBody(resBody);
					return resMsg;
				}
				if(RspCodeConstant.Upay.UPAY_U99998.getValue().equals(forwardMsg.getRspCode())){
					marketOperLogger.error("CRM前置响应超时!内部交易流水:{},发起方:{},接收方:{},手机号:{}",new Object[] { transIDH,
									reqMsg.getReqSys(),forwardMsg.getMsgReceiver() ,transLog.getIdValue()});
					marketLogger.error("CRM前置响应超时!内部交易流水:{},发起方:{},接收方:{},手机号:{}",new Object[] { transIDH,
									reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
					
					String errCode =forwardMsg.getRspCode();
					errCode = BankErrorCodeCache.getBankErrCode(errCode);
					txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
					txnLog.setChlRspCode(errCode);
					txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
					txnLog.setChlRspCode(forwardMsg.getRspCode());
					txnLog.setChlRspDesc(forwardMsg.getRspDesc());
					txnLog.setChlRspType(forwardMsg.getRspType());
					/*------7*/
					txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
					upayCsysTxnLogService.modify(txnLog);
					resMsg.setRspCode(errCode);
					resMsg.setRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
					resMsg.setBody(resBody);
					return resMsg;
				}
				MsgHandle.unmarshaller(forwardRtBody, forwardMsg.getBody()
						.toString());

				if (RspCodeConstant.Wzw.WZW_0000.getValue().equals(
						forwardMsg.getRspCode())
						&& RspCodeConstant.Crm.CRM_0000.getValue().equals(
								forwardRtBody.getRspCode())) {
					marketOperLogger.succ("CRM响应成功!内部交易流水:{},发起方:{},接收方:{},手机号:{}",
							new Object[] { transIDH,reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue()});
					marketLogger.info("CRM响应成功!内部交易流水:{},发起方:{},接收方:{},手机号:{}",
							new Object[] { transIDH,reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
					txnLog.setChlRspCode(RspCodeConstant.Bank.BANK_010A00.getValue());
					txnLog.setChlRspDesc(RspCodeConstant.Bank.BANK_010A00.getDesc());
					txnLog.setChlRspCode(forwardMsg.getRspCode());
					txnLog.setChlRspDesc(forwardMsg.getRspDesc());
					txnLog.setChlRspType(forwardMsg.getRspType());
					txnLog.setStatus(CommonConstant.TxnStatus.TxnSuccess.getValue());
					txnLog.setChlSubRspCode(forwardRtBody.getRspCode());
					txnLog.setChlSubRspDesc(forwardRtBody.getRspInfo());
					resMsg.setRspCode(RspCodeConstant.Market.MARKET_010A00.getValue());
					resMsg.setRspDesc(RspCodeConstant.Market.MARKET_010A00.getDesc());
				} else {
					marketOperLogger.error("CRM响应失败!内部交易流水:{},发起方:{},接收方:{},手机号:{}",
							new Object[] { transIDH,reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue()});
					marketLogger.error("CRM响应失败!内部交易流水:{},发起方:{},接收方:{},手机号:{}",
							new Object[] { transIDH,reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
					
					
					String errName = BankErrorCodeCache.getBankErrCode(forwardRtBody.getRspCode());
					txnLog.setChlRspCode(errName);
					txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errName));
					txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
					txnLog.setChlRspCode(forwardMsg.getRspCode());
					txnLog.setChlRspDesc(forwardMsg.getRspDesc());
					txnLog.setChlRspCode(forwardRtBody.getRspCode());
					txnLog.setChlRspDesc(forwardRtBody.getRspInfo());
					txnLog.setChlRspType(forwardMsg.getRspType());
					resMsg.setRspCode(RspCodeConstant.Market.MARKET_010A00.getValue());
					resMsg.setRspDesc(RspCodeConstant.Market.MARKET_010A00.getDesc());
				}
				resBody.setQueryType(reqBody.getQueryType());
				resBody.setOriRcvDate(reqBody.getOriReqDate());
				resBody.setOriRcvTransID(reqBody.getOriReqTransID());
				resBody.setOriOrderID(reqBody.getOriOrderID());
				resBody.setOriResultCode(RspCodeConstant.Market.MARKET_010A00.getValue());
				resBody.setOriResultDesc(RspCodeConstant.Market.MARKET_010A00.getDesc());
				resBody.setOriResultTime(transIDHTime);
				resMsg.setBody(resBody);
				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				upayCsysTxnLogService.modify(txnLog);
				marketLogger.debug("TmallTransResultQueryAction execute(Object) - end");
				return resMsg;
			} else {
				
				marketOperLogger.error("落地方机构状态异常:{},内部交易流水:{},发起方:{},接收方:{},手机号:{}",new Object[] { forwardOrg, transIDH,
						reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
				marketLogger.error("落地方机构状态异常:{},内部交易流水:{},发起方:{},接收方:{},手机号:{}",new Object[] { forwardOrg, transIDH,
						reqMsg.getReqSys(),forwardMsg.getMsgReceiver(),transLog.getIdValue() });
//				resBody.setOriRspCode(RspCodeConstant.Bank.BANK_012A16.getValue());
//				resBody.setOriRspDesc(RspCodeConstant.Bank.BANK_012A16.getDesc()
//						+ "接收方" + forwardOrg + "交易权限关闭");
				/*------10*/
//				txnLog.setTmallRspCode(RspCodeConstant.Bank.BANK_012A16.getValue());
//				txnLog.setTmallRspDesc(RspCodeConstant.Bank.BANK_012A16.getDesc()+checkFlag);
				txnLog.setChlRspCode(RspCodeConstant.Bank.BANK_012A16.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Bank.BANK_012A16.getDesc()+checkFlag);
				/*------10*/
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				upayCsysTxnLogService.modify(txnLog);

				resMsg.setRspCode(RspCodeConstant.Bank.BANK_012A16.getValue());
				resMsg.setRspDesc(RspCodeConstant.Bank.BANK_012A16.getDesc());

				resMsg.setBody(resBody);
				marketLogger.debug("TmallTransResultQueryAction execute(Object) - end");
				return resMsg;
			}
		} catch (AppRTException e) {
			String errCode = e.getCode();
			marketOperLogger.error("运行异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {RspCodeConstant.Bank.getDescByValue(errCode),
							transIDH, reqMsg.getReqSys() });
			marketLogger.error("运行异常,代码:{},内部交易流水号:{},业务发起方:{}",
					new Object[] { errCode,
							transIDH, reqMsg.getReqSys() });
			marketLogger.error("运行异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			
			/*------11*/
//			txnLog.setTmallRspCode(errCode);
//			txnLog.setTmallRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			txnLog.setChlRspCode(errCode);
			txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			/*------11*/
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
			upayCsysTxnLogService.modify(txnLog);

//			resBody.setOriRspCode(MessageHandler.getBankErrCode(errCode));
//			resBody.setOriRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setRspCode(errCode);
			resMsg.setRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setBody(resBody);
			return resMsg;
		} catch (AppBizException e) {
			String errCode = e.getCode();
			errCode = BankErrorCodeCache.getBankErrCode(errCode);
			marketOperLogger.error("业务异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {RspCodeConstant.Bank.getDescByValue(errCode),
							transIDH, reqMsg.getReqSys() });
			marketLogger.error("业务异常,代码:{},内部交易流水号:{},业务发起方:{}",
					new Object[] { errCode,
							transIDH, reqMsg.getReqSys() });
			marketLogger.error("业务异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			/*------12*/
//			txnLog.setTmallRspCode(errCode);
//			txnLog.setTmallRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			txnLog.setChlRspCode(errCode);
			txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			/*------12*/
			
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
			upayCsysTxnLogService.modify(txnLog);

//			resBody.setOriRspCode(MessageHandler.getBankErrCode(errCode));
//			resBody.setOriRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setRspCode(errCode);
			resMsg.setRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setBody(resBody);
			return resMsg;
		} catch (Exception e) {
			String errCode = RspCodeConstant.Bank.BANK_015A06.getValue();
			errCode = BankErrorCodeCache.getBankErrCode(errCode);
			marketOperLogger.error("系统异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {transIDH, reqMsg.getReqSys() });
			marketLogger.info("系统异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {transIDH, reqMsg.getReqSys() });
			marketLogger.error("系统异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			/*------12*/
//			txnLog.setTmallRspCode(errCode);
//			txnLog.setTmallRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			txnLog.setChlRspCode(errCode);
			txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			/*------12*/
			
			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			upayCsysTxnLogService.modify(txnLog);

//			resBody.setOriRspCode(MessageHandler.getBankErrCode(errCode));
//			resBody.setOriRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setRspCode(errCode);
			resMsg.setRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setBody(resBody);
			return resMsg;
		}
	}

	/**
	 * 初始化交易流水
	 * 
	 * @param txnLog
	 * @param seqId
	 * @param transIDH
	 * @param intTxnDate
	 * @param transIDHTime
	 * @param reqMsg
	 * @param transCode
	 * @param resMsg
	 * @param reqBody
	 */
	private void initLog(UpayCsysTxnLog txnLog, Long seqId, String transIDH,
			String intTxnDate, String transIDHTime, MobileShopMsgVo reqMsg,
			UpayCsysTransCode transCode, MobileShopMsgVo resMsg,MobileShopQueryMsgReqVo reqBody ) {
		
		txnLog.setSeqId(seqId);
		txnLog.setIntTxnSeq(transIDH);
		txnLog.setIntTxnDate(intTxnDate);
		txnLog.setIntTxnTime(transIDHTime);
		txnLog.setIntMqSeq(reqMsg.getMqSeq());
		txnLog.setBussType(transCode.getBussType());
		txnLog.setBussChl(transCode.getBussChl());
		txnLog.setIntTransCode(transCode.getTransCode());
		txnLog.setPayMode(transCode.getPayMode());
		txnLog.setStatus(CommonConstant.TxnStatus.InitStatus.getValue());
		txnLog.setReconciliationFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setReqActivityCode(reqMsg.getActivityCode());
		
		txnLog.setReqDomain(reqMsg.getReqSys());
		txnLog.setReqSessionId(reqMsg.getReqTransID());

		txnLog.setReqTransId(reqMsg.getReqTransID());
		txnLog.setReqTransTm(reqMsg.getReqDateTime());
		txnLog.setReqTransDt(StrUtil.subString(reqMsg.getReqDateTime(), 0, 8));
		txnLog.setReqTranshDt(StrUtil.subString(resMsg.getReqDateTime(), 0, 8));
		txnLog.setReqTranshId(resMsg.getRcvTransID());
		txnLog.setReqTranshTm(resMsg.getRcvDateTime());
		txnLog.setReqOprId(resMsg.getReqTransID());
		txnLog.setReqOprDt(StrUtil.subString(reqMsg.getRcvDateTime(), 0, 8));
		txnLog.setReqOprTm(reqMsg.getRcvDateTime());

		txnLog.setOriReqDate(reqMsg.getReqDate());
		txnLog.setOriOprTransId(reqBody.getOriReqTransID());

		txnLog.setBackFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setRefundFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setReverseFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
	}
}
