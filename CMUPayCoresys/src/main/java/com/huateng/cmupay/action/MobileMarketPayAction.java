package com.huateng.cmupay.action;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.huateng.cmupay.constant.CommonConstant;
import com.huateng.cmupay.constant.ExcConstant;
import com.huateng.cmupay.constant.RspCodeConstant;
import com.huateng.cmupay.controller.cache.BankErrorCodeCache;
import com.huateng.cmupay.controller.cache.SysMapCache;
import com.huateng.cmupay.exception.AppBizException;
import com.huateng.cmupay.exception.AppRTException;
import com.huateng.cmupay.jms.business.crm.CrmChargeBus;
import com.huateng.cmupay.logFormat.MobileMarketMessageLogger;
import com.huateng.cmupay.models.ProvincePhoneNum;
import com.huateng.cmupay.models.UpayCsysTransCode;
import com.huateng.cmupay.models.UpayCsysTxnLog;
import com.huateng.cmupay.parseMsg.reflect.handle.MsgHandle;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopMsgReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopMsgResVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.MobileShopMsgVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmChargeResVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMsgVo;
import com.huateng.cmupay.utils.Serial;
import com.huateng.cmupay.utils.StringFormat;
import com.huateng.toolbox.utils.DateUtil;
import com.huateng.toolbox.utils.StrUtil;
import com.huateng.toolbox.utils.StringUtil;

/**
 * 商城交易
 * 
 * @author oul
 *  
 */
@Controller("mmarkertPayAction")
@Scope("prototype")
public class MobileMarketPayAction extends AbsBaseAction<MobileShopMsgVo, MobileShopMsgVo> {
	private MobileMarketMessageLogger marketOperLogger = MobileMarketMessageLogger.getLogger(this.getClass());
	private final Logger marketLogger = LoggerFactory.getLogger("CMUPayCoresys_MMarket");
	@Autowired
	private CrmChargeBus crmChargeBus;

