package com.huateng.cmupay.action;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import com.huateng.cmupay.constant.CommonConstant;
import com.huateng.cmupay.constant.ExcConstant;
import com.huateng.cmupay.constant.RspCodeConstant;
import com.huateng.cmupay.controller.cache.CrmErrorCodeCache;
import com.huateng.cmupay.controller.cache.ProvAreaCache;
import com.huateng.cmupay.controller.cache.SysMapCache;
import com.huateng.cmupay.exception.AppBizException;
import com.huateng.cmupay.exception.AppRTException;
import com.huateng.cmupay.jms.business.bank.BankMainBindBankBus;
import com.huateng.cmupay.models.ProvincePhoneNum;
import com.huateng.cmupay.models.UpayCsysBindInfo;
import com.huateng.cmupay.models.UpayCsysSysMapInfo;
import com.huateng.cmupay.models.UpayCsysTxnLog;
import com.huateng.cmupay.parseMsg.reflect.handle.MsgHandle;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.BankMainMobileBindReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.BankMainMobileBindRespVo;
import com.huateng.cmupay.parseMsg.reflect.vo.bank.BankMsgVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMainMobileBindReqVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMainMobileBindRespVo;
import com.huateng.cmupay.parseMsg.reflect.vo.crm.CrmMsgVo;
import com.huateng.cmupay.utils.StringFormat;
import com.huateng.toolbox.utils.DateUtil;
import com.huateng.toolbox.utils.StrUtil;

/**
 * 
 * @author fan_kui 省中心主号签约交易
 */
@Controller("crmMainMobileBindAction")
@Scope("prototype")
public class CrmMainMobileBindAction extends AbsBaseAction<CrmMsgVo, CrmMsgVo> {

	@Autowired
	private BankMainBindBankBus mainBindBankBus;

