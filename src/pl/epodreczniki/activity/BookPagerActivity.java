package pl.epodreczniki.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import pl.epodreczniki.R;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.ImageCache;
import pl.epodreczniki.util.ImageUtil;
import pl.epodreczniki.util.Util;
import pl.epodreczniki.view.CircularProgressBar;
import pl.epodreczniki.view.ImageProgressBar;
import pl.epodreczniki.view.PagerSeekBar;
import pl.epodreczniki.view.UpdateMarker;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BookPagerActivity extends AbstractBookListActivity {

	private RetainedBooks rb;
	private TextView tvNoBooks;
	private ViewPager pager;
	private NewBookPagerAdapter adapter;
	private PagerSeekBar seekBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_book_pager);

		tvNoBooks = (TextView) findViewById(R.id.abp_tv_no_books);

		final FragmentManager fm = getFragmentManager();
		final Fragment ret = fm.findFragmentByTag(RetainedBooks.TAG);
		if (ret == null) {
			final FragmentTransaction ft = fm.beginTransaction();
			rb = new RetainedBooks();
			ft.add(rb, RetainedBooks.TAG);
			ft.commit();
		} else {
			rb = (RetainedBooks) ret;
		}
		if (rb.retainedBooks == null) {
			rb.retainedBooks = new ArrayList<Book>();
		}
		seekBar = (PagerSeekBar) findViewById(R.id.abp_sb);
		pager = (ViewPager) findViewById(R.id.abp_vp);
		pager.setOffscreenPageLimit(1);
		adapter = new NewBookPagerAdapter(this);
		adapter.updateBooks(rb.retainedBooks);
		pager.setAdapter(adapter);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(
				ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setDisplayUseLogoEnabled(true);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.m_viewChanger);		
		item.setVisible(false);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.m_viewChanger:
			startActivity(new Intent(this, BookListActivity.class));
			break;
		}
		return true;
	}

	@Override
	protected void onBooksUpdate(List<Book> books) {
		if (books.size() > 0) {
			tvNoBooks.setVisibility(View.GONE);
			seekBar.setVisibility(View.VISIBLE);
			pager.setVisibility(View.VISIBLE);
		} else {
			tvNoBooks.setVisibility(View.VISIBLE);
			seekBar.setVisibility(View.INVISIBLE);
			pager.setVisibility(View.GONE);
		}
		
		if(books==null || adapter.books.size()!=books.size()){
			pager.setAdapter(null);
			adapter.updateBooks(books);
			pager.setAdapter(adapter);
		}else{
			adapter.updateBooks(books);
		}		
		if (books.size() > 0 && rb.lastItem==-1) {
			pager.setCurrentItem(1, false);
		}else{
			pager.setCurrentItem(rb.lastItem,false);
		}
	}

	public static class RetainedBooks extends Fragment {

		public static final String TAG = "retBooks";

		public List<Book> retainedBooks;
		
		private int lastItem = -1;

		public RetainedBooks() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return null;
		}

	}

	private static class NewBookPagerAdapter extends PagerAdapter implements OnPageChangeListener {

		private final BookPagerActivity activity;

		private List<Book> books = new ArrayList<Book>();

		NewBookPagerAdapter(BookPagerActivity activity) {
			this.activity = activity;
			activity.pager.setOnPageChangeListener(this);	
		}

		@Override
		public int getCount() {
			return books.size() + 2;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Log.e("TTT", "instantiate item: "+position+" "+activity.pager.getCurrentItem());
			final View res = LayoutInflater.from(activity).inflate(R.layout.f_book_list_pages, container, false);
			final ImageProgressBar cover = (ImageProgressBar) res.findViewById(R.id.fblp_iv_bookCover);
			final TextView title = (TextView) res.findViewById(R.id.fblp_tv_bookTitle);
			final TextView subtitle = (TextView) res.findViewById(R.id.fblp_tv_bookSubtitle);
			final ImageButton detBtn = (ImageButton) res.findViewById(R.id.fblp_ib_details);
			final CircularProgressBar cpb = (CircularProgressBar) res.findViewById(R.id.fblp_cpb);
			final UpdateMarker um = (UpdateMarker) res.findViewById(R.id.fblp_um);
			res.setTag(R.id.fblp_tv_bookTitle, title);
			res.setTag(R.id.fblp_tv_bookSubtitle, subtitle);
			res.setTag(R.id.fblp_iv_bookCover, cover);
			res.setTag(R.id.fblp_ib_details, detBtn);
			res.setTag(R.id.fblp_cpb, cpb);
			res.setTag(R.id.fblp_um, um);
			final Book book = getBookAt(position);
			if (book != null) {
				res.setTag(book.getMdContentId());
				updateControls(book, res);
			}
			container.addView(res);
			return res;
		}

		private void updateControls(final Book book, final View view) {
			final ImageProgressBar cover = (ImageProgressBar) view.getTag(R.id.fblp_iv_bookCover);
			if (book.getCoverLocalPath() != null) {
				final String coverPath = Uri.parse(book.getCoverLocalPath()).getPath();
				final Bitmap bm = ImageCache.get(activity).get(coverPath);
				if (bm != null) {
					cover.setImageBitmap(bm);
				} else {
					cover.setImageDrawable(activity.getResources().getDrawable(R.drawable.cover));
					new NewCoverLoader(activity.pager, book.getMdContentId()).execute(book.getCoverLocalPath());
				}
			} else {
				cover.setImageDrawable(activity.getResources().getDrawable(R.drawable.cover));
			}
			final TextView title = (TextView) view.getTag(R.id.fblp_tv_bookTitle);
			final TextView subtitle = (TextView) view.getTag(R.id.fblp_tv_bookSubtitle);
			final ImageButton detBtn = (ImageButton) view.getTag(R.id.fblp_ib_details);
			final CircularProgressBar cpb = (CircularProgressBar) view.getTag(R.id.fblp_cpb);
			final UpdateMarker um = (UpdateMarker) view.getTag(R.id.fblp_um);
			final BookStatus bs = BookStatus.fromInteger(book.getStatus());
			um.setVisibility(book.isUpdateAvailable() ? View.VISIBLE : View.GONE);

			title.setText(book.getMdTitle());
			subtitle.setText(book.getSubject() == null ? "" : Util.capitalize(book.getSubject()));
			title.setEnabled(bs == BookStatus.READY);
			subtitle.setEnabled(bs == BookStatus.READY);

			switch (bs) {
			case REMOTE:
				cover.setProgress(0);
				cpb.setVisibility(View.GONE);
				break;
			case EXTRACTING:				
				cpb.setIndeterminate(false);
				cpb.setVisibility(View.VISIBLE);
				cpb.setProgress(book.getBytesSoFar());
				cover.setProgress(100);
				break;
			case READY:
				cover.setProgress(100);
				cpb.setVisibility(View.GONE);
				break;
			case UPDATE_EXTRACTING:
				cover.setProgress(100);
				cpb.setIndeterminate(false);
				cpb.setVisibility(View.VISIBLE);
				cpb.setProgress(book.getBytesSoFar());
				break;
			case DOWNLOADING:
				cover.setProgress(Util.calculatePercentage(book));
				cpb.setVisibility(View.GONE);
				break;
			case UPDATE_DOWNLOADING:
				cpb.setVisibility(View.GONE);
				final Book calc = book.getVersions().size() > 0 ? book.getVersions().get(0) : null;
				if (calc != null) {
					cover.setProgress(Util.calculatePercentage(calc));
				}
				break;
			case UPDATE_DELETING:
				cover.setProgress(100);
				um.setVisibility(View.VISIBLE);
				cpb.setIndeterminate(true);
				cpb.setVisibility(View.VISIBLE);
				break;
			case DELETING:
			default:
				cover.setProgress(0);
				um.setVisibility(View.GONE);
				cpb.setIndeterminate(true);
				cpb.setVisibility(View.VISIBLE);

			}

			detBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final Intent i = new Intent(activity, BookDetailsActivity.class);
					i.putExtra(AbstractSingleBookActivity.EXTRA_MD_CONTENT_ID, book.getMdContentId());
					i.putExtra(AbstractSingleBookActivity.EXTRA_MD_VERSION, book.getMdVersion());
					activity.startActivity(i);
				}
			});
			final BookStatus status = BookStatus.fromInteger(book.getStatus());

			cover.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final Animation btnAnimation = AnimationUtils.loadAnimation(activity, R.anim.image_click);
					btnAnimation.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationStart(Animation animation) {
						}

						@Override
						public void onAnimationEnd(Animation animation) {
							if (status == BookStatus.READY) {
								if (Util.checkTocEntryFiles(activity, book.getMdContentId(), book.getMdVersion())) {
									final Intent i = new Intent(activity, SwipeReaderActivity.class);
									i.putExtra(SwipeReaderActivity.EXTRA_BOOK, book);
									activity.startActivity(i);
								} else if (Util.checkIndexEntryFile(activity, book.getMdContentId(),
										book.getMdVersion())) {
									final Intent i = new Intent(activity, ReaderActivity.class);
									String base = "file://" + book.getLocalPath();
									if (!base.endsWith("index.html")) {
										base += File.separator + Constants.INDEX_ENTRY;
									}
									final Uri u = Uri.parse(base);
									i.putExtra(ReaderActivity.EXTRA_URL, u.toString());
									i.putExtra(ReaderActivity.BOOK_TITLE, book.getMdTitle());
									activity.startActivity(i);
								} else {
									Toast.makeText(activity, "Nieznany format e-podręcznika", Toast.LENGTH_LONG).show();
								}

							} else {
								final Intent i = new Intent(activity, BookDetailsActivity.class);
								i.putExtra(AbstractSingleBookActivity.EXTRA_MD_CONTENT_ID, book.getMdContentId());
								i.putExtra(AbstractSingleBookActivity.EXTRA_MD_VERSION, book.getMdVersion());
								activity.startActivity(i);
							}
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

					});
					v.startAnimation(btnAnimation);
				}
			});

		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		private Book getBookAt(int position) {
			if (books.size() == 0)
				return null;
			if (position == 0)
				return books.get(books.size() - 1);
			if (position == books.size() + 1)
				return books.get(0);
			return books.get(position - 1);
		}

		public void updateBooks(List<Book> books) {			
			if (books != null) {
				Log.e("TTT", "this: "+this.books.size());				
				Log.e("TTT", "new: "+books.size());
				boolean shouldNotify = this.books.size()!=books.size();
				this.books = books;				
				if(shouldNotify){
					Log.e("TTT", "dataset changed");					
					notifyDataSetChanged();
					activity.seekBar.setMax(books.size());
					activity.seekBar.setProgress(Math.max(1, activity.pager.getCurrentItem()));
					activity.pager.setCurrentItem(Math.max(1, activity.rb.lastItem), false);					
				}								
				for (Book b : books) {
					final View toUpdate = activity.pager.findViewWithTag(b.getMdContentId());
					if (toUpdate != null) {
						updateControls(b, toUpdate);
					}
				}
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			if (activity.pager.getCurrentItem() == 0 && state == ViewPager.SCROLL_STATE_IDLE) {
				activity.pager.setCurrentItem(books.size(), false);				
			}
			if (activity.pager.getCurrentItem() == books.size() + 1 && state == ViewPager.SCROLL_STATE_IDLE) {
				activity.pager.setCurrentItem(1, false);				
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int position) {
			Log.e("BPA", "setting curr item: "+position);
			activity.rb.lastItem = position;
			if (position == 0)
				activity.seekBar.setProgress(books.size());
			else if (position == books.size() + 1)
				activity.seekBar.setProgress(1);
			else
				activity.seekBar.setProgress(position);
		}

	}

	private static class NewCoverLoader extends AsyncTask<String, Void, Bitmap> {

		private final WeakReference<ViewPager> pagerRef;

		private final String mdContentId;

		private String coverLocalPath;

		NewCoverLoader(ViewPager pager, String mdContentId) {
			pagerRef = new WeakReference<ViewPager>(pager);
			this.mdContentId = mdContentId;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap res = null;
			if (params != null && params.length > 0) {
				final Uri u = Uri.parse(params[0]);
				coverLocalPath = u.getPath();
				final ViewPager pager = pagerRef.get();
				if (pager != null && coverLocalPath != null) {
					res = decode(pager.getContext());
				}
			}
			return res;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			final ViewPager pager = pagerRef.get();
			if (pager != null && result != null) {
				final View v = pager.findViewWithTag(mdContentId);
				if (v != null) {
					final ImageProgressBar img = (ImageProgressBar) v.findViewById(R.id.fblp_iv_bookCover);
					img.setImageBitmap(result);
				}
				if (coverLocalPath != null) {
					Log.e("CL", "adding to cache " + coverLocalPath);
					ImageCache.get(pager.getContext()).put(coverLocalPath, result);
				}
			}
		}

		private Bitmap decode(Context ctx) {
			Bitmap bm = null;
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(coverLocalPath, options);
			options.inSampleSize = ImageUtil.calculateInSampleSizeWithRequiredHeight(options,
					ImageCache.getLongerScreenEdge(ctx) / 2);
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			bm = BitmapFactory.decodeFile(coverLocalPath, options);
			return bm;
		}

	}

}
