/*
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
 */

import org.apache.ofbiz.entity.condition.EntityExpr
import org.apache.ofbiz.entity.condition.EntityFunction
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.condition.EntityFieldValue
import org.apache.ofbiz.entity.condition.EntityConditionList
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.base.util.UtilDateTime
import java.text.SimpleDateFormat
import org.apache.ofbiz.order.order.OrderReadHelper

import java.sql.Timestamp

def sdf = new SimpleDateFormat("yyyy-MM-dd")

import org.apache.ofbiz.entity.model.DynamicViewEntity

DynamicViewEntity dynamicViewEntity = new DynamicViewEntity()

fromDate = parameters.fildate1From
thruDate = parameters.fildate1From
orderShipBeforeFrom = parameters.orderShipBeforeFrom
orderShipBeforeTo = parameters.orderShipBeforeTo
partyIdTo = parameters.partnerId
showNoOrderShipItems = parameters.showNoOrderShipItems

List searchCond = []
if (fromDate) {
	def parseDate = sdf.parse(fromDate)
	searchCond.add(EntityCondition.makeCondition("shipmentDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.toTimestamp(parseDate)))
}
if (thruDate) {
	def parseDate = sdf.parse(thruDate)
	searchCond.add(EntityCondition.makeCondition("shipmentDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.toTimestamp(parseDate)))
}
if (partyIdTo) {
	searchCond.add(EntityCondition.makeCondition("partnerId", EntityOperator.EQUALS, partyIdTo))
}

List<HashMap<String,Object>> hashMaps = new ArrayList<HashMap<String,Object>>()
if ("Y".equals(showNoOrderShipItems)){

	orderItemShippingItem = select("shipmentDate","orderId","productId","name","quantity").from("VvShippingItemView").where(searchCond).cache(false).queryList()

	orderItemShippingItem = EntityUtil.orderBy(orderItemShippingItem,  ["shipmentDate"])

	for (GenericValue entry: orderItemShippingItem){
		Map<String,Object> e = new HashMap<String,Object>()
		e.put("shipmentDate",entry.get("shipmentDate"))
		e.put("orderId",entry.get("orderId"))
		e.put("productId",entry.get("productId"))
		e.put("name",entry.get("name"))
		BigDecimal quantity = entry.get("quantity")
		e.put("quantity",entry.get("quantity"))


		hashMaps.add(e)
	}
}else{

	orderItemShippingItem = select("shipmentDate","orderId","productId","name","quantity","quantityShipped","quantityShippable").from("VvShipmentsReport").where(searchCond).cache(false).queryList()

	orderItemShippingItem = EntityUtil.orderBy(orderItemShippingItem,  ["shipmentDate"])
	orderItemShippingItem = EntityUtil.orderBy(orderItemShippingItem,  ["productId"])


	for (GenericValue entry: orderItemShippingItem){
		Map<String,Object> e = new HashMap<String,Object>()
		e.put("shipmentDate",entry.get("shipmentDate"))
		e.put("orderId",entry.get("orderId"))
		e.put("productId",entry.get("productId"))
		e.put("name",entry.get("name"))
		BigDecimal quantity = entry.get("quantity")
		e.put("quantity",entry.get("quantity"))
		e.put("quantityShipped",entry.get("quantityShipped"))
		e.put("quantityShippable",entry.get("quantityShippable"))
		hashMaps.add(e)
	}
}
List searchCond2 = []
if (orderShipBeforeFrom) {
	searchCond2.add(EntityCondition.makeCondition("shipBeforeDate", EntityOperator.GREATER_THAN_EQUAL_TO, Timestamp.valueOf(orderShipBeforeFrom)))
}
if (orderShipBeforeTo) {
	searchCond2.add(EntityCondition.makeCondition("shipBeforeDate", EntityOperator.LESS_THAN_EQUAL_TO, Timestamp.valueOf(orderShipBeforeTo)))
}
/*if (partyIdTo) {
 searchCond2.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyIdTo))
 }*/
//searchCond2.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "BILL_FROM_VENDOR"))

notShippedItems = select("orderId","orderItemSeqId","quantity","quantityShipped","productId","name","shipBeforeDate","weight","orderName").from("VvOrderItemShippingItemView").where(searchCond2).cache(false).queryList()

notShippedItems = EntityUtil.orderBy(notShippedItems,  ["shipBeforeDate"])

List<HashMap<String,Object>> hashMaps2 = new ArrayList<HashMap<String,Object>>()
int i=0
BigDecimal progresNetWeigh = BigDecimal.ZERO
for (GenericValue entry: notShippedItems){
	i=notShippedItems.size()
	Map<String,Object> e = new HashMap<String,Object>()
	e.put("orderItemSeqId",entry.get("orderItemSeqId"))
	e.put("orderId",entry.get("orderId"))
	e.put("orderName",entry.get("orderName"))
	e.put("productId",entry.get("productId"))
	e.put("shipBeforeDate",entry.get("shipBeforeDate"))
	e.put("name",entry.get("name"))
	BigDecimal quantity = entry.get("quantity")
	e.put("quantity",entry.get("quantity"))
	e.put("weight",entry.get("weight"))
	BigDecimal quantityShipped = entry.get("quantityShipped")
	if (quantityShipped ==null){
		quantityShipped = BigDecimal.ZERO
	}
	BigDecimal quantityShippable =quantity.subtract(quantityShipped)
	e.put("quantityShipped",quantityShipped)
	e.put("quantityShippable",quantityShippable)

	BigDecimal productWeight = entry.get("weight")
	BigDecimal netWeight = quantityShippable.multiply(productWeight)
	e.put("netWeight",netWeight)


	status = entry.get("orderHStatusId")
	if (!quantity.equals(quantityShipped)){
		//extract order party
		orderId = entry.get("orderId")
		if (orderId) {
			orderHeader = from("VvOrder").where("orderId", orderId).queryOne()

			partyId = orderHeader.partnerId
			if (partyIdTo){
				if (partyIdTo.equals(partyId)){
					progresNetWeigh = progresNetWeigh.add(netWeight)
					e.put("progresNetWeight",progresNetWeigh)
					hashMaps2.add(e)
				}

			}else{
				progresNetWeigh = progresNetWeigh.add(netWeight)
				e.put("progresNetWeight",progresNetWeigh)
				hashMaps2.add(e)
			}

		}






	}
}


dynamicViewEntity.addMemberEntity("SIV", "VvShippingItemView");
//dynamicViewEntity.addAliasAll("SIV", null, null)
dynamicViewEntity.addAlias("SIV", "productId", null, null, null,true, null);
dynamicViewEntity.addAlias("SIV", "name", null, null, null,true, null);
dynamicViewEntity.addAlias("SIV", "orderId", null, null, null,null, null);
dynamicViewEntity.addAlias("SIV", "partyIdTo", null, null, null,null, null);
dynamicViewEntity.addAlias("SIV", "quantity", "quantity", null, null, null, "sum");
extraShippedProducts= null
if (partyIdTo) {
	extraShippedProducts = select("productId","name","quantity").from(dynamicViewEntity).where("orderId", null,"partnerId", partyIdTo).cache(false).queryList()
}else{
	extraShippedProducts = select("productId","name","quantity").from(dynamicViewEntity).where("orderId", null).cache(false).queryList()
}



extraShippedProducts = EntityUtil.orderBy(extraShippedProducts,  ["productId"])

List<HashMap<String,Object>> exShippedPr = new ArrayList<HashMap<String,Object>>()
for (GenericValue entry: extraShippedProducts){
	Map<String,Object> e = new HashMap<String,Object>()
	e.put("productId",entry.get("productId"))
	e.put("name",entry.get("name"))
	BigDecimal qty = entry.get("quantity")
	e.put("quantity",qty)
	if (qty.compareTo(BigDecimal.ZERO)!=0){
		exShippedPr.add(e)
	}
}


shippingWeight = select("shipmentDate","shipmentId","netWeight").from("VvShippingWeight").where(searchCond).cache(false).queryList()

shippingWeight = EntityUtil.orderBy(shippingWeight,  ["shipmentDate"])

List<HashMap<String,Object>> shipWeights = new ArrayList<HashMap<String,Object>>()
BigDecimal totalShippedWeight = BigDecimal.ZERO
for (GenericValue entry: shippingWeight){
	Map<String,Object> e = new HashMap<String,Object>()
	e.put("shipmentDate",entry.get("shipmentDate"))
	e.put("shipmentId",entry.get("shipmentId"))
	BigDecimal netWeight = entry.get("netWeight")
	e.put("netWeight",netWeight)

	totalShippedWeight= totalShippedWeight.add(netWeight)
	shipWeights.add(e)

}


productQuantity = select("productId","name","weight","quantity").from("VvShippingProductQuantity").where(searchCond).cache(false).queryList()

productQuantity = EntityUtil.orderBy(productQuantity,  ["productId"])

List<HashMap<String,Object>> prodQty = new ArrayList<HashMap<String,Object>>()
BigDecimal progresQuantity = BigDecimal.ZERO
for (GenericValue entry: productQuantity){
	Map<String,Object> e = new HashMap<String,Object>()
	e.put("productId",entry.get("productId"))
	e.put("name",entry.get("name"))
	e.put("weight",entry.get("weight"))
	//e.put("handlingInstructions",entry.get("handlingInstructions"))
	BigDecimal qty = entry.get("quantity")
	e.put("quantity",qty)

	prodQty.add(e)
}



context.totalShippedWeight=totalShippedWeight
context.orderItems2 = hashMaps2
context.orderItems = hashMaps
context.shipWeights = shipWeights
context.exShippedPr = exShippedPr
context.prodQty = prodQty
