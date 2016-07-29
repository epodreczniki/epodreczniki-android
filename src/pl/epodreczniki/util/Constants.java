package pl.epodreczniki.util;

public final class Constants {	
	
	public static final String DEV_FILE_NAME = "dev.zip";
	
	public static final String INDEX_ENTRY = "content/index.html";
	
	public static final String TOC_ENTRY = "content/toc.json";
	
	public static final String PAGES_ENTRY = "content/pages.json";
	
	public static final String COVERS_DIR = "covers";
	
	public static final String COVERS_TMP = "covers_tmp";
	
	public static final String BOOKS_DIR = "books";
	
	public static final String DEV_DIR = "dev";
	
	public static final String LINK_WRAP = "<a href='%s'>%s</a> ";
	
	public static final String PREF_ALLOW_MOBILE_DATA = "pref_allow_use_mobile_data";
	
	public static final String PREF_CHECK_FOR_UPDATES_ON_START = "pref_check_for_updates_on_start";
	
	public static final String PREF_ALLOW_ACCOUNT_CREATION_ON_LOGIN_SCREEN = "pref_allow_account_creation_on_login_screen";
	
	public static final String PREF_IS_TEACHER = "pref_is_teacher";
	
	public static final String PREF_VALUE_LAST_BOOK_LIST_VIEW = "LAST_BOOK_LIST_VIEW_WAS_LIST";
	
	public static final String PREF_ACCEPTED_VERSION = "ACCEPTED_VERSION";
	
	public static final String KEY_FONT_SIZE = "reader_font_size";
	
	public static final String PLATFORM_CSS = "content/A_mobile_app.css";
	
	public static final String GENERAL_CSS = "content/mobile_app.css";
	
	public static final String PLATFORM_JS = "content/js/device/A_device.js";
	
	public static final String JS_TARGET = "content/js/device/device.js";
	
	public static final String RE_ZIP = "(?i)zip-(480|980|1440|1920)";
	
	public static final String RE_COVER = "(?i)(jpg|jpeg|png)-(480|980|1440|1920)";
	
	public static final String RE_RES_INT = "\\d+$";
	
	public static final String RE_USER_NAME = "(?=^.{4,10}$)^([a-zA-Z0-9]\\.?)+$";
	
	public static final int MIN_PASSWORD_LENGTH = 6;
	
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	
	public static final String CONTENT_TYPE_JSON = "application/json";		
	
	private Constants(){		
	}	
	
}
