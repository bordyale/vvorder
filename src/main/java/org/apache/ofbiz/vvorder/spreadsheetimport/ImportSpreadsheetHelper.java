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

package org.apache.ofbiz.vvorder.spreadsheetimport;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

public final class ImportSpreadsheetHelper {

	public static final String module = ImportSpreadsheetHelper.class.getName();

	private ImportSpreadsheetHelper() {
	}

	// prepare the product map
	public static Map<String, Object> prepareProduct(String productId) {
		Map<String, Object> fields = new HashMap<>();
		fields.put("productId", productId);
		/*fields.put("productTypeId", "FINISHED_GOOD");
		fields.put("internalName", "Product_" + productId);
		fields.put("isVirtual", "N");
		fields.put("isVariant", "N");*/
		return fields;
	}

	// check if product already exists in database
	public static boolean checkProductExists(String productId, Delegator delegator) {
		GenericValue tmpProductGV;
		boolean productExists = false;
		try {
			tmpProductGV = EntityQuery.use(delegator).from("VvProduct").where("productId", productId).queryOne();
			if (tmpProductGV != null && productId.equals(tmpProductGV.getString("productId"))) {
				productExists = true;
			}
		} catch (GenericEntityException e) {
			Debug.logError("Problem in reading data of product", module);
		}
		return productExists;
	}
}
