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

import java.io.IOException;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.ofbiz.vvorder.spreadsheetimport.ImportSpreadsheetHelper;

import java.io.RandomAccessFile;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
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
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.pricat.AbstractPricatParser;
import org.apache.ofbiz.pricat.InterfacePricatParser;
import org.apache.ofbiz.pricat.PricatParseExcelHtmlThread;
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
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Vforder events.
 */
public class VvorderEvents {

	public static String module = VvorderEvents.class.getName();

	public static final String resource = "OrderUiLabels";
	public static final String resource_error = "OrderErrorUiLabels";

	private static final String NO_ERROR = "noerror";
	private static final String NON_CRITICAL_ERROR = "noncritical";
	private static final String ERROR = "error";

	public static final MathContext generalRounding = new MathContext(10);

	/*
	 * public static String addShippingItemWithOpenOrder(HttpServletRequest
	 * request, HttpServletResponse response) { HttpSession session =
	 * request.getSession(); GenericValue userLogin = (GenericValue)
	 * session.getAttribute("userLogin"); Delegator delegator = (Delegator)
	 * request.getAttribute("delegator"); LocalDispatcher dispatcher =
	 * (LocalDispatcher) request.getAttribute("dispatcher");
	 * 
	 * String controlDirective = null; Map<String, Object> result = null;
	 * 
	 * // Get the parameters as a MAP, remove the productId and quantity //
	 * params. Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
	 * 
	 * String shipmentId = null;
	 * 
	 * String productId = null;
	 * 
	 * BigDecimal quantity = BigDecimal.ZERO; BigDecimal quantityShipped =
	 * BigDecimal.ZERO; BigDecimal quantityToShip = BigDecimal.ZERO; BigDecimal
	 * quantityShippable = BigDecimal.ZERO;
	 * 
	 * controlDirective = null; // re-initialize each time
	 * 
	 * // get the params if (paramMap.containsKey("shipmentId")) { shipmentId =
	 * (String) paramMap.remove("shipmentId"); } if
	 * (paramMap.containsKey("productId")) { productId = (String)
	 * paramMap.remove("productId"); }
	 * 
	 * request.setAttribute("shipmentId", shipmentId); String quantityToShipStr
	 * = null; if (paramMap.containsKey("quantityToShip")) { quantityToShipStr =
	 * (String) paramMap.remove("quantityToShip"); } if ((quantityToShipStr ==
	 * null) || (quantityToShipStr.equals(""))) { quantityToShipStr = "0"; } try
	 * { quantityToShip = new BigDecimal(quantityToShipStr); } catch (Exception
	 * e) { Debug.logWarning(e, "Problems parsing quantity string: " +
	 * quantityToShipStr, module); quantityToShip = BigDecimal.ZERO; }
	 * 
	 * GenericValue vfOrdItemShipItem = null; GenericValue shipment = null; try
	 * { shipment = delegator.findOne("VvShipment", UtilMisc.toMap("shipmentId",
	 * shipmentId), false); } catch (GenericEntityException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } Integer shipItemId = 1;
	 * 
	 * try {
	 * 
	 * String partnerId = (String) shipment.get("partnerId"); // lookup payment
	 * applications which took place before the // asOfDateTime for this invoice
	 * 
	 * List<EntityExpr> othExpr =
	 * UtilMisc.toList(EntityCondition.makeCondition("quantityShippable",
	 * EntityOperator.EQUALS, null));
	 * othExpr.add(EntityCondition.makeCondition("quantityShippable",
	 * EntityOperator.GREATER_THAN, BigDecimal.ZERO)); EntityCondition con1 =
	 * EntityCondition.makeCondition(othExpr, EntityJoinOperator.OR);
	 * EntityCondition prodExpr = EntityCondition.makeCondition("productId",
	 * EntityOperator.EQUALS, productId); EntityCondition con2 =
	 * EntityCondition.makeCondition(UtilMisc.toList(con1, prodExpr),
	 * EntityOperator.AND); EntityCondition partExpr =
	 * EntityCondition.makeCondition("partnerId", EntityOperator.EQUALS,
	 * partnerId); EntityCondition con3 =
	 * EntityCondition.makeCondition(UtilMisc.toList(con2, partExpr),
	 * EntityOperator.AND);
	 * 
	 * 
	 * List<GenericValue> vfOrdItemShipItems =
	 * delegator.findList("VvOrderItemShippingItemView", con3, null,
	 * UtilMisc.toList("shipBeforeDate"), null, false);
	 * 
	 * if (quantityToShip.equals(BigDecimal.ZERO)) { return "success"; }
	 * BigDecimal qtyRemainToShip = quantityToShip; for (GenericValue item :
	 * vfOrdItemShipItems) { BigDecimal itemQty = (BigDecimal)
	 * item.get("quantityShippable"); String orderId = (String)
	 * item.get("orderId"); String orderItemSeqId = (String)
	 * item.get("orderItemSeqId"); ; if (itemQty == null) { itemQty =
	 * (BigDecimal) item.get("quantity"); } if
	 * (qtyRemainToShip.compareTo(itemQty) >= 0) {
	 * 
	 * try { Map<String, Object> tmpResult =
	 * dispatcher.runSync("createVvShipmentItem", UtilMisc.<String, Object>
	 * toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
	 * productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId,
	 * "quantity", itemQty));
	 * 
	 * } catch (GenericServiceException e) { Debug.logError(e, module); }
	 * 
	 * qtyRemainToShip = qtyRemainToShip.subtract(itemQty); } else {
	 * 
	 * try { Map<String, Object> tmpResult =
	 * dispatcher.runSync("createVvShipmentItem", UtilMisc.<String, Object>
	 * toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
	 * productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId,
	 * "quantity", qtyRemainToShip));
	 * 
	 * } catch (GenericServiceException e) { Debug.logError(e, module); }
	 * 
	 * qtyRemainToShip = BigDecimal.ZERO; } if
	 * (qtyRemainToShip.compareTo(BigDecimal.ZERO) == 0) break;
	 * 
	 * } if (qtyRemainToShip.compareTo(BigDecimal.ZERO) > 0) { try { Map<String,
	 * Object> tmpResult = dispatcher.runSync("createVvShipmentItem",
	 * UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId",
	 * shipmentId, "productId", productId, "quantity", qtyRemainToShip));
	 * 
	 * } catch (GenericServiceException e) { Debug.logError(e, module); } }
	 * 
	 * } catch (GenericEntityException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); }
	 * 
	 * return "success";
	 * 
	 * }
	 */

