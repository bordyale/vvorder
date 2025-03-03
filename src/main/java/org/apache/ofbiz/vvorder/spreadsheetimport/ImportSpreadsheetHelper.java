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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilGenerics;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public final class ImportSpreadsheetHelper {

	public static final String module = ImportSpreadsheetHelper.class.getName();

	private ImportSpreadsheetHelper() {
	}

	// prepare the product map
	public static Map<String, Object> prepareProduct(String productId) {
		Map<String, Object> fields = new HashMap<>();
		fields.put("productId", productId);
		/*
		 * fields.put("productTypeId", "FINISHED_GOOD");
		 * fields.put("internalName", "Product_" + productId);
		 * fields.put("isVirtual", "N"); fields.put("isVariant", "N");
		 */
		return fields;
	}

	// check if product already exists in database
	public static boolean checkProductExists(String productId,
			Delegator delegator) {
		GenericValue tmpProductGV;
		boolean productExists = false;
		try {
			tmpProductGV = EntityQuery.use(delegator).from("VvProduct")
					.where("productId", productId).queryOne();
			if (tmpProductGV != null
					&& productId.equals(tmpProductGV.getString("productId"))) {
				productExists = true;
			}
		} catch (GenericEntityException e) {
			Debug.logError("Problem in reading data of product", module);
		}
		return productExists;
	}

	public static String importString(Integer columnIndex,HSSFRow row) {
		HSSFCell cell = row.getCell(columnIndex);
		if (cell == null)
			return null;
		cell.setCellType(CellType.STRING);
		String field = cell.getRichStringCellValue().toString();
		return field;
	}

	public static BigDecimal importBigDecimal(Integer columnIndex,HSSFRow row) {
		HSSFCell cell = row.getCell(columnIndex);
		if (cell == null)
			return null;
		BigDecimal field = BigDecimal.ZERO;
		if (cell != null && cell.getCellType() == CellType.NUMERIC) {
			field = new BigDecimal(cell.getNumericCellValue());
		}
		return field;
	}

	public static Timestamp importDate(Integer columnIndex,HSSFRow row) {
		HSSFCell cell = row.getCell(columnIndex);
		if (cell == null)
			return null;
		// String cellValue = String.valueOf(cell.getNumericCellValue());
		Date date;
		// if (HSSFDateUtil.isCellDateFormatted(cell2)) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		date = cell.getDateCellValue();
		// fields.put("date", UtilDateTime.toTimestamp(date));
		// cellValue = df.format(date);
		// }

		return UtilDateTime.toTimestamp(date);
	}
	public static File uploadFile(HttpServletRequest request,String dirPath) throws GeneralException{
		//dont forget to delete uploaded returnet File
		HttpSession session = request.getSession();
		GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
		String prefix = System.getProperty("user.dir");
		Map<String, Object> results = new HashMap<String, Object>();
		ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory(10240, new File(new File("runtime"), "tmp")));
		List<FileItem> lst = null;

		try {
			lst = UtilGenerics.checkList(fu.parseRequest(request));
		} catch (FileUploadException e4) {
		}

		if (lst.size() == 0 && UtilValidate.isNotEmpty(request.getAttribute("fileItems"))) {
			lst = UtilGenerics.cast(request.getAttribute("fileItems"));
		}
		if (lst.size() == 0) {
			return null;
		}

		// This code finds the idField and the upload FileItems
//		request.removeAttribute("fileItems");

		FileItem fi = null;
		FileItem imageFi = null;
		for (int i = 0; i < lst.size(); i++) {
			fi = lst.get(i);
			String fieldName = fi.getFieldName();
			String fieldStr = fi.getString();
			if (fieldName.equals("uploadFile")) {
				imageFi = fi;
				// MimeType of upload file
				results.put("uploadMimeType", fi.getContentType());
			}
		}

		byte[] imageBytes = imageFi.get();
		ByteBuffer byteWrap = ByteBuffer.wrap(imageBytes);
		results.put("imageData", byteWrap);
		results.put("imageFileName", imageFi.getName());

		String imageName = results.get("imageFileName").toString();
		if (imageName != null & !imageName.endsWith(".xls")){
            request.setAttribute("_ERROR_MESSAGE_", "Ádááááááám  csak .xls lehet:-)");
           	throw new GeneralException("Erro file extention, just .xsl allowed");
		}
		String mimType = results.get("uploadMimeType").toString();
		ByteBuffer imageData = (ByteBuffer) results.get("imageData");
		try {
//			String dirPath = "/plugins/vvorder/orderitemupload/";
			String completeDirPath = prefix + dirPath;
			System.out.println("dirPath: " + completeDirPath);
			File dir = new File(completeDirPath);
			if (!dir.exists()) {
				boolean createDir = dir.mkdir();
				if (!createDir) {
					request.setAttribute("_ERROR_MESSAGE_", completeDirPath);
					return null;
				}
			}
			String imagePath = dirPath + imageName;
			System.out.println("imagePath: " + prefix + imagePath);
			File file = new File(prefix + "/" + imagePath);
			if (file.exists()) {
				request.setAttribute("_ERROR_MESSAGE_", "There is an existing frame, please select from the existing frame.");
				return null;
			}
			Path tmpFile = Files.createTempFile(null, null);
			Files.write(tmpFile, imageData.array(), StandardOpenOption.APPEND);
			Files.delete(tmpFile);
			RandomAccessFile out = new RandomAccessFile(file, "rw");
			out.write(imageData.array());
			out.close();

			return file;
		} catch (Exception e) {
			System.out.println(e);

		}

			return null;
	}
}
