package gov.fcc.itc.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import gov.fcc.itc.utils.AwsUtil;
import gov.fcc.itc.utils.FileUtil;
import gov.fcc.itc.utils.Constants;
import gov.fcc.itc.utils.AsposeLicense;

import com.aspose.cells.*;
import com.aspose.cells.TxtLoadOptions;
import com.aspose.words.*;
import com.aspose.pdf.*;
import com.aspose.pdf.MemoryCleaner;

public class PostUploadFileProcessor {
	
	private String sourceExt = null;
	private String sourceNameNoExt = null;
	
	private AwsUtil awsUtil = new AwsUtil();
	private byte[] fileContent = null;
	private com.aspose.cells.Workbook asposeWorkbook = null;
	private com.aspose.words.Document asposeDocument = null;
	private com.aspose.pdf.Document asposePdfDocument = null;
	private JSONObject response = null;
	private JSONArray operations = null;
	
	private static JSONObject operationDictionary = (JSONObject) JSONValue.parse(
			"{" +
			"\"cdp\":{\"type\":\"cleanse\",\"desc\":\"Clear Document Properties\"}," +
	        "\"rds\":{\"type\":\"cleanse\",\"desc\":\"Reset Document Statistics\"}," +
	        "\"ccdp\":{\"type\":\"cleanse\",\"desc\":\"Clear Custom Document Properties\"}," +
	        "\"aatc\":{\"type\":\"cleanse\",\"desc\":\"Accept All Track Changes\"}," +
	        "\"dac\":{\"type\":\"cleanse\",\"desc\":\"Delete All Comments\"}," +
	        "\"dht\":{\"type\":\"cleanse\",\"desc\":\"Delete Hidden Text\"}," +
	        "\"cdv\":{\"type\":\"cleanse\",\"desc\":\"Clear Document Variables\"}," +
	        "\"rcx\":{\"type\":\"cleanse\",\"desc\":\"Remove Custom XML\"}," +
	        "\"rcc\":{\"type\":\"cleanse\",\"desc\":\"Remove Content Controls\"}," +
	        "\"rif\":{\"type\":\"cleanse\",\"desc\":\"Remove Include Fields\"}," +
	        "\"rsf\":{\"type\":\"cleanse\",\"desc\":\"Remove Small Font\"}," +
	        "\"rfmb\":{\"type\":\"cleanse\",\"desc\":\"Remove Font Matching Background\"}," +
	        "\"rpn\":{\"type\":\"transform\",\"desc\":\"Remove Page Numbers\"}," +
	        "\"pdf\":{\"type\":\"convert\",\"desc\":\"Convert to PDF\"}," +
	        "\"txt\":{\"type\":\"convert\",\"desc\":\"Convert to TXT\"}," +
	        "\"orig\":{\"type\":\"convert\",\"desc\":\"Preserve Original Format\"}" +
	        "}");
	
	private static String[] word_formats = new String[] {"doc", "docx", "htm", "html", "odt", "pdf", "rtf", "txt"};
	private static String[] supports_rfmb = new String[] {"doc", "docx", "rtf"};
	private static String[] supports_rpn = new String[] {"doc", "docx", "odt", "rtf"};

	private static String[] excel_formats = new String[] {"csv", "xls", "xlsx"};
	//private static String[] known_extensions = new String[] {"bmp", "csv", "doc", "docx", "gif", "htm", "html", "jpeg", "jpg", "mpp", "odt", "pdf", "png", "ppt", "pptx", "psd", "rtf", "tif", "tiff", "txt", "vsd", "wks", "wpd", "xls", "xlsx"};
	
	private static LambdaLogger logger;
	
	public PostUploadFileProcessor(JSONArray ops, LambdaLogger l) {
		operations = ops;		
		response = new JSONObject();
		logger = l;
		AsposeLicense.applyLicense();

		List<String> fontPaths = com.aspose.pdf.Document.getLocalFontPaths();
		fontPaths.add("/var/task/fonts/msttcore/");
		com.aspose.pdf.Document.setLocalFontPaths(fontPaths);
		
	}
	
