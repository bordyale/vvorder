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

import java.math.RoundingMode
import java.sql.Timestamp

def sdf = new SimpleDateFormat("yyyy-MM-dd")

import org.apache.ofbiz.entity.model.DynamicViewEntity

DynamicViewEntity dynamicViewEntity = new DynamicViewEntity()

fromDate = parameters.fildate1From
thruDate = parameters.fildate2From
orderShipBeforeFrom = parameters.orderShipBeforeFrom
orderShipBeforeTo = parameters.orderShipBeforeTo
partyIdTo = parameters.filpartnerId
productId = parameters.filproductId
type = parameters.filtype
filshowFaseA = parameters.filshowFaseA

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
if (productId) {
	searchCond.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId))
}
if (type) {
	searchCond.add(EntityCondition.makeCondition("type", EntityOperator.EQUALS, type))
}

List<HashMap<String,Object>> hashMaps = new ArrayList<HashMap<String,Object>>()


if(filshowFaseA.equals("Y")){
orderItemShippingItem = select("shipmentDate","orderId","productId","name","quantity","partnerId").from("VvShippingItemView").where(searchCond).cache(false).queryList()

orderItemShippingItem = EntityUtil.orderBy(orderItemShippingItem,  ["shipmentDate"])

for (GenericValue entry: orderItemShippingItem){
	Map<String,Object> e = new HashMap<String,Object>()
	e.put("shipmentDate",entry.get("shipmentDate"))
	e.put("orderId",entry.get("orderId"))
	e.put("productId",entry.get("productId"))
	e.put("partnerId",entry.get("partnerId"))
	e.put("name",entry.get("name"))
	BigDecimal quantity = entry.get("quantity")
	e.put("quantity",entry.get("quantity"))


	hashMaps.add(e)
}
}
List filCond= []
List activCond = []
	activCond.add(EntityCondition.makeCondition("quantityShippable", EntityOperator.EQUALS, null))
	activCond.add(EntityCondition.makeCondition("quantityShippable", EntityOperator.GREATER_THAN, BigDecimal.ZERO))
if (partyIdTo) {
	filCond.add(EntityCondition.makeCondition("partnerId", EntityOperator.EQUALS, partyIdTo))
}
if (productId) {
	filCond.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId))
}

activCondOR = EntityCondition.makeCondition(activCond,EntityOperator.OR)
filCondAND = EntityCondition.makeCondition(filCond, EntityOperator.AND)
searchCond2 = EntityCondition.makeCondition([filCondAND, activCondOR], EntityOperator.AND)
notShippedItems = select("orderId","orderItemSeqId","quantity","quantityShipped","productId","name","shipBeforeDate","weight","orderName","orderDate","partnerId").from("VvOrderItemShippingItemView").where(searchCond2).cache(false).queryList()

notShippedItems = EntityUtil.orderBy(notShippedItems,  ["shipBeforeDate"])

List<HashMap<String,Object>> hashMaps2 = new ArrayList<HashMap<String,Object>>()
int i=0
BigDecimal progresNetWeigh = BigDecimal.ZERO
BigDecimal quantityTot = BigDecimal.ZERO
for (GenericValue entry: notShippedItems){
	i=notShippedItems.size()
	Map<String,Object> e = new HashMap<String,Object>()
	e.put("orderItemSeqId",entry.get("orderItemSeqId"))
	e.put("orderId",entry.get("orderId"))
	e.put("partnerId",entry.get("partnerId"))
	e.put("orderName",entry.get("orderName"))
	e.put("productId",entry.get("productId"))
	e.put("orderDate",entry.get("orderDate"))
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
    quantityTot = quantityTot.add(quantityShippable)
	BigDecimal productWeight = entry.get("weight")
	BigDecimal netWeight = quantityShippable.multiply(productWeight)
	e.put("netWeight",netWeight)
	progresNetWeigh = progresNetWeigh.add(netWeight)
	e.put("progresNetWeight",progresNetWeigh)
	hashMaps2.add(e)
}

List notShippedOrdersCond = []
List<HashMap<String,Object>> notShippedOrdersMap = new ArrayList<HashMap<String,Object>>()
if (notShippedItems) {
	orderIds = EntityUtil.getFieldListFromEntityList(notShippedItems, "orderId", true)
	notShippedOrdersCond.add(EntityCondition.makeCondition("orderId", EntityOperator.IN, orderIds))
	notShippedOrders = from("VvOrder").where(notShippedOrdersCond).orderBy("shipBeforeDate").queryList()
	for (GenericValue entry: notShippedOrders){
		
		Map<String,Object> e = new HashMap<String,Object>()
		e.put("orderId",entry.get("orderId"))
		e.put("partnerId",entry.get("partnerId"))
		e.put("orderName",entry.get("orderName"))
		e.put("orderWeight",entry.get("orderWeight"))	
		e.put("orderDate",entry.get("orderDate"))
		e.put("shipBeforeDate",entry.get("shipBeforeDate"))
		notShippedOrdersMap.add(e)
	}
	
}




List<HashMap<String,Object>> shipWeights = new ArrayList<HashMap<String,Object>>()
BigDecimal totalShippedWeight = BigDecimal.ZERO
if(filshowFaseA.equals("Y")){
	shippingWeight = select("shipmentDate","shipmentId","netWeight").from("VvShippingWeight").where(searchCond).cache(false).queryList()

	shippingWeight = EntityUtil.orderBy(shippingWeight,  ["shipmentDate"])

	for (GenericValue entry: shippingWeight){
		Map<String,Object> e = new HashMap<String,Object>()
		e.put("shipmentDate",entry.get("shipmentDate"))
		e.put("shipmentId",entry.get("shipmentId"))
		BigDecimal netWeight = entry.get("netWeight")
		e.put("netWeight",netWeight)

		totalShippedWeight= totalShippedWeight.add(netWeight)
		shipWeights.add(e)

	}
}


List<HashMap<String,Object>> prodQty = new ArrayList<HashMap<String,Object>>()
if(filshowFaseA.equals("Y")){
	productQuantity = select("productId","name","weight","quantity").from("VvShippingProductQuantity").where(searchCond).cache(false).queryList()

	productQuantity = EntityUtil.orderBy(productQuantity,  ["productId"])

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
}

BigDecimal openAverageWeight = BigDecimal.ZERO 

if (quantityTot.compareTo(BigDecimal.ZERO)!=0){
	openAverageWeight = progresNetWeigh.divide(quantityTot,2,RoundingMode.HALF_UP)
}

context.totalShippedWeight=totalShippedWeight
context.orderItems2 = hashMaps2
context.orderItems = hashMaps
context.notShippedOrders = notShippedOrdersMap
context.shipWeights = shipWeights
context.prodQty = prodQty
context.progresNetWeigh = progresNetWeigh 
context.quantityTot= quantityTot
context.openAverageWeight = openAverageWeight