	@Override
	public MobileShopMsgVo execute(MobileShopMsgVo msgVo) throws AppBizException {
		//日志
		marketLogger.debug("MobileMarketPayAction execute(Object) - start");
		// 请求报文报文头
		MobileShopMsgVo reqMsg = msgVo;
		//请求报文体
		MobileShopMsgReqVo reqBody = new MobileShopMsgReqVo();
		//响应报文体
		MobileShopMsgResVo resBody = new MobileShopMsgResVo();
		//响应报文头
		MobileShopMsgVo resMsg = new MobileShopMsgVo() ;
		
		//交易流水表
		UpayCsysTxnLog txnLog = new UpayCsysTxnLog();

		try {
			//请求报文xml转化为bean
			MsgHandle.unmarshaller(reqBody, (String) reqMsg.getBody());
			//请求报文体bean中加入请求报文体
			reqMsg.setBody(reqBody);
			//获取平台交易流水号
			String transIDH = msgVo.getTxnSeq();
			//获取平台交易时间
			String transIDHTime = msgVo.getTxnTime();
			//平台交易数据库日切日期
			String intTxnDate = msgVo.getTxnDate();
			//交易流水表唯一流水号
			Long seqId = msgVo.getSeqId();
			
			resMsg.setReqDate(reqMsg.getReqDate());
			resMsg.setReqTransID(reqMsg.getReqTransID());
			resMsg.setReqDateTime(reqMsg.getReqDateTime());
			resMsg.setReqSys(reqMsg.getRcvSys());
			resMsg.setRcvSys(reqMsg.getReqSys());
			resMsg.setReqChannel("00");
			resMsg.setActionCode(CommonConstant.ActionCode.Requset.getValue());
			resMsg.setActivityCode(CommonConstant.MarketTrans.Market01.getValue());

			resBody.setOrderID(reqBody.getOrderID());
			resBody.setOriReqDate(reqMsg.getReqDate());
			resBody.setOriReqTransID(reqMsg.getReqTransID());
			resBody.setResultTime(StrUtil.subString(transIDHTime, 0, 14));
			//内部交易码
			UpayCsysTransCode transCode = msgVo.getTransCode();
			
//			判断交易是否是重发交易（在交易成功明细表里查询）
			
			marketOperLogger.info("判断是否重复交易----->开始:");
			Map<String, Object> params1 = new HashMap<String, Object>();
			params1.put("reqTransId", msgVo.getReqTransID());
			params1.put("reqDomain", msgVo.getReqSys());
			params1.put("settleDate", msgVo.getReqDate());
			params1.put("status", CommonConstant.TxnStatus.TxnSuccess.getValue());
			/**
			 * 重复交易
			 * */
			UpayCsysTxnLog txnLog_isExist1=new UpayCsysTxnLog();
			txnLog_isExist1 = upayCsysTxnLogService.findObj(params1);
			if(txnLog_isExist1 != null){
				BeanUtils.copyProperties(txnLog_isExist1, txnLog);
				marketOperLogger.info("重复交易:{},内部交易流水:{},发起方:{}",
						new Object[] { msgVo.getTxnSeq(), reqBody.getIDValue(),
						reqMsg.getReqSys() });
				marketOperLogger.info("重复交易:{},内部交易流水:{},发起方:{}",
						new Object[] { msgVo.getTxnSeq(), reqBody.getIDValue(),
								reqMsg.getReqSys() });
				resBody.setResultCode(RspCodeConstant.Market.MARKET_013A34.getValue());
				resBody.setResultDesc(RspCodeConstant.Market.MARKET_013A34.getDesc());
				resMsg.setBody(resBody);
				return resMsg;
			}
			marketOperLogger.info("判断是否重复交易----->结束");
			marketOperLogger.info("判断此交易是否需要重发----->开始");
			UpayCsysTxnLog txnLog_isExist2=new UpayCsysTxnLog();
			txnLog_isExist2 = upayCsysTxnLogService.findIsResend(params1);
			boolean isResend=false;
			if(txnLog_isExist2 != null){
				BeanUtils.copyProperties(txnLog_isExist2, txnLog);
				marketOperLogger.info("重发交易:{},内部交易流水:{},发起方:{}",
						new Object[] { msgVo.getTxnSeq(), reqBody.getIDValue(),
						reqMsg.getReqSys() });
				marketOperLogger.info("重发交易:{},内部交易流水:{},发起方:{}",
						new Object[] { msgVo.getTxnSeq(), reqBody.getIDValue(),
								reqMsg.getReqSys() });
				isResend=true;
			}
			marketOperLogger.info("判断此交易是否需要重发----->结束");
			initLog(txnLog, reqMsg, resMsg, reqBody, seqId, transIDH, intTxnDate, transIDHTime);
			txnLog.setSettleDate(reqMsg.getReqDate());
			if(!isResend){
				upayCsysTxnLogService.add(txnLog);
			}
			
			/* 验证消息 */
			String checkrtn = validateModel(reqBody);
			System.out.println("验证消息:"+checkrtn);
			if (!"".equals(StringUtil.toTrim(checkrtn))) {
				marketOperLogger.error("报文体校验失败:{},内部交易流水:{},发起方:{}", new Object[] {
						checkrtn, msgVo.getTxnSeq(), reqMsg.getReqSys() });
				marketLogger.error("报文体校验失败:{},内部交易流水:{},发起方:{}", new Object[] {
						checkrtn, msgVo.getTxnSeq(), reqMsg.getReqSys() });
				//发起方应答代码
				txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_014A04.getValue());
				//发起方应答描述
				txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_014A04.getDesc() + checkrtn);
				//状态
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				//最后修改时间
				txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
				//修改操作
				txnLog.setOriOprTransId(reqMsg.getReqTransID());
				upayCsysTxnLogService.modify(txnLog);
				
				//应答/错误代码
				resBody.setResultCode(RspCodeConstant.Market.MARKET_014A04.getValue());
				//
				resBody.setResultDesc(RspCodeConstant.Market.MARKET_014A04.getDesc());
				//
				resMsg.setBody(resBody);
				return resMsg;
			}
			