	public void LoadSourceFile(String bucketName, String fileName) throws Exception {
		awsUtil.setS3BucketName(bucketName);
		
		sourceExt = FileUtil.getFileExtension(fileName).toLowerCase();
		sourceNameNoExt = FileUtil.getFileNameNoExtension(fileName);

		String sourceFile = System.getenv("SOURCE_LOCATION") + fileName;
				
		if (awsUtil.isFileExists(sourceFile)) {
			fileContent = awsUtil.getObject(sourceFile);
			InputStream stream = new ByteArrayInputStream(fileContent);		 	   
			if (isExcelFormat()) {
				if (sourceExt.toLowerCase().equals("csv")) {
					com.aspose.cells.TxtLoadOptions opts = new TxtLoadOptions();
					opts.setSeparator(',');
					asposeWorkbook = new com.aspose.cells.Workbook(stream, opts);					
				} else {
			    	asposeWorkbook = new com.aspose.cells.Workbook(stream);					
				}
			} else if (isWordFormat()) {
				if (isPDFFormat()) {
					asposePdfDocument = new com.aspose.pdf.Document(stream);
					ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
					
					if (!wasTransformationOrCleanseRequested()) {
						// This is PDF that doesn't have to be transformed - OPIF legacy use (loading PDF and converting to PDF)
		    	   		asposeDocument = null; 						
					} else if (hasOnlyImages(asposePdfDocument)) {
		    	   		// This is a scan PDF. Treat it as such
						asposeDocument = null;
					} else {
			    	   	try {
			    	   		com.aspose.pdf.DocSaveOptions dso = new com.aspose.pdf.DocSaveOptions();
			    	   		dso.setFormat(com.aspose.pdf.DocSaveOptions.DocFormat.DocX);
			    	   		dso.setMode(com.aspose.pdf.DocSaveOptions.RecognitionMode.Flow);
			    	   		dso.setRecognizeBullets(true);
				    	   	asposePdfDocument.save(dstStream, dso);
				    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
				    	   	
				    	   	asposeDocument = new com.aspose.words.Document(srcStream);
				          
				            dstStream.close();
				        	srcStream.close();
							
				        	MemoryCleaner.clear();
							
			    	   	} catch (Exception e) {
			    	   		// This is a scan PDF. Treat it as such
			    	   		asposeDocument = null; 
			    	   		// throw new RuntimeException (e.getMessage());
			    	   	}						
					}
				} else {
			 	   	asposeDocument = new com.aspose.words.Document(stream);					
				}
			}
    	   	stream.close();
		} else {
			throw new RuntimeException ("Source file not found");
		}
	}
		
	public JSONObject ApplyOperations() {
		if (fileContent == null && asposeDocument == null && asposeWorkbook == null) {
			return response;
		}
		
		ApplyCleansing();
		ApplyTransformations();
		ApplyExports();
		
		return response;
	}
	
	private void ApplyCleansing() {		
		// Handle known operations, ignore unknown
        if (inOperations("rds"))  ResetDocumentStatistics();
		if (inOperations("cdp"))  ClearDocumentProperties();
        if (inOperations("ccdp")) ClearCustomDocumentProperties();
        if (inOperations("aatc")) AcceptAllTrackChanges();
		if (inOperations("dac"))  DeleteAllComments();
		if (inOperations("dht"))  DeleteHiddenText();
		if (inOperations("cdv"))  ClearDocumentVariables();
		if (inOperations("rcx"))  RemoveCustomXML();
		if (inOperations("rcc"))  RemoveContentControls();
		if (inOperations("rif"))  RemoveIncludeFields();
		if (inOperations("rsf"))  RemoveSmallFont();
		if (inOperations("rfmb")) RemoveFontMatchingBackground();
	}

	private void ApplyTransformations() {
		// Handle known operations, ignore unknown
		if (inOperations("rpn")) RemovePageNumbers();		
	}
	
	private void ApplyExports() {
		// Handle known formats, ignore unknown
		// Save as original format must go first to avoid having global options affect the content
		if (inOperations("orig")) SaveAsOriginalFormat();		
		if (inOperations("pdf")) SaveAsPDF();		
		if (inOperations("txt")) SaveAsTXT();
	}

