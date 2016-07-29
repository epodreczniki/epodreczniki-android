package pl.epodreczniki.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipFile;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import pl.epodreczniki.R;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.model.ExerciseState;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.User;

public final class Util {	
	
	public static final String LP_PREFIX = "__lp__";
	
	private Util(){}	
	
	public static int getLongerEdge(Context ctx){
		final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		final Display d = wm.getDefaultDisplay();
		final Point p = new Point();
		d.getSize(p);
		return p.x > p.y ? p.x : p.y;
	}
	
	public static File getExternalStorageBookDir(Context ctx,Book b){				
		return getExternalStorageBookDir(ctx, b.getMdContentId(), b.getMdVersion());
	}
	
	public static File getExternalStorageBookDir(Context ctx,String mdContentId,int mdVersion){
		final String filePath = mdContentId + File.separator + mdVersion;
		final File dir = new File(ctx.getExternalFilesDir(Constants.BOOKS_DIR),filePath);
		return dir;
	}
	
	public static File getExternalStorageDevDir(Context ctx, String timestamp){		
		return new File(ctx.getExternalFilesDir(Constants.DEV_DIR),timestamp);
	}
	
	public static File getExternalStorageDevCssFile(Context ctx, String timestamp){
		return new File(getExternalStorageDevDir(ctx, timestamp),"content/mobile_app.css");
	}
	
	public static File getExternalStorageDevCssTempFile(Context ctx, String timestamp){
		return new File(getExternalStorageDevDir(ctx, timestamp),"content/mobile_app.css.temp");
	}
	
	public static File getExternalStorageDevDir(Context ctx){
		return ctx.getExternalFilesDir(Constants.DEV_DIR);
	}
	
	public static boolean checkIndexEntryFile(Context ctx, String mdContentId, int mdVersion){
		final File base = getExternalStorageBookDir(ctx, mdContentId, mdVersion);
		final File entryFile = new File(base,Constants.INDEX_ENTRY);
		return entryFile.exists()&&entryFile.canRead();
	}
	
	public static boolean checkTocEntryFiles(Context ctx, String mdContentId, int mdVersion){
		final File base = getExternalStorageBookDir(ctx, mdContentId, mdVersion);
		final File toc = new File(base,Constants.TOC_ENTRY);
		final File pages = new File(base,Constants.PAGES_ENTRY);		
		return toc.exists()&&toc.canRead()&&pages.exists()&&pages.canRead();
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static long getAvailableSpace(){
		final StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2){
			return stat.getAvailableBytes();
		}else{
			return (long)stat.getAvailableBlocks()*(long)stat.getBlockSize();
		}
	}
	