			//归属地号码
			ProvincePhoneNum provincePhoneNum = findProvinceByMobileNumber(reqBody.getIDValue());
			String idProvince = provincePhoneNum == null ? null : provincePhoneNum.getProvinceCode();
			if (null == idProvince) {
				marketOperLogger.error("手机号码不正确:{},内部交易流水:{},发起方:{}",
						new Object[] { msgVo.getTxnSeq(), reqBody.getIDValue(),
								reqMsg.getReqSys() });
				marketLogger.error("手机号码不正确:{},内部交易流水:{},发起方:{}",
						new Object[] { msgVo.getTxnSeq(), reqBody.getIDValue(),
								reqMsg.getReqSys() });
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
				txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_012A11.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_012A11.getDesc());
				txnLog.setOriOprTransId(reqMsg.getReqTransID());
				upayCsysTxnLogService.modify(txnLog);
				
				resBody.setResultCode(RspCodeConstant.Market.MARKET_012A11.getValue());
				resBody.setResultDesc(RspCodeConstant.Market.MARKET_012A11.getDesc());
				resMsg.setBody(resBody);
				return resMsg;
			}
			txnLog.setIdProvince(idProvince);
			String forwardOrg = SysMapCache.getProvCd(idProvince).getSysCd();// 转发方机构代�?

			/** 报文头 */
			CrmMsgVo forwardMsg = new CrmMsgVo();
			CrmMsgVo forwardRtMsg = new CrmMsgVo();
			forwardMsg.setTransCode(transCode);
			forwardMsg.setVersion(ExcConstant.CRM_VERSION);
			forwardMsg.setTestFlag(testFlag);
			forwardMsg.setBIPCode(CommonConstant.Bip.Biz22.getValue());
			forwardMsg.setActivityCode(CommonConstant.CrmTrans.Crm07.getValue());
			forwardMsg.setActionCode(CommonConstant.ActionCode.Requset.getValue());
			forwardMsg.setOrigDomain(CommonConstant.OrgDomain.UPSS.getValue());
			forwardMsg.setHomeDomain(CommonConstant.OrgDomain.BOSS.getValue());
			forwardMsg.setRouteType(CommonConstant.RouteType.RoutePhone.getValue());
			forwardMsg.setRouteValue(reqBody.getIDValue());
			forwardMsg.setSessionID(transIDH);
			forwardMsg.setTransIDO(transIDH);
			forwardMsg.setTransIDOTime(StrUtil.subString(transIDHTime, 0, 14));
			forwardMsg.setMsgSender(CommonConstant.BankOrgCode.CMCC.getValue());
			forwardMsg.setMsgReceiver(forwardOrg);
	
			txnLog.setOriOrgId(forwardOrg);
		
	       String checkFlag = offOrgTrans(reqMsg.getReqSys(), forwardOrg, msgVo.getTransCode().getTransCode(), 
	    		   provincePhoneNum == null ? CommonConstant.PhoneNumType.CHINA_MOBILE.getType() : provincePhoneNum.getPhoneNumFlag());
			if (checkFlag == null) { 
				String oprId=Serial.genSerialNum(CommonConstant.Sequence.OprId.toString());
				Map<String, String> params = new HashMap<String, String>();
				params.put("idType", reqBody.getIDType());
				params.put("idValue", reqBody.getIDValue());
				params.put("transactionID", oprId);
				params.put("actionDate", intTxnDate);
				params.put("actionTime", StrUtil.subString(transIDHTime, 0, 14));
				params.put("cnlTyp", reqMsg.getReqChannel());
				params.put("payedType", CommonConstant.PayType.PayPre.getValue());
				params.put("settleDate", reqMsg.getReqDate());
				/**
				 * 新充值改造字段
				 * time:20131106
				 * author:ol
				 * */
				params.put("busiTransID", reqMsg.getReqTransID());//移动商城流水号
				params.put("payTransID", reqBody.getPayTransID());//移动商城扣款流水号
				params.put("organID", reqMsg.getReqSys());//机构编码
				params.put("chargeMoney", String.valueOf(txnLog.getPayAmt()+""));//充值金额
				params.put("orderNo", txnLog.getOrderId());//订单号
				params.put("productNo", txnLog.getProductNo());//产品编号
				params.put("payment", txnLog.getPayAmt()==null?null:txnLog.getPayAmt().toString());//订单总金额
				params.put("orderCnt", txnLog.getOrderCnt()==null?null:txnLog.getOrderCnt().toString());//订单总数量
				params.put("prodDiscount",txnLog.getProdDiscount()==null?null:txnLog.getProdDiscount().toString());//产品减折金额
				params.put("creditCardFee",txnLog.getCreditCardFee()==null?null:txnLog.getCreditCardFee().toString());//信用卡费用
				params.put("activityNo",txnLog.getActivityNo());//营销活动号
				params.put("productShelfNo", txnLog.getProductShelfNo());//商品上架编码
				marketLogger.debug(
						"开始手机:{}充值,内部交易流水:{},发起方:{},接收方:{}",
						new Object[] { reqBody.getIDValue(), msgVo.getTxnSeq(),
								reqMsg.getReqSys(), forwardMsg.getMsgReceiver() });
				forwardRtMsg = crmChargeBus.execute(forwardMsg, params, txnLog, null);
			
				marketLogger.debug(
						"手机:{}充值返回,内部交易流水:{},发起方:{},接收方:{}",
						new Object[] { reqBody.getIDValue(), msgVo.getTxnSeq(),
								reqMsg.getReqSys(), forwardMsg.getMsgReceiver() });
				CrmChargeResVo rtBody = null;
				if ("".equals(forwardRtMsg.getBody())) {
					marketOperLogger.error(
							"充值返回报文体为空,内部交易流水:{},发起方:{},接收方:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver() });
					marketLogger.error(
							"充值返回报文体为空,内部交易流水:{},发起方:{},接收方:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver() });

					txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
					txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
				
					txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_015A03.getValue());
					txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_015A03.getDesc());
					txnLog.setOriOprTransId(reqMsg.getReqTransID());
					upayCsysTxnLogService.modify(txnLog);
					
					resBody.setResultCode(RspCodeConstant.Market.MARKET_015A03.getValue());
					resBody.setResultDesc(RspCodeConstant.Market.MARKET_015A03.getDesc());
					resMsg.setBody(resBody);
					return resMsg;
				}else{
					rtBody =  (CrmChargeResVo) forwardRtMsg.getBody();
				}
				
				if (RspCodeConstant.Wzw.WZW_0000.getValue().equals(
						forwardRtMsg.getRspCode())
						&& RspCodeConstant.Crm.CRM_0000.getValue().equals(
								rtBody.getRspCode())) {
					marketOperLogger.succ(
							"充值返回成功,内部交易流水:{},发起方:{},接收方:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver() });
					marketLogger.info(
							"充值返回成功,内部交易流水:{},发起方:{},接收方:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver() });
					txnLog.setStatus(CommonConstant.TxnStatus.TxnSuccess.getValue());
					
					txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_010A00.getValue());
					txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_010A00.getDesc());
					txnLog.setOriOprTransId(reqMsg.getReqTransID());
					upayCsysTxnLogService.modify(txnLog);
					
					//充值结果body
					resBody.setResultCode(RspCodeConstant.Market.MARKET_010A00.getValue());
					resBody.setResultDesc(RspCodeConstant.Market.MARKET_010A00.getDesc());
					resMsg.setBody(resBody);
					marketLogger.debug("BankPayAction execute(Object) - end");
					return resMsg;
				} else if (RspCodeConstant.Upay.UPAY_U99998.getValue().equals(
						forwardRtMsg.getRspCode())) {
					marketOperLogger.error(
							"充值返回超时,内部交易流水:{},发起方:{},接收方:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver() });
					marketLogger.error(
							"充值返回超时,内部交易流水:{},发起方:{},接收方:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver() });

					txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());

					txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_015A07.getValue());
					txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_015A07.getDesc());
					txnLog.setOriOprTransId(reqMsg.getReqTransID());
					upayCsysTxnLogService.modify(txnLog);
					
					resBody.setResultCode(RspCodeConstant.Market.MARKET_015A07.getValue());
					resBody.setResultDesc(RspCodeConstant.Market.MARKET_015A07.getDesc());
					
					resMsg.setBody(resBody);
					marketLogger.debug("BankPayAction execute(Object) - end");
					return resMsg;
				} else {
					marketOperLogger.error(
							"充值返回失败,内部交易流水:{},发起方:{},接收方:{},返回码:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver(),
									rtBody.getRspCode() });
					marketLogger.error(
							"充值返回失败,内部交易流水:{},发起方:{},接收方:{},返回码:{}",
							new Object[] { msgVo.getTxnSeq(),
									reqMsg.getReqSys(),
									forwardMsg.getMsgReceiver(),
									rtBody.getRspCode() });
					String errName = BankErrorCodeCache.getBankErrCode(rtBody.getRspCode());
					marketLogger.debug(
							"内部交易流水:{}转换银行返回码crm 返回码:{} ，转化为银行的返回码:{}",
							new Object[] { msgVo.getTxnSeq(),
									rtBody.getRspCode(), errName });
				
					txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
					txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
					
					txnLog.setChlRspCode(errName);
					txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errName));
					txnLog.setOriOprTransId(reqMsg.getReqTransID());
					upayCsysTxnLogService.modify(txnLog);
					
					resBody.setResultCode(errName);
					resBody.setResultDesc(RspCodeConstant.Bank.getDescByValue(errName));
					resMsg.setBody(resBody);

					marketLogger.debug("BankPayAction execute(Object) - end");
					return resMsg;
				}
			} else {
				marketOperLogger.error("接收方机构状态异常,内部交易流水:{},发起方:{},接收方:{}",
						new Object[] { msgVo.getTxnSeq(), reqMsg.getReqSys(),
								forwardMsg.getMsgReceiver() });
				marketLogger.error(
						"接收方机构状态异常,内部交易流水:{},发起方:{},接收方:{}",
						new Object[] { new Object[] { msgVo.getTxnSeq(),
								reqMsg.getReqSys(), forwardMsg.getMsgReceiver() } });
				
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());

				txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_012A16.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_012A16.getDesc()+checkFlag);
				txnLog.setOriOprTransId(reqMsg.getReqTransID());
				upayCsysTxnLogService.modify(txnLog);
				
				resBody.setResultCode(RspCodeConstant.Market.MARKET_012A16.getValue());
				resBody.setResultDesc(RspCodeConstant.Market.MARKET_012A16.getDesc());
				resMsg.setBody(resBody);
				marketLogger.debug("BankPayAction execute(Object) - end");
				return resMsg;
			}
		} catch (AppRTException e) {
			String errCode = e.getCode();
			errCode = BankErrorCodeCache.getBankErrCode(errCode);
			marketOperLogger.error(
					"运行异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {
							RspCodeConstant.Bank.getDescByValue(errCode),
							reqMsg.getTxnSeq(), reqMsg.getReqSys() });
			marketLogger.error(
					"运行异常!内部交易流水号:{},业务发起方:{}}",
					new Object[] { 
							reqMsg.getTxnSeq(), reqMsg.getReqSys() });
			marketLogger.error("运行异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());

			txnLog.setChlRspCode(errCode);
			txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			txnLog.setOriOprTransId(reqMsg.getReqTransID());
			upayCsysTxnLogService.modify(txnLog);
			
			resBody.setResultCode(errCode);
			resBody.setResultDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setBody(resBody);
			return resMsg;
		} catch (AppBizException e) {
			String errCode = e.getCode();
			errCode = BankErrorCodeCache.getBankErrCode(errCode);
			marketOperLogger.error(
					"业务异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {reqMsg.getTxnSeq(), reqMsg.getReqSys() });
			marketLogger.error(
					"业务异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {
							reqMsg.getTxnSeq(), reqMsg.getReqSys() });
			marketLogger.error("业务异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
		
			txnLog.setChlRspCode(errCode);
			txnLog.setChlRspDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			txnLog.setOriOprTransId(reqMsg.getReqTransID());
			upayCsysTxnLogService.modify(txnLog);
			
			resBody.setResultCode(errCode);
			resBody.setResultDesc(RspCodeConstant.Bank.getDescByValue(errCode));
			resMsg.setBody(resBody);
			return resMsg;
		} catch (Exception e) {
			marketOperLogger.error("系统异常!内部交易流水号:{},业务发起方:{}",
					new Object[] { reqMsg.getTxnSeq(), reqMsg.getReqSys() });
			marketLogger.error("系统异常!内部交易流水号:{},业务发起方:{}",
					new Object[] {reqMsg.getTxnSeq(), reqMsg.getReqSys() });
			marketLogger.error("系统异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Market.MARKET_015A06.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Market.MARKET_015A06.getDesc());
			txnLog.setOriOprTransId(reqMsg.getReqTransID());
			upayCsysTxnLogService.modify(txnLog);
			
			resBody.setResultCode(RspCodeConstant.Market.MARKET_015A06.getValue());
			resBody.setResultDesc(RspCodeConstant.Market.MARKET_015A06.getDesc());
			resMsg.setBody(resBody);
			return resMsg;
		}
		
	}

	/**
	 * 初始化交易流水
	 * 
	 * @param txnLog
	 * @param reqMsg
	 * @param resMsg
	 * @param reqBody
	 * @param seqId
	 * @param transIDH
	 * @param intTxnDate
	 * @param transIDHTime
	 */
	private void initLog(UpayCsysTxnLog txnLog, MobileShopMsgVo reqMsg,
			MobileShopMsgVo resMsg, MobileShopMsgReqVo reqBody, Long seqId,
			String transIDH, String intTxnDate, String transIDHTime) {
		UpayCsysTransCode transCode = reqMsg.getTransCode();
		txnLog.setRebateFee(StringFormat.paseLong(reqBody.getRebateFee()));
		txnLog.setCommision(StringFormat.paseLong(reqBody.getCommision()));
		txnLog.setCreditCardFee(StringFormat.paseLong(reqBody.getCreditCardFee()));
		txnLog.setActivityNo(reqBody.getActivityNO());
		txnLog.setProductNo(reqBody.getProdID());
		txnLog.setOrderCnt(StringFormat.paseLong(reqBody.getProdCnt()));
		txnLog.setProdDiscount(StringFormat.paseLong(reqBody.getProdDiscount()));
		txnLog.setProductShelfNo(reqBody.getProdShelfNO());
		txnLog.setPayAmt(StringFormat.paseLong(reqBody.getChargeMoney()));
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
		txnLog.setSettleDate(reqMsg.getReqDate());
		txnLog.setReqDomain(reqMsg.getReqSys());
		txnLog.setReqCnlType(reqMsg.getReqChannel());
		txnLog.setReqActivityCode(reqMsg.getActivityCode());
		txnLog.setReqTransDt(reqMsg.getReqDate());
		txnLog.setReqTransTm(reqMsg.getReqDateTime());
		txnLog.setReqTranshId(resMsg.getRcvTransID());
		txnLog.setReqTranshDt(resMsg.getRcvDate());
		txnLog.setReqTranshTm(resMsg.getRcvDateTime());
		txnLog.setIdType(reqBody.getIDType());
		txnLog.setIdValue(reqBody.getIDValue());
		txnLog.setPayAmt(StringFormat.paseLong(reqBody.getPayment()));
		txnLog.setBankId(reqMsg.getReqSys());
		txnLog.setOrderId(reqBody.getOrderID());
		txnLog.setReqOprDt(reqMsg.getReqDate());
		txnLog.setReqOprTm(reqMsg.getReqDateTime());
		txnLog.setOriReqDate(reqMsg.getReqDate());
		txnLog.setReqOprId(reqMsg.getReqTransID());
		txnLog.setReqTransId(reqMsg.getReqTransID());
		txnLog.setBackFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setRefundFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setReverseFlag(CommonConstant.YesOrNo.No.toString());
		txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil.getDateyyyyMMddHHmmssSSS());
		txnLog.setMainFlag(null);
	}
	
}
