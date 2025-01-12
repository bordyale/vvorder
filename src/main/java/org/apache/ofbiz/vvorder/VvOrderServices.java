/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

package org.apache.ofbiz.vvorder;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Services for Agreement (Accounting)
 */

public class VvOrderServices {

	public static final String module = VvOrderServices.class.getName();

	public static final String resource = "VvorderUiLabels";

	public static Map<String, Object> createOrderItemJava(DispatchContext ctx, Map<String, Object> context) {
		Delegator delegator = ctx.getDelegator();
		LocalDispatcher dispatcher = ctx.getDispatcher();
		Locale locale = (Locale) context.get("locale");
		String errMsg = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String orderId = (String) context.get("orderId");
		String productId = (String) context.get("productId");
		BigDecimal quantity = (BigDecimal) context.get("quantity");
		try {
			GenericValue order = delegator.findOne("VvOrder", UtilMisc.toMap("orderId", orderId), false);
			GenericValue product = delegator.findOne("VvProduct", UtilMisc.toMap("productId", productId), false);
			BigDecimal productWeight = (BigDecimal) product.get("weight");
			BigDecimal orderItemWeight = productWeight.multiply(quantity);
			BigDecimal orderWeight = (BigDecimal) order.get("orderWeight");
			if (orderWeight == null) {
					orderWeight = BigDecimal.ZERO; 
			}
			orderWeight = orderWeight.add(orderItemWeight);
			order.put("orderWeight", orderWeight);
			Map<String, Object> tmpResult = dispatcher.runSync("createVvOrderItem", context);
			result.put("orderId", orderId);
			result.put("orderItemSeqId", tmpResult.get("orderItemSeqId"));
			dispatcher.runSync("updateVvOrder", UtilMisc.<String, Object> toMap("userLogin", userLogin,
					"orderId", orderId, "orderWeight", orderWeight));
		} catch (GenericServiceException e) {
			Debug.logWarning(e, module);
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}

		return result;
	}
	public static Map<String, Object> updateOrderItemJava(DispatchContext ctx, Map<String, Object> context) {
		Delegator delegator = ctx.getDelegator();
		LocalDispatcher dispatcher = ctx.getDispatcher();
		Locale locale = (Locale) context.get("locale");
		String errMsg = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String orderId = (String) context.get("orderId");
		String orderItemSeqId = (String) context.get("orderItemSeqId");
		BigDecimal modQuantity= (BigDecimal) context.get("quantity");
		try {
			GenericValue orderItem = delegator.findOne("VvOrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), false);
			String productId = (String) orderItem.get("productId");
			GenericValue order = delegator.findOne("VvOrder", UtilMisc.toMap("orderId", orderId), false);
			GenericValue product = delegator.findOne("VvProduct", UtilMisc.toMap("productId", productId), false);
			BigDecimal productWeight = (BigDecimal) product.get("weight");
			BigDecimal quantity = (BigDecimal) orderItem.get("quantity");
			BigDecimal orderItemWeightSaved = productWeight.multiply(quantity);
			BigDecimal orderItemWeight = productWeight.multiply(modQuantity);
			BigDecimal orderWeight = (BigDecimal) order.get("orderWeight");
			if (orderWeight == null) {
					orderWeight = BigDecimal.ZERO; 
			}
			orderWeight = orderWeight.subtract(orderItemWeightSaved);
			orderWeight = orderWeight.add(orderItemWeight);
			order.put("orderWeight", orderWeight);
			dispatcher.runSync("updateVvOrderItem", context);
			result.put("orderId", orderId);
			result.put("orderItemSeqId", orderItemSeqId);
			dispatcher.runSync("updateVvOrder", UtilMisc.<String, Object> toMap("userLogin", userLogin,
					"orderId", orderId, "orderWeight", orderWeight));
		} catch (GenericServiceException e) {
			Debug.logWarning(e, module);
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}

		return result;
	}
	public static Map<String, Object> deleteOrderItemJava(DispatchContext ctx, Map<String, Object> context) {
		Delegator delegator = ctx.getDelegator();
		LocalDispatcher dispatcher = ctx.getDispatcher();
		Locale locale = (Locale) context.get("locale");
		String errMsg = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String orderId = (String) context.get("orderId");
		String orderItemSeqId = (String) context.get("orderItemSeqId");
		try {
			GenericValue orderItem = delegator.findOne("VvOrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), false);
			String productId = (String) orderItem.get("productId");
			GenericValue order = delegator.findOne("VvOrder", UtilMisc.toMap("orderId", orderId), false);
			GenericValue product = delegator.findOne("VvProduct", UtilMisc.toMap("productId", productId), false);
			BigDecimal productWeight = (BigDecimal) product.get("weight");
			BigDecimal quantity = (BigDecimal) orderItem.get("quantity");
			BigDecimal orderItemWeightSaved = productWeight.multiply(quantity);
			BigDecimal orderWeight = (BigDecimal) order.get("orderWeight");
			if (orderWeight == null) {
					orderWeight = BigDecimal.ZERO; 
			}
			orderWeight = orderWeight.subtract(orderItemWeightSaved);
			order.put("orderWeight", orderWeight);
			dispatcher.runSync("deleteVvOrderItem", context);
			result.put("orderId", orderId);
			dispatcher.runSync("updateVvOrder", UtilMisc.<String, Object> toMap("userLogin", userLogin,
					"orderId", orderId, "orderWeight", orderWeight));
			
		} catch (GenericServiceException e) {
			Debug.logWarning(e, module);
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}

		return result;
	}

	public static Map<String, Object> createPromotionJava(DispatchContext ctx, Map<String, Object> context) {
		Delegator delegator = ctx.getDelegator();
		LocalDispatcher dispatcher = ctx.getDispatcher();
		Locale locale = (Locale) context.get("locale");
		String errMsg = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();

		GenericValue userLogin = (GenericValue) context.get("userLogin");

		try {
			String sellInEquOut = (String) context.get("sellInEquOut");
			if (sellInEquOut!=null && sellInEquOut.equals("Y")) {
				context.put("selloutFrom", context.get("sellinFrom"));
				context.put("selloutTo", context.get("sellinTo"));
			}
			String sellInAEquOutA = (String) context.get("sellInAEquOutA");
			if (sellInAEquOutA!=null && sellInAEquOutA.equals("Y")) {
				context.put("selloutTo", context.get("sellinTo"));
			}
			Map<String, Object> tmpResult = dispatcher.runSync("createPromotion", context);
			result.put("promotionId", tmpResult.get("promotionId"));

		} catch (GenericServiceException e) {
			Debug.logWarning(e, module);
			Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
			errMsg = UtilProperties.getMessage(resource, "RefRevenueZero", messageMap, locale);
			return ServiceUtil.returnError(errMsg);
		}
		return result;
	}
}
