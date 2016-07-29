package pl.epodreczniki.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import pl.epodreczniki.R;
import pl.epodreczniki.fragment.UpdateDialog;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.ImageCache;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import pl.epodreczniki.view.CircularProgressBar;
import pl.epodreczniki.view.UpdateMarker;
import pl.epodreczniki.view.ImageProgressBar;

public class BookGridActivity extends AbstractBookListActivity {
	
	private GridAdapter adapter;
	private GridRetainedFragment rf;
	private TextView tvNoBooks;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_book_list_grid);
		tvNoBooks = (TextView) findViewById(R.id.ablg_tv_no_books);

		FragmentManager fm = getFragmentManager();
		Fragment ret = fm.findFragmentByTag(GridRetainedFragment.TAG);
		if (ret == null) {
			FragmentTransaction ft = fm.beginTransaction();
			rf = new GridRetainedFragment();
			ft.add(rf, GridRetainedFragment.TAG);
			ft.commit();
		} else {
			rf = (GridRetainedFragment) ret;
		}
		if (rf.retainedReversed == null) {
			rf.retainedReversed = new HashSet<String>();
		}
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setDisplayUseLogoEnabled(true);
		adapter = new GridAdapter(this);
		adapter.setReversed(rf.retainedReversed);
		final GridView grid = (GridView) findViewById(R.id.ablg_gv_data);
		grid.setAdapter(adapter);
	}	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.m_viewChanger).setVisible(false);
		return true;
	}	

	@Override
	protected void onBooksUpdate(List<Book> books) {
		adapter.updateBooks(books);
		tvNoBooks.setVisibility(books.size()>0?View.GONE:View.VISIBLE);		
	}
	
	public void requestXfer(Book b){
		requestTransfer(b);
	}
	
	private void showUpdateDialog(Book book){		
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

	private static class GridAdapter extends BaseAdapter {

		private static final long FLIP_DURATION = 300;
		
		private static final float FLIP_SCALE = 0.5f;
		
		private final BookGridActivity ctx;

		private List<Book> data;

		private Set<String> reversed;

		GridAdapter(BookGridActivity ctx) {			
			this.ctx = ctx;
			reversed = new HashSet<String>();			
		}
		
		static AnimatorSet createFlipAnimation(final View hide,final View show){
			final AnimatorSet res  = new AnimatorSet();
			
			final AnimatorSet hSet = new AnimatorSet();
			hSet.setInterpolator(new AccelerateDecelerateInterpolator());
			hSet.setDuration(FLIP_DURATION);
			final ObjectAnimator hRotY = ObjectAnimator.ofFloat(hide, "rotationY", 0f, 90f);
			final ObjectAnimator hScaleX = ObjectAnimator.ofFloat(hide, "scaleX", 1f,FLIP_SCALE);
			final ObjectAnimator hScaleY = ObjectAnimator.ofFloat(hide, "scaleY", 1f,FLIP_SCALE);
			hSet.playTogether(hRotY,hScaleX,hScaleY);
			hSet.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation) {
					hide.setVisibility(View.GONE);
					show.setVisibility(View.VISIBLE);
					clearTransformations(hide);
				}
			});
			
			final AnimatorSet sSet = new AnimatorSet();
			sSet.setInterpolator(new AccelerateDecelerateInterpolator());
			sSet.setDuration(FLIP_DURATION);
			final ObjectAnimator sRotY = ObjectAnimator.ofFloat(show, "rotationY", 270f, 360f);
			final ObjectAnimator sScaleX = ObjectAnimator.ofFloat(show, "scaleX", FLIP_SCALE, 1f);
			final ObjectAnimator sScaleY = ObjectAnimator.ofFloat(show, "scaleY", FLIP_SCALE, 1f);
			sSet.playTogether(sRotY,sScaleX,sScaleY);
			res.playSequentially(hSet,sSet);
			res.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationCancel(Animator animation) {
					clearTransformations(hide);
					clearTransformations(show);
				}
				@Override
				public void onAnimationEnd(Animator animation) {
					clearTransformations(hide);
					clearTransformations(show);
				}
			});
			return res;
		}
		
		static void clearTransformations(View v){
			v.setRotationY(0f);
			v.setScaleX(1f);
			v.setScaleY(1f);
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
			ViewHolder holder;
			final Book book = getItem(position);
			String previousId = null;
			if (convertView == null) {
				holder = new ViewHolder();
				final LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.v_new_book_grid_item, parent, false);
				holder.title = (TextView) convertView.findViewById(R.id.vblgi_tv_bookTitle);
				holder.subtitle = (TextView) convertView.findViewById(R.id.vblgi_tv_bookSubtitle);
				holder.image = (ImageProgressBar) convertView.findViewById(R.id.vblgi_iv_bookCover);
				holder.imageFrame = (FrameLayout) convertView.findViewById(R.id.imageFrame);
				holder.reverse = convertView.findViewById(R.id.vblgi_ll_coverReverse);
				holder.back = (ImageButton) convertView.findViewById(R.id.vblgi_rewers_iv_back);
				holder.download = (ImageButton) convertView.findViewById(R.id.vblgi_rewers_iv_download_update);
				holder.delete = (ImageButton) convertView.findViewById(R.id.vblgi_rewers_iv_delete);
				holder.read = (ImageButton) convertView.findViewById(R.id.vblgi_rewers_iv_read);
				holder.options = (ImageButton) convertView.findViewById(R.id.vblgi_rewers_iv_details);
				holder.imageProgress = (ProgressBar) convertView.findViewById(R.id.vblgi_pb_cover);
				holder.imageProgressRevers = (CircularProgressBar) convertView.findViewById(R.id.vblgi_pb_coverRevers);
				holder.tv_progress_percent = (TextView) convertView.findViewById(R.id.vblgi_tv_progress_percent);
				holder.updateMarker = (UpdateMarker) convertView.findViewById(R.id.vblgi_um);
				holder.headsAnim = createFlipAnimation(holder.imageFrame, holder.reverse);
				holder.tailsAnim = createFlipAnimation(holder.reverse, holder.imageFrame);
				convertView.setTag(holder);
				convertView.setTag(R.id.grid_item_prev_id,book.getMdContentId());
			} else {
				previousId = (String) convertView.getTag(R.id.grid_item_prev_id);
				convertView.setTag(R.id.grid_item_prev_id, book.getMdContentId());
				holder = (ViewHolder) convertView.getTag();				
				AsyncGridBitmapLoader loader = getTask(holder.image);
				if (loader != null) {
					if (book != null && book.getCoverLocalPath() != null) {
						if (!book.getCoverLocalPath().equals(loader.getCoverPath())) {
							loader.cancel(true);
						}
					}
				}
			}

			final ViewHolder hldr = holder;						

			if(!book.getMdContentId().equals(previousId)){
				if(hldr.headsAnim.isRunning()){
					hldr.headsAnim.cancel();
				}
				if(hldr.tailsAnim.isRunning()){
					hldr.tailsAnim.cancel();
				}
			}
			
			if(!hldr.headsAnim.isRunning()&&!hldr.tailsAnim.isRunning()){
				if (reversed.contains(book.getMdContentId())) {
					holder.imageFrame.setVisibility(View.GONE);
					holder.reverse.setVisibility(View.VISIBLE);
				} else {
					holder.imageFrame.setVisibility(View.VISIBLE);
					holder.reverse.setVisibility(View.GONE);
				}	
			}			

			holder.title.setText(book.getMdTitle());
			
			if(book.getSubject() != null && !book.getSubject().isEmpty()) {
				holder.subtitle.setText(Util.capitalize(book.getSubject()));				
			}else if(book.getMdSubtitle() != null && !book.getMdSubtitle().isEmpty()){				
				holder.subtitle.setText(book.getMdSubtitle());
			}
			
			holder.image.setContentDescription(book.getMdTitle());			

			holder.image.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if(!hldr.headsAnim.isRunning()){
						hldr.headsAnim.start();
						reversed.add(book.getMdContentId());	
					}					
				}

			});

			holder.back.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if(!hldr.tailsAnim.isRunning()){
						hldr.tailsAnim.start();
						reversed.remove(book.getMdContentId());	
					}					
				}

			});

			holder.download.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					final BookStatus status = BookStatus.fromInteger(book.getStatus());
					switch(status){
					case REMOTE:													
						ctx.requestTransfer(book);							
						updateGUI(book, hldr);	
						break;									
					case READY:
						if(book.isUpdateAvailable()){
							ctx.showUpdateDialog(book);
						}
						break;
					case DOWNLOADING:						
					case UPDATE_DOWNLOADING:
						ctx.stop(book);						
						break;					
						default:
					}				
				}
			});

			holder.delete.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(ctx).setTitle(R.string.dialog_title_terms_confirm).setMessage(R.string.dialog_message_delete_book_confirm)
							.setPositiveButton(R.string.dialog_ans_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									ctx.delete(book);
									Toast.makeText(ctx, ctx.getResources().getString(R.string.toast_bookDelete), Toast.LENGTH_LONG).show();
								}
							}).setNegativeButton(R.string.dialog_ans_no, null).show();
				}
			});

			holder.read.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (BookStatus.fromInteger(book.getStatus()).equals(BookStatus.READY)) {
						if(Util.checkTocEntryFiles(ctx, book.getMdContentId(), book.getMdVersion())){
							final Intent i = new Intent(ctx,SwipeReaderActivity.class);
							i.putExtra(SwipeReaderActivity.EXTRA_BOOK, book);
							ctx.startActivity(i);
							reversed.clear();
						}else if(Util.checkIndexEntryFile(ctx, book.getMdContentId(), book.getMdVersion())){
							String base = "file://"+book.getLocalPath();
							if(!base.endsWith("index.html")){
								base+=File.separator+Constants.INDEX_ENTRY;
							}
							Uri u = Uri.parse(base);
							final Intent i = new Intent(ctx, ReaderActivity.class);
							i.putExtra(ReaderActivity.EXTRA_URL, u.toString());
							i.putExtra(ReaderActivity.BOOK_TITLE, book.getMdTitle());
							ctx.startActivity(i);
							reversed.clear();
						}else{
							Toast.makeText(ctx, "Nieznany format e-podręcznika",  Toast.LENGTH_LONG).show();
						}												
					}
				}

			});

			holder.options.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(ctx, BookDetailsActivity.class);
					intent.putExtra(AbstractSingleBookActivity.EXTRA_MD_CONTENT_ID, book.getMdContentId());
					intent.putExtra(AbstractSingleBookActivity.EXTRA_MD_VERSION, book.getMdVersion());
					ctx.startActivity(intent);
					reversed.clear();
				}
			});

			loadBitmap(book, hldr);

			updateGUI(book, hldr);

			return convertView;
		}

		private void updateGUI(Book book, ViewHolder holder) {
			if(!ctx.checkUserLoggedIn()){
				return;
			}
			final BookStatus status = BookStatus.fromInteger(book.getStatus());			
			holder.download.setVisibility(View.VISIBLE);
			holder.updateMarker.setVisibility(book.isUpdateAvailable()?View.VISIBLE:View.GONE);
			if(status == BookStatus.UPDATE_DELETING){				
				holder.download.setEnabled(false);								
				holder.delete.setEnabled(false);				
				holder.read.setVisibility(View.VISIBLE);				
				holder.read.setEnabled(true);
				holder.read.setImageResource(R.drawable.ico_read_biggest);
				
				holder.image.setProgress(100);
				holder.imageProgressRevers.setIndeterminate(true);
				holder.imageProgressRevers.setVisibility(View.VISIBLE);
				holder.tv_progress_percent.setVisibility(View.GONE);
				
				holder.title.setEnabled(true);
				holder.subtitle.setEnabled(true);
			}else if(status == BookStatus.DELETING){				
				holder.download.setEnabled(false);
				
				holder.delete.setEnabled(false);
				
				holder.read.setEnabled(false);
				holder.read.setVisibility(View.INVISIBLE);				
				holder.image.setProgress(0);				
				holder.imageProgressRevers.setIndeterminate(true);
				holder.imageProgressRevers.setVisibility(View.VISIBLE);
				holder.tv_progress_percent.setVisibility(View.GONE);
				
				holder.title.setEnabled(false);
				holder.subtitle.setEnabled(false);			
			} else if(status == BookStatus.READY) {				
				holder.download.setEnabled(book.isUpdateAvailable()&&UserContext.getCurrentUser().canManageBooks());
				holder.download.setImageResource(book.isUpdateAvailable()?R.drawable.ico_refresh_med:R.drawable.ico_download_med);
				holder.delete.setEnabled(UserContext.getCurrentUser().canManageBooks());

				holder.read.setVisibility(View.VISIBLE);
				holder.read.setEnabled(true);
				holder.read.setImageResource(R.drawable.ico_read_biggest);

				holder.image.setProgress(100);

				holder.imageProgressRevers.setVisibility(View.GONE);
				holder.tv_progress_percent.setVisibility(View.GONE);
				
				holder.title.setEnabled(true);
				holder.subtitle.setEnabled(true);
			} else if(status == BookStatus.REMOTE) {				
				holder.download.setEnabled(UserContext.getCurrentUser().canManageBooks());
				holder.download.setImageResource(R.drawable.ico_download_med);
				
				holder.delete.setEnabled(false);

				holder.read.setVisibility(View.VISIBLE);				
				holder.read.setEnabled(false);
				holder.read.setImageResource(R.drawable.ico_read_biggest);
				holder.read.setVisibility(View.VISIBLE);

				holder.image.setProgress(0);

				holder.imageProgressRevers.setVisibility(View.GONE);
				holder.tv_progress_percent.setVisibility(View.GONE);
				
				holder.title.setEnabled(false);
				holder.subtitle.setEnabled(false);				
			} else { 				
				holder.delete.setEnabled(false);
				
				holder.imageProgressRevers.setVisibility(View.VISIBLE);
				holder.read.setVisibility(View.INVISIBLE);
				
				holder.read.setEnabled(false);
				holder.download.setImageResource(R.drawable.ico_stop_med);				
				holder.download.setEnabled(false);
				
				holder.title.setEnabled(false);
				holder.subtitle.setEnabled(false);
				if (status == BookStatus.DOWNLOADING) {					
					holder.download.setEnabled(UserContext.getCurrentUser().canManageBooks());
					holder.imageProgressRevers.setIndeterminate(true);
					holder.imageProgressRevers.setVisibility(View.VISIBLE);
					if (book.getBytesTotal() > book.getBytesSoFar() && book.getBytesSoFar() > 0) {
						int currPercent = (int) ((book.getBytesSoFar() * 1.0 / book.getBytesTotal()) * 100);
						holder.image.setProgress(currPercent);
						holder.tv_progress_percent.setVisibility(View.VISIBLE);
						holder.tv_progress_percent.setText(Html.fromHtml("<big><big>" + currPercent + "</big></big>" +  "<small>%</small>" ));											
					}
				} else if (status == BookStatus.EXTRACTING) {					
					holder.tv_progress_percent.setVisibility(View.GONE);
					holder.image.setProgress(100);
					holder.imageProgressRevers.setIndeterminate(false);
					holder.imageProgressRevers.setProgress(book.getBytesSoFar());
					holder.read.setVisibility(View.INVISIBLE);
				} else if (status == BookStatus.UPDATE_DOWNLOADING){
					final Book bookUpdate = book.getVersions().size()>0?book.getVersions().get(0):null;
					if(bookUpdate!=null){
						int progress = Util.calculatePercentage(bookUpdate);
						holder.image.setProgress(progress);
						holder.tv_progress_percent.setVisibility(View.VISIBLE);
						holder.imageProgressRevers.setIndeterminate(true);
						holder.imageProgressRevers.setVisibility(View.VISIBLE);
						holder.tv_progress_percent.setText(Html.fromHtml("<big><big>" + progress + "</big></big>" +  "<small>%</small>" ));						
					}										
					holder.download.setEnabled(UserContext.getCurrentUser().canManageBooks());
				} else if (status == BookStatus.UPDATE_EXTRACTING){
					holder.read.setVisibility(View.INVISIBLE);
					holder.tv_progress_percent.setVisibility(View.GONE);
					holder.image.setProgress(100);
					holder.imageProgressRevers.setIndeterminate(false);
					holder.imageProgressRevers.setProgress(book.getVersions().get(0).getBytesSoFar());
				}
			}
		}		

		public void updateBooks(List<Book> books) {
			data = books;
			notifyDataSetChanged();
		}

		public void setReversed(Set<String> reversed) {
			this.reversed = reversed;
		}

		private void loadBitmap(Book b, ViewHolder holder) {
			if (b != null && b.getCoverLocalPath() != null) {
				final String coverPath = Uri.parse(b.getCoverLocalPath()).getPath();
				Bitmap cover = ImageCache.get(ctx).get(coverPath);
				if (cover != null) {					
					holder.imageProgress.setVisibility(View.INVISIBLE);
					holder.image.setImageBitmap(cover);
				} else {
					holder.image.setImageDrawable(ctx.getResources().getDrawable(R.drawable.cover));		
					if (cancelPotentialWork(b, holder.image)) {
						final AsyncGridBitmapLoader task = new AsyncGridBitmapLoader(holder, coverPath);
						holder.image.setTag(task);
						task.execute();
					}
				}
			} else {
				holder.imageProgress.setVisibility(View.INVISIBLE);
				holder.image.setImageDrawable(ctx.getResources().getDrawable(R.drawable.cover));				
			}
		}

		private boolean cancelPotentialWork(Book b, ImageView imageView) {
			final AsyncGridBitmapLoader task = getTask(imageView);

			if (task != null) {
				final String coverPath = task.getCoverPath();
				if (b.getCoverLocalPath() != coverPath) {
					task.cancel(true);
				} else {
					return false;
				}
			}
			return true;
		}

		private AsyncGridBitmapLoader getTask(ImageView imageView) {
			if (imageView != null) {
				return (AsyncGridBitmapLoader) imageView.getTag();
			}
			return null;
		}

	}

	private static class ViewHolder {
		TextView title;
		TextView subtitle;
		ImageProgressBar image;
		View reverse;
		FrameLayout imageFrame;
		ImageButton back;
		ImageButton download;
		ImageButton delete;
		ImageButton read;
		ImageButton options;
		ProgressBar imageProgress;
		TextView tv_progress_percent;
		CircularProgressBar imageProgressRevers;
		UpdateMarker updateMarker;
		AnimatorSet headsAnim;
		AnimatorSet tailsAnim;		
	}

	private static class AsyncGridBitmapLoader extends AsyncTask<Void, Void, Bitmap> {

		private final WeakReference<ViewHolder> holderRef;

		private final String coverPath;

		AsyncGridBitmapLoader(ViewHolder holder, String coverPath) {
			holderRef = new WeakReference<ViewHolder>(holder);
			this.coverPath = coverPath;
		}

		public String getCoverPath() {
			return coverPath;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			final ViewHolder holder = holderRef.get();
			if (holder != null) {
				if (holder.imageProgress != null) {
					if(new File(coverPath).exists()){						
						holder.imageProgress.setVisibility(View.VISIBLE);						
					}else{						
						cancel(true);
					}					
				}
			}
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap res = null;
			final ViewHolder holder = holderRef.get();
			if (holder != null) {
				final ImageProgressBar img = holder.image;
				if (img != null) {
					Context ctx = img.getContext();
					if (ctx != null) {
						res = decode(ctx);
					}
				}
			}
			return res;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			final ViewHolder holder = holderRef.get();
			if (holder != null) {
				final ProgressBar prog = holder.imageProgress;
				if (prog != null) {
					prog.setVisibility(View.INVISIBLE);
				}
				final ImageProgressBar img = holder.image;
				if (img != null) {
					if (result != null) {
						img.setImageBitmap(result);
						final Context ctx = img.getContext();
						if (ctx != null && coverPath != null) {
							ImageCache.get(ctx).put(coverPath, result);
						}
						img.setTag(null);
					}
				}
			}
		}

		private Bitmap decode(Context ctx) {
			Bitmap bm = null;
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(coverPath, options);
			options.inSampleSize = calculateInSampleSize(options, ImageCache.getLongerScreenEdge(ctx) / 3);
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			bm = BitmapFactory.decodeFile(coverPath, options);
			return bm;
		}

		private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth) {
			final int width = options.outWidth;
			int inSampleSize = 1;
			if (width > reqWidth) {
				final int halfWidth = width / 2;
				while (halfWidth / inSampleSize > reqWidth) {
					inSampleSize *= 2;
				}
			}
			return inSampleSize;
		}

	}

	public static class GridRetainedFragment extends Fragment {

		public static final String TAG = "gridRetFgmt";

		public Set<String> retainedReversed;

		public GridRetainedFragment() {
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
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if(adapter != null)
			adapter.notifyDataSetChanged();
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onResume() {
		if(adapter != null)
			adapter.notifyDataSetChanged();
		super.onResume();
	}

}