	public static void closeSilently(Closeable c){
		if(c!=null){
			try {
				c.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static void closeZipFileSilently(ZipFile z){
		if(z!=null){
			try{
				z.close();
			}catch (IOException e){
			}
		}
	}
	
	public static int getAppVersion(Context ctx){
		int appVersion = -1;
		try {
			final PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			appVersion = info.versionCode;
		} catch (NameNotFoundException e) {
		}
		return appVersion;
	}
	
	public static boolean isTablet(Context ctx){
		return ctx.getResources().getBoolean(R.bool.tablet);
	}	
	
	public static boolean termsAccepted(Context ctx){
		final int appVersion = getAppVersion(ctx);
		final int acceptedVersion = PreferenceManager.getDefaultSharedPreferences(ctx).getInt(Constants.PREF_ACCEPTED_VERSION, 0);
		return appVersion == acceptedVersion;
	}
	
	public static boolean isTeacher(Context ctx){
		if(UserContext.getCurrentUser()!=null){
			return UserContext.getCurrentUser().getSettings().isTeacher();
		}
		return false;				
	}
	
	public static boolean isMobileNetworkAllowed(Context ctx){	
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean res = prefs.getBoolean(Constants.PREF_ALLOW_MOBILE_DATA, false);
		Log.e("UTIL","is mobile allowed: "+res);
		return res;
	}
	
	public static boolean isAccountCreationOnLoginScreenAllowed(Context ctx){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean res = prefs.getBoolean(Constants.PREF_ALLOW_ACCOUNT_CREATION_ON_LOGIN_SCREEN, false);
		Log.e("UTIL","account create allowed: "+res);
		return res;
	}
	
	public static boolean shouldCheckForUpdates(Context ctx){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean res = prefs.getBoolean(Constants.PREF_CHECK_FOR_UPDATES_ON_START, false);
		Log.e("UTIL","should check for updates: "+res);
		return res;
	}
	
	public static boolean isMobileOnlyNetwork(Context ctx){
		boolean isWifiEnabled = false;
		boolean isMobileEnabled = false;
		final ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo info = cm
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if(info!=null){
				isWifiEnabled = info.isConnected();
			}
			info = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if(info!=null){
				isMobileEnabled = info.isConnected();
			}
		}
		return !isWifiEnabled && isMobileEnabled;
	}
	
	public static boolean delete(File f){
		boolean res = true;
		if(f.isDirectory()){
			for(File child : f.listFiles()){
				res&=delete(child);
			}				
		}	
		res&=f.delete();				
		return res;
	}		

	public static Uri getUriForBooks(String mdContentId){
		if(!TextUtils.isEmpty(mdContentId)){
			return new Uri.Builder().scheme(BooksProvider.SCHEME).authority(BooksProvider.AUTHORITY).appendPath(BooksProvider.BOOKS_PATH).appendPath(mdContentId).build();
		}
		return null;
	}
	
	public static Uri getUriForBook(String mdContentId, Integer mdVersion){		
		if(mdVersion != null && mdVersion>0 && !TextUtils.isEmpty(mdContentId)){
			return new Uri.Builder().scheme(BooksProvider.SCHEME).authority(BooksProvider.AUTHORITY).appendPath(BooksProvider.BOOKS_PATH).appendPath(mdContentId).appendPath(BooksProvider.VERSIONS_PATH).appendPath(String.valueOf(mdVersion)).build();
		}			
		return null;
	}
	
	public static String getMdContentId(Cursor c, int position) {
		String res = null;
		if(c!=null){
			final int mdContentIdIdx = c.getColumnIndex(BooksTable.C_MD_CONTENT_ID);
			if(c.moveToPosition(position)){
				res = c.getString(mdContentIdIdx);
			}
		}				
		return res;
	}
	
	public static Integer getMdVersion(Cursor c, int position){
		Integer res = null;
		if(c!=null){
			final int mdVersionIdx = c.getColumnIndex(BooksTable.C_MD_VERSION);
			if(c.moveToPosition(position)){
				res = c.getInt(mdVersionIdx);
			}
		}
		return res;
	}	
	
	public static ContentProviderOperation updateStatusOperation(String mdContentId, Integer mdVersion, BookStatus newStatus){
		if(!TextUtils.isEmpty(mdContentId) && mdVersion!=null){
			return ContentProviderOperation.newUpdate(getUriForBook(mdContentId, mdVersion))
			.withValue(BooksTable.C_STATUS, newStatus.getIntVal())
			.build();
		}		
		return null;
	}
	
	public static ContentProviderOperation deleteOperation(String mdContentId, Integer mdVersion){
		if(!TextUtils.isEmpty(mdContentId) && mdVersion!=null){
			return ContentProviderOperation.newDelete(getUriForBook(mdContentId, mdVersion)).build();
		}
		return null;
	}		
	
	public static int calculatePercentage(Book b){
		final double bytesSoFar = b.getBytesSoFar()!=null?b.getBytesSoFar():-1;
		final double bytesTotal = b.getBytesTotal()!=null?b.getBytesTotal():-1;
		double res = 0;
		if(bytesSoFar>-1 && bytesTotal>0){
			res = 100*bytesSoFar/bytesTotal;
		}
		return (int)res;
	}
	
	public static Book.Builder getBookBuilderFromCursor(Cursor c, int position){
		Book.Builder res = null;
		if(c.moveToPosition(position)){
			res = new Book.Builder();
			res.withMdContentId(c.getString(0));
			res.withMdTitle(c.getString(1));
			res.withMdAbstract(c.getString(2));
			res.withMdPublished(c.getInt(3)==1);
			res.withMdVersion(c.getInt(4));
			res.withMdLanguage(c.getString(5));
			res.withMdLicense(c.getString(6));
			res.withMdCreated(c.getString(7));
			res.withMdRevised(c.getString(8));
			res.withCover(c.getString(9));
			res.withLink(c.getString(10));
			res.withMdSubtitle(c.getString(11));
			res.withAuthors(c.getString(12));
			res.withMainAuthor(c.getString(13));
			res.withEducationLevel(c.getString(14));
			res.withEpClass(c.getInt(15));
			res.withSubject(c.getString(16));
			res.withZip(c.getString(17));
			res.withSizeExtracted(c.getLong(18));
			res.withStatus(c.getInt(19));
			res.withTransferId(c.getLong(20));
			res.withLocalPath(c.getString(21));
			res.withCoverStatus(c.getInt(22));
			res.withCoverTransferId(c.getLong(23));
			res.withCoverLocalPath(c.getString(24));
			res.withBytesSoFar(c.getInt(25));
			res.withBytesTotal(c.getInt(26));
			res.withZipLocal(c.getString(27));
			res.withAppVersion(c.getInt(28));
		}
		return res;
	}
	
	public static User.Builder getUserBuilderFromCursor(Cursor c, int position){
		User.Builder res = null;
		if(c.moveToPosition(position)){
			res = new User.Builder();
			res.withLocalUserId(c.getLong(0))
			.withUserName(c.getString(1))
			.withAccountType(c.getInt(2))
			.withPassword64(c.getString(3))
			.withSalt64(c.getString(4))		
			.withRecoveryQuestion(c.getString(5))
			.withRecoveryAnswer64(c.getString(6))
			.withRecoverySalt64(c.getString(7))
			.withSettings(Settings.fromJsonString(c.getString(8)));
		}
		return res;
	}
	
	public static ExerciseState.Builder getExerciseStateBuilderFromCursor(Cursor c, int position){
		ExerciseState.Builder res = null;
		if(c.moveToPosition(position)){
			res = new ExerciseState.Builder();
			res.withId(c.getLong(0));
			res.withLocalUserId(c.getLong(1));
			res.withWomiId(c.getString(2));
			res.withMdContentId(c.getString(3));
			res.withMdVersion(c.getInt(4));
			res.withValue(c.getString(5));
		}
		return res;
	}
		
	private static int dev;
	
	public static boolean isDev(Context ctx){
		if(dev==0){
			try {
				PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_PERMISSIONS);
				if(pi.permissions!=null){			
					Log.e("UTIL", "this is dev");
					dev = 1;
				}			
			} catch (NameNotFoundException e) {				
			}	
		}
		return dev==1;
	}
	
	public static void storeLastPage(Context ctx, long localUserId, String tocKey, int globalPageIdx){
		SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
		Log.e("UTIL", "storing page for "+localUserId+" "+tocKey);
		ed.putInt(LP_PREFIX+localUserId+":"+tocKey, globalPageIdx);
		ed.apply();		
	}
	
	public static int getLastPage(Context ctx, long localUserId, String tocKey){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Log.e("UTIL", "getting page for "+localUserId+" "+tocKey);
		return prefs.getInt(LP_PREFIX+localUserId+":"+tocKey, 0);		
	}
	
	public static void checkLong(String toCheck){
		try{
			Long.parseLong(toCheck);
		}catch(Exception e){
			throw new IllegalArgumentException("not a number: "+toCheck);
		}
	}
	
	public static String capitalize(String input){
		if(input==null || input.trim().length()<1){
			return input;
		}
		return input.substring(0, 1).toUpperCase(Locale.ENGLISH)+input.substring(1);
	}

}
