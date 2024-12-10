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
package org.apache.ofbiz.vforder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.config.ProductConfigWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreSurveyWrapper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.RequestHandler;

/**
 * Vforder events.
 */
public class VvorderEvents {

	public static String module = VforderEvents.class.getName();

	public static final String resource = "OrderUiLabels";
	public static final String resource_error = "OrderErrorUiLabels";

	private static final String NO_ERROR = "noerror";
	private static final String NON_CRITICAL_ERROR = "noncritical";
	private static final String ERROR = "error";

	public static final MathContext generalRounding = new MathContext(10);

	

	public static String addShippingItemWithOpenOrder(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

		String controlDirective = null;
		Map<String, Object> result = null;

		// Get the parameters as a MAP, remove the productId and quantity
		// params.
		Map<String, Object> paramMap = UtilHttp.getParameterMap(request);

		String shipmentId = null;

		String productId = null;

		BigDecimal quantity = BigDecimal.ZERO;
		BigDecimal quantityShipped = BigDecimal.ZERO;
		BigDecimal quantityToShip = BigDecimal.ZERO;
		BigDecimal quantityShippable = BigDecimal.ZERO;

		controlDirective = null; // re-initialize each time

		// get the params
		if (paramMap.containsKey("shipmentId")) {
			shipmentId = (String) paramMap.remove("shipmentId");
		}
		if (paramMap.containsKey("productId")) {
			productId = (String) paramMap.remove("productId");
		}

		request.setAttribute("shipmentId", shipmentId);
		String quantityToShipStr = null;
		if (paramMap.containsKey("quantityToShip")) {
			quantityToShipStr = (String) paramMap.remove("quantityToShip");
		}
		if ((quantityToShipStr == null) || (quantityToShipStr.equals(""))) {
			quantityToShipStr = "0";
		}
		try {
			quantityToShip = new BigDecimal(quantityToShipStr);
		} catch (Exception e) {
			Debug.logWarning(e, "Problems parsing quantity string: " + quantityToShipStr, module);
			quantityToShip = BigDecimal.ZERO;
		}

		GenericValue vfOrdItemShipItem = null;
		GenericValue shipment = null;
		try {
			shipment = delegator.findOne("Shipment", UtilMisc.toMap("shipmentId", shipmentId), false);
		} catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Integer shipItemId = 1;

		/*try {

			// lookup payment applications which took place before the
			// asOfDateTime for this invoice
			EntityConditionList<EntityExpr> dateCondition = EntityCondition.makeCondition(
					UtilMisc.toList(EntityCondition.makeCondition("quantityShippable", EntityOperator.EQUALS, null),
							EntityCondition.makeCondition("quantityShippable", EntityOperator.GREATER_THAN, BigDecimal.ZERO)), EntityOperator.OR);

			EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(
					UtilMisc.toList(dateCondition, EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
							EntityCondition.makeCondition("orderHStatusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);

			List<GenericValue> vfOrdItemShipItems = delegator.findList("OrderItemShippingItemView", conditions, null, UtilMisc.toList("shipBeforeDate"), null, false);

			if (quantityToShip.equals(BigDecimal.ZERO)) {
				return "success";
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
						Map<String, Object> tmpResult = dispatcher.runSync("createShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
								productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "quantity", itemQty));

						Map<String, Object> tmpResult2 = dispatcher.runSync("createVfShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId,
								"shipmentItemSeqId", (String) tmpResult.get("shipmentItemSeqId"), "orderId", orderId, "orderItemSeqId", orderItemSeqId));

					} catch (GenericServiceException e) {
						Debug.logError(e, module);
					}

					qtyRemainToShip = qtyRemainToShip.subtract(itemQty);
				} else {

					try {
						Map<String, Object> tmpResult = dispatcher.runSync("createShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
								productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "quantity", qtyRemainToShip));

						Map<String, Object> tmpResult2 = dispatcher.runSync("createVfShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId,
								"shipmentItemSeqId", (String) tmpResult.get("shipmentItemSeqId"), "orderId", orderId, "orderItemSeqId", orderItemSeqId));

					} catch (GenericServiceException e) {
						Debug.logError(e, module);
					}

					qtyRemainToShip = BigDecimal.ZERO;
				}
				if (qtyRemainToShip.compareTo(BigDecimal.ZERO)==0)
					break;

			}
			if (qtyRemainToShip.compareTo(BigDecimal.ZERO) > 0) {
				try {
					Map<String, Object> tmpResult = dispatcher.runSync("createShipmentItem",
							UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId", productId, "quantity", qtyRemainToShip));

					Map<String, Object> tmpResult2 = dispatcher.runSync("createVfShipmentItem",
							UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "shipmentItemSeqId", (String) tmpResult.get("shipmentItemSeqId")));

				} catch (GenericServiceException e) {
					Debug.logError(e, module);
				}
			}

		} catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		return "success";

	}

}
