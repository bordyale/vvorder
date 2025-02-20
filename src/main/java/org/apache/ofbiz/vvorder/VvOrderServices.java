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
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;

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
	public static Map<String, Object> addShippingItemWithOpenOrder(DispatchContext ctx, Map<String, Object> context) {
		Delegator delegator = ctx.getDelegator();
		LocalDispatcher dispatcher = ctx.getDispatcher();
		Locale locale = (Locale) context.get("locale");
		String errMsg = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String shipmentId = (String) context.get("shipmentId");
		String productId = (String) context.get("productId");
		BigDecimal quantityToShip = (BigDecimal) context.get("quantityToShip");

		result.put("shipmentId", shipmentId);
		BigDecimal quantity = BigDecimal.ZERO;
		BigDecimal quantityShipped = BigDecimal.ZERO;
		BigDecimal quantityShippable = BigDecimal.ZERO;

		GenericValue vfOrdItemShipItem = null;
		GenericValue shipment = null;
		try {
			shipment = delegator.findOne("VvShipment", UtilMisc.toMap("shipmentId", shipmentId), false);
		} catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Integer shipItemId = 1;

		try {

			// lookup payment applications which took place before the
			// asOfDateTime for this invoice
			String partnerId = (String) shipment.get("partnerId");
			
			
			List<EntityExpr> othExpr = UtilMisc.toList(EntityCondition.makeCondition("quantityShippable", EntityOperator.EQUALS, null));
			    othExpr.add(EntityCondition.makeCondition("quantityShippable", EntityOperator.GREATER_THAN, BigDecimal.ZERO));
			    EntityCondition con1 = EntityCondition.makeCondition(othExpr, EntityJoinOperator.OR);
			    EntityCondition prodExpr = EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId);
			    EntityCondition con2 = EntityCondition.makeCondition(UtilMisc.toList(con1, prodExpr), EntityOperator.AND);
			    EntityCondition partExpr = EntityCondition.makeCondition("partnerId", EntityOperator.EQUALS, partnerId);
			    EntityCondition con3 = EntityCondition.makeCondition(UtilMisc.toList(con2, partExpr), EntityOperator.AND);


			List<GenericValue> vfOrdItemShipItems = delegator.findList("VvOrderItemShippingItemView", con3, null, UtilMisc.toList("shipBeforeDate"), null, false);

			
			
			
			
			

			if (quantityToShip.equals(BigDecimal.ZERO)) {
				return result; 
			}
			BigDecimal qtyRemainToShip = quantityToShip;
			for (GenericValue item : vfOrdItemShipItems) {
				BigDecimal itemQty = (BigDecimal) item.get("quantityShippable");
				String orderId = (String) item.get("orderId");
				String orderItemSeqId = (String) item.get("orderItemSeqId");
				;
				if (itemQty == null) {
					itemQty = (BigDecimal) item.get("quantity");
				}
				if (qtyRemainToShip.compareTo(itemQty) >= 0) {

					try {
						Map<String, Object> tmpResult = dispatcher.runSync("createVvShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
								productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "quantity", itemQty));

					} catch (GenericServiceException e) {
						Debug.logError(e, module);
					}

					qtyRemainToShip = qtyRemainToShip.subtract(itemQty);
				} else {

					try {
						Map<String, Object> tmpResult = dispatcher.runSync("createVvShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
								productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "quantity", qtyRemainToShip));

					} catch (GenericServiceException e) {
						Debug.logError(e, module);
					}

					qtyRemainToShip = BigDecimal.ZERO;
				}
				if (qtyRemainToShip.compareTo(BigDecimal.ZERO) == 0)
					break;

			}
			if (qtyRemainToShip.compareTo(BigDecimal.ZERO) > 0) {
				try {
					Map<String, Object> tmpResult = dispatcher.runSync("createVvShipmentItem",
							UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId", productId, "quantity", qtyRemainToShip));

				} catch (GenericServiceException e) {
					Debug.logError(e, module);
				}
			}

		} catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
