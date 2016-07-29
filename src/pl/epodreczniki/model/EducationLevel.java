package pl.epodreczniki.model;

import java.util.Arrays;

public enum EducationLevel {
	
	WSZYSTKIE("Wszystkie",null,new int[]{}),PODSTAWOWA("Szkoła podstawowa","II",new int[]{4,5,6}),GIMNAZJUM("Gimnazjum","III",new int[]{1,2,3}),PONADGIMNAZJALNA("Szkoła ponadgimnazjalna","IV",new int[]{1,2,3});
	
	private final String displayName;
	
	private final String value;
	
	private final int[] classes;
	
	EducationLevel(String displayName, String value, int[] classes){
		this.displayName = displayName;
		this.value = value;
		this.classes = classes;
	}
	
	public String getDisplayName(){
		return displayName;
	}
	
	public String getValue(){
		return value;
	}
	
	public int[] getClasses(){		
		return Arrays.copyOf(classes,classes.length);
	}
	
	public static EducationLevel getByValue(String value){
		EducationLevel res = WSZYSTKIE;
		for(EducationLevel el : EducationLevel.values()){
			if(el.getValue()==null||el.getValue().equals(value)){
				res = el;
			}
		}
		return res;
	}
	
}
