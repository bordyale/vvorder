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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.ofbiz.vvorder.spreadsheetimport.ImportSpreadsheetHelper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImportSpreadsheetServices {

	public static final String module = ImportSpreadsheetServices.class
			.getName();
	public static final String resource = "VvorderUiLabels";

	/**
	 * This method is responsible to import spreadsheet data into "Product" and
	 * "InventoryItem" entities into database. The method uses the
	 * ImportProductHelper class to perform its operation. The method uses
	 * "Apache POI" api for importing spreadsheet (xls files) data.
	 *
	 * Note : Create the spreadsheet directory in the ofbiz home folder and keep
	 * your xls files in this folder only.
	 *
	 * @param dctx
	 *            the dispatch context
	 * @param context
	 *            the context
	 * @return the result of the service execution
	 * @throws IOException
	 */
	public static Map<String, Object> productImportFromSpreadsheet(
			DispatchContext dctx, Map<String, ? extends Object> context)
			throws IOException {
		Delegator delegator = dctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String tableName = "VvProduct";
		// System.getProperty("user.dir") returns the path upto ofbiz home
		// directory
		String path = System.getProperty("user.dir") + "/spreadsheet";
		List<File> fileItems = new LinkedList<>();

		if (UtilValidate.isNotEmpty(path)) {
			File importDir = new File(path);
			if (importDir.isDirectory() && importDir.canRead()) {
				File[] files = importDir.listFiles();
				// loop for all the containing xls file in the spreadsheet
				// directory
				if (files == null) {
					return ServiceUtil.returnError(UtilProperties.getMessage(
							resource, "FileFilesIsNull", locale));
				}
				for (File file : files) {
					if (file.getName().toUpperCase(Locale.getDefault())
							.endsWith("XLS")) {
						fileItems.add(file);
					}
				}
			} else {
				return ServiceUtil.returnError(UtilProperties.getMessage(
						resource, "ProductProductImportDirectoryNotFound",
						locale));
			}
		} else {
			return ServiceUtil.returnError(UtilProperties.getMessage(resource,
					"ProductProductImportPathNotSpecified", locale));
		}

		if (fileItems.size() < 1) {
			return ServiceUtil.returnError(UtilProperties.getMessage(resource,
					"ProductProductImportPathNoSpreadsheetExists", locale)
					+ path);
		}

		for (File item : fileItems) {
			// read all xls file and create workbook one by one.
			List<Map<String, Object>> dbrows = new LinkedList<>();
			POIFSFileSystem fs = null;
			HSSFWorkbook wb = null;
			try {
				fs = new POIFSFileSystem(new FileInputStream(item));
				wb = new HSSFWorkbook(fs);
			} catch (IOException e) {
				Debug.logError("Unable to read or create workbook from file",
						module);
				return ServiceUtil.returnError(UtilProperties.getMessage(
						resource,
						"ProductProductImportCannotCreateWorkbookFromFile",
						locale));
			}

			// get first sheet
			HSSFSheet sheet = wb.getSheetAt(0);
			wb.close();
			int sheetLastRowNumber = sheet.getLastRowNum();
			for (int j = 1; j <= sheetLastRowNumber; j++) {
				HSSFRow row = sheet.getRow(j);
				if (row != null) {
					// read productId from first column "sheet column index
					// starts from 0"
					/*
					 * HSSFCell cell0 = row.getCell(0);
					 * cell0.setCellType(CellType.STRING); String field0 =
					 * cell0.getRichStringCellValue().toString();
					 * 
					 * HSSFCell cell2 = row.getCell(2);
					 * cell2.setCellType(CellType.STRING); String field2 =
					 * cell2.getRichStringCellValue().toString();
					 * 
					 * HSSFCell cell3 = row.getCell(3);
					 * cell3.setCellType(CellType.STRING); String field3 =
					 * cell3.getRichStringCellValue().toString();
					 * 
					 * HSSFCell cell6 = row.getCell(6);
					 * cell6.setCellType(CellType.STRING); String field6 =
					 * cell6.getRichStringCellValue().toString();
					 * 
					 * HSSFCell cell7 = row.getCell(7); BigDecimal field7 =
					 * BigDecimal.ZERO; if (cell7 != null && cell7.getCellType()
					 * == CellType.NUMERIC) { field7 = new
					 * BigDecimal(cell7.getNumericCellValue()); }
					 * 
					 * HSSFCell cell8 = row.getCell(8);
					 * cell8.setCellType(CellType.STRING); String field8 =
					 * cell8.getRichStringCellValue().toString();
					 */

					/*
					 * HSSFCell cell2 = row.getCell(2);
					 * 
					 * String cellValue = String.valueOf(cell2
					 * .getNumericCellValue()); Date date;
					 */

					Map<String, Object> fields = new HashMap<>();
					// fields.put("productId",
					// delegator.getNextSeqId(tableName));

					/*
					 * fields.put("partnerId",
					 * ImportSpreadsheetHelper.importString(0,row));
					 * fields.put("productId",
					 * ImportSpreadsheetHelper.importString(2,row));
					 * fields.put("name",
					 * ImportSpreadsheetHelper.importString(3,row));
					 * fields.put("comment",
					 * ImportSpreadsheetHelper.importString(6,row));
					 * fields.put("weight",
					 * ImportSpreadsheetHelper.importBigDecimal(7,row));
					 * fields.put("wageCode",
					 * ImportSpreadsheetHelper.importString(8,row));
					 */

					fields.put("partnerId",ImportSpreadsheetHelper.importString(0, row));
					fields.put("productId",
							ImportSpreadsheetHelper.importString(2, row));
					fields.put("name",
							ImportSpreadsheetHelper.importString(3, row));

					fields.put("comment",
							ImportSpreadsheetHelper.importString(6, row));

					fields.put("weight",ImportSpreadsheetHelper.importBigDecimal(7,row));
					fields.put("wageCode",ImportSpreadsheetHelper.importString(9, row));
						

					/*
					 * fields.put("partnerId", field0); fields.put("productId",
					 * field2); fields.put("name", field3);
					 * fields.put("comment", field6); fields.put("weight",
					 * field7); fields.put("wageCode", field8);
					 */

					/*
					 * if (HSSFDateUtil.isCellDateFormatted(cell2)) { DateFormat
					 * df = new SimpleDateFormat("yyyy-MM-dd"); date =
					 * cell2.getDateCellValue(); fields.put("date",
					 * UtilDateTime.toTimestamp(date)); cellValue =
					 * df.format(date); }
					 */
					Debug.logInfo("Processed row:" + j + " " + row.toString(), module);

					dbrows.add(fields);

				}

			}
			// create and store values in "Product" and "InventoryItem" entity
			// in database
			for (int j = 0; j < dbrows.size(); j++) {
				GenericValue productGV = delegator.makeValue(tableName,
						dbrows.get(j));

				try {
					delegator.create(productGV);
					Debug.logInfo("Inserted row: " + j, module); 
				} catch (GenericEntityException e) {
					Debug.logError("Cannot store product", module);
					return ServiceUtil.returnError(UtilProperties.getMessage(
							resource, "ProductProductImportCannotStoreProduct",
							locale));
				}

			}
			int uploadedProducts = dbrows.size() + 1;
			if (dbrows.size() > 0) {
				Debug.logInfo("Uploaded " + uploadedProducts
						+ " products from file " + item.getName(), module);
			}
		}
		return ServiceUtil.returnSuccess();
	}
}