	@Override
	public CrmMsgVo execute(CrmMsgVo paramData) throws AppBizException {

		logger.debug("开始执行主号签约信息同步交易,CrmMainMobileBindAction execute(Object)-start");

		CrmMsgVo crmMsgVo = paramData;
		CrmMsgVo msgVoRtn = new CrmMsgVo();
		CrmMainMobileBindReqVo reqVo = new CrmMainMobileBindReqVo();
		CrmMainMobileBindRespVo respVO = new CrmMainMobileBindRespVo();
		UpayCsysTxnLog txnLog = new UpayCsysTxnLog();

		// 设置内部事件，日期，流水号
		String intTxnDate = crmMsgVo.getTxnDate();
		txnLog.setSettleDate(intTxnDate);
		String txnSeq = crmMsgVo.getTxnSeq();
		Long seqId = crmMsgVo.getSeqId();
		String intTxnTime = crmMsgVo.getTxnTime();
		msgVoRtn = crmMsgVo;
		msgVoRtn.setActionCode(CommonConstant.ActionCode.Respone.toString());
		msgVoRtn.setTransIDH(txnSeq);
		msgVoRtn.setTransIDHTime(StrUtil.subString(intTxnTime, 0, 14));

		try {

			
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
				logger.error("省中心主号签约接口!rechamount格式不正确！");
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				respVO.setRspCode(RspCodeConstant.Bank.BANK_019A26.getValue());
				respVO.setRspInfo(RspCodeConstant.Bank.BANK_019A26.getDesc());
				respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				msgVoRtn.setBody(respVO);

				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
				txnLog.setChlSubRspCode(RspCodeConstant.Bank.BANK_019A26.getValue());
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setChlSubRspDesc(RspCodeConstant.Bank.BANK_019A26.getDesc());
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}

			beginIndex = crmMsgVo.getBody().toString()
					.indexOf("<RechThreshold>");
			endIndex = crmMsgVo.getBody().toString()
					.indexOf("</RechThreshold>");
			if ((beginIndex > -1) && (endIndex > -1)) {
				rechThreshold = crmMsgVo.getBody().toString()
						.substring(beginIndex + 15, endIndex);
			}
			try {
				Long.valueOf(rechThreshold);
			} catch (Exception e) {
				// rechThreshold格式不正确
				logger.error("省中心主号签约接口!rechThreshold格式不正确！");
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				respVO.setRspCode(RspCodeConstant.Bank.BANK_019A25.getValue());
				respVO.setRspInfo(RspCodeConstant.Bank.BANK_019A25.getDesc());
				respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				msgVoRtn.setBody(respVO);

				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
				txnLog.setChlSubRspCode(RspCodeConstant.Bank.BANK_019A25.getValue());
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setChlSubRspDesc(RspCodeConstant.Bank.BANK_019A25.getDesc());
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}

			
			
			// 解析报文体
			MsgHandle.unmarshaller(reqVo, (String) crmMsgVo.getBody());
			respVO.setTransactionID(reqVo.getTransactionID());
			respVO.setActionDate(reqVo.getActionDate());

			// 校验报文体格式
			String validateMsg = this.validateModel(reqVo);
			if (!StringUtils.isNotBlank(validateMsg)) {
				logger.debug("body validate success");
			} else {
				logger.warn(
						"省中心主号签约接口!格式校验失败!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},错误字段:{}",
						new Object[] { paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue(),
								validateMsg });
				log.warn(
						"省中心主号签约接口!格式校验失败!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},错误字段:{}",
						new Object[] { paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue(),
								validateMsg });
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				respVO.setRspCode(RspCodeConstant.Crm.CRM_4A99.getValue());
				respVO.setRspInfo(RspCodeConstant.Crm.CRM_4A99.getDesc()
						+ validateMsg);
				respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				msgVoRtn.setBody(respVO);
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_4A99.getValue());
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_4A99.getDesc());
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());

				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}

			// 根据发起方机构，操作流水号查询操作交易流水是否重复
			if (!StringUtils.isBlank(reqVo.getTransactionID())) {
				if (isRepeatTrans(reqVo.getTransactionID(),
						crmMsgVo.getMsgSender())) {
					logger.warn(
							"省中心主号签约接口!操作流水重复!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{}",
							new Object[] { paramData.getTransIDO(),
									reqVo.getTransactionID(), txnSeq,
									paramData.getMsgSender(),
									reqVo.getIDValue() });
					log.warn(
							"省中心主号签约接口!操作流水重复!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{}",
							new Object[] { paramData.getTransIDO(),
									reqVo.getTransactionID(), txnSeq,
									paramData.getMsgSender(),
									reqVo.getIDValue() });
					msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr
							.getValue());
					msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
					msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
					respVO.setRspCode(RspCodeConstant.Crm.CRM_3A17.getValue());
					respVO.setRspInfo(RspCodeConstant.Crm.CRM_3A17.getDesc());
					respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
					msgVoRtn.setBody(respVO);
					logger.debug("CrmMainMobileBindAction execute(Object)-end");
					return msgVoRtn;
				}
			}
			// 根据发起方机构号查找出对应的省代码
			UpayCsysSysMapInfo upayCsysSysMapInfo = SysMapCache
					.getSysCd(crmMsgVo.getMsgSender());
			String proId = upayCsysSysMapInfo.getAreaCd();
			// 添加一条新的交易流水
			txnLog.setSeqId(seqId);
			txnLog.setIntTransCode((crmMsgVo.getTransCode()).getTransCode());
			txnLog.setIntTxnSeq(txnSeq);
			txnLog.setIntTxnDate(intTxnDate);
			txnLog.setIntTxnTime(intTxnTime);
			txnLog.setIntMqSeq(crmMsgVo.getMqSeq());
			txnLog.setBussType(crmMsgVo.getTransCode().getBussType());
			txnLog.setBussChl(crmMsgVo.getTransCode().getBussChl());
			txnLog.setPayMode(crmMsgVo.getTransCode().getPayMode());
			// txnLog.setTxnCat(crmMsgVo.getTransCode().getTxnCat());
			txnLog.setReqVersion(ExcConstant.CRM_VERSION);
			txnLog.setReqBipCode(crmMsgVo.getBIPCode());
			txnLog.setReqActivityCode(crmMsgVo.getActivityCode());
			txnLog.setReqDomain(crmMsgVo.getMsgSender());
			txnLog.setReqRouteType(crmMsgVo.getRouteType());
			txnLog.setReconciliationFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setReqRouteVal(crmMsgVo.getRouteValue());
			txnLog.setReqSessionId(crmMsgVo.getSessionID());
			txnLog.setPayType(reqVo.getPayType());
			txnLog.setReqTransTm(crmMsgVo.getTransIDOTime());
			txnLog.setReqTransDt(StrUtil.subString(crmMsgVo.getTransIDOTime(),
					0, 8));
			txnLog.setReqTransId(crmMsgVo.getTransIDO());
			txnLog.setReqTranshTm(txnLog.getIntTxnTime());
			txnLog.setReqTranshDt(txnLog.getIntTxnDate());
			txnLog.setReqTranshId(txnSeq);
			txnLog.setReqOprId(reqVo.getTransactionID());
			txnLog.setReqOprDt(reqVo.getActionDate());
			txnLog.setReqOprTm(crmMsgVo.getTransIDOTime());
			txnLog.setIdProvince(proId);
			txnLog.setIdType(reqVo.getIDType());
			txnLog.setIdValue(reqVo.getIDValue());
			txnLog.setRcvDomain(reqVo.getBankID());
			// txnLog.setUserStatus("00");
			txnLog.setUserCat(reqVo.getUserCat());
			// txnLog.setBalance(0L);
			txnLog.setUserId(StringFormat.formatCodeString(reqVo.getUserID()));
			txnLog.setUserName(StringFormat.formatNameString(reqVo
					.getUserName()));
			txnLog.setUserType(reqVo.getUserIDType());
			txnLog.setBankId(reqVo.getBankID());
			txnLog.setBankAcctType(reqVo.getBankAcctType());
			txnLog.setBankAccId(StringFormat.formatCodeString(reqVo
					.getBankAcctID()));
			txnLog.setReqCnlType(reqVo.getCnlTyp());
			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setBackFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setRefundFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setReverseFlag(CommonConstant.YesOrNo.No.toString());
			txnLog.setStatus(CommonConstant.TxnStatus.InitStatus.toString());
			txnLog.setSettleDate(DateUtil.getDateyyyyMMdd());
			upayCsysTxnLogService.add(txnLog);

			// 对主号签约请求报文中的渠道+银行卡类型字段进行校验 渠道网厅以及信用卡就不能签约
			if (!CommonConstant.CnlType.CmccHall.getValue().equals(
					reqVo.getCnlTyp())) {
				logger.warn(
						"省中心主号签约接口!渠道或银行卡类型不对,CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},渠道:{},卡类型:{}",
						new Object[] { reqVo.getBankID(),
								paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue(),
								reqVo.getCnlTyp(), reqVo.getBankAcctType() });

				log.warn(
						"省中心主号签约接口!渠道或银行卡类型不对,CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},渠道:{},卡类型:{}",
						new Object[] { reqVo.getBankID(),
								paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue(),
								reqVo.getCnlTyp(), reqVo.getBankAcctType() });

				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				respVO.setRspCode(RspCodeConstant.Crm.CRM_3A11.getValue());
				respVO.setRspInfo(RspCodeConstant.Crm.CRM_3A11.getDesc());
				respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				msgVoRtn.setBody(respVO);
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_3A11.getValue());
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_3A11.getDesc());
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}
			// 通过银行编号校验落地方是否有服务权限

			// boolean orgFlag=isO2OTransOn(paramData.getMsgSender(),
			// reqVo.getBankID(),
			// paramData.getTransCode().getTransCode());
			// 查询该交易的号码段属于移动还是联通电信的。
			ProvincePhoneNum provincePhoneNum = ProvAreaCache
					.getProvAreaByPrimary(reqVo.getIDValue());
			String orgFlag = offOrgTrans(
					paramData.getMsgSender(),
					reqVo.getBankID(),
					paramData.getTransCode().getTransCode(),
					provincePhoneNum != null ? provincePhoneNum
							.getPhoneNumFlag()
							: CommonConstant.PhoneNumType.CHINA_MOBILE
									.getType());
			if (orgFlag != null) {
				logger.warn(
						"省中心主号签约接口!BANK无权限:{},CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{}",
						new Object[] { reqVo.getBankID(),
								paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue() });

				log.warn(
						"省中心主号签约接口!BANK无权限:{},CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{}",
						new Object[] { reqVo.getBankID(),
								paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue() });

				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				respVO.setRspCode(RspCodeConstant.Crm.CRM_3A35.getValue());
				respVO.setRspInfo(RspCodeConstant.Crm.CRM_3A35.getDesc() + ":"
						+ "落地方机构服务的权限关闭。");
				respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				msgVoRtn.setBody(respVO);
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_3A35.getValue());
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_3A35.getDesc()
						+ orgFlag);
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}

			// 查询签约关系
			Map<String, Object> paramBind = new HashMap<String, Object>();
			paramBind.put("idValue", reqVo.getIDValue());
			paramBind.put("idType",
					CommonConstant.UserSignType.Phone.toString());
			UpayCsysBindInfo uPayCsysBindInfo = upayCsysBindInfoService
					.findObj(paramBind);
			if (null != uPayCsysBindInfo
					&& CommonConstant.Mainflag.Slave.getValue().equals(
							uPayCsysBindInfo.getMainFlag())
					&& CommonConstant.BindStatus.Bind.getValue().equals(
							uPayCsysBindInfo.getStatus())) {
				logger.warn(
						"该号码已签约为主号，CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{}",
						new Object[] { paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue() });

				log.info(
						"该号码已签约为主号，CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{}",
						new Object[] { paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue() });

				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				respVO.setRspCode(RspCodeConstant.Crm.CRM_3A23.getValue());
				respVO.setRspInfo(RspCodeConstant.Crm.CRM_3A23.getDesc());
				respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				msgVoRtn.setBody(respVO);
				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_3A23.getValue());
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_3A23.getDesc());
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}
			// 设置签约关系存在标志位
			Boolean flag = false;
			// 已有签约关系，但是签约状态为非签约时，可继续签约
			if (uPayCsysBindInfo != null) {
				// if (CommonConstant.BindStatus.Bind.toString().equals(
				// uPayCsysBindInfo.getStatus())) {
				// logger.info("签约存在!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},签约银行:{},协议号:{}",
				// new
				// Object[]{paramData.getTransIDO(),reqVo.getTransactionID(),txnSeq,
				// paramData.getMsgSender(),reqVo.getIDValue(),uPayCsysBindInfo.getBankId(),
				// uPayCsysBindInfo.getSubId()});
				//
				// log.warn("省中心主号签约接口!签约存在!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},签约银行:{},协议号:{}",
				// new
				// Object[]{paramData.getTransIDO(),reqVo.getTransactionID(),txnSeq,
				// paramData.getMsgSender(),reqVo.getIDValue(),uPayCsysBindInfo.getBankId(),
				// uPayCsysBindInfo.getSubId()});
				//
				// msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				// msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				// msgVoRtn.setRspType(parseRspType(RspCodeConstant.Crm.CRM_2A01.getValue()));
				// respVO.setRspCode(RspCodeConstant.Crm.CRM_2A01.getValue());
				// respVO.setRspInfo(RspCodeConstant.Crm.CRM_2A01.getDesc());
				// respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				// msgVoRtn.setBody(respVO);
				// txnLog.setStatus(CommonConstant.TxnStatus.TxnFail
				// .toString());
				// txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				// txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				// txnLog.setChlRspType(msgVoRtn.getRspType());
				// txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_2A01.getValue());
				// txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_2A01.getDesc());
				// txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				// upayCsysTxnLogService.modify(txnLog);
				// return msgVoRtn;
				// }
				// 说明虽然是有签约记录，但不是签约状态，所以可继续签约
				flag = true;
			}
			BankMainMobileBindReqVo bankMainMobileBindReqVo = new BankMainMobileBindReqVo();
			bankMainMobileBindReqVo.setBankAcctID(reqVo.getBankAcctID());
			bankMainMobileBindReqVo.setIDValue(reqVo.getIDValue());
			bankMainMobileBindReqVo.setIDType(reqVo.getIDType());
			bankMainMobileBindReqVo.setSubID(reqVo.getSubID());
			bankMainMobileBindReqVo.setUserID(reqVo.getUserID());
			bankMainMobileBindReqVo.setUserIDType(reqVo.getUserIDType());
			bankMainMobileBindReqVo.setUserName(reqVo.getUserName());
			bankMainMobileBindReqVo.setHomeProv(proId);

			BankMsgVo bankMsgVo = new BankMsgVo();
			bankMsgVo.setBody(bankMainMobileBindReqVo);
			bankMsgVo.setActionCode(CommonConstant.ActionCode.Requset
					.toString());
			bankMsgVo.setActivityCode(CommonConstant.BankTrans.Bank02
					.toString());
			bankMsgVo.setReqSys(CommonConstant.BankOrgCode.CMCC.toString());
			bankMsgVo.setReqChannel(reqVo.getCnlTyp());
			bankMsgVo.setReqDate(intTxnDate);
			bankMsgVo.setReqDateTime(intTxnTime);
			bankMsgVo.setTransCode(crmMsgVo.getTransCode());
			bankMsgVo.setReqTransID(txnSeq);
			bankMsgVo.setRcvSys(reqVo.getBankID());
			// 记录发送到银行端的请求报文流水
			txnLog.setRcvTransDt(intTxnDate);
			txnLog.setRcvTransId(txnSeq);
			txnLog.setRcvTransTm(intTxnTime);
			txnLog.setRcvCnlType(bankMsgVo.getReqChannel());
			txnLog.setRcvDomain(reqVo.getBankID());
			txnLog.setRcvActivityCode(CommonConstant.BankTrans.Bank02
					.toString());

			// 将组装好的银行端报文发送到银行前置
			Map<String, Object> params = new HashMap<String, Object>();
			BankMsgVo bankMsgVoRtn = mainBindBankBus.execute(bankMsgVo, params,
					txnLog, uPayCsysBindInfo);
			txnLog.setSettleDate(bankMsgVoRtn.getRcvDate());
			txnLog.setRcvTranshDt(bankMsgVoRtn.getRcvDate());
			txnLog.setRcvTranshId(bankMsgVoRtn.getRcvTransID());
			txnLog.setRcvTranshTm(bankMsgVoRtn.getRcvDateTime());
			txnLog.setRcvOprId(bankMsgVoRtn.getRcvTransID());
			// 判断应答是否超时，U99998为平台自定义超时
			// if (RspCodeConstant.Upay.UPAY_U99998.getValue().equals(
			// bankMsgVoRtn.getRspCode())) {
			// log.warn("省中心主号签约接口!BANK前置超时!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{}",
			// new
			// Object[]{paramData.getTransIDO(),reqVo.getTransactionID(),txnSeq,
			// paramData.getMsgSender(),reqVo.getiDValue(),reqVo.getBankID()});
			// logger.info("BANK前置超时!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{}",
			// new
			// Object[]{paramData.getTransIDO(),reqVo.getTransactionID(),txnSeq,
			// paramData.getMsgSender(),reqVo.getiDValue(),reqVo.getBankID()});
			// msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			// msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			// msgVoRtn.setRspType(CommonConstant.CrmRspType.SysErr.toString());
			// respVO.setRspCode(RspCodeConstant.Crm.CRM_5A07.getValue());
			// respVO.setRspInfo(RspCodeConstant.Crm.CRM_5A07.getDesc());
			// respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
			// msgVoRtn.setBody(respVO);
			// txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			// txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			// txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			// txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr
			// .toString());
			// txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_5A07.getValue());
			// txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_5A07.getDesc());
			// txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			// upayCsysTxnLogService.modify(txnLog);
			// return msgVoRtn;
			//
			// }
			// //判断得到的银行端报文体是否为空，为空将返回错误报文
			// if(StringUtils.isBlank(bankMsgVoRtn.getBody().toString())){
			// log.warn("省中心主号签约接口!应答报文空!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{},BANK流水:{},BANK应答码:{},BANK应答描述:{}",
			// new
			// Object[]{paramData.getTransIDO(),reqVo.getTransactionID(),txnSeq,
			// paramData.getMsgSender(),reqVo.getiDValue(),reqVo.getBankID(),
			// bankMsgVoRtn.getRcvTransID(),bankMsgVoRtn.getRspCode(),bankMsgVoRtn.getRspDesc()});
			// logger.info("应答报文空!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{},BANK流水:{},BANK应答码:{},BANK应答描述:{}",
			// new
			// Object[]{paramData.getTransIDO(),reqVo.getTransactionID(),txnSeq,
			// paramData.getMsgSender(),reqVo.getiDValue(),reqVo.getBankID(),
			// bankMsgVoRtn.getRcvTransID(),bankMsgVoRtn.getRspCode(),bankMsgVoRtn.getRspDesc()});
			// msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			// msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			// msgVoRtn.setRspType(CommonConstant.CrmRspType.SysErr.toString());
			// respVO.setRspCode(RspCodeConstant.Crm.CRM_5A07.getValue());
			// respVO.setRspInfo(RspCodeConstant.Crm.CRM_5A07.getDesc());
			// respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
			// msgVoRtn.setBody(respVO);
			//
			// txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			// txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			// txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			// txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr
			// .toString());
			// txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_5A07.getValue());
			// txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_5A07.getDesc());
			// txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			// upayCsysTxnLogService.modify(txnLog);
			// return msgVoRtn;
			// }

			BankMainMobileBindRespVo bankMainMobileBindRespVo = new BankMainMobileBindRespVo();
			// 解析银行应答报文体内容
			MsgHandle.unmarshaller(bankMainMobileBindRespVo,
					(String) bankMsgVoRtn.getBody());

			// 判断此时是否有签约记录了
			Map<String, Object> checkBind = new HashMap<String, Object>();
			checkBind.put("idValue", reqVo.getIDValue());
			checkBind.put("idType",
					CommonConstant.UserSignType.Phone.toString());
			UpayCsysBindInfo checkBindInfo = upayCsysBindInfoService
					.findObj(checkBind);
			if (checkBindInfo != null
					&& CommonConstant.BindStatus.Bind.toString().equals(
							checkBindInfo.getStatus())) {
				logger.warn(
						"省中心主号签约接口!签约存在!签约流水:{},签约时间:{},号码:{},签约银行:{},协议号:{},签约省:{}",
						new Object[] { checkBindInfo.getSignTxnSeq(),
								checkBindInfo.getSignTxnTime(),
								checkBindInfo.getIdValue(),
								checkBindInfo.getBankId(),
								checkBindInfo.getSubId(),
								checkBindInfo.getIdProvince() });

				log.warn(
						"省中心主号签约接口!签约存在!签约流水:{},签约时间:{},号码:{},签约银行:{},协议号:{},签约省:{}",
						new Object[] { checkBindInfo.getSignTxnSeq(),
								checkBindInfo.getSignTxnTime(),
								checkBindInfo.getIdValue(),
								checkBindInfo.getBankId(),
								checkBindInfo.getSubId(),
								checkBindInfo.getIdProvince() });
				// msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				// msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				// msgVoRtn.setRspType(parseRspType(RspCodeConstant.Crm.CRM_2A01.getValue()));
				// respVO.setRspCode(RspCodeConstant.Crm.CRM_2A01.getValue());
				// respVO.setRspInfo(RspCodeConstant.Crm.CRM_2A01.getDesc());
				// respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
				// msgVoRtn.setBody(respVO);
				// txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				// txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				// txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				// txnLog.setChlRspType(msgVoRtn.getRspType());
				// txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_2A01.getValue());
				// txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_2A01.getDesc());
				// txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				// upayCsysTxnLogService.modify(txnLog);
				// return msgVoRtn;
				flag = true;
			}
			// 判断银行返回的应答码是不是成功的应答码，不是成功交易，将返回错误报文
			if (!(RspCodeConstant.Bank.BANK_020A00.getValue()
					.equals(bankMsgVoRtn.getRspCode()))) {
				log.warn(
						"省中心主号签约接口!交易失败!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{},BANK流水:{},BANK应答码:{},BANK应答描述:{}",
						new Object[] { paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue(),
								reqVo.getBankID(),
								bankMsgVoRtn.getRcvTransID(),
								bankMsgVoRtn.getRspCode(),
								bankMsgVoRtn.getRspDesc() });
				logger.warn(
						"省中心主号签约接口!交易失败!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{},BANK流水:{},BANK应答码:{},BANK应答描述:{}",
						new Object[] { paramData.getTransIDO(),
								reqVo.getTransactionID(), txnSeq,
								paramData.getMsgSender(), reqVo.getIDValue(),
								reqVo.getBankID(),
								bankMsgVoRtn.getRcvTransID(),
								bankMsgVoRtn.getRspCode(),
								bankMsgVoRtn.getRspDesc() });
				String errCode = CrmErrorCodeCache.getCrmErrCode(bankMsgVoRtn
						.getRspCode());
				msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
				respVO.setRspCode(errCode);
				respVO.setRspInfo(RspCodeConstant.Crm.getDescByValue(errCode));
				respVO.setSubTime(bankMainMobileBindRespVo.getSubTime());
				msgVoRtn.setBody(respVO);
				msgVoRtn.setTransIDH(txnSeq);
				msgVoRtn.setTransIDHTime(StrUtil.subString(intTxnTime, 0, 14));

				txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
				txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
				txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
				txnLog.setChlRspType(msgVoRtn.getRspType());
				txnLog.setChlSubRspCode(errCode);
				txnLog.setChlSubRspDesc(RspCodeConstant.Crm
						.getDescByValue(errCode));
				txnLog.setRcvRspType(msgVoRtn.getRspType());
				txnLog.setRcvRspCode(bankMsgVoRtn.getRspCode());
				txnLog.setRcvRspDesc(RspCodeConstant.Bank
						.getDescByValue(bankMsgVoRtn.getRspCode()));
				txnLog.setRcvSubRspCode(bankMsgVoRtn.getRspCode());
				txnLog.setRcvSubRspDesc(RspCodeConstant.Bank
						.getDescByValue(bankMsgVoRtn.getRspCode()));
				// txnLog.setRcvSubRspDesc(MessageHandler.getBankErrMsg(bankMsgVoRtn.getRspCode()));
				txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
				upayCsysTxnLogService.modify(txnLog);
				logger.debug("CrmMainMobileBindAction execute(Object)-end");
				return msgVoRtn;
			}
			// txnLog.setSubTime(bankMainMobileBindRespVo.getSubTime());
			respVO.setSubID(bankMainMobileBindRespVo.getSubID());
			respVO.setRspCode(RspCodeConstant.Crm.CRM_0000.getValue());
			respVO.setRspInfo(RspCodeConstant.Crm.CRM_0000.getDesc());
			respVO.setSettleDate(bankMsgVoRtn.getRcvDate());
			respVO.setSubTime(bankMainMobileBindRespVo.getSubTime());
			msgVoRtn.setBody(respVO);
			msgVoRtn.setActionCode(CommonConstant.ActionCode.Respone.toString());
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_0000.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_0000.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.Success.toString());

			logger.info("intTxnSeq:{},开始添加签约关系表", txnSeq);
			UpayCsysBindInfo upayCsysBindInfo = new UpayCsysBindInfo();
			upayCsysBindInfo.setBankAccId(StringFormat.formatCodeString(reqVo
					.getBankAcctID()));
			upayCsysBindInfo.setBankAcctType(reqVo.getBankAcctType());
			upayCsysBindInfo.setBankId(reqVo.getBankID());
			upayCsysBindInfo.setSignCnlType(reqVo.getCnlTyp());
			upayCsysBindInfo.setSignOrgId(crmMsgVo.getMsgSender());
			upayCsysBindInfo.setSubId(bankMainMobileBindRespVo.getSubID());
			upayCsysBindInfo.setMainFlag(CommonConstant.Mainflag.Master
					.toString());
			upayCsysBindInfo.setPayType(reqVo.getPayType());
			upayCsysBindInfo.setIdType(reqVo.getIDType());
			upayCsysBindInfo.setIdValue(reqVo.getIDValue());
			// upayCsysBindInfo.setRechAmount(reqVo.getRechAmount());
			// upayCsysBindInfo.setRechThreshold(reqVo.getRechThreshold());
			upayCsysBindInfo.setUserType(reqVo.getUserIDType());
			upayCsysBindInfo.setUserId(StringFormat.formatCodeString(reqVo
					.getUserID()));
			upayCsysBindInfo.setStatus(CommonConstant.BindStatus.Bind
					.toString());
			upayCsysBindInfo.setUserName(StringFormat.formatNameString(reqVo
					.getUserName()));
			upayCsysBindInfo.setRechAmount(reqVo.getRechAmount());
			upayCsysBindInfo.setRechThreshold(reqVo.getRechThreshold());
			upayCsysBindInfo.setIdProvince(proId);
			upayCsysBindInfo.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmss());
			upayCsysBindInfo.setSignSubTime(bankMainMobileBindRespVo
					.getSubTime());
			upayCsysBindInfo.setSignTxnDate(reqVo.getActionDate());
			upayCsysBindInfo.setSignTxnSeq(crmMsgVo.getTransIDO());
			upayCsysBindInfo.setSignTxnTime(reqVo.getSubTime());
			upayCsysBindInfo.setReserved1(DateUtil.getDateyyyyMMddHHmmssSSS());
			upayCsysBindInfo.setUserCat(reqVo.getUserCat());
			upayCsysBindInfo.setSettleDate(bankMsgVoRtn.getReqDate());
			// txnLog.setSubTime(bankMainMobileBindRespVo.getSubTime());
			// txnLog.setSubId(bankMainMobileBindRespVo.getSubID());
			txnLog.setRechThreshold(upayCsysBindInfo.getRechThreshold());
			txnLog.setRechAmount(upayCsysBindInfo.getRechAmount());
			txnLog.setStatus(CommonConstant.TxnStatus.TxnSuccess.toString());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_0000.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_0000.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.Success.toString());
			txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_0000.getValue());
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_0000.getDesc());
			txnLog.setRcvRspType(msgVoRtn.getRspType());
			txnLog.setRcvRspCode(bankMsgVoRtn.getRspCode());
			txnLog.setRcvRspDesc(RspCodeConstant.Bank
					.getDescByValue(bankMsgVoRtn.getRspCode()));
			txnLog.setRcvSubRspCode(bankMsgVoRtn.getRspCode());
			txnLog.setRcvSubRspDesc(RspCodeConstant.Bank
					.getDescByValue(bankMsgVoRtn.getRspCode()));
			// txnLog.setRcvRspCode(RspCodeConstant.Bank.BANK_020A00.getValue());
			// txnLog.setRcvRspDesc(RspCodeConstant.Bank.BANK_020A00.getDesc());
			// txnLog.setRcvSubRspCode(RspCodeConstant.Bank.BANK_020A00.getValue());
			// txnLog.setRcvSubRspDesc(RspCodeConstant.Bank.BANK_020A00.getDesc());
			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setMainFlag(CommonConstant.Mainflag.Master.toString());
			txnLog.setSubId(bankMainMobileBindRespVo.getSubID());
			txnLog.setSubTime(bankMainMobileBindRespVo.getSubTime());
			if (flag) {
				upayCsysBindInfo.setSeqId(uPayCsysBindInfo.getSeqId());
				// upayCsysBindInfoService.modifyLogAndBinfInfo(null,upayCsysBindInfo,txnLog);
				upayCsysBindInfoService.modifyLogAndCleanBindInfo(
						upayCsysBindInfo, txnLog);
			} else {
				upayCsysBindInfo.setSeqId(seqId);
				upayCsysBindInfoService.modifyLogAndAddBindInfo(
						upayCsysBindInfo, txnLog);
			}
			log.succ(
					"交易成功!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{},BANK流水:{},BANK应答码:{},BANK应答描述:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID(), bankMsgVoRtn.getRcvTransID(),
							bankMsgVoRtn.getRspCode(),
							bankMsgVoRtn.getRspDesc() });
		} catch (AppRTException e) {

			String errCode = e.getCode();
			errCode = CrmErrorCodeCache.getCrmErrCode(errCode);

			log.error(
					"省中心主号签约接口!运行异常!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID() });
			logger.error(
					"省中心主号签约接口!运行异常!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{},异常码:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID(), e.getCode() });
			logger.error("省中心主号签约接口!运行异常:", e);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			respVO.setRspCode(errCode);
			respVO.setRspInfo(RspCodeConstant.Crm.getDescByValue(errCode));
			respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
			msgVoRtn.setBody(respVO);

			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(errCode);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.getDescByValue(errCode));
			upayCsysTxnLogService.modify(txnLog);
			logger.debug("CrmMainMobileBindAction execute(Object)-end");
			return msgVoRtn;
		} catch (AppBizException e) {
			String errCode = e.getCode();
			errCode = CrmErrorCodeCache.getCrmErrCode(errCode);
			log.error(
					"省中心主号签约接口!系统异常!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID() });
			logger.error(
					"省中心主号签约接口!系统异常!发起方交易流水号为:{},发起方操作流水号为:{},平台流水号为:{},发起省机构为:{},手机号码为:{},银行机构为:{},用户类型为:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID(), reqVo.getIDType() });
			logger.error("省中心主号签约接口!系统异常:", e);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			respVO.setRspCode(errCode);
			respVO.setRspInfo(RspCodeConstant.Crm.getDescByValue(errCode));
			respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
			msgVoRtn.setBody(respVO);

			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(errCode);
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.getDescByValue(errCode));
			upayCsysTxnLogService.modify(txnLog);
			logger.debug("CrmMainMobileBindAction execute(Object)-end");
			return msgVoRtn;
		} catch (Exception e) {

			log.error(
					"省中心主号签约接口!未知异常!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID() });
			logger.error(
					"省中心主号签约接口!未知异常!CRM流水:{},CRM操作流水:{},UPAY流水:{},发起省:{},号码:{},银行:{}",
					new Object[] { paramData.getTransIDO(),
							reqVo.getTransactionID(), txnSeq,
							paramData.getMsgSender(), reqVo.getIDValue(),
							reqVo.getBankID() });
			logger.error("省中心主号签约接口!未知异常:", e);
			msgVoRtn.setRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			msgVoRtn.setRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			msgVoRtn.setRspType(CommonConstant.CrmRspType.BusErr.getValue());
			respVO.setRspCode(RspCodeConstant.Crm.CRM_5A06.getValue());

			String errDesc = e.getMessage().length() <= ExcConstant.MSG_LENGTH_230 ? e
					.getMessage() : e.getMessage().substring(0,
					ExcConstant.MSG_LENGTH_230);
			respVO.setRspInfo(RspCodeConstant.Crm.CRM_5A06.getDesc() + ":"
					+ errDesc);
			respVO.setSubTime(DateUtil.getDateyyyyMMddHHmmss());
			msgVoRtn.setBody(respVO);
			txnLog.setLastUpdTime(DateUtil.getDateyyyyMMddHHmmssSSS());
			txnLog.setChlRspCode(RspCodeConstant.Wzw.WZW_2998.getValue());
			txnLog.setChlRspDesc(RspCodeConstant.Wzw.WZW_2998.getDesc());
			txnLog.setChlRspType(CommonConstant.CrmRspType.BusErr.getValue());
			txnLog.setChlSubRspCode(RspCodeConstant.Crm.CRM_5A06.getValue());
			txnLog.setStatus(CommonConstant.TxnStatus.TxnFail.toString());
			txnLog.setChlSubRspDesc(RspCodeConstant.Crm.CRM_5A06.getDesc()
					+ ":" + e.getMessage());
			upayCsysTxnLogService.modify(txnLog);
			logger.debug("CrmMainMobileBindAction execute(Object)-end");
			return msgVoRtn;
		}
		logger.debug("CrmMainMobileBindAction execute(Object)-end");
		return msgVoRtn;
	}
}