	public static String updateInventoryShipment(HttpServletRequest request,
			HttpServletResponse response) {

		Delegator delegator = (Delegator) request.getAttribute("delegator");
		LocalDispatcher dispatcher = (LocalDispatcher) request
				.getAttribute("dispatcher");
		GenericValue userLogin = (GenericValue) request.getSession()
				.getAttribute("userLogin");
		String shipmentId = null;
		String facilityId = null;
		String isReturnShipment = null;

		// Get the parameters as a MAP, remove the productId and quantity
		// params.
		Map<String, Object> paramMap = UtilHttp.getParameterMap(request);

		if (paramMap.containsKey("shipmentId")) {
			shipmentId = (String) paramMap.remove("shipmentId");
		}
		// if isReturnShipment == Y we restore the items in the facility.
		if (paramMap.containsKey("isReturnShipment")) {
			isReturnShipment = (String) paramMap.remove("isReturnShipment");
		}

		GenericValue shipment = null;
		try {
			shipment = delegator.findOne("VvShipment",
					UtilMisc.toMap("shipmentId", shipmentId), false);

			if ("Y".equals(isReturnShipment)) {
				List<GenericValue> invDetails = delegator.findByAnd(
						"VvWarehouse",
						UtilMisc.toMap("shipmentId", shipmentId), null, false);
				if (invDetails != null) {
					List<EntityCondition> exprs = new LinkedList<EntityCondition>();
					for (GenericValue det : invDetails) {
						exprs.add(EntityCondition.makeCondition("whId",
								EntityOperator.EQUALS, det.get("whId")));
					}
					// delegator.removeByAnd("VvWarehouse",
					// UtilMisc.toMap("shipmentId", shipmentId));
					EntityConditionList<EntityCondition> ecl = EntityCondition
							.makeCondition(exprs, EntityOperator.OR);
					List<GenericValue> inv = EntityQuery.use(delegator)
							.from("VvWarehouse").where(ecl).queryList();
					delegator.removeAll(inv);
				}

				shipment.put("statusId", "VV_SHIP_STATUS_1");
			} else {
				List<GenericValue> shipmentItems = null;
				try {
					shipmentItems = delegator.findByAnd("VvShipmentItem",
							UtilMisc.toMap("shipmentId", shipmentId), null,
							false);

				} catch (GenericEntityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (shipmentItems != null) {
					for (GenericValue si : shipmentItems) {

						try {
							BigDecimal qty = (BigDecimal) si.get("quantity");
							Map serviceCtx = UtilMisc.toMap("productId",
									si.get("productId"), "shipmentId",
									shipmentId, "userLogin", userLogin,
									"quantity", qty.negate(), "statusId",
									"VV_WH_CHANGE_2", "date",
									shipment.get("shipmentDate"));

							dispatcher.runSync("createVvWh", serviceCtx);

						} catch (GenericServiceException e) {
							Debug.logError(e, module);
							return "error";
						}

					}

				}
				shipment.put("statusId", "VV_SHIP_STATUS_2");

			}
			delegator.store(shipment);
		} catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "success";

	}

	public static String uploadVvOrderItem(HttpServletRequest request,
			HttpServletResponse response) {
		Locale locale = UtilHttp.getLocale(request);
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		HttpSession session = request.getSession();
		GenericValue userLogin = (GenericValue) session
				.getAttribute("userLogin");
		LocalDispatcher dispatcher = (LocalDispatcher) request
				.getAttribute("dispatcher");
		String prefix = System.getProperty("user.dir");
		Map<String, Object> results = new HashMap<String, Object>();
		ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory(
				10240, new File(new File("runtime"), "tmp")));
		List<FileItem> lst = null;

		Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
		String orderId = null;
		if (paramMap.containsKey("orderId")) {
			orderId = (String) paramMap.get("orderId");
		}
		// request.setAttribute("orderId", orderId);
		String rowsNumberStr = null;
		BigDecimal rowsNumber = null;
		if (paramMap.containsKey("rows")) {
			rowsNumberStr = (String) paramMap.remove("rows");
		}
		try {
			rowsNumber = new BigDecimal(rowsNumberStr);
		} catch (Exception e) {
		}
		/*
		 * try { lst = UtilGenerics.checkList(fu.parseRequest(request)); } catch
		 * (FileUploadException e4) { }
		 * 
		 * if (lst.size() == 0 &&
		 * UtilValidate.isNotEmpty(request.getAttribute("fileItems"))) { lst =
		 * UtilGenerics.cast(request.getAttribute("fileItems")); } if
		 * (lst.size() == 0) { return "error"; }
		 * 
		 * // This code finds the idField and the upload FileItems //
		 * request.removeAttribute("fileItems");
		 * 
		 * FileItem fi = null; FileItem imageFi = null; for (int i = 0; i <
		 * lst.size(); i++) { fi = lst.get(i); String fieldName =
		 * fi.getFieldName(); String fieldStr = fi.getString(); if
		 * (fieldName.equals("uploadFile")) { imageFi = fi; // MimeType of
		 * upload file results.put("uploadMimeType", fi.getContentType()); } }
		 * 
		 * byte[] imageBytes = imageFi.get(); ByteBuffer byteWrap =
		 * ByteBuffer.wrap(imageBytes); results.put("imageData", byteWrap);
		 * results.put("imageFileName", imageFi.getName());
		 * 
		 * String imageName = results.get("imageFileName").toString(); String
		 * mimType = results.get("uploadMimeType").toString(); ByteBuffer
		 * imageData = (ByteBuffer) results.get("imageData"); try { String
		 * dirPath = "/plugins/vvorder/orderitemupload/"; String completeDirPath
		 * = prefix + dirPath; System.out.println("dirPath: " +
		 * completeDirPath); File dir = new File(completeDirPath); if
		 * (!dir.exists()) { boolean createDir = dir.mkdir(); if (!createDir) {
		 * request.setAttribute("_ERROR_MESSAGE_", completeDirPath); return
		 * "error"; } } String imagePath = "/plugins/vvorder/orderitemupload/" +
		 * imageName; System.out.println("imagePath: " + prefix + imagePath);
		 * File file = new File(prefix + "/" + imagePath); if (file.exists()) {
		 * request.setAttribute("_ERROR_MESSAGE_",
		 * "There is an existing frame, please select from the existing frame."
		 * ); return "error"; } Path tmpFile = Files.createTempFile(null, null);
		 * Files.write(tmpFile, imageData.array(), StandardOpenOption.APPEND);
		 * Files.delete(tmpFile); RandomAccessFile out = new
		 * RandomAccessFile(file, "rw"); out.write(imageData.array());
		 * out.close();
		 */
		try {
			File file = ImportSpreadsheetHelper.uploadFile(request,
					"/plugins/vvorder/orderitemupload/");
			//
			// // read all xls file and create workbook one by one.
			List<Map<String, Object>> dbrows = new LinkedList<>();
			POIFSFileSystem fs = null;
			HSSFWorkbook wb = null;
			try {
				fs = new POIFSFileSystem(new FileInputStream(file));
				wb = new HSSFWorkbook(fs);
			} catch (IOException e) {
				Debug.logError("Unable to read or create workbook from file",
						module);
			}

			// get first sheet
			HSSFSheet sheet = wb.getSheetAt(0);
			wb.close();
			String tableName = "VvOrderItem";
			int sheetLastRowNumber = 0;
			if (rowsNumber == null) {
				sheetLastRowNumber = sheet.getLastRowNum();
			} else {
				sheetLastRowNumber = rowsNumber.intValue();
			}
			for (int j = 1; j <= sheetLastRowNumber; j++) {
				HSSFRow row = sheet.getRow(j);
				if (row != null) {

					Map<String, Object> fields = new HashMap<>();
					fields.put("orderItemSeqId",
							delegator.getNextSeqId(tableName));
					fields.put("orderId", orderId);
					
					String cell0 = ImportSpreadsheetHelper.importString(0, row);
					if (cell0 == null || cell0.length() == 0) {
						break;
					}
					fields.put("productId", cell0);
					BigDecimal cell1 = ImportSpreadsheetHelper
							.importBigDecimal(1, row);
					if (cell1 == null || cell1.equals(BigDecimal.ZERO)) {
						break;
					}
					fields.put("quantity", cell1);

					Debug.logInfo("Processed row:" + j + " " + row.toString(),
							module);

					dbrows.add(fields);

				}

			}
			// // create and store values in "Product" and "InventoryItem"
			// entity
			// // in database
			file.delete();
			for (int j = 0; j < dbrows.size(); j++) {
				// GenericValue productGV = delegator.makeValue(tableName,
				// dbrows.get(j));

				// try {
				// delegator.create(productGV);
				// Debug.logInfo("Inserted row: " + j, module);
				// } catch (GenericEntityException e) {
				// Debug.logError("Cannot store product", module);
				// }

				try {
					Map serviceCtx = UtilMisc.toMap("userLogin", userLogin,
							"productId", dbrows.get(j).get("productId"),
							"orderId", dbrows.get(j).get("orderId"),
							"orderItemSeqId",
							dbrows.get(j).get("orderItemSeqId"), "quantity",
							dbrows.get(j).get("quantity"));

					dispatcher.runSync("createVvOrderItemJava", serviceCtx);

				} catch (GenericServiceException e) {
					Debug.logError(e, module);
					request.setAttribute("_ERROR_MESSAGE_",
							"Üres cellak a fajlban.");
					return "error";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "error";

		}

		return "success";
	}

	public static String uploadVvShipmentItem(HttpServletRequest request,
			HttpServletResponse response) {
		Locale locale = UtilHttp.getLocale(request);
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		HttpSession session = request.getSession();
		GenericValue userLogin = (GenericValue) session
				.getAttribute("userLogin");
		LocalDispatcher dispatcher = (LocalDispatcher) request
				.getAttribute("dispatcher");
		String prefix = System.getProperty("user.dir");
		Map<String, Object> results = new HashMap<String, Object>();
		ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory(
				10240, new File(new File("runtime"), "tmp")));
		List<FileItem> lst = null;

		Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
		String shipmentId = null;
		if (paramMap.containsKey("shipmentId")) {
			shipmentId = (String) paramMap.get("shipmentId");
		}
		// request.setAttribute("orderId", orderId);
		String rowsNumberStr = null;
		BigDecimal rowsNumber = null;
		if (paramMap.containsKey("rows")) {
			rowsNumberStr = (String) paramMap.remove("rows");
		}
		try {
			rowsNumber = new BigDecimal(rowsNumberStr);
		} catch (Exception e) {
		}
		/*
		 * try { lst = UtilGenerics.checkList(fu.parseRequest(request)); } catch
		 * (FileUploadException e4) { }
		 * 
		 * if (lst.size() == 0 &&
		 * UtilValidate.isNotEmpty(request.getAttribute("fileItems"))) { lst =
		 * UtilGenerics.cast(request.getAttribute("fileItems")); } if
		 * (lst.size() == 0) { return "error"; }
		 * 
		 * // This code finds the idField and the upload FileItems //
		 * request.removeAttribute("fileItems");
		 * 
		 * FileItem fi = null; FileItem imageFi = null; for (int i = 0; i <
		 * lst.size(); i++) { fi = lst.get(i); String fieldName =
		 * fi.getFieldName(); String fieldStr = fi.getString(); if
		 * (fieldName.equals("uploadFile")) { imageFi = fi; // MimeType of
		 * upload file results.put("uploadMimeType", fi.getContentType()); } }
		 * 
		 * byte[] imageBytes = imageFi.get(); ByteBuffer byteWrap =
		 * ByteBuffer.wrap(imageBytes); results.put("imageData", byteWrap);
		 * results.put("imageFileName", imageFi.getName());
		 * 
		 * String imageName = results.get("imageFileName").toString(); String
		 * mimType = results.get("uploadMimeType").toString(); ByteBuffer
		 * imageData = (ByteBuffer) results.get("imageData"); try { String
		 * dirPath = "/plugins/vvorder/orderitemupload/"; String completeDirPath
		 * = prefix + dirPath; System.out.println("dirPath: " +
		 * completeDirPath); File dir = new File(completeDirPath); if
		 * (!dir.exists()) { boolean createDir = dir.mkdir(); if (!createDir) {
		 * request.setAttribute("_ERROR_MESSAGE_", completeDirPath); return
		 * "error"; } } String imagePath = "/plugins/vvorder/orderitemupload/" +
		 * imageName; System.out.println("imagePath: " + prefix + imagePath);
		 * File file = new File(prefix + "/" + imagePath); if (file.exists()) {
		 * request.setAttribute("_ERROR_MESSAGE_",
		 * "There is an existing frame, please select from the existing frame."
		 * ); return "error"; } Path tmpFile = Files.createTempFile(null, null);
		 * Files.write(tmpFile, imageData.array(), StandardOpenOption.APPEND);
		 * Files.delete(tmpFile); RandomAccessFile out = new
		 * RandomAccessFile(file, "rw"); out.write(imageData.array());
		 * out.close();
		 */
		try {
			File file = ImportSpreadsheetHelper.uploadFile(request,
					"/plugins/vvorder/orderitemupload/");

			//
			// // read all xls file and create workbook one by one.
			List<Map<String, Object>> dbrows = new LinkedList<>();
			POIFSFileSystem fs = null;
			HSSFWorkbook wb = null;
			try {
				fs = new POIFSFileSystem(new FileInputStream(file));
				wb = new HSSFWorkbook(fs);
			} catch (IOException e) {
				Debug.logError("Unable to read or create workbook from file",
						module);
			}

			// get first sheet
			HSSFSheet sheet = wb.getSheetAt(0);
			wb.close();
			String tableName = "VvShipmentItem";
			int sheetLastRowNumber = 0;
			if (rowsNumber == null) {
				sheetLastRowNumber = sheet.getLastRowNum();
			} else {
				sheetLastRowNumber = rowsNumber.intValue();
			}
			for (int j = 1; j <= sheetLastRowNumber; j++) {
				HSSFRow row = sheet.getRow(j);
				if (row != null) {

					Map<String, Object> fields = new HashMap<>();
					// fields.put("shipmentItemSeqId",
					// delegator.getNextSeqId(tableName));
					fields.put("shipmentId", shipmentId);
					String cell0 = ImportSpreadsheetHelper.importString(0, row);
					if (cell0 == null || cell0.length() == 0) {
						break;
					}
					fields.put("productId", cell0);
					BigDecimal cell1 = ImportSpreadsheetHelper
							.importBigDecimal(1, row);
					if (cell1 == null || cell1.equals(BigDecimal.ZERO)) {
						break;
					}
					fields.put("quantity", cell1);

					Debug.logInfo("Processed row:" + j + " " + row.toString(),
							module);

					dbrows.add(fields);

				}

			}
			// // create and store values in "Product" and "InventoryItem"
			// entity
			// // in database
			file.delete();
			for (int j = 0; j < dbrows.size(); j++) {
				// GenericValue productGV = delegator.makeValue(tableName,
				// dbrows.get(j));

				// try {
				// delegator.create(productGV);
				// Debug.logInfo("Inserted row: " + j, module);
				// } catch (GenericEntityException e) {
				// Debug.logError("Cannot store product", module);
				// }

				try {
					Map serviceCtx = UtilMisc.toMap("userLogin", userLogin,
							"productId", dbrows.get(j).get("productId"),
							"shipmentId", dbrows.get(j).get("shipmentId"),
							"quantityToShip", dbrows.get(j).get("quantity"));

					dispatcher.runSync("addShippingItemWithOpenOrder",
							serviceCtx);

				} catch (GenericServiceException e) {
					Debug.logError(e, module);
					request.setAttribute("_ERROR_MESSAGE_",
							"Rossz cellak tartalom a fajlban.");
					return "error";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "error";
		}

		return "success";
	}

	public static String updateOrderShippingItems(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

		String controlDirective = null;
		Map<String, Object> result = null;

		Map<String, Object> paramMap = UtilHttp.getParameterMap(request);

		String itemGroupNumber = request.getParameter("itemGroupNumber");


		// The number of multi form rows is retrieved
		int rowCount = UtilHttp.getMultiFormRowCount(paramMap);
		if (rowCount < 1) {
			Debug.logWarning("No rows to process, as rowCount = " + rowCount, module);
		} else {
			for (int i = 0; i < rowCount; i++) {
				String shipmentId = null;
				String orderId = null;
				String orderItemSeqId = null;
				String productId = null;

				BigDecimal quantity = BigDecimal.ZERO;
				BigDecimal quantityShipped = BigDecimal.ZERO;
				BigDecimal quantityToShip = BigDecimal.ZERO;
				BigDecimal quantityShippable = BigDecimal.ZERO;

				controlDirective = null; // re-initialize each time
				String thisSuffix = UtilHttp.getMultiRowDelimiter() + i;

				// get the params
				if (paramMap.containsKey("shipmentId" + thisSuffix)) {
					shipmentId = (String) paramMap.remove("shipmentId" + thisSuffix);
				}
				if (paramMap.containsKey("orderId" + thisSuffix)) {
					orderId = (String) paramMap.remove("orderId" + thisSuffix);
				}
				request.setAttribute("shipmentId", shipmentId);
				request.setAttribute("orderId", orderId);
				if (paramMap.containsKey("orderItemSeqId" + thisSuffix)) {
					orderItemSeqId = (String) paramMap.remove("orderItemSeqId" + thisSuffix);
				}
				String quantityToShipStr = null;
				if (paramMap.containsKey("quantityToShip" + thisSuffix)) {
					quantityToShipStr = (String) paramMap.remove("quantityToShip" + thisSuffix);
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
				if (quantityToShip.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}

				GenericValue vfOrdItemShipItem = null;
				GenericValue shipment = null;
				try {
					shipment = delegator.findOne("VvShipment", UtilMisc.toMap("shipmentId", shipmentId), false);
				} catch (GenericEntityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Integer shipItemId = 1;
				if (orderId != null) {
					try {
						vfOrdItemShipItem = delegator.findOne("VvOrderItemShippingItemView", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), false);

						quantity = (BigDecimal) vfOrdItemShipItem.get("quantity");
						productId = (String) vfOrdItemShipItem.get("productId");

						// BigDecimal tmp = (BigDecimal) shipment.get("estimatedShipCost");
						// shipItemId = (tmp == null) ? null : tmp.toBigInteger().intValue();

					} catch (GenericEntityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (vfOrdItemShipItem != null) {
						quantityShipped = vfOrdItemShipItem.getBigDecimal("quantityShipped");
						if (quantityShipped == null) {
							quantityShipped = BigDecimal.ZERO;
						}
						quantityShippable = quantity.subtract(quantityShipped);
					}
				} else {
					if (paramMap.containsKey("productId" + thisSuffix)) {
						productId = (String) paramMap.remove("productId" + thisSuffix);
					}
				}
				if (orderId != null && quantityToShip.compareTo(quantityShippable) > 0) {
					request.setAttribute("_ERROR_MESSAGE_", "errore");
					return "error";
				} else {

					BigDecimal qty = BigDecimal.ZERO;
					GenericValue vfshipmentItem = null;
					vfshipmentItem = delegator.makeValue("VvShipmentItem");
					vfshipmentItem.set("shipmentId", shipmentId);
					vfshipmentItem.set("orderId", orderId);
					vfshipmentItem.set("orderItemSeqId", orderItemSeqId);
					qty = qty.add(quantityToShip);

					if (qty.compareTo(BigDecimal.ZERO) != 0) {
						try {
							Map<String, Object> tmpResult = dispatcher.runSync("createVvShipmentItem", UtilMisc.<String, Object> toMap("userLogin", userLogin, "shipmentId", shipmentId, "productId",
									productId, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "quantity", qty));
							// vfshipmentItem.set("shipmentItemSeqId", (String) tmpResult.get("shipmentItemSeqId"));

						} catch (GenericServiceException e) {
							Debug.logError(e, module);
						}
						// delegator.createOrStore(shipmentItem);
						// delegator.createOrStore(vfshipmentItem);
					}

				}

			}
			return "success";

		}

		return "error";

	}

}
