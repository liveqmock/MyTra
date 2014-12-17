/**
 * 
 */
package com.huateng.cmupay.controller.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.huateng.cmupay.constant.CommonConstant;
import com.huateng.cmupay.controller.mapper.UpayCsysDictCodeMapper;
import com.huateng.cmupay.controller.mapper.UpayCsysDictInfoMapper;
import com.huateng.cmupay.models.UpayCsysDictCode;
import com.huateng.cmupay.models.UpayCsysDictInfo;

/**
 * 数据字典索引类
 * 
 * @author cmt
 * 
 */

@Component
public class DictCodeCache {

	protected static final  Logger logger = LoggerFactory.getLogger(RouteCache.class);
	private final static Map<String, Map<String ,UpayCsysDictCode>>  DICTCODE_MAP_MAP= new HashMap<String, Map<String ,UpayCsysDictCode>>();
	
	private final static Map<String, List<UpayCsysDictCode>>  DICTCODE_LIST_MAP= new HashMap<String, List<UpayCsysDictCode>>();

	static final private List<UpayCsysDictCode> DICTCODE_LIST = new ArrayList<UpayCsysDictCode>();

	@Autowired
	private UpayCsysDictCodeMapper epayCsysDictCodeMapper;

	@Autowired
	private UpayCsysDictInfoMapper epayCsysDictInfoMapper;

	/**
	 * 初始化数据字典
	 * 
	 * @author cmt
	 */
	@PostConstruct
	private void init() {

		DICTCODE_LIST_MAP.clear();
		
		Map<String, Object> paramInfo = new HashMap<String, Object>();
		paramInfo.put("dictStatus", CommonConstant.IsActive.True.toString());

		List<UpayCsysDictInfo> infoList = epayCsysDictInfoMapper
				.selectAllListByParams(paramInfo, null);

		for (UpayCsysDictInfo info : infoList) {
			Map<String, Object> paramCode = new HashMap<String, Object>();
			paramCode
					.put("dictStatus", CommonConstant.IsActive.True.toString());
			paramCode.put("dictId", info.getDictId());
			List<UpayCsysDictCode> codeList = epayCsysDictCodeMapper
					.selectAllListByParams(paramCode, null);
			
			
			Map<String ,UpayCsysDictCode> codeMap= new HashMap<String ,UpayCsysDictCode>();
			
			for(UpayCsysDictCode  sysdictCode :codeList){
				codeMap.put(sysdictCode.getDictId(), sysdictCode);
			}
			List<UpayCsysDictCode> pListDict = DICTCODE_LIST_MAP.get(info.getDictId());
			if (pListDict == null) {
				DICTCODE_LIST_MAP.put(info.getDictId(), codeList);
			
			}
			Map<String ,UpayCsysDictCode> pMapDict = DICTCODE_MAP_MAP.get(info.getDictId());
			if (pMapDict == null) {
				DICTCODE_MAP_MAP.put(info.getDictId(), codeMap);
			
			}
		}

	}

	/**
	 * 重载数据字典
	 * 
	 * @author cmt
	 */
	public void reLoad() {
		init();
	}

	/**
	 * @return
	 */
	public static List<UpayCsysDictCode> getDictCodeList(String dictId) {
		if (dictId == null || dictId.trim().equals(""))
			return null;
		return DICTCODE_LIST_MAP.get(dictId);
	}
	
	/**
	 * @return
	 */
	public static Map<String ,UpayCsysDictCode> getDictCodeMap(String dictId) {
		if (dictId == null || dictId.trim().equals(""))
			return null;
		return DICTCODE_MAP_MAP.get(dictId);
	}

	/**
	 * @return
	 */
	public static UpayCsysDictCode getDictCode(String dictId, String codeId) {
		if (dictId == null || dictId.trim().equals(""))
			return null;
		if (codeId == null || codeId.trim().equals(""))
			return null;

		List<UpayCsysDictCode> dictCodeList = DICTCODE_LIST_MAP.get(dictId);

		for (UpayCsysDictCode dictCode : dictCodeList) {
			if (codeId.equals(dictCode.getCodeId().toString())) {
				return dictCode;
			}
		}

		return null;
	}

	/**
	 * @return
	 */
	public static UpayCsysDictCode getBankTOrg(String codeId) {
		if (codeId == null || codeId.trim().equals(""))
			return null;
		List<UpayCsysDictCode> dictCodeList = DICTCODE_LIST_MAP.get(codeId);
		for (UpayCsysDictCode dictCode : dictCodeList) {
			if (codeId.equals(dictCode.getCodeValue1().toString())) {
				return dictCode;
			}
		}

		return null;
	}
	/**
	 * @return
	 */
	public static List<UpayCsysDictCode> getAllDictCodeList() {
		return DICTCODE_LIST;
	}
	
	/**
	 * @return
	 */
	public static Map<String, List<UpayCsysDictCode>> getAllDictCodeMap() {
		return DICTCODE_LIST_MAP;
	}
	
	
	

//	@SuppressWarnings("unchecked")
//	public static UpayCsysDictCode getDictCode(String dictId, int codeId) {
//		if (dictId == null || dictId.trim().equals(""))
//			return null;
//		if ("".equals(codeId) )
//			return null;
//		UpayCsysDictCode dictCode;
//		try {
//			dictCode = BeanUtil.toBean(UpayCsysDictCode.class,
//					UpayMemCache.getDictCodea(dictId, codeId));
//		} catch (Exception e) {
//			logger.error("内存数据库调用异常。",e);
//			return null;
//		}
//
//		return dictCode;
//	}
//	
//	
//	
//
//	@SuppressWarnings("unchecked")
//	public static UpayCsysDictInfo getDictInfo(String dictId) {
//		if (dictId == null || dictId.trim().equals(""))
//			return null;
//		UpayCsysDictInfo dictInfo;
//		try {
//			dictInfo = BeanUtil.toBean(UpayCsysDictInfo.class,
//					UpayMemCache.getDictInfo(dictId));
//		} catch (Exception e) {
//			logger.error("内存数据库调用异常。",e);
//			return null;
//		}
//
//		return dictInfo;
//	}
	
	
	
	
	

}
