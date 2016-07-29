package pl.epodreczniki.util;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;

import pl.epodreczniki.R;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.model.Note.LocationPart;

public final class NotesUtil {

	private static final Gson GSON = new Gson();

	private NotesUtil() {
	}

	public static String noteToJsonString(Note note) {
		return GSON.toJson(note);
	}

	public static Note.Builder getNoteBuilderFromCursor(Cursor c, int position) {
		Note.Builder res = null;
		if (c.moveToPosition(position)) {
			res = new Note.Builder();
			res.withId(c.getLong(0));
			res.withLocalNoteId(c.getString(1));
			res.withLocalUserId(c.getLong(2));
			res.withNoteId(c.getString(3));
			res.withUserId(c.getString(4));
			res.withHandbookId(c.getString(5));
			res.withModuleId(c.getString(6));
			res.withPageId(c.getString(7));
			res.withPageIdx(c.getInt(8));
			LocationPart[] loc = noteLocationFromString(c.getString(9));
			for (LocationPart lp : loc) {
				res.withLocationPart(lp);
			}
			res.withSubject(c.getString(10));
			res.withValue(c.getString(11));
			res.withType(c.getInt(12));
			res.withAccepted(c.getInt(13) != 0);
			res.withReferenceTo(c.getString(14));
			res.withReferencedBy(c.getString(15));
			res.withModifyTime(c.getString(16));
		}
		return res;
	}

	public static String getNotesForUserBookModuleAsString(Context ctx,
			long localUserId, String mdContentId, int mdVersion,
			String mdModuleId) {
		String res = null;
		final Cursor c = ctx.getContentResolver().query(
				NotesProvider.UriHelper.noteByLocalUserBookModule(localUserId,
						mdContentId, mdVersion, mdModuleId),
				NotesTable.COLUMNS, null, null, null);
		if (c != null) {
			List<Note> notes = new ArrayList<Note>();
			while (c.moveToNext()) {
				final Note note = NotesUtil.getNoteBuilderFromCursor(c,
						c.getPosition()).build();
				notes.add(note);
			}
			res = GSON.toJson(notes);
			c.close();
		}
		return res;
	}

	public static String getNotesForUserBookPageAsString(Context ctx,
			long localUserId, String mdContentId, int mdVersion, String pageId) {
		String res = null;
		final Cursor c = ctx.getContentResolver().query(
				NotesProvider.UriHelper.noteByLocalUserBookPage(localUserId,
						mdContentId, mdVersion, pageId), NotesTable.COLUMNS,
				null, null, null);
		if (c != null) {
			List<Note> notes = new ArrayList<Note>();
			while (c.moveToNext()) {
				final Note note = NotesUtil.getNoteBuilderFromCursor(c,
						c.getPosition()).build();
				notes.add(note);
			}
			res = notes.size() > 0 ? GSON.toJson(notes) : "";
			c.close();
		}
		return res;
	}

	public static LocationPart[] noteLocationFromString(String locationJson) {
		try {
			return GSON.fromJson(locationJson, LocationPart[].class);
		} catch (Exception e) {
			Log.e("NOTE_UTIL", "unable to parse note location: " + locationJson);
			return null;
		}
	}

	public static String noteLocationToJsonString(LocationPart[] location) {
		return GSON.toJson(location);
	}

	public static String[] noteIdsFromString(String noteIdList) {
		try {
			return GSON.fromJson(noteIdList, String[].class);
		} catch (Exception e) {
			Log.e("NOTE_UTIL", "unable to parse note id list: " + noteIdList);
			return null;
		}
	}

	public static String stringArrayToJson(String[] list) {
		return GSON.toJson(list);
	}

	public static ContentValues noteToContentValues(Note note) {
		final ContentValues res = new ContentValues();
		res.put(NotesTable.C_LOCAL_NOTE_ID, note.getLocalNoteId());
		res.put(NotesTable.C_LOCAL_USER_ID, note.getLocalUserId());
		res.put(NotesTable.C_NOTE_ID, note.getNoteId());
		res.put(NotesTable.C_USER_ID, note.getUserId());
		res.put(NotesTable.C_HANDBOOK_ID, note.getHandbookId());
		res.put(NotesTable.C_MODULE_ID, note.getModuleId());
		res.put(NotesTable.C_PAGE_ID, note.getPageId());
		res.put(NotesTable.C_PAGE_IDX, note.getPageIdx());
		res.put(NotesTable.C_LOCATION,
				noteLocationToJsonString(note.getLocation()));
		res.put(NotesTable.C_SUBJECT, note.getSubject());
		res.put(NotesTable.C_VALUE, note.getValue());
		res.put(NotesTable.C_TYPE, note.getType());
		res.put(NotesTable.C_ACCEPTED,
				note.getAccepted() != null && note.getAccepted() ? "1" : "0");
		res.put(NotesTable.C_REFERENCE_TO, note.getReferenceTo());
		res.put(NotesTable.C_REFERENCED_BY, note.getReferencedBy());
		res.put(NotesTable.C_MODIFY_TIME, note.getModifyTime());
		return res;
	}

	public static String getMdContentIdFromHandbookId(String handbookId) {
		String[] res = handbookId.split(":");
		if (res.length == 2) {
			return res[0];
		}
		return null;
	}

	public static int getMdVersionFromHandbookId(String handbookId) {
		String[] res = handbookId.split(":");
		if (res.length == 2) {
			try {
				return Integer.parseInt(res[1]);
			} catch (Exception e) {
			}
		}
		return -1;
	}

	public static int getColorIdForNoteType(int noteType) {
		switch (noteType) {
		case 0:
			return R.color.bookmark_green;
		case 1:
			return R.color.bookmark_blue;
		case 2:
			return R.color.bookmark_red;			
		case 3:
			return R.color.bookmark_yellow;
		case 4:
			return R.color.note_green;
		case 5:
			return R.color.note_blue;
		case 6:
			return R.color.note_red;
		default:
			return 0xff0000;
		}
	}

}
