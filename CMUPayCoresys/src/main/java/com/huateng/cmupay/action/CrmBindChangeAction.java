package com.huateng.cmupay.action;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.huateng.cmupay.constant.CommonConstant;
import com.huateng.cmupay.constant.ExcConstant;
import com.huateng.cmupay.constant.RspCodeConstant;
import com.huateng.cmupay.controller.cache.CrmErrorCodeCache;
import com.huateng.cmupay.exception.AppBizException;
import com.huateng.cmupay.exception.AppRTException;
import com.huateng.cmupay.models.UpayCsysBindInfo;
import com.huateng.cmupay.models.UpayCsysTransCode;
import com.huateng.cmupay.models.UpayCsysTxnLog;
import com.huateng.cmupay.parseMsg.reflect.handle.MsgHandle;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmBindChangeReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMainMobileBindRespVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMsgVo;
import com.huateng.cmupay.utils.StringFormat;
import com.huateng.cmupay.utils.UUIDGenerator;
import com.huateng.toolbox.utils.DateUtil;
import com.huateng.toolbox.utils.StrUtil;

/**
 * @author ning.z 签约关系变更
 */
@Controller("crmBindChangeAction")
@Scope("prototype")
public class CrmBindChangeAction extends AbsBaseAction<CrmMsgVo, CrmMsgVo> {

