package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.fragment.ChangePasswordDialog;
import pl.epodreczniki.fragment.UserDialog;
import pl.epodreczniki.model.EducationLevel;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class ProfileActivity extends AbstractUserAwareActivity{
	
	private CheckBox cbTeacher;
	
	private RadioGroup rgLibrary;
	
	private Spinner educationLevel;
	
	private EducationLevelAdapter educationLevelAdapter;
	
	private Spinner clazz;
	
	private ClassAdapter classAdapter;
	
	private CheckBox cbSkipPassword;
	
	private Button btnChangePassword;		
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_profile);
		if(!checkUserLoggedIn()){
			return;
		}
		cbTeacher = (CheckBox) findViewById(R.id.ap_cb_teacher);
		cbTeacher.setChecked(UserContext.getCurrentUser().getSettings().isTeacher());
		cbTeacher.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				new UpdateProfileTask(ProfileActivity.this).execute();
			}
			
		});
		rgLibrary = (RadioGroup) findViewById(R.id.ap_rg_library);
		int preferredView = UserContext.getCurrentUser().getSettings().getPreferredListType();
		rgLibrary.check(preferredView==Settings.LIST_TYPE_FLAT?R.id.ap_rb_list:R.id.ap_rb_pager);
		rgLibrary.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				new UpdateProfileTask(ProfileActivity.this).execute();
			}
						
		});
				
		
		rgLibrary.setVisibility(Util.isTablet(this)?View.GONE:View.VISIBLE);
		
		educationLevel = (Spinner) findViewById(R.id.ap_spnr_education_level);
		educationLevelAdapter = new EducationLevelAdapter(this, -8);
		educationLevel.setAdapter(educationLevelAdapter);
		
		clazz = (Spinner) findViewById(R.id.ap_spnr_class);
		classAdapter = new ClassAdapter(this,-17);
		clazz.setAdapter(classAdapter);		
		
		String filterEl = UserContext.getCurrentUser().getSettings().getBookFilterEducationLevel();
		String filterCl = UserContext.getCurrentUser().getSettings().getBookFilterClass();
		int filterClInt = filterCl==null?-1:Integer.valueOf(filterCl);
		EducationLevel el = EducationLevel.getByValue(filterEl);
		int elIdx = 0;
		for(int i = 0; i<EducationLevel.values().length; i++){
			if(EducationLevel.values()[i]==el){
				elIdx = i;
			}
		}		
		educationLevel.setSelection(elIdx,false);		
		int clIdx = 0;
		for(int i = 0; i<el.getClasses().length ; i++){
			Log.e("TEST", filterClInt+" "+el.getClasses()[i]);
			if(filterClInt == el.getClasses()[i]){
				clIdx = i;
			}
		}
		Log.e("TEST", "setting selected item to: "+clIdx);
		classAdapter.setData(el.getClasses());
		clazz.setSelection(clIdx);
		
		educationLevel.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {			
				EducationLevel el = EducationLevel.values()[position];				
				classAdapter.setData(el.getClasses());				
				if(el.getClasses().length>0){
					clazz.setEnabled(true);
					clazz.setSelection(0,false);
				}else{
					clazz.setEnabled(false);
				}
				new UpdateProfileTask(ProfileActivity.this).execute();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {					
			}
			
		});		
		
		clazz.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				new UpdateProfileTask(ProfileActivity.this).execute();				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {				
			}
		});		
		
		cbSkipPassword = (CheckBox) findViewById(R.id.ap_cb_password);
		if(UserContext.getCurrentUser().canOpenAdminSettings()){
			cbSkipPassword.setVisibility(View.GONE);
		}
		cbSkipPassword.setChecked(!UserContext.getCurrentUser().getSettings().isPasswordRequired());
		cbSkipPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				new UpdateProfileTask(ProfileActivity.this).execute();
			}
		});
		
		btnChangePassword = (Button) findViewById(R.id.ap_btn_change_password);
		btnChangePassword.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {						
				showChangePasswordDialog();
			}
		});
	}
	
	@Override
	protected void onResume() {		
		super.onResume();
		if(checkUserLoggedIn()){
			btnChangePassword.setVisibility(UserContext.getCurrentUser().isInitialAccount()?View.INVISIBLE:View.VISIBLE);	
		}		
	}
	
	@Override
	public void onBackPressed() {		
		if(!Util.isTablet(this)){
			if(checkUserLoggedIn()){
				int preferredList = UserContext.getCurrentUser().getSettings().getPreferredListType();
				if(preferredList==Settings.LIST_TYPE_FLAT){
					startActivity(new Intent(this,BookListActivity.class));										
				}else{					
					startActivity(new Intent(this,BookPagerActivity.class));										
				}
				finish();
			}	
		}else{
			startActivity(new Intent(this,BookGridActivity.class));
			finish();
		}		
	}
	
	private void showChangePasswordDialog(){
		if(checkUserLoggedIn()){
			final FragmentTransaction ft = getFragmentManager().beginTransaction();
			final Fragment prev = getFragmentManager().findFragmentByTag(UserDialog.TAG);
			if(prev!=null){
				ft.remove(prev);
			}
			ft.addToBackStack(null);
			ft.commit();
			final ChangePasswordDialog dial = ChangePasswordDialog.getInstance(UserContext.getCurrentUser());
			dial.show(getFragmentManager(), ChangePasswordDialog.TAG);	
		}		
	}
	
	static class UpdateProfileTask extends AsyncTask<Void, Void, Void>{
		
		private ProfileActivity ctx;
		
		public UpdateProfileTask(ProfileActivity ctx) {
			this.ctx = ctx;
		}		
		
		@Override
		protected Void doInBackground(Void... params) {			
			if(ctx.checkUserLoggedIn()){
				int eduLevelPos = ctx.educationLevel.getSelectedItemPosition();
				EducationLevel el = EducationLevel.values()[eduLevelPos];
				String eduLevelVal = null;
				String clazzVal = null;
				if(eduLevelPos==0){		
				}else{
					eduLevelVal = el.getValue();
					int clazzPos = ctx.clazz.getSelectedItemPosition();
					clazzVal = String.valueOf(el.getClasses()[clazzPos]);
				}
				ContentValues vals = new ContentValues();				
				Settings s = UserContext.getCurrentUser().getSettings().buildUpon()					
						.withPasswordRequired(!ctx.cbSkipPassword.isChecked())
						.withPreferredListType(ctx.rgLibrary.getCheckedRadioButtonId()==R.id.ap_rb_list?Settings.LIST_TYPE_FLAT:Settings.LIST_TYPE_PAGER)
						.withTeacher(ctx.cbTeacher.isChecked())
						.withBookFilterEducationLevel(eduLevelVal)
						.withBookFilterClass(clazzVal)
						.build();			
				vals.put(UsersTable.C_SETTINGS, s.toJsonString());
				ctx.getContentResolver().update(UsersProvider.UriHelper.userByLocalUserId(UserContext.getCurrentUser().getLocalUserId()), vals, null, null);	
			}			
			return null;
		}
		
		@Override
		protected void onCancelled(Void result) {		
			super.onCancelled(result);
			ctx = null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			ctx = null;
		}
		
	}
	
	static class EducationLevelAdapter extends ArrayAdapter<EducationLevel>{
				
		public EducationLevelAdapter(Context context, int resource) {
			super(context, resource);
		}
		
		@Override
		public int getCount() {
			return EducationLevel.values().length;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {			
			final LayoutInflater inf = LayoutInflater.from(getContext());
			if(convertView==null){
				convertView = inf.inflate(android.R.layout.simple_spinner_item, parent, false);
			}			
			((TextView)convertView).setText(EducationLevel.values()[position].getDisplayName());			
			return convertView;
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			final LayoutInflater inf = LayoutInflater.from(getContext());
			if(convertView==null){
				convertView = inf.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);				
			}
			((TextView)convertView).setText(EducationLevel.values()[position].getDisplayName());
			return convertView;
		}		
		
	}
	
	static class ClassAdapter extends ArrayAdapter<String>{

		private int[] data = new int[]{};
		
		public ClassAdapter(Context context, int resource) {
			super(context, resource);
		}
		
		@Override
		public int getCount() {
			return data.length;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final LayoutInflater inf = LayoutInflater.from(getContext());
			if(convertView==null){
				convertView = inf.inflate(android.R.layout.simple_spinner_item, parent, false);
			}
			TextView tv = (TextView)convertView;		
			setText(position, tv);
			return tv;
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			final LayoutInflater inf = LayoutInflater.from(getContext());
			if(convertView==null){
				convertView = inf.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);				
			}
			TextView tv = (TextView)convertView;		
			setText(position, tv);
			return tv;
		}	
		
		private void setText(int position, TextView tv){
			int cls = data[position];
			String disp = "";
			switch(cls){
			case 1:
				disp = "Pierwsza";
				break;
			case 2:
				disp = "Druga";
				break;
			case 3:
				disp = "Trzecia";
				break;
			case 4:
				disp = "Czwarta";
				break;
			case 5:
				disp = "Piąta";
				break;
			case 6:
				disp = "Szósta";
			}
			tv.setText(disp);
		}
		
		public void setData(int[] data){
			this.data = data;
			notifyDataSetChanged();
		}
		
	}

}
