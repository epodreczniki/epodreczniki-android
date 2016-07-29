package pl.epodreczniki.model;

public interface Navigable {

	void showPage(String pageId, String mdContentId, Integer mdVersion);
	
	void showPage(String pageId, String mdContentId, Integer mdVersion, String noteId);
	
	void onTocDetached();
	
	void showNoteContent(Note note);
	
}
