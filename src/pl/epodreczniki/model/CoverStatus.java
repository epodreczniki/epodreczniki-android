package pl.epodreczniki.model;

public enum CoverStatus {
	
	REMOTE(0),IN_PROGRESS(1),READY(2);
	
	private int intVal;
	
	CoverStatus(int intVal){
		this.intVal = intVal;
	}
	
	public int getIntVal(){
		return intVal;
	}
	
	public String getStringVal(){
		return String.valueOf(intVal);
	}

}
