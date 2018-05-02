package gov.fcc.itc.utils;

public interface Constants {

	/** Use this in your parameter list for a string that is null */
	public static final Object NULL_STRING = new NullObject(1);

	/** Converter API Status code */
	public static final String CONVERSION_IN_PROGRESS = "CIP";
	public static final String CLEANSING_IN_PROGRESS = "CLIP";
	public static final String CONVERSION_COMPLETE = "com_cvt";
	public static final String CONVERSION_ERROR = "pen_cv";
	public static final String CONVERSION_COMPLETE_ERROR = "err_cvt";
	public static final String CLEAN_COMPLETE_ERROR = "err_cln";
	
	public static final String PROCESSING_COMPLETE = "com_prc";
	public static final String PROCESSING_ERROR = "err_prc";
	public static final String PROCESSING_SUCCESS_MESSAGE = "File Processed Successfully.";
	public static final String PROCESSING_FAIL_MESSAGE = "File Processing Failed.";
	
	public static final String INVALID_FILE = "IF";

	/** Converter API Status Message */
	public static final String UNKNOWN_FILE_FORMAT = "Unknown file format";
	public static final String CONVERSION_SUCCESS_MESSAGE = "File Converted Successfully.";
	public static final String CONVERSION_FAIL_MESSAGE = "File Conversion Failed.";
	public static final String FILE_NOT_FOUND = "File not found.";
	public static final String  FILE_CONVERSION_EXCEPTION = "Exception in File Conversion Process: ";
	public static final String SOURCE_LOCATION_UNAVAILABLE = "Source Location not available.";
	
	public static final String EXTENSION_PDF = "pdf";
	public static final String EXTENSION_TXT = "txt";
	public static final String DOT = ".";
		
	
	public static final String CLEAN_SUCCESS_MESSAGE = "File cleaned Successfully.";
	public static final String CLEAN_COMPLETE = "com_cln";
    static final String CLEAN_FAIL_MESSAGE = "File Cleansing Failed.";
	public static class NullObject extends EmptyObject {
		public NullObject(int id) {
	         super(id);
		}
	}

	public static class RowObject extends EmptyObject {
		public RowObject(int id) {
			super(id);
		}
	}

}