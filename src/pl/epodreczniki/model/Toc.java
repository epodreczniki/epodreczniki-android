package pl.epodreczniki.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Toc {
	
	private final String mdContentId;
	
	private final int mdVersion;

	private final String tocKey;		
	
	private final Page[] pages;
	
	private final TocItem[] items;
	
	private Map<String, TocItem> parents = new HashMap<String, TocItem>();
	
	private int[] studentIndices;
	
	public Toc(Page[] pages, TocItem[] items, String mdContentId, int mdVersion){
		this.pages = pages;
		this.items = items;
		this.mdContentId = mdContentId;
		this.mdVersion = mdVersion;
		this.tocKey = mdContentId+"_"+mdVersion;
		initParents();
		initStudentIndices();
	}
	
	private void initParents(){
		for(TocItem item : items){
			initParent(item);
		}
	}
	
	private void initParent(TocItem parent) {
		if (parent != null) {
			if (parent.getChildren() != null) {
				for (TocItem child : parent.getChildren()) {
					parents.put(child.getId(), parent);
					initParent(child);
				}
			}
		}
	}
	
	private void initStudentIndices(){
		int[] tmp = new int[pages.length];
		int cnt = 0;		
		for(int i=0;i<pages.length;i++){
			if(!pages[i].isTeacher()){
				tmp[cnt++]=i;
			}
		}	
		studentIndices = Arrays.copyOfRange(tmp, 0, cnt);		
	}
	
	public TocItem[] getItems(){
		return items;
	}
	
	public String getMdContentId(){
		return mdContentId;
	}
	
	public int getMdVersion(){
		return mdVersion;
	}
	
	public String getTocKey(){
		return tocKey;
	}
	
	private List<TocItem> getAllChildren(TocItem parent){
		List<TocItem> res = new ArrayList<TocItem>();
		if(parent==null){
			for(TocItem t : items){
				if(!parents.containsKey(t.getId())){
					res.add(t);
				}
			}
		}else{
			res = parent.getChildren();
		}
		return res;
	}
	
	public List<TocItem> getChildren(TocItem parent, boolean isTeacher){
		List<TocItem> res = getAllChildren(parent);
		if(!isTeacher){
			List<TocItem> sRes = new ArrayList<TocItem>();
			for(TocItem t : res){
				if(!t.isTeacher()){
					sRes.add(t);
				}
			}
			res = sRes;
		}
		return res;
	}
	
	public TocItem getParent(TocItem item){
		if(item==null){
			return null;
		}
		return parents.get(item.getId());
	}
	
	public Page[] getPages(boolean isTeacher){
		Page[] res = new Page[pages.length];
		if(!isTeacher){
			int cnt=0;
			for(Page p : pages){
				if(!p.isTeacher()){
					res[cnt++]=p;
				}
			}
			res = Arrays.copyOfRange(res, 0, cnt);
		}else{
			res = pages;
		}
		return res;
	}
	
	public Page getPageByPath(String path){
		for(Page p : pages){
			if(p.getPath().equals(path)){
				return p;
			}
		}
		return null;
	}	
	
	public int getPageIndexById(String pageId){
		int idx = 0;
		for(Page p : pages){
			if(p.getPageId()!=null&&p.getPageId().equals(pageId)){
				return idx;
			}
			idx++;
		}
		return -1;
	}
	
	public Page getPageByGlobalIdx(int globalIdx){
		if(globalIdx>-1 && pages.length>globalIdx){
			return pages[globalIdx];
		}
		return null;
	}
	
	public int getPageIndex(String path){
		int idx = 0;
		for(Page p : pages){
			if(p.getPath().equals(path)){
				return idx;
			}
			idx++;
		}
		return -1;
	}
	
	public int getPageIndex(int displayIndex, boolean isTeacher){
		return isTeacher?displayIndex:studentToGlobal(displayIndex);
	}
		
	public int getPageDisplayIndex(String path, boolean isTeacher){
		int global = getPageIndex(path);
		return getPageDisplayIndex(global, isTeacher);			
	}
	
	public int getPageDisplayIndex(int global, boolean isTeacher){
		return isTeacher?global:globalToStudent(global);
	}
	
	private int studentToGlobal(int studentIdx){
		return studentIdx<studentIndices.length&&studentIdx>-1?studentIndices[studentIdx]:-1;
	}
	
	private int globalToStudent(int globalIdx){
		int cnt = 0;
		for(int i : studentIndices){
			if(i==globalIdx){
				return cnt;
			}
			++cnt;
		}
		return -1;
	}
	
	public String getTocItemTitleById(String idRef){
		for(TocItem t : items){
			String res = checkTocItemId(idRef, t);
			if(!res.equals("")){
				return res;
			}
		}
		return "";
	}
	
	private String checkTocItemId(String idRef,TocItem t){
		if(t.getId().equals(idRef)){
			return t.getTitle();
		}
		for(TocItem c : t.getChildren()){
			String res = checkTocItemId(idRef, c);
			if(!res.equals("")){
				return res;
			}
		}
		return "";
	}
	
	public TocItem getTocItemById(String idRef){
		for(TocItem t : items){
			TocItem res = checkTocItem(idRef, t);
			if(res!=null){
				return res;
			}
		}
		return null;
	}
	
	private TocItem checkTocItem(String idRef, TocItem t){
		if(t.getId().equals(idRef)){
			return t;
		}
		for(TocItem c : t.getChildren()){
			TocItem res = checkTocItem(idRef, c);
			if(res!=null){
				return res;
			}
		}
		return null;
	}
	
}