	@Override
	public CrmMsgVo execute(CrmMsgVo paramData) throws AppBizException {
		logger.debug("CrmBindChangeAction execute(Object-start)");
		CrmMsgVo crmMsgVo = (CrmMsgVo) paramData;
		
		// 应答报文
		CrmMsgVo msgVoRtn = crmMsgVo;
		CrmBindChangeReqVo mainBindReq = new CrmBindChangeReqVo();
		CrmMainMobileBindRespVo mainBindRes = new CrmMainMobileBindRespVo();
		UpayCsysTxnLog txnLog = new UpayCsysTxnLog();
		
		
		//校验rechamount,rechThreshold数据格式 by xuyunbo 20131206
		String rechamount = null; 
		String rechThreshold = null;
		int beginIndex = crmMsgVo.getBody().toString().indexOf("<RechAmount>");
		int endIndex = crmMsgVo.getBody().toString().indexOf("</RechAmount>");
		if ((beginIndex > -1) && (endIndex > -1)) {
			rechamount = crmMsgVo.getBody().toString().substring(beginIndex + 12, endIndex);
		}
		try {
			Long.valueOf(rechamount);
		} catch (Exception e) {
			// rechamount格式不正确
			logger.error("签约关系变更接口!rechamount格式不正确");
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil
					.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlSubRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			mainBindRes.setRspCode(RspCodeConstant.Bank.BANK_019A26.getValue());
			
			mainBindRes.setRspInfo(RspCodeConstant.Bank.BANK_019A26.getDesc());
			msgVoRtn.setBody(mainBindRes);
			logger.debug("CrmBindChangeAction execute(Object-end)");
			return msgVoRtn;
		}

		beginIndex = crmMsgVo.getBody().toString().indexOf("<RechThreshold>");
		endIndex = crmMsgVo.getBody().toString().indexOf("</RechThreshold>");
		if ((beginIndex > -1) && (endIndex > -1)) {
			rechThreshold = crmMsgVo.getBody().toString().substring(beginIndex + 15, endIndex);
		}
		try {
			Long.valueOf(rechThreshold);
		} catch (Exception e) {
			// rechThreshold格式不正确

			logger.error("签约关系变更接口!rechThreshold格式不正确");
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil
					.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlSubRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			mainBindRes.setRspCode(RspCodeConstant.Bank.BANK_019A25.getValue());
			
			mainBindRes.setRspInfo(RspCodeConstant.Bank.BANK_019A25.getDesc());
			msgVoRtn.setBody(mainBindRes);
			logger.debug("CrmBindChangeAction execute(Object-end)");
			return msgVoRtn;
		}
		
		//解析报文
		MsgHandle.unmarshaller(mainBindReq, (String) crmMsgVo.getBody());
		
		String transIDH = crmMsgVo.getTxnSeq();// 落地方交易流水号
		String intTxnTime = paramData.getTxnTime();// 落地方处理时间
		String intTxnDate = crmMsgVo.getTxnDate();// TODO 从数据库获取
		Long seqId = crmMsgVo.getSeqId();

		

		msgVoRtn.setActionCode(CommonConstant.ActionCode.Respone.getValue());
		msgVoRtn.setTransIDH(transIDH);
		msgVoRtn.setTransIDHTime(StrUtil.subString(intTxnTime, 0, 14));
		mainBindRes.setTransactionID(mainBindReq.getTransactionID());
		mainBindRes.setActionDate(mainBindReq.getActionDate());
		mainBindRes.setSubID(mainBindReq.getSubID());
		mainBindRes.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
		try {
			// 获取交易代码
			UpayCsysTransCode transCode = crmMsgVo.getTransCode();
			if (!StringUtils.isBlank(mainBindReq.getTransactionID())) {
				logger.debug("重复订单检查");
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("reqDomain", crmMsgVo.getMsgSender());
				param.put("reqOprId", mainBindReq.getTransactionID());
				UpayCsysTxnLog upayCsysTxnLog = upayCsysTxnLogService
						.findObj(param);
				if (upayCsysTxnLog != null) {
					log.warn("签约关系变更接口!订单重复,内部交易流水号:{},业务发起方:{}", new Object[] {
							transIDH, crmMsgVo.getMsgSender() });
					logger.warn(
							"订单重复,内部交易流水号:{},业务发起方:{},ReqOprId:{}",
							new Object[] { transIDH,
									crmMsgVo.getMsgSender(),
									mainBindReq.getTransactionID() });
					msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
					msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
					msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
					mainBindRes.setRspCode(RspCodeConstant.Crm.CRM_3A17
							.getValue());
					mainBindRes.setRspInfo(RspCodeConstant.Crm.CRM_3A17
							.getDesc());
					msgVoRtn.setBody(mainBindRes);
					logger.debug("CrmBindChangeAction execute(Object-end)");
					return msgVoRtn;
				}
			}
			logger.debug("记录交易流水--start ");
			txnLog.setSeqId(seqId);
			txnLog.setIntTxnDate(intTxnDate);// 内部交易日期
			txnLog.setIntTxnSeq(transIDH);
			txnLog.setIntTransCode(transCode.getTransCode());
			txnLog.setIntTxnTime(intTxnTime);
			txnLog.setPayMode(transCode.getPayMode());
			txnLog.setBussType(transCode.getBussType());
			txnLog.setBussChl(transCode.getBussChl());
//			txnLog.setTxnCat(transCode.getTxnCat());// TODO
			txnLog.setStatus(CommonConstant.TxnStatus.InitStatus.getValue());
			txnLog.setReqVersion(crmMsgVo.getVersion());
			txnLog.setReqBipCode(crmMsgVo.getBIPCode());
			txnLog.setReqActivityCode(crmMsgVo.getActivityCode());
			txnLog.setReqDomain(crmMsgVo.getMsgSender());
			txnLog.setReqRouteType(crmMsgVo.getRouteType());
			txnLog.setReqRouteVal(crmMsgVo.getRouteValue());
			txnLog.setReqSessionId(crmMsgVo.getSessionID());// 发起方业务流水号
			txnLog.setReqTransId(crmMsgVo.getTransIDO());
			txnLog.setReqTransDt(StrUtil.subString(crmMsgVo.getTransIDOTime(),
					0, 8));// 截取时间
			txnLog.setReqOprId(mainBindReq.getTransactionID());
			txnLog.setReqOprDt(mainBindReq.getActionDate());
			txnLog.setReqOprTm(crmMsgVo.getTransIDOTime());
			txnLog.setReqTransTm(crmMsgVo.getTransIDOTime());
			txnLog.setReqCnlType(mainBindReq.getCnlTyp());
			txnLog.setReqTranshId(transIDH);
			txnLog.setReconciliationFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setReqTranshDt(intTxnDate);// 截取时间
			txnLog.setReqTranshTm(intTxnTime);
			txnLog.setMainFlag(CommonConstant.Mainflag.Master.getValue());
			txnLog.setIdType(mainBindReq.getIDType());
			txnLog.setIdValue(mainBindReq.getIDValue());
			txnLog.setOriOrgId(null);// TODO
			txnLog.setOriOprTransId(null);// 原交易流水号
			txnLog.setOriReqDate(null);// 不用填
			txnLog.setRcvTransId(transIDH);
			txnLog.setRcvOprId(UUIDGenerator.generateUUID());
			txnLog.setBackFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setRefundFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setReverseFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setSettleDate(DateUtil.getDateyyyyMMdd());
			upayCsysTxnLogService.add(txnLog);
			// 报文体内容check
			logger.info("报文体check ---start ");
			String validateMsg = this.validateModel(mainBindReq);
			if (validateMsg == null || "".equals(validateMsg)) {
				logger.info("内部交易流水号:{},body体校验成功",
						new Object[] { transIDH });
			} else {
				logger.warn("签约关系变更接口!内部交易流水号:{},报文体校验失败:{},业务发起方:{}",
						new Object[] {transIDH, validateMsg,
								paramData.getMsgSender() });
				log.warn("签约关系变更接口!内部交易流水号:{},报文体校验失败:{},业务发起方:{}",
						new Object[] { transIDH, validateMsg,
								paramData.getMsgSender() });
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				mainBindRes.setRspCode(RspCodeConstant.Crm.CRM_4A99.getValue());
				mainBindRes.setRspInfo(RspCodeConstant.Crm.CRM_4A99.getDesc());
				msgVoRtn.setBody(mainBindRes);
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_4A99.getValue());
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_4A99.getDesc()
						+ validateMsg);
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				txnLog.setLastUpdTime(intTxnTime);
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmBindChangeAction execute(Object-end)");
				return msgVoRtn;
			}
			// 校验主号是否填错
			Map<String, Object> subBindParams = new HashMap<String, Object>();
			if (mainBindReq.getIDValue() != null
					&& !"".equals(mainBindReq.getIDValue())) {
				subBindParams.put("idValue", mainBindReq.getIDValue());
			}
			subBindParams.put("subId", mainBindReq.getSubID());
			subBindParams.put("mainFlag",
					CommonConstant.Mainflag.Master.getValue());
			subBindParams.put("status",
					CommonConstant.BindStatus.Bind.getValue());
			UpayCsysBindInfo subBind = upayCsysBindInfoService
					.findObj(subBindParams);
			if (subBind == null) {
				logger.info("内部交易流水号:{},主号:{}签约关系不存在,业务发起方:{}", new Object[] {
						transIDH, mainBindReq.getIDValue(),
						paramData.getMsgSender() });
				log.info("内部交易流水号:{},主号:{}签约关系不存在,业务发起方:{}", new Object[] {
						transIDH, mainBindReq.getIDValue(),
						paramData.getMsgSender() });
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				mainBindRes.setRspCode(RspCodeConstant.Crm.CRM_2A13.getValue());
				mainBindRes.setRspInfo(RspCodeConstant.Crm.CRM_2A13.getDesc());
				msgVoRtn.setBody(mainBindRes);
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_2A13.getValue());
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_2A13.getDesc());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmBindChangeAction execute(Object-end)");
				return msgVoRtn;
			}
			txnLog.setBankAccId(StringFormat.formatCodeString(subBind.getBankAccId()));
			txnLog.setBankAcctType(subBind.getBankAcctType());
			txnLog.setBankId(subBind.getBankId());
			txnLog.setUserId(StringFormat.formatCodeString(subBind.getUserId()));
			txnLog.setUserName(StringFormat.formatNameString(subBind.getUserName()));
			txnLog.setUserType(subBind.getUserType());
			txnLog.setSubTime(subBind.getSignSubTime());
			mainBindRes.setSubTime(subBind.getSignSubTime());
