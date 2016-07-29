package pl.epodreczniki.activity;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.UpdateDialog;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.AsyncBitmapLoader;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.ImageCache;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import pl.epodreczniki.view.CircularProgressBar;
import pl.epodreczniki.view.ImageProgressBar;
import pl.epodreczniki.view.UpdateMarker;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BookDetailsActivity extends AbstractSingleBookActivity{
	
	private final Map<String,String> EDU_LEVELS = Collections.unmodifiableMap(new HashMap<String,String>(){
		
		private static final long serialVersionUID = 2958245674154551225L;

		{
			put("I", "Podstawowa");
			put("II", "Podstawowa");
			put("III", "Gimnazjum");
			put("IV", "Liceum");
		}
		
	});	
	
	private Book book;
	
	private TextView tvTitle;
	
	private LinearLayout llSchool;
	
	private TextView tvSchool;	
	
	private LinearLayout llClass;
	
	private TextView tvClass;	
	
	private LinearLayout llSubject;
	
	private TextView tvSubject;
	
	private TextView tvAbstractLabel;
	
	private TextView tvAbstract;
	
	private TextView tvAuthorsLabel;
	
	private TextView tvAuthors;	
	
	private TextView tvLicenseLabel;
	
	private TextView tvLicense;
	
	private TextView tvSizeLabel;
	
	private TextView tvSize;
	
	private TextView tvVersionLabel;
	
	private TextView tvVersion;

	private FrameLayout flProgress;
	
	private TextView tvProgressPercent;
	
	private UpdateMarker updateMarker;
	
	private CircularProgressBar pieProgress;
	
	private ImageButton btnRead;
	
	private ImageButton btnDownload;
	
	private ImageButton btnDelete;
	
	private ImageButton btnNotes;
	
	private ImageProgressBar imageProgress;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_book_details);
		Bundle extras = getIntent().getExtras();
		if(extras == null){
			finish();
			return;
		}		
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayUseLogoEnabled(true);		
		
		tvTitle = (TextView) findViewById(R.id.abed_tv_title);
		llSchool = (LinearLayout) findViewById(R.id.abed_ll_school);
		tvSchool = (TextView) findViewById(R.id.abed_tv_school);
		llClass = (LinearLayout) findViewById(R.id.abed_ll_class);
		tvClass = (TextView) findViewById(R.id.abed_tv_class);
		llSubject = (LinearLayout) findViewById(R.id.abed_ll_subject);
		tvSubject = (TextView) findViewById(R.id.abed_tv_subject);
		tvAbstractLabel = (TextView) findViewById(R.id.abed_tv_abstract_label);
		tvAbstract = (TextView) findViewById(R.id.abed_tv_abstract);
		tvAuthorsLabel =  (TextView) findViewById(R.id.abed_tv_authors_label);
		tvAuthors = (TextView) findViewById(R.id.abed_tv_authors);
		tvLicenseLabel = (TextView) findViewById(R.id.abed_tv_license_label);
		tvLicense = (TextView) findViewById(R.id.abed_tv_license);
		tvSizeLabel = (TextView) findViewById(R.id.abed_tv_book_size_label);
		tvSize = (TextView) findViewById(R.id.abed_tv_book_size);
		tvVersionLabel = (TextView) findViewById(R.id.abed_tv_book_version_label);
		tvVersion = (TextView) findViewById(R.id.abed_tv_book_version);
		
		flProgress = (FrameLayout) findViewById(R.id.abed_fl_progress);
		tvProgressPercent = (TextView) findViewById(R.id.abed_tv_progress_percent);
		imageProgress = (ImageProgressBar) findViewById(R.id.abed_vpb_cover);
		imageProgress.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {			
				if(BookStatus.fromInteger(book.getStatus())==BookStatus.READY){
					final Animation btnAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_click);
					btnAnimation.setAnimationListener(new AnimationListener() {
						
						@Override
						public void onAnimationStart(Animation animation) {
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {
						}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							readBook();
						}
					});
					v.startAnimation(btnAnimation);					
				}
			}
		});
		
		btnRead = (ImageButton) findViewById(R.id.abed_btn_read);
		btnDownload = (ImageButton) findViewById(R.id.abed_btn_download);				
		btnDownload.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				final BookStatus status = BookStatus.fromInteger(book.getStatus());
				switch(status){
				case REMOTE:						
					requestTransfer(book);					
					break;
				case DOWNLOADING:
				case UPDATE_DOWNLOADING:
					stop(book);					
					break;
				case READY:
					if(book.isUpdateAvailable()){
						showUpdateDialog();
					}
					break;				
				default:
				}
			}
		});
		
		btnDelete = (ImageButton) findViewById(R.id.abed_btn_delete);
		btnDelete.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(BookDetailsActivity.this).setTitle(R.string.dialog_title_terms_confirm)
					.setMessage(R.string.dialog_message_delete_book_confirm)
					.setPositiveButton(R.string.dialog_ans_yes,new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							pieProgress.setIndeterminate(true);
							pieProgress.setVisibility(View.VISIBLE);
							tvProgressPercent.setVisibility(View.GONE);
							flProgress.setVisibility(View.VISIBLE);
							btnRead.setVisibility(View.GONE);
							btnDelete.setVisibility(View.GONE);
							deleteBook();
							Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_bookDelete), Toast.LENGTH_LONG)
							.show();
						}
					})
					.setNegativeButton(R.string.dialog_ans_no, null).show();
			}
			
		});
		btnRead.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				readBook();
			}
			
		});
	
		
		btnNotes = (ImageButton) findViewById(R.id.abed_btn_notes);
		btnNotes.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final Intent i = new Intent(BookDetailsActivity.this, NoteListActivity.class);
				i.putExtra(NoteListActivity.EXTRA_BOOK, book);
				startActivity(i);
			}
		});
		
		updateMarker = (UpdateMarker) findViewById(R.id.abed_um);
		pieProgress = (CircularProgressBar) findViewById(R.id.abed_pp_extract);		
	}		
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		}
		return (super.onOptionsItemSelected(item));
	}

	@Override
	protected void onBookUpdate(Book book) {
		this.book = book;
		
		tvTitle.setText(book.getMdTitle());
		
		imageProgress.setContentDescription(book.getMdTitle());
		
		if(!TextUtils.isEmpty(book.getCoverLocalPath())){			
			final String coverPath = Uri.parse(book.getCoverLocalPath()).getPath();
			Bitmap cover = ImageCache.get(this).get(coverPath);
			if(cover!=null){
				imageProgress.setImageBitmap(cover);
			}else{							
				imageProgress.setImageDrawable(getResources().getDrawable(R.drawable.cover));
				new AsyncBitmapLoader(this,imageProgress).execute(coverPath);
			}
		}else{
			imageProgress.setImageDrawable(getResources().getDrawable(R.drawable.cover));
		}				
		 
		if(!TextUtils.isEmpty(book.getEducationLevel())){
			llSchool.setVisibility(View.VISIBLE);
			final String edLevel = EDU_LEVELS.get(book.getEducationLevel());
			tvSchool.setText(" "+(edLevel!=null?edLevel:""));
		}else{
			llSchool.setVisibility(View.GONE);
		}
		
		if(book.getEpClass()!=null && book.getEpClass()>0){
			llClass.setVisibility(View.VISIBLE);
			tvClass.setText(" "+book.getEpClass());
		}else{
			llClass.setVisibility(View.GONE);
		}
		
		if(!TextUtils.isEmpty(book.getSubject())){
			llSubject.setVisibility(View.VISIBLE);
			tvSubject.setText(" "+Util.capitalize(book.getSubject()));
		}else{
			llSubject.setVisibility(View.GONE);
		}
		
		if(!TextUtils.isEmpty(book.getMdAbstract())){
			tvAbstractLabel.setVisibility(View.VISIBLE);
			tvAbstract.setVisibility(View.VISIBLE);
			tvAbstract.setText(book.getMdAbstract());
		}else{
			tvAbstractLabel.setVisibility(View.GONE);
			tvAbstract.setVisibility(View.GONE);
		}
		
		if(!TextUtils.isEmpty(book.getAuthors())){			
			tvAuthors.setVisibility(View.VISIBLE);
			if(book.getAuthors().lastIndexOf("<b>")>-1){
				tvAuthorsLabel.setVisibility(View.GONE);
			}else{
				tvAuthorsLabel.setVisibility(View.VISIBLE);
			}			
			tvAuthors.setText(Html.fromHtml(book.getAuthors()));
		}else{
			tvAuthorsLabel.setVisibility(View.GONE);
			tvAuthors.setVisibility(View.GONE);			
		}
		
		if(!TextUtils.isEmpty(book.getMdLicense())){
			tvLicenseLabel.setVisibility(View.VISIBLE);
			tvLicense.setVisibility(View.VISIBLE);
			tvLicense.setText(Html.fromHtml(String.format(Constants.LINK_WRAP,book.getMdLicense(),book.getMdLicense())));
			tvLicense.setMovementMethod(LinkMovementMethod.getInstance());
		}else{
			tvLicenseLabel.setVisibility(View.GONE);
			tvLicense.setVisibility(View.GONE);
		}
				
		if(book.getSizeExtracted()!=null){
			tvSizeLabel.setVisibility(View.VISIBLE);
			tvSize.setVisibility(View.VISIBLE);
			tvSize.setText(String.format("%.2f MB", (float)book.getSizeExtracted()/(1024*1024)));
		}else{
			tvSizeLabel.setVisibility(View.GONE);
			tvSize.setVisibility(View.GONE);
		}
		
		if(book.getMdVersion()!=null){
			tvVersionLabel.setVisibility(View.VISIBLE);
			tvVersion.setVisibility(View.VISIBLE);
			tvVersion.setText(""+book.getMdVersion());
		}else{
			tvVersionLabel.setVisibility(View.GONE);
			tvVersion.setVisibility(View.GONE);
		}
		
		final BookStatus status = BookStatus.fromInteger(book.getStatus());
		switch(status){
		case REMOTE:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setVisibility(View.VISIBLE);
			btnDownload.setImageResource(Util.isTablet(this)?R.drawable.ico_download_med:R.drawable.ico_download_small);
			btnDownload.setEnabled(true);
			btnRead.setVisibility(View.GONE);
			btnNotes.setVisibility(View.GONE);
			flProgress.setVisibility(View.GONE);
			imageProgress.setProgress(0);
			updateMarker.setVisibility(View.GONE);
			pieProgress.setVisibility(View.GONE);
			break;
		case DOWNLOADING:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setImageResource(Util.isTablet(this)?R.drawable.ico_stop_med:R.drawable.ico_stop_small);
			btnDownload.setVisibility(View.VISIBLE);
			btnDownload.setEnabled(true);
			btnRead.setVisibility(View.GONE);
			btnNotes.setVisibility(View.GONE);
			flProgress.setVisibility(View.VISIBLE);
			pieProgress.setIndeterminate(true);
			pieProgress.setVisibility(View.VISIBLE);			
			tvProgressPercent.setVisibility(View.VISIBLE);
			final int downloadPercent = Util.calculatePercentage(book);
			imageProgress.setProgress(downloadPercent);
			tvProgressPercent.setText(Html.fromHtml("<big>" + downloadPercent + "</big>" +  "<small>%</small>" ));
			updateMarker.setVisibility(View.GONE);
			break;
		case EXTRACTING:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setVisibility(View.GONE);
			btnRead.setVisibility(View.GONE);
			btnNotes.setVisibility(View.GONE);
			flProgress.setVisibility(View.VISIBLE);
			tvProgressPercent.setVisibility(View.GONE);
			imageProgress.setProgress(100);
			updateMarker.setVisibility(View.GONE);
			pieProgress.setIndeterminate(false);
			pieProgress.setVisibility(View.VISIBLE);
			pieProgress.setProgress(book.getBytesSoFar());
			break;
		case READY:
			if(book.isUpdateAvailable()){
				btnDownload.setVisibility(View.VISIBLE);
				btnDownload.setImageResource(Util.isTablet(this)?R.drawable.ico_refresh_med:R.drawable.ico_refresh_small);
				btnDownload.setEnabled(true);
				updateMarker.setVisibility(View.VISIBLE);
			}else{
				btnDownload.setVisibility(View.GONE);				
				updateMarker.setVisibility(View.GONE);
			}
			btnDelete.setVisibility(View.VISIBLE);
			btnRead.setVisibility(View.VISIBLE);
			btnNotes.setVisibility(View.VISIBLE);
			flProgress.setVisibility(View.GONE);
			imageProgress.setProgress(100);
			pieProgress.setVisibility(View.GONE);
			break;
		case UPDATE_DOWNLOADING:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setImageResource(Util.isTablet(this)?R.drawable.ico_stop_med:R.drawable.ico_stop_small);
			btnDownload.setVisibility(View.VISIBLE);	
			btnDownload.setEnabled(true);
			btnRead.setVisibility(View.GONE);
			btnNotes.setVisibility(View.GONE);
			flProgress.setVisibility(View.VISIBLE);
			pieProgress.setIndeterminate(true);
			pieProgress.setVisibility(View.VISIBLE);			
			tvProgressPercent.setVisibility(View.VISIBLE);
			final Book bookUpdate = book.getVersions().size()>0?book.getVersions().get(0):null;
			if(bookUpdate!=null){
				final int updatePercent = Util.calculatePercentage(bookUpdate);
				imageProgress.setProgress(updatePercent);
				tvProgressPercent.setText(Html.fromHtml("<big>" + updatePercent + "</big>" +  "<small>%</small>" ));
			}else{
				Log.e("BookDetailsActivity","new version of book is null, this should never happen");
			}
			updateMarker.setVisibility(View.VISIBLE);
			break;
		case UPDATE_EXTRACTING:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setVisibility(View.GONE);
			btnRead.setVisibility(View.GONE);
			btnNotes.setVisibility(View.GONE);
			flProgress.setVisibility(View.VISIBLE);
			tvProgressPercent.setVisibility(View.GONE);
			imageProgress.setProgress(100);
			updateMarker.setVisibility(View.VISIBLE);			
			final Book bookUpdateE = book.getVersions().size()>0?book.getVersions().get(0):null;
			if(bookUpdateE!=null){
				pieProgress.setIndeterminate(false);
				pieProgress.setVisibility(View.VISIBLE);
				pieProgress.setProgress(bookUpdateE.getBytesSoFar());				
			}
			break;
		case UPDATE_DELETING:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setVisibility(View.GONE);
			btnRead.setVisibility(View.VISIBLE);
			btnNotes.setVisibility(View.VISIBLE);
			pieProgress.setIndeterminate(true);
			pieProgress.setVisibility(View.VISIBLE);
			tvProgressPercent.setVisibility(View.GONE);
			imageProgress.setProgress(100);
			updateMarker.setVisibility(View.VISIBLE);		
			break;
		case DELETING:
		default:
			btnDelete.setVisibility(View.GONE);
			btnDownload.setVisibility(View.GONE);
			btnRead.setVisibility(View.GONE);
			btnNotes.setVisibility(View.GONE);
			flProgress.setVisibility(View.VISIBLE);
			pieProgress.setIndeterminate(true);
			pieProgress.setVisibility(View.VISIBLE);
			tvProgressPercent.setVisibility(View.GONE);
			imageProgress.setProgress(0);
			updateMarker.setVisibility(View.GONE);										
		}
		disableButtons();
	}
	
	private void disableButtons(){
		if(checkUserLoggedIn()){
			btnDownload.setEnabled(UserContext.getCurrentUser().canManageBooks());
			btnDelete.setEnabled(UserContext.getCurrentUser().canManageBooks());	
		}		
	}
	
	private void deleteBook(){
		delete(book);
	}
	
	private void readBook() {
		if (BookStatus.fromInteger(book.getStatus()).equals(BookStatus.READY)) {
			if(Util.checkTocEntryFiles(this, book.getMdContentId(), book.getMdVersion())){
				final Intent i = new Intent(BookDetailsActivity.this,SwipeReaderActivity.class);
				i.putExtra(SwipeReaderActivity.EXTRA_BOOK, book);
				startActivity(i);
			}else if(Util.checkIndexEntryFile(this, book.getMdContentId(), book.getMdVersion())){
				final Intent i = new Intent(BookDetailsActivity.this,ReaderActivity.class);
				String base = "file://"+book.getLocalPath();				
				if(!base.endsWith("index.html")){
					base+=File.separator+Constants.INDEX_ENTRY;
				}				
				Uri u = Uri.parse(base);
				i.putExtra(ReaderActivity.EXTRA_URL, u.toString());
				i.putExtra(ReaderActivity.BOOK_TITLE, book.getMdTitle());
				startActivity(i);
			}else{
				Toast.makeText(this, "Nieznany format e-podręcznika",  Toast.LENGTH_LONG).show();
			}			
			
		}
	}
	
	public void requestXfer(Book b){
		requestTransfer(b);
	}
	
	private void showUpdateDialog(){		
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(UpdateDialog.TAG);
		if(prev!=null){
			ft.remove(prev);			
		}
		ft.addToBackStack(null);
		ft.commit();
		final UpdateDialog dial = UpdateDialog.newInstance(book);		
		dial.show(getFragmentManager(), UpdateDialog.TAG);
	}		

}
