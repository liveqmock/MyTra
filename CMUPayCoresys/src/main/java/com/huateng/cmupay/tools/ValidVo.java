package com.huateng.cmupay.tools;

import java.util.Iterator;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * @author cmt
 * @version 创建时间：2013-8-8 下午11:13:15 类说明
 */
public class ValidVo {

	private ValidVo() {
	}

	private static ValidVo validVo = null;
	private static Validator validator = null;

	public static ValidVo getInstance() {
		if (validVo == null) {
			validVo = new ValidVo();
		}
		if (validator == null) {
			validator = Validation.buildDefaultValidatorFactory()
					.getValidator();
		}
		return validVo;

	}

	// 1.@AssertTrue //用于boolean字段，该字段只能为true
	// 2.@AssertFalse//该字段的值只能为false
	// 3.@CreditCardNumber//对信用卡号进行一个大致的验证
	// 4.@DecimalMax//只能小于或等于该值
	// 5.@DecimalMin//只能大于或等于该值
	// 6.@Digits(integer=2,fraction=20)//检查是否是一种数字的整数、分数,小数位数的数字。
	// 7.@Email//检查是否是一个有效的email地址
	// 8.@Future//检查该字段的日期是否是属于将来的日期
	// 9.@Length(min=,max=)//检查所属的字段的长度是否在min和max之间,只能用于字符串
	// 10.@Max//该字段的值只能小于或等于该值
	// 11.@Min//该字段的值只能大于或等于该值
	// 12.@NotNull//不能为null
	// 13.@NotBlank//不能为空，检查时会将空格忽略
	// 14.@NotEmpty//不能为空，这里的空是指空字符串
	// 15.@Null//检查该字段为空
	// 16.@Past//检查该字段的日期是在过去
	// 17.@Size(min=, max=)//检查该字段的size是否在min和max之间，可以是字符串、数组、集合、Map等
	// 18.@URL(protocol=,host,port)//检查是否是一个有效的URL，如果提供了protocol，host等，则该URL还需满足提供的条件
	// 19.@Valid//该注解只要用于字段为一个包含其他对象的集合或map或数组的字段，或该字段直接为一个其他对象的引用，
	// 20. //这样在检查当前对象的同时也会检查该字段所引用的对象

	public  String validateModel(Object obj) {// 验证某一个对象

		StringBuilder buffer = new StringBuilder(64);// 用于存储验证后的错误信息
		Set<ConstraintViolation<Object>> constraintViolations = validator
				.validate(obj);// 验证某个对象,，其实也可以只验证其中的某一个属性的

		Iterator<ConstraintViolation<Object>> iter = constraintViolations
				.iterator();
		while (iter.hasNext()) {
			String message = iter.next().getMessage();
			buffer.append(message);
		}
		return "".equals(buffer.toString()) ? null : buffer.toString();
	}

}