//				else if (!(null == mainBindReq.getRechThreshold())) {
//				if (subBind.getUserCat().equals(CommonConstant.UpayFeeType.AfterFee.getValue())) {
//					logger.info("号码为后付费，不允许更改阀值");
//					msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
//					msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
//					msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr
//							.toString());
//					mainBindRes.setRspCode(RspCodeConstant.Crm.CRM_3A28
//							.getValue());
//					mainBindRes.setRspInfo(RspCodeConstant.Crm.CRM_3A28
//							.getDesc());
//					msgVoRtn.setBody(mainBindRes);
//					txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr
//							.toString());
//					txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_3A28
//							.getValue());
//					txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_3A28
//							.getDesc());
//					txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998
//							.getValue());
//					txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
//					txnLog.setStatus(CommonConstant.TxnStatus.TxnFail
//							.getValue());
//					upayCsysTxnLogService.modify(txnLog);
//					return msgVoRtn;
//				}
//			}
			// 签约关系变更
			UpayCsysBindInfo subBindInfos = new UpayCsysBindInfo();
			subBindInfos.setSeqId(subBind.getSeqId());
			subBindInfos.setPayType(mainBindReq.getPayType());
			subBindInfos.setUserCat(mainBindReq.getUserCat());
			subBindInfos.setRechAmount(mainBindReq.getRechAmount());
			subBindInfos.setRechThreshold(mainBindReq.getRechThreshold());
			subBindInfos.setLastUpdTime(intTxnTime);
			txnLog.setSubId(mainBindReq.getSubID());
			txnLog.setUserCat(mainBindReq.getUserCat());
			txnLog.setPayType(mainBindReq.getPayType());
			txnLog.setIdProvince(subBind.getIdProvince());
			txnLog.setChlRspType(CommonConstant.CrmRspType.Success.toString());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_0000.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_0000.getDesc());
			txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_0000.getValue());
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_0000.getDesc());
			txnLog.setRechAmount(mainBindReq.getRechAmount());
			txnLog.setRechThreshold(mainBindReq.getRechThreshold());
			txnLog.setStatus(CommonConstant.TxnStatus.TxnSuccess.getValue());
			logger.info("intTxnSeq:{},更新交易流水表，签约关系", transIDH);
			upayCsysBindInfoService
					.modifyTxnAndBindChange(subBindInfos, txnLog);
			logger.info("intTxnSeq:{},签约关系更更成功", transIDH);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_0000.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_0000.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.Success.toString());
			mainBindRes.setRspCode(RspCodeConstant.Crm.CRM_0000.getValue());
			mainBindRes.setRspInfo(RspCodeConstant.Crm.CRM_0000.getDesc());
			msgVoRtn.setBody(mainBindRes);
			logger.info("内部交易流水号:{},业务发起方:{},业务应答码:{}", new Object[] {
					transIDH, paramData.getMsgSender(),
					mainBindRes.getRspCode() });
			log.info(
					"内部交易流水号:{},业务发起方:{},业务应答码:{}",
					new Object[] { transIDH,
							paramData.getMsgSender(), mainBindRes.getRspCode() });
			logger.debug("CrmBindChangeAction execute(Object-end)");
			return msgVoRtn;
		} catch (AppRTException e) {
			log.error(
					"签约关系变更接口!内部异常!内部交易流水号:{},业务发起方:{}",
					new Object[] { transIDH,
							paramData.getMsgSender() });
			logger.error("签约关系变更接口!内部异常,代码:{},内部交易流水号:{},业务发起方:{}",
					new Object[] { e.getCode(),transIDH,
							paramData.getMsgSender() });
			logger.error("签约关系变更接口!内部异常:",e);
			String errCode = e.getCode();
			errCode = CrmErrorCodeCache.getCrmErrCode(errCode);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			mainBindRes.setRspCode(errCode);
			mainBindRes.setRspInfo(RspCodeConstant.Crm.getDescByValue(errCode));
//			mainBindRes.setRspInfo(MessageHandler.getCrmErrMsg(errCode));
			msgVoRtn.setBody(mainBindRes);
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(errCode);
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.getDescByValue(errCode));
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil
					.getDateyyyyMMddHHmmssSSS());
			upayCsysTxnLogService.modify(txnLog);
			logger.debug("CrmBindChangeAction execute(Object-end)");
			return msgVoRtn;
		} catch (AppBizException e) {
			log.error(
					"签约关系变更接口!业务异常!内部交易流水号:{},业务发起方:{}",
					new Object[] { transIDH,
							paramData.getMsgSender() });
			logger.error("签约关系变更接口!业务异常,代码:{},内部交易流水号:{},业务发起方:{}",
					new Object[] { e.getCode(),transIDH,
							paramData.getMsgSender() });
			logger.error("签约关系变更接口!业务异常:",e);
			String errCode = e.getCode();
			errCode = CrmErrorCodeCache.getCrmErrCode(errCode);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			mainBindRes.setRspCode(errCode);
			mainBindRes.setRspInfo(RspCodeConstant.Crm.getDescByValue(errCode));
			msgVoRtn.setBody(mainBindRes);
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.getValue());
			txnLog.setChlRspType(msgVoRtn.getRspType());
			txnLog.setChlSubRspCode(errCode);
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.getDescByValue(errCode));
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil
					.getDateyyyyMMddHHmmssSSS());
			upayCsysTxnLogService.modify(txnLog);
			logger.debug("CrmBindChangeAction execute(Object-end)");
			return msgVoRtn;
		} catch (Exception e) {
			log.error(
					"签约关系变更接口!未知异常!内部交易流水号:{},业务发起方:{}",
					new Object[] { transIDH,
							paramData.getMsgSender() });
			logger.error("签约关系变更接口!未知异常!内部交易流水号:{},业务发起方:{}", new Object[] {
					transIDH, paramData.getMsgSender() });
			logger.error("签约关系变更接口!未知异常:",e);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			txnLog.setLastUpdTime(com.huateng.toolbox.utils.DateUtil
					.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_5A06.getValue());
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_5A06.getDesc()+":"+e.getMessage());
			upayCsysTxnLogService.modify(txnLog);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			mainBindRes.setRspCode(RspCodeConstant.Crm.CRM_5A06.getValue());
			
			String errDesc=e.getMessage().length()<=ExcConstant.MSG_LENGTH_230?e.getMessage():e.getMessage().substring(0, ExcConstant.MSG_LENGTH_230);
			mainBindRes.setRspInfo(RspCodeConstant.Crm.CRM_5A06.getDesc()+":"+errDesc);
			msgVoRtn.setBody(mainBindRes);
			logger.debug("CrmBindChangeAction execute(Object-end)");
			return msgVoRtn;
		}

	}
}
