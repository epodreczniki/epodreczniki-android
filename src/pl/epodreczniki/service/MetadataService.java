package pl.epodreczniki.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import pl.epodreczniki.R;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.model.CoverStatus;
import pl.epodreczniki.model.JSONBook;
import pl.epodreczniki.model.JSONBook.JSONAuthor;
import pl.epodreczniki.model.JSONBook.JSONFormat;
import pl.epodreczniki.util.ConnectionDetector;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.TransferHelper;
import pl.epodreczniki.util.Util;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class MetadataService extends IntentService {

	public static final String ACTION_STATUS_CHANGED = "pl.epodreczniki.service.MetadataService.response";
	
	private static final String KEY_ROLELESS = "___bezrolny___obyniktniezdefiniowaltakiejroli";
		
	private static volatile boolean inProgress = false;
	
	private Handler handler;

	public MetadataService() {
		super("MetadataService");
	}

	public synchronized static boolean isInProgress() {
		return inProgress;
	}
		
	@Override
	public void onCreate() {	
		super.onCreate();
		handler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent intent) {		
		try {
			Log.e("MS","getting data");
			changeStatus(true);
			final String jsonData = TransferHelper.getMetaData();
			final Collection<JSONBook> books = parseBooks(jsonData);
			final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
			for (JSONBook b : books) {
				operations.addAll(processBook(b));
			}
			if (operations.size() > 0) {
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, operations);
			}
			
			Set<String> mdContentIdFromJson = new HashSet<String>();
			for(JSONBook b : books){
				mdContentIdFromJson.add(b.getMdContentId());
			}
			ArrayList<ContentProviderOperation> delCpos = new ArrayList<ContentProviderOperation>();
			Cursor c  = getContentResolver().query(BooksProvider.BOOKS_URI, new String[]{BooksTable.C_MD_CONTENT_ID,BooksTable.C_STATUS}, null, null, null);
			if(c!=null){
				while(c.moveToNext()){
					String mdContentId = c.getString(0);
					BookStatus bs = BookStatus.fromInteger(c.getInt(1));
					if(bs==BookStatus.REMOTE && !mdContentIdFromJson.contains(mdContentId)){						
						delCpos.add(ContentProviderOperation.newDelete(Util.getUriForBooks(mdContentId)).build());
					}
				}
				c.close();
			}
			if(delCpos.size() > 0){
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, delCpos);
			}
		} catch (Exception e) {		
			showToast(R.string.ms_toast_downloadingBookFailed,Toast.LENGTH_LONG);			
			Log.e("GET DATA", e.toString());
		} finally {			
			changeStatus(false);			
			requestCovers();	
			ConnectionDetector.checkDb(this);
		}		
	}
	
	private void showToast(final int message, final int duration){		
		showToast(getResources().getString(message),duration);		
	}
	
	private void showToast(final String message, final int duration){
		if(message!=null){
			handler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(MetadataService.this, message, duration).show();
				}			
			});			
		}		
	}

	private List<ContentProviderOperation> processBook(JSONBook apiBook) {
		final List<ContentProviderOperation> res = new ArrayList<ContentProviderOperation>();
		if (apiBook.getMdVersion() != null) {
			final Uri uri = Util.getUriForBooks(apiBook.getMdContentId());
			final Cursor c = getContentResolver().query(uri, BooksTable.COLUMNS, null, null,
					BooksTable.C_STATUS + " DESC, " + BooksTable.C_MD_VERSION + " ASC");
			if (c != null) {
				Book main = null;
				Book second = null;
				Book third = null;
				switch (c.getCount()) {
				case 0:
					res.add(toInsertOperation(apiBook));
					break;
				case 1:
					main = Util.getBookBuilderFromCursor(c, 0).build();
					if (main.getMdVersion() != null) {
						if (main.getMdVersion() < apiBook.getMdVersion()) {
							if (BookStatus.fromInteger(main.getStatus()) == BookStatus.REMOTE) {
								final ContentProviderOperation deleteMain = Util.deleteOperation(main.getMdContentId(), main.getMdVersion());
								final ContentProviderOperation newMain = toInsertOperation(apiBook);
								if (deleteMain != null && newMain != null) {
									res.add(deleteMain);
									res.add(newMain);
								}
							} else {
								res.add(toInsertOperation(apiBook));
							}
						}
					}
					break;
				case 2:
					main = Util.getBookBuilderFromCursor(c, 0).build();
					second = Util.getBookBuilderFromCursor(c, 1).build();
					if (second.getMdVersion() != null) {
						if (second.getMdVersion() < apiBook.getMdVersion()) {
							if (BookStatus.fromInteger(second.getStatus()) == BookStatus.REMOTE) {
								final ContentProviderOperation deleteSecond = Util.deleteOperation(second.getMdContentId(), second.getMdVersion());
								final ContentProviderOperation newSecond = toInsertOperation(apiBook);
								if (deleteSecond != null && newSecond != null) {
									res.add(deleteSecond);
									res.add(newSecond);
								}
							} else {
								res.add(toInsertOperation(apiBook));
							}
						}
					}
					break;
				case 3:
					main = Util.getBookBuilderFromCursor(c, 0).build();
					second = Util.getBookBuilderFromCursor(c, 1).build();
					third = Util.getBookBuilderFromCursor(c, 2).build();
					if (third.getMdVersion() != null) {
						if (third.getMdVersion() < apiBook.getMdVersion()) {
							final ContentProviderOperation deleteThird = Util.deleteOperation(third.getMdContentId(), third.getMdVersion());
							final ContentProviderOperation newThird = toInsertOperation(apiBook);
							if (deleteThird != null && newThird != null) {
								res.add(deleteThird);
								res.add(newThird);
							}
						}
					}
					break;
				default:
					Log.e("MS", "this should not happen");
				}
				c.close();
			}
		}

		return res;
	}

	private void changeStatus(boolean newInProgress) {
		synchronized(MetadataService.class){
			inProgress = newInProgress;			
		}			
		Intent intent = new Intent(ACTION_STATUS_CHANGED);			
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void requestCovers() {
		Cursor c = getContentResolver().query(BooksProvider.BOOKS_URI, BooksTable.COLUMNS, BooksTable.C_COVER_STATUS + "=?",
				new String[] { CoverStatus.REMOTE.getStringVal() }, null);
		List<Book> booksWithoutCovers = new ArrayList<Book>();
		if (c != null) {
			while (c.moveToNext()) {
				booksWithoutCovers.add(Util.getBookBuilderFromCursor(c, c.getPosition()).build());
			}
			c.close();
		}
		for (Book b : booksWithoutCovers) {
			requestCoverDownload(b);
		}
	}

	private void requestCoverDownload(Book b) {
		try{
			TransferHelper.requestCoverDownload(this, b);
		}catch(Exception e){
			Log.e("MS","DownloadManager is probably disabled, ignore for now");
		}
	}	

	private static Collection<JSONBook> parseBooks(String jsonBooks) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Type collectionType = new TypeToken<Collection<JSONBook>>() {
		}.getType();
		return gson.fromJson(jsonBooks, collectionType);
	}		
	
	private Map<String,List<JSONAuthor>> authorsByRoles(List<JSONAuthor> allAuthors){
		Map<String,List<JSONAuthor>> res = new HashMap<String, List<JSONAuthor>>();
		for(JSONAuthor auth : allAuthors){
			final String key = TextUtils.isEmpty(auth.getRoleType())?KEY_ROLELESS:auth.getRoleType();
			if(!res.containsKey(key)){
				res.put(key, new ArrayList<JSONAuthor>());
			}
			res.get(key).add(auth);
		}
		return res;
	}

	private ContentProviderOperation toInsertOperation(JSONBook book) {
		if (book == null) {
			return null;
		}

		int longerEdge = Util.getLongerEdge(this);
		StringBuilder authorsStringBuilder = new StringBuilder();
		String mainAuthor = null;
		if (book.getMdAuthors() != null) {		
			final JSONAuthor.JSONAuthorComparator cmp = new JSONAuthor.JSONAuthorComparator();
			Map<String,List<JSONAuthor>> byRoles = authorsByRoles(book.getMdAuthors());
			for(Map.Entry<String,List<JSONAuthor>> entry: byRoles.entrySet()){
				final String roleName = entry.getKey();
				if(!roleName.equals(KEY_ROLELESS)){
					authorsStringBuilder.append("<b>").append(roleName).append("</b><br />");
				}else{
					authorsStringBuilder.append("<b>").append("Autorzy").append("</b><br />");
				}	
				Collections.sort(entry.getValue(), cmp);
				for(JSONAuthor auth : entry.getValue()){
					final String fullName = TextUtils.isEmpty(auth.getMdFullName())?auth.getFullName():auth.getMdFullName();
					if(!TextUtils.isEmpty(fullName)){						
						authorsStringBuilder.append(fullName);
						authorsStringBuilder.append(", ");
						if(mainAuthor == null){
							mainAuthor = fullName;
						}
					}
				}
				if (authorsStringBuilder.toString().endsWith(", ")) {
					authorsStringBuilder.replace(authorsStringBuilder.length()-2, authorsStringBuilder.length(), "<br />");
				}
			}				
		}

		String educationLevel = null;
		Integer epClass = null;
		if (book.getMdSchool() != null) {
			educationLevel = book.getMdSchool().getMdEducationLevel();
			epClass = book.getMdSchool().getEpClass();
		}

		String subject = null;
		if (book.getMdSubject() != null) {
			subject = book.getMdSubject().getMdName();
		}

		String zip = null;
		Long extractedSize = null;
		if (book.getFormats() != null) {
			Map<Integer, UrlAndSize> resolutions = new TreeMap<Integer, UrlAndSize>();
			for (JSONFormat format : book.getFormats()) {
				final String formatStr = format.getFormat();
				if (!TextUtils.isEmpty(formatStr)) {
					if (formatStr.matches(Constants.RE_ZIP)) {
						final int resolution = Integer.parseInt(formatStr.split("-")[1]);
						final String url = format.getUrl();
						final Long size = format.getSize();
						resolutions.put(resolution, new UrlAndSize(url, size));
					}
				}
			}
			int chosen = 0;
			for (Map.Entry<Integer, UrlAndSize> entry : resolutions.entrySet()) {
				chosen = chosen == 0 ? entry.getKey() : (entry.getKey() <= longerEdge ? entry.getKey() : chosen);
			}
			final UrlAndSize choice = resolutions.get(chosen);
			zip = choice.url;
			extractedSize = choice.size;
		}

		String cover = null;
		if (book.getCovers() != null) {
			Map<Integer, String> resolutions = new TreeMap<Integer, String>();
			for (JSONFormat cvr : book.getCovers()) {
				final String formatStr = cvr.getFormat();
				if (!TextUtils.isEmpty(formatStr)) {
					if (formatStr.matches(Constants.RE_COVER)) {
						final int resolution = Integer.parseInt(formatStr.split("-")[1]);
						final String url = cvr.getUrl();
						resolutions.put(resolution, url);
					}
				}
			}
			int chosen = 0;
			for (Map.Entry<Integer, String> entry : resolutions.entrySet()) {
				chosen = chosen == 0 ? entry.getKey() : (entry.getKey() <= longerEdge ? entry.getKey() : chosen);
			}
			Log.e("MDS", "chosen cover resolution: " + chosen);
			cover = resolutions.get(chosen);
		}
		
		
		ContentProviderOperation.Builder res = ContentProviderOperation.newInsert(BooksProvider.BOOKS_URI).withValue(BooksTable.C_MD_CONTENT_ID, book.getMdContentId())
				.withValue(BooksTable.C_MD_TITLE, book.getMdTitle()).withValue(BooksTable.C_MD_ABSTRACT, book.getMdAbstract())
				.withValue(BooksTable.C_MD_PUBLISHED, book.getMdPublished() == null ? 0 : book.getMdPublished() ? 1 : 0)
				.withValue(BooksTable.C_MD_VERSION, book.getMdVersion()).withValue(BooksTable.C_MD_LANGUAGE, book.getMdLanguage())
				.withValue(BooksTable.C_MD_LICENSE, book.getMdLicense()).withValue(BooksTable.C_MD_CREATED, book.getMdCreated())
				.withValue(BooksTable.C_MD_REVISED, book.getMdRevised()).withValue(BooksTable.C_COVER, cover)
				.withValue(BooksTable.C_LINK, book.getLink()).withValue(BooksTable.C_MD_SUBTITLE, book.getMdSubtitle())
				.withValue(BooksTable.C_AUTHORS, authorsStringBuilder.toString()).withValue(BooksTable.C_MAIN_AUTHOR, mainAuthor)
				.withValue(BooksTable.C_EDUCATION_LEVEL, educationLevel).withValue(BooksTable.C_CLASS, epClass)
				.withValue(BooksTable.C_SUBJECT, subject).withValue(BooksTable.C_ZIP, zip).withValue(BooksTable.C_EXTRACTED_SIZE, extractedSize);
		if(book.getAppVersion()!=null){			
			return res.withValue(BooksTable.C_APP_VERSION, book.getAppVersion()).build();
		}else{			
			return res.build();
		}
	}

	private static class UrlAndSize {
		public final String url;
		public final Long size;

		UrlAndSize(String url, Long size) {
			this.url = url;
			this.size = size;
		}
	}

}
