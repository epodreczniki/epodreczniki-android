package pl.epodreczniki.model;

import android.util.SparseArray;

public enum BookStatus {
	
	REMOTE(0),DOWNLOADING(1),EXTRACTING(2),READY(3),UPDATE_DOWNLOADING(4),UPDATE_EXTRACTING(5),DELETING(6),UPDATE_DELETING(7),UNKNOWN(-1);
		
	private static final SparseArray<BookStatus> integerToEnum = new SparseArray<BookStatus>(values().length){{
		for(BookStatus bs : values()){
			put(bs.intVal, bs);
		}
	}};
	
	private final int intVal;	
	
	BookStatus(int intVal){
		this.intVal = intVal;
	}
	
	public int getIntVal(){
		return intVal;
	}
	
	public String getStringVal(){
		return String.valueOf(intVal);
	}		
	
	public static BookStatus fromInteger(Integer i){
		BookStatus res = integerToEnum.get(i);
		return res==null?UNKNOWN:res;
	}
	
}