	private void ClearCustomDocumentProperties() {
		String opCode = "ccdp";
		try {		
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.CustomDocumentProperties customProperties = asposeDocument.getCustomDocumentProperties();
			    customProperties.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else if (isExcelFormat()) {
				com.aspose.cells.CustomDocumentPropertyCollection customProperties = asposeWorkbook.getWorksheets().getCustomDocumentProperties();
			    customProperties.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");
			}
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		} 
	}

	private void ResetDocumentStatistics() {
		String opCode = "rds";
		try {
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.BuiltInDocumentProperties bip = asposeDocument.getBuiltInDocumentProperties();
				Iterator it = bip.iterator();
				while (it.hasNext()) {
					com.aspose.words.DocumentProperty prop = (com.aspose.words.DocumentProperty) it.next();
					if (prop.getName().equals("LastSavedBy")) {
						prop.setValue("");
					}
					if (prop.getName().equals("RevisionNumber")) {
						prop.setValue(1);
					}
					if (prop.getName().equals("TotalEditingTime")) {
						prop.setValue(0);
					}
				}
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else if (isExcelFormat()) {
			    com.aspose.cells.BuiltInDocumentPropertyCollection bip = asposeWorkbook.getWorksheets().getBuiltInDocumentProperties();
				Iterator it = bip.iterator();
				while (it.hasNext()) {
					com.aspose.cells.DocumentProperty prop = (com.aspose.cells.DocumentProperty) it.next();
					if (prop.getName().equals("LastSavedBy")) {
						prop.setValue("");
					}
					if (prop.getName().equals("RevisionNumber")) {
						prop.setValue(1);
					}
					if (prop.getName().equals("TotalEditingTime")) {
						prop.setValue(0);
					}
				}
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");
			}		
		} catch (Exception e) {
		    addResultToResponse("cleanse", opCode, "fail", e.getMessage());								
		}
	}
	
