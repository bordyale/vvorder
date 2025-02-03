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

shipmentId = parameters.shipmentId

List searchCond = []
if (shipmentId) {
	searchCond.add(EntityCondition.makeCondition("shipmentId", EntityOperator.EQUALS, shipmentId))
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
shipItems = select("shipmentId","productId", "quantity").from("VvShipmentItem").where(searchCond).cache(false).queryList()

shipItemsTot = select("shipmentId","productId", "quantityShipped").from("VvShippingItemQtySumViewNoOrder").where(searchCond).cache(false).queryList()

Map<String,Object> itemsTotMap = new HashMap<String,Object>()
for (GenericValue entry: shipItemsTot){
	String productId = entry.get("productId")
	System.out.println(productId + " " + entry.get("quantityShipped") )
	itemsTotMap.put(productId, entry.get("quantityShipped"))
}
List<HashMap<String,Object>> shipItemsList= new ArrayList<HashMap<String,Object>>()
for (GenericValue entry: shipItems){
	Map<String,Object> e = new HashMap<String,Object>()
	String productId = entry.get("productId")
	BigDecimal quantityShipped = itemsTotMap.get(productId)
	e.put("productId", productId)
	e.put("quantityShipped", quantityShipped)
    itemsTotMap.remove(productId)
	if (quantityShipped) {
	shipItemsList.add(e)
	}

}




context.totalShippedWeight=totalShippedWeight
context.shipItemsList=shipItemsList
