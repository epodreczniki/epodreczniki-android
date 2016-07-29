package pl.epodreczniki.activity;

import java.io.File;
import java.util.List;

import pl.epodreczniki.R;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import pl.epodreczniki.view.BackgroundProgressBar;
import pl.epodreczniki.view.UpdateMarker;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BookListActivity extends AbstractBookListActivity {

	private BookListAdapter adapter;
	
	private TextView tvNoBooks;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_book_list);
		tvNoBooks = (TextView) findViewById(R.id.abl_tv_no_books);				
		adapter = new BookListAdapter(this);
		final ListView list = (ListView) findViewById(R.id.abl_lv);
		list.setAdapter(adapter);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setDisplayUseLogoEnabled(true);		
	}	

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem item = menu.findItem(R.id.m_viewChanger);
		item.setTitle(R.string.mbl_viewCovers);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId()){
		case R.id.m_viewChanger:
			startActivity(new Intent(this,BookPagerActivity.class));
			finish();
			break;
		}
		return true;
	}	

	@Override
	protected void onBooksUpdate(List<Book> books) {
		adapter.updateBooks(books);
		tvNoBooks.setVisibility(books.size()>0?View.GONE:View.VISIBLE);		
	}

	private static class BookListAdapter extends BaseAdapter {

		private final Context ctx;

		private List<Book> data;

		BookListAdapter(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public int getCount() {
			return data == null ? 0 : data.size();
		}

		@Override
		public Book getItem(int position) {
			return data != null && position < data.size() ? data.get(position) : null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Book book = getItem(position);
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				final LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.v_book_list_item, parent, false);
				holder.bpb = (BackgroundProgressBar) convertView.findViewById(R.id.vbli_bpb);
				holder.title = (TextView) convertView.findViewById(R.id.vbli_tv);
				holder.details = (ImageButton) convertView.findViewById(R.id.vbli_ib_details);
				holder.um = (UpdateMarker) convertView.findViewById(R.id.vbli_um);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			if(book.getSubject()!=null && !TextUtils.isEmpty(book.getSubject().trim())){
				holder.title.setText(Util.capitalize(book.getSubject())+" : "+book.getMdTitle());
			}else{
				holder.title.setText(book.getMdTitle());
			}
			holder.title.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (BookStatus.READY.equals(BookStatus.fromInteger(book.getStatus()))) {
						if(Util.checkTocEntryFiles(ctx, book.getMdContentId(), book.getMdVersion())){
							final Intent i = new Intent(ctx,SwipeReaderActivity.class);
							i.putExtra(SwipeReaderActivity.EXTRA_BOOK, book);
							ctx.startActivity(i);
						}else if(Util.checkIndexEntryFile(ctx, book.getMdContentId(), book.getMdVersion())){
							final Intent i = new Intent(ctx, ReaderActivity.class);
							String base = "file://"+book.getLocalPath();
							if(!base.endsWith("index.html")){
								base+=File.separator+Constants.INDEX_ENTRY;
							}
							final Uri u = Uri.parse(base);
							i.putExtra(ReaderActivity.EXTRA_URL, u.toString());
							i.putExtra(ReaderActivity.BOOK_TITLE, book.getMdTitle());
							ctx.startActivity(i);
						}else{
							Toast.makeText(ctx, "Nieznany format e-podręcznika",  Toast.LENGTH_LONG).show();
						}
						
					}else{			
						openDetails(book.getMdContentId(),book.getMdVersion());
					}
				}

			});
			holder.details.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {					
					openDetails(book.getMdContentId(),book.getMdVersion());					
				}

			});

			final BookStatus status = BookStatus.fromInteger(book.getStatus());
			holder.um.setVisibility(book.isUpdateAvailable()?View.VISIBLE:View.GONE);
			holder.bpb.setVisibility(View.INVISIBLE);
			switch (status) {
			case REMOTE:
				holder.title.setAlpha(0.4f);
				holder.bpb.reset();
				break;
			case DOWNLOADING:
				holder.title.setAlpha(1f);
				holder.bpb.setVisibility(View.VISIBLE);
				holder.bpb.setProgress(Util.calculatePercentage(book));				
				break;
			case EXTRACTING:
				holder.title.setAlpha(1f);				
				holder.bpb.setVisibility(View.VISIBLE);
				holder.bpb.setExtractProgress(book.getBytesSoFar());
				break;
			case READY:
				holder.title.setAlpha(1f);
				holder.bpb.reset();
				break;		
			case UPDATE_DOWNLOADING:
				holder.title.setAlpha(1f);
				holder.bpb.setVisibility(View.VISIBLE);
				holder.bpb.setProgress(Util.calculatePercentage(book.getVersions().get(0)));
				break;
			case UPDATE_EXTRACTING:
				holder.title.setAlpha(1f);
				holder.bpb.setVisibility(View.VISIBLE);
				holder.bpb.setExtractProgress(book.getBytesSoFar());
				break;
			case UPDATE_DELETING:
			case DELETING:
			default:	
				holder.title.setAlpha(0.4f);
				holder.bpb.setProgress(BackgroundProgressBar.MIN);
				holder.um.setVisibility(View.GONE);	
				holder.details.setVisibility(View.VISIBLE);
			}			

			return convertView;
		}
		
		private void openDetails(String mdContentId, int mdVersion){
			Intent i = new Intent(ctx, BookDetailsActivity.class);
			i.putExtra(AbstractSingleBookActivity.EXTRA_MD_CONTENT_ID, mdContentId);
			i.putExtra(AbstractSingleBookActivity.EXTRA_MD_VERSION, mdVersion);
			ctx.startActivity(i);
		}		

		public void updateBooks(List<Book> books) {
			data = books;
			notifyDataSetChanged();
		}					

	}

	private static class ViewHolder {
		BackgroundProgressBar bpb;
		TextView title;
		ImageButton details;
		UpdateMarker um;
	}

}
