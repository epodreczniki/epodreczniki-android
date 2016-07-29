package pl.epodreczniki.fragment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.epodreczniki.R;
import pl.epodreczniki.model.Navigable;
import pl.epodreczniki.model.Page;
import pl.epodreczniki.model.Toc;
import pl.epodreczniki.model.TocItem;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TocFragment extends Fragment{
	
	public static final String TAG = "toc_fragment";
	
	private Navigable navigationActivity;
	
	private Toc toc = null;
	
	private View top;
	
	private TextView titleTv;
	
	private ImageButton backBtn;
	
	private ProgressBar pb;
	
	private ListView list;	
	
	private TocAdapter adapter;
	
	private TocItem root = null;
	
	private TocItem highlight = null;
	
	private Set<String> highlightSet = new HashSet<String>();
	
	@Override
	public void onAttach(Activity activity) {		
		super.onAttach(activity);
		navigationActivity = (Navigable) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {		
		View res = inflater.inflate(R.layout.f_toc, container, false);
		top = res.findViewById(R.id.ft_top);		titleTv = (TextView) res.findViewById(R.id.ft_current);
		titleTv.setTextColor(0xff000000);		
		backBtn = (ImageButton) res.findViewById(R.id.ft_back);
		backBtn.getDrawable().setColorFilter(0xff000000,Mode.MULTIPLY);
		backBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				changeRoot(toc.getParent(root));
			}
			
		});
		pb = (ProgressBar) res.findViewById(R.id.ft_pb);
		list = (ListView) res.findViewById(R.id.ft_list);
		adapter = new TocAdapter();
		list.setAdapter(adapter);
		if(toc!=null){
			changeRoot(root);
		}
		return res;
	}
	
	@Override
	public void onDetach() {
		navigationActivity.onTocDetached();
		navigationActivity = null;
		super.onDetach();
	}
	
	public void setToc(Toc toc, int displayIdx){
		if(this.toc == null || !this.toc.getTocKey().equals(toc.getTocKey())){
			this.toc = toc;					
			int globalIdx = toc.getPageIndex(displayIdx, Util.isTeacher(getActivity()));
			if(globalIdx!=-1){
				Log.e("TOCF", "global idx != -1");
				setHighlight(globalIdx);
			}else{
				Log.e("TOCF", "global idx -1");
				changeRoot(null);
			}
		}
	}
	
	public void setHighlight(int globalIdx){		
		if(toc==null)
			return;
		Page currPage = toc.getPageByGlobalIdx(globalIdx);
		highlight = toc.getTocItemById(currPage.getIdRef());
		if(highlight==null){
			Log.e("TOCF", "nothing to highlight: "+currPage.getIdRef());
		}
		highlightSet.clear();
		TocItem it = highlight;
		while(it!=null){
			highlightSet.add(it.getId());
			it = toc.getParent(it);
		}
		if(highlight!=null){
			changeRoot(toc.getParent(highlight));
		}else{
			changeRoot(null);			
		}
		
	}
	
	private void changeRoot(TocItem newRoot){
		root = newRoot;
		if(newRoot==null){
			top.setVisibility(View.GONE);			
		}else{
			top.setVisibility(View.VISIBLE);
			titleTv.setText(newRoot.getTitle());
		}
		adapter.setItems(toc.getChildren(newRoot, Util.isTeacher(getActivity())));
		pb.setVisibility(View.GONE);
	}

	private class TocAdapter extends BaseAdapter{

		private List<TocItem> list;
		
		@Override
		public int getCount() {
			return list == null?0:list.size();
		}

		@Override
		public TocItem getItem(int position) {			
			return list!=null && position<list.size()?list.get(position):null;
		}

		@Override
		public long getItemId(int position) {			
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView title;
			View divider;
			ImageButton expandBtn;
			final TocItem ti = getItem(position);
			if(convertView == null){
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.v_imageless_toc_item, parent, false);
				title = (TextView) convertView.findViewById(R.id.viti_title);
				divider = convertView.findViewById(R.id.viti_divider);
				expandBtn = (ImageButton) convertView.findViewById(R.id.viti_expand);
				convertView.setTag(R.id.viti_title, title);
				convertView.setTag(R.id.viti_divider, divider);
				convertView.setTag(R.id.viti_expand, expandBtn);
			}else{
				title = (TextView) convertView.getTag(R.id.viti_title);
				divider = (View) convertView.getTag(R.id.viti_divider);
				expandBtn = (ImageButton) convertView.getTag(R.id.viti_expand);
			}
			convertView.setBackgroundColor(position%2!=0?0x10101010:0x00000000);			
			title.setTextColor(0xff000000);
			if(highlightSet.contains(ti.getId())){
				title.setTextColor(0xff33b5e5);				
			}
			title.setText(ti.getTitle());
			divider.setVisibility(ti.hasChildren()?View.VISIBLE:View.GONE);
			expandBtn.setVisibility(ti.hasChildren()?View.VISIBLE:View.GONE);
			expandBtn.getDrawable().setColorFilter(0xff000000,Mode.MULTIPLY);
			title.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					if(navigationActivity!=null){
						final Page p = toc.getPageByPath(ti.getPathRef());
						if(p!=null){
							navigationActivity.showPage(p.getPageId(), toc.getMdContentId(), toc.getMdVersion());	
						}											
					}
				}				
			});
			expandBtn.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {					
					changeRoot(ti);
				}				
			});
			return convertView;
		}
		
		public void setItems(List<TocItem> items){
			this.list = items;
			notifyDataSetChanged();
		}
		
	}
	
}