	private void ClearDocumentProperties() {
		// Clears everything except Template
		String opCode = "cdp";
		try {
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.BuiltInDocumentProperties bip = asposeDocument.getBuiltInDocumentProperties();
			    
			    String template = "";
			    Iterator it = bip.iterator();
				while (it.hasNext()) {
					com.aspose.words.DocumentProperty prop = (com.aspose.words.DocumentProperty) it.next();
					if (prop.getName().equals("Template")) {
						template = (String) prop.getValue();
					}
				}
			    bip.clear();
			    bip.setTemplate(template);
			    
			    addResultToResponse("cleanse", opCode, "ok", null);		
			} else if (isExcelFormat()) {
			    com.aspose.cells.BuiltInDocumentPropertyCollection bip = asposeWorkbook.getWorksheets().getBuiltInDocumentProperties();
			    
			    String template = "";
			    Iterator it = bip.iterator();
				while (it.hasNext()) {
					com.aspose.cells.DocumentProperty prop = (com.aspose.cells.DocumentProperty) it.next();
					if (prop.getName().equals("Template")) {
						template = (String) prop.getValue();
					}
				}
			    bip.clear();
			    bip.setTemplate(template);

			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");
			}				
		} catch (Exception e) {
		    addResultToResponse("cleanse", opCode, "fail", e.getMessage());								
		}
	}
	
	private void AcceptAllTrackChanges() {
		String opCode = "aatc";
		try {
			if (isWordFormat() && asposeDocument != null) {
				asposeDocument.acceptAllRevisions();
			    addResultToResponse("cleanse", opCode, "ok", null);
			} else if (isExcelFormat()) {
				asposeWorkbook.acceptAllRevisions();
			    addResultToResponse("cleanse", opCode, "ok", null);
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}					
		} catch (Exception e) {
		    addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		} 

	}
	
	private void DeleteAllComments() {
		String opCode = "dac";
		try {
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.NodeCollection comments = asposeDocument.getChildNodes(com.aspose.words.NodeType.COMMENT, true);
			    comments.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else if (isExcelFormat()) {
				com.aspose.cells.WorksheetCollection worksheets = asposeWorkbook.getWorksheets();
				Iterator it = worksheets.iterator();
				while (it.hasNext()) {
					Worksheet worksheet = (Worksheet) it.next();
					worksheet.getComments().clear();
				}
				addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");
			}			
		} catch (Exception e) {
		    addResultToResponse("cleanse", opCode, "fail", e.getMessage());								
		}
	}
	
	private void DeleteHiddenText() {
		String opCode = "dht";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null) {
				com.aspose.words.NodeCollection runs = asposeDocument.getChildNodes(com.aspose.words.NodeType.RUN, true);
				Iterator it = runs.iterator();
				while (it.hasNext()) {
					com.aspose.words.Run run = (com.aspose.words.Run) it.next();
				    if (run.getFont().getHidden()) run.remove();
				}
				addResultToResponse("cleanse", opCode, "ok", null);
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}			
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}
	}
	
	private void ClearDocumentVariables() {
		String opCode = "cdv";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.VariableCollection vars = asposeDocument.getVariables();
			    vars.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}					
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}
	}

	private void RemoveCustomXML() {
		String opCode = "rcx";
		try {
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.CustomXmlPartCollection xmlColl = asposeDocument.getCustomXmlParts();
			    xmlColl.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else if (isExcelFormat()) {
			    com.aspose.cells.CustomXmlPartCollection xmlColl = asposeWorkbook.getCustomXmlParts();
			    xmlColl.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}					
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}
	}
	
	private void RemoveContentControls() {
		String opCode = "rcc";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null) {
			    com.aspose.words.NodeCollection ccNodes = asposeDocument.getChildNodes(com.aspose.words.NodeType.STRUCTURED_DOCUMENT_TAG, true);
			    ccNodes.clear();
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}							
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}
	}
	
	private void RemoveIncludeFields() {
		String opCode = "rif";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null) {
				com.aspose.words.FieldCollection fields = asposeDocument.getRange().getFields();
				Iterator it = fields.iterator();
				while (it.hasNext()) {
					com.aspose.words.Field field = (com.aspose.words.Field) it.next();
					if (field.getType() == com.aspose.words.FieldType.FIELD_INCLUDE || 
						field.getType() == com.aspose.words.FieldType.FIELD_INCLUDE_TEXT ||
						field.getType() == com.aspose.words.FieldType.FIELD_INCLUDE_PICTURE) field.remove();
				}
			    addResultToResponse("cleanse", opCode, "ok", null);				
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}								
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}
	}
	
	private void RemoveSmallFont() {
		String opCode = "rsf";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null) {
				com.aspose.words.NodeCollection runs = asposeDocument.getChildNodes(com.aspose.words.NodeType.RUN, true);
				Iterator it = runs.iterator();
				while (it.hasNext()) {
					com.aspose.words.Run run = (com.aspose.words.Run) it.next();
				    if (run.getFont().getSize() <= 1.0 && !run.getText().replaceAll("\\f|\\n|\\r", "").isEmpty()) { 
				    	run.remove();
				    }
				}
				addResultToResponse("cleanse", opCode, "ok", null);
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}			
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}		
	}
	
	private void RemoveFontMatchingBackground() {
		String opCode = "rfmb";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null && supportsRFMB()) {
				com.aspose.words.NodeCollection runs = asposeDocument.getChildNodes(com.aspose.words.NodeType.RUN, true);
				Iterator it = runs.iterator();
				while (it.hasNext()) {

					com.aspose.words.Run run = (com.aspose.words.Run) it.next();
					java.awt.Color fgr = run.getFont().getColor();
					java.awt.Color bgr = run.getFont().getHighlightColor();
					
					if (isExplicitlySet(fgr) && fgr.equals(java.awt.Color.WHITE) && !isExplicitlySet(bgr)) {
						// white font on default background
						run.remove();
					} else if (isExplicitlySet(bgr) && bgr.equals(java.awt.Color.BLACK) && !isExplicitlySet(fgr)) {
						// default font on black background
						run.remove();
					} else if (isExplicitlySet(fgr) && isExplicitlySet(bgr) && bgr.equals(fgr)) {
						// both font color and background color are set explicitly and match each other
						run.remove();
					}
					
				}
				addResultToResponse("cleanse", opCode, "ok", null);
			} else {
				addResultToResponse("cleanse", opCode, "skip", "Not Supported For File Format");			
			}			
		} catch (Exception e) {
			addResultToResponse("cleanse", opCode, "fail", e.getMessage());
		}				
	}
	
	private void RemovePageNumbers() {
		String opCode = "rpn";
		// Only supported for Word
		try {
			if (isWordFormat() && asposeDocument != null && supportsRPN()) {
				for (com.aspose.words.Section section : asposeDocument.getSections()) {
					for (com.aspose.words.HeaderFooter hf : section.getHeadersFooters()) {
						if (hf.getHeaderFooterType() == HeaderFooterType.HEADER_PRIMARY ||
							hf.getHeaderFooterType() == HeaderFooterType.FOOTER_PRIMARY) {
							com.aspose.words.NodeCollection fscoll = hf.getChildNodes(NodeType.FIELD_START, true);
							Iterator it = fscoll.iterator();
							while (it.hasNext()) {
								com.aspose.words.FieldStart fs = (com.aspose.words.FieldStart) it.next();
								if (fs.getFieldType() == com.aspose.words.FieldType.FIELD_PAGE) fs.getField().remove();
							}
						}
					}
				}
				addResultToResponse("transform", opCode, "ok", null);
			} else {
				addResultToResponse("transform", opCode, "skip", "Not Supported For File Format");			
			}						
		} catch (Exception e) {
			addResultToResponse("transform", opCode, "fail", e.getMessage());
		}
		
	}
		
	private void SaveAsPDF() {
		String opCode = "pdf";

		String convertedFileName = sourceNameNoExt + Constants.DOT + Constants.EXTENSION_PDF;
		String destFile = System.getenv("DESTINATION_LOCATION") + convertedFileName;
        
		if (isWordFormat()) {
			if (isPDFFormat() && !wasTransformationOrCleanseRequested() && asposePdfDocument != null ) {
    			ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
        	   	
        	   	try {
    	    	   	asposePdfDocument.save(dstStream, com.aspose.pdf.SaveFormat.Pdf);
    	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
    	    	   	
    	    	    ObjectMetadata meta = new ObjectMetadata();
    	            meta.setContentLength(dstStream.toByteArray().length);
    	            meta.setContentType("application/pdf");  
    	            
    	            dstStream.close();
    	
    	    	   	awsUtil.putObject(destFile, srcStream, meta);
    	    	   	
    	        	srcStream.close();
    	     
    	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to PDF.");
        			addResultToResponse("convert", opCode, "ok", null);
        			
    				MemoryCleaner.clear();
        	   	} catch (Exception e) {
        			addResultToResponse("convert", opCode, "fail", e.getMessage());
        	   	}			
			} else {
	    	   	if (asposeDocument != null) {
	    			ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
	        	   	
	        	   	try {

	        	   		FontSettings FontSettings = new FontSettings();
	        	   		String[] fontPaths = new String[]  { 
	        	   				"/usr/share/fonts/", 
	        	   				"/var/task/fonts/msttcore/" 
	        	   				};
	        	   		FontSettings.setFontsFolders(fontPaths, true);
	        	   		asposeDocument.setFontSettings(FontSettings);       	   		
	        	   		asposeDocument.save(dstStream, com.aspose.words.SaveFormat.PDF);
	        	   		
	    	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
	    	    	   	
	    	    	    ObjectMetadata meta = new ObjectMetadata();
	    	            meta.setContentLength(dstStream.toByteArray().length);
	    	            meta.setContentType("application/pdf");  
	    	            
	    	            dstStream.close();
	    	
	    	    	   	awsUtil.putObject(destFile, srcStream, meta);
	    	    	   	
	    	        	srcStream.close();
	    	     
	    	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to PDF.");
	        			addResultToResponse("convert", opCode, "ok", null);
	        			
	    				MemoryCleaner.clear();
	    				
	        	   	} catch (Exception e) {
	        			addResultToResponse("convert", opCode, "fail", e.getMessage());
	        	   	}    	   		
	    	   	} else if (asposePdfDocument != null) {
	    			ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
	        	   	
	        	   	try {
	    	    	   	asposePdfDocument.save(dstStream, com.aspose.pdf.SaveFormat.Pdf);
	    	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
	    	    	   	
	    	    	    ObjectMetadata meta = new ObjectMetadata();
	    	            meta.setContentLength(dstStream.toByteArray().length);
	    	            meta.setContentType("application/pdf");  
	    	            
	    	            dstStream.close();
	    	
	    	    	   	awsUtil.putObject(destFile, srcStream, meta);
	    	    	   	
	    	        	srcStream.close();
	    	     
	    	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to PDF.");
	        			addResultToResponse("convert", opCode, "ok", null);
	        			
	    				MemoryCleaner.clear();
	        	   	} catch (Exception e) {
	        			addResultToResponse("convert", opCode, "fail", e.getMessage());
	        	   	}				    	   		
	    	   	}				
			}

			
			
		} else if (isExcelFormat()) {

			com.aspose.cells.PdfSaveOptions option = new com.aspose.cells.PdfSaveOptions();
        	option.setAllColumnsInOnePagePerSheet(true);
        	option.setClearData(true);

        	ByteArrayOutputStream dstStream = new ByteArrayOutputStream();

			try {
	        	asposeWorkbook.save(dstStream, option);

	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
	    	   	
	    	    ObjectMetadata meta = new ObjectMetadata();
	            meta.setContentLength(dstStream.toByteArray().length);
	            meta.setContentType("application/pdf");  
	            
	            dstStream.close();

	    	   	awsUtil.putObject(destFile, srcStream, meta);
	    	   	
	        	srcStream.close();
	     
	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to PDF.");
    			addResultToResponse("convert", opCode, "ok", null);

    			MemoryCleaner.clear();
    			
			} catch (Exception e) {
    			addResultToResponse("convert", opCode, "fail", e.getMessage());				
			}
		} else {
			addResultToResponse("convert", opCode, "skip", "Not Supported");							
		}
	}

	private void SaveAsTXT() {
		String opCode = "txt";
		
		String convertedFileName = sourceNameNoExt + Constants.DOT + Constants.EXTENSION_TXT;
		String destFile = System.getenv("DESTINATION_LOCATION") + convertedFileName;
        
		if (isWordFormat()) {
			if (isPDFFormat() && !wasTransformationOrCleanseRequested() && asposePdfDocument != null ) {
    			ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
        	   	
        	   	try {
        	   		com.aspose.pdf.facades.PdfExtractor extractor = new com.aspose.pdf.facades.PdfExtractor();
        	   		extractor.bindPdf(asposePdfDocument);
        	   		extractor.extractText();
        	   		extractor.getText(dstStream);
        	   		
    	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
    	    	   	
    	    	    ObjectMetadata meta = new ObjectMetadata();
    	            
    	            dstStream.close();
    	
    	    	   	awsUtil.putObject(destFile, srcStream, meta);
    	    	   	
    	        	srcStream.close();
    	     
    	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to TXT.");
        			addResultToResponse("convert", opCode, "ok", null);
        			
    				MemoryCleaner.clear();
        	   	} catch (Exception e) {
        			addResultToResponse("convert", opCode, "fail", e.getMessage());
        	   	}	
        	} else if (asposeDocument != null) {
				ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
	    	   	
	    	   	try {
		    	   	asposeDocument.save(dstStream, com.aspose.words.SaveFormat.TEXT);
		    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
		    	   	
		    	    ObjectMetadata meta = new ObjectMetadata();
		            
		            dstStream.close();
		
		    	   	awsUtil.putObject(destFile, srcStream, meta);
		    	   	
		        	srcStream.close();
		     
		        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to TXT.");
	    			addResultToResponse("convert", opCode, "ok", null);
	    			
					MemoryCleaner.clear();
					
	    	   	} catch (Exception e) {
	    			addResultToResponse("convert", opCode, "fail", e.getMessage());
	    	   	}
    	   	}
		} else {
			addResultToResponse("convert", opCode, "skip", "Not Supported");							
		}
		
	}
	
	private void SaveAsOriginalFormat() {
		String opCode = "orig";
		
		String convertedFileName = sourceNameNoExt + Constants.DOT + sourceExt;
		String destFile = System.getenv("DESTINATION_LOCATION") + convertedFileName;
        
		if (isWordFormat()) {
			if (isPDFFormat() && !wasTransformationOrCleanseRequested() && asposePdfDocument != null ) {
    			ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
        	   	
        	   	try {
    	    	   	asposePdfDocument.save(dstStream, com.aspose.pdf.SaveFormat.Pdf);
    	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
    	    	   	
    	    	    ObjectMetadata meta = new ObjectMetadata();
    	            meta.setContentLength(dstStream.toByteArray().length);
    	            meta.setContentType("application/pdf");  
    	            
    	            dstStream.close();
    	
    	    	   	awsUtil.putObject(destFile, srcStream, meta);
    	    	   	
    	        	srcStream.close();
    	     
    	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to PDF.");
        			addResultToResponse("convert", opCode, "ok", null);
        			
    				MemoryCleaner.clear();
        	   	} catch (Exception e) {
        			addResultToResponse("convert", opCode, "fail", e.getMessage());
        	   	}				
			} else {
				if (asposeDocument != null) {
					ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
		    	   	
		    	   	try {
			    	   	asposeDocument.save(dstStream, getSaveFormat(sourceExt));
			    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
			    	   	
			    	    ObjectMetadata meta = new ObjectMetadata();
			            meta.setContentLength(dstStream.toByteArray().length);
			            
			            dstStream.close();
			
			    	   	awsUtil.putObject(destFile, srcStream, meta);
			    	   	
			        	srcStream.close();
			     
			        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Saved in original format.");
		    			addResultToResponse("convert", opCode, "ok", null);
		    			
						MemoryCleaner.clear();
						
		    	   	} catch (Exception e) {
		    			addResultToResponse("convert", opCode, "fail", e.getMessage());
		    	   	}				
				} else if (asposePdfDocument != null) {
	    			ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
	        	   	
	        	   	try {
	    	    	   	asposePdfDocument.save(dstStream, com.aspose.pdf.SaveFormat.Pdf);
	    	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
	    	    	   	
	    	    	    ObjectMetadata meta = new ObjectMetadata();
	    	            meta.setContentLength(dstStream.toByteArray().length);
	    	            meta.setContentType("application/pdf");  
	    	            
	    	            dstStream.close();
	    	
	    	    	   	awsUtil.putObject(destFile, srcStream, meta);
	    	    	   	
	    	        	srcStream.close();
	    	     
	    	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Converted to PDF.");
	        			addResultToResponse("convert", opCode, "ok", null);
	        			
	    				MemoryCleaner.clear();
	        	   	} catch (Exception e) {
	        			addResultToResponse("convert", opCode, "fail", e.getMessage());
	        	   	}				
				}				
			}
		} else if (isExcelFormat()) {

        	ByteArrayOutputStream dstStream = new ByteArrayOutputStream();

			try {
	        	asposeWorkbook.save(dstStream, getSaveFormat(sourceExt));

	    	   	InputStream srcStream =  new ByteArrayInputStream(dstStream.toByteArray());
	    	   	
	    	    ObjectMetadata meta = new ObjectMetadata();
	            meta.setContentLength(dstStream.toByteArray().length);
	            
	            dstStream.close();

	    	   	awsUtil.putObject(destFile, srcStream, meta);
	    	   	
	        	srcStream.close();
	     
	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Saved in original format.");
    			addResultToResponse("convert", opCode, "ok", null);

    			MemoryCleaner.clear();
    			
			} catch (Exception e) {
    			addResultToResponse("convert", opCode, "fail", e.getMessage());				
			}
		} else {
			try {
	    	   	InputStream srcStream =  new ByteArrayInputStream(fileContent);
	    	   	
	    	    ObjectMetadata meta = new ObjectMetadata();
	            meta.setContentLength(fileContent.length);
	            
	    	   	awsUtil.putObject(destFile, srcStream, meta);
	    	   	
	        	logger.log(sourceNameNoExt + Constants.DOT  + sourceExt + " - Saved in original format.");
				addResultToResponse("convert", opCode, "ok", null);
				
				MemoryCleaner.clear();        					
			} catch (Exception e) {
    			addResultToResponse("convert", opCode, "fail", e.getMessage());								
			}
		}
	}
		
	private boolean isWordFormat() {
		return Arrays.asList(word_formats).contains(sourceExt);
	}

	private boolean supportsRFMB() {
		return Arrays.asList(supports_rfmb).contains(sourceExt);		
	}

	private boolean supportsRPN() {
		return Arrays.asList(supports_rpn).contains(sourceExt);		
	}

	private boolean isExcelFormat() {
		return Arrays.asList(excel_formats).contains(sourceExt);
	}
	
	private boolean isPDFFormat() {
		return sourceExt.toLowerCase().equals("pdf");
	}	
	
	private boolean inOperations(String op) {
		if (operations != null) {
			return operations.toString().contains(op);			
		} else {
			return false;
		}
	}
	
	private void addResultToResponse(String type, String op, String result, String msg) {
		if (response.get(type) == null) {
			response.put(type, (JSONObject) JSONValue.parse("{}"));
		}
		
		String descOp = (String) ((JSONObject) operationDictionary.get(op)).get("desc");
		
		JSONObject typeData = (JSONObject) response.get(type);
		if (op == "orig") {
			if (result.equals("ok")) {
				typeData.put(op, (JSONObject) JSONValue.parse("{\"description\":\"" + descOp + 
						                                      "\",\"result\":\"" + result + 
						                                      "\",\"original_ext\":\"" + sourceExt + 
						                                      "\"}"));
			} else {
				typeData.put(op, (JSONObject) JSONValue.parse("{\"description\":\"" + descOp + 
						                                      "\",\"result\":\"" + result + 
						                                      "\",\"original_ext\":\"" + sourceExt +  
						                                      "\",\"message\":\"" + msg + 
						                                      "\"}"));
			}						
		} else {
			if (result.equals("ok")) {
				typeData.put(op, (JSONObject) JSONValue.parse("{\"description\":\"" + descOp + 
						                                      "\",\"result\":\"ok\"}"));
			} else {
				typeData.put(op, (JSONObject) JSONValue.parse("{\"description\":\"" + descOp + 
						                                      "\",\"result\":\"" + result + 
						                                      "\",\"message\":\"" + msg + 
						                                      "\"}"));
			}			
		}
		
		response.put(type, typeData);
	}
	
	private int getSaveFormat(String ext) {
    	if (ext == null) {
			return com.aspose.words.SaveFormat.UNKNOWN;
		}		

		switch (ext) {
			case "doc":
				return com.aspose.words.SaveFormat.DOC;
			case "docx":
				return com.aspose.words.SaveFormat.DOCX;
			case "htm":
			case "html":
				return com.aspose.words.SaveFormat.HTML;
			case "odt":
				return com.aspose.words.SaveFormat.ODT;
			case "pdf":
				return com.aspose.words.SaveFormat.PDF;
			case "rtf":
				return com.aspose.words.SaveFormat.RTF;
			case "txt":
				return com.aspose.words.SaveFormat.TEXT;
			case "ppt":
				return com.aspose.slides.SaveFormat.Ppt;
			case "pptx":
				return com.aspose.slides.SaveFormat.Pptx;
			case "csv":
				return com.aspose.cells.SaveFormat.CSV;
			case "xls":
				return com.aspose.cells.SaveFormat.EXCEL_97_TO_2003;
			case "xlsx":
				return com.aspose.cells.SaveFormat.XLSX;				
			default: 
				return com.aspose.words.SaveFormat.UNKNOWN;
		}
	}
	
	private boolean isExplicitlySet(java.awt.Color color) {
		return (color.getComponents(null)[3] > 0);
	}
	
	private boolean hasOnlyImages(com.aspose.pdf.Document doc) {
		com.aspose.pdf.OperatorSelector os;
		com.aspose.pdf.PageCollection pc = doc.getPages();
		Iterator it = pc.iterator();
		while (it.hasNext()) {
			os = new com.aspose.pdf.OperatorSelector(new com.aspose.pdf.operators.ShowText());
			com.aspose.pdf.Page page = (com.aspose.pdf.Page) it.next();
			page.getContents().accept(os);
			if (os.getSelected().size() != 0) {
				return false;
			}
		}
		return true;		
	}
	
	private boolean wasTransformationOrCleanseRequested() {
		if (inOperations("rds")  || inOperations("cdp") || inOperations("ccdp") || 
        	inOperations("aatc") || inOperations("dac") || inOperations("dht") || 
            inOperations("cdv")  || inOperations("rcx") || inOperations("rcc") || 
            inOperations("rif")  || inOperations("rsf") || inOperations("rfmb") || 
            inOperations("rpn")) { 
        	return true;
        } else {
        	return false;
        }
	}
}