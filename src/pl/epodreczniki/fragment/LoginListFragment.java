package pl.epodreczniki.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.epodreczniki.R;
import pl.epodreczniki.db.ExerciseStatesProvider;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LoginListFragment extends Fragment{

	public static final String TAG = "login.list.fragment.tag";
	
	public static final String ARGS_LIST_MODE = "llf_args_list_mode";
	
	public static final String ARGS_ADMIN = "llf_args_admin";
	
	private LoginListCallback callback;
	
	private LoginAdapter adapter;
	
	private int mListMode;
	
	private boolean mAdmin;
	
	private TextView message;
	
	private ListView list;
	
	private Button addAccount;
	
	private View addAccountWrap;
	
	public interface LoginListCallback{
		
		void showLoginFrom(User user);
		
		void showUserDialog();	
		
	}
	
	public static LoginListFragment newInstance(boolean twoPane){
		final LoginListFragment res = new LoginListFragment();
		final int listMode = twoPane?ListView.CHOICE_MODE_SINGLE:ListView.CHOICE_MODE_NONE;
		final Bundle args = new Bundle();
		args.putInt(ARGS_LIST_MODE, listMode);
		args.putBoolean(ARGS_ADMIN, false);
		res.setArguments(args);
		return res;
	}
	
	public LoginListFragment(){		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if(activity instanceof LoginListCallback){
			callback = (LoginListCallback) activity;
		}else{
			throw new RuntimeException("wrong activity");
		}
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		callback = null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mListMode = getArguments().getInt(ARGS_LIST_MODE);
		mAdmin = getArguments().getBoolean(ARGS_ADMIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.f_login_list, container, false);
		message = (TextView) res.findViewById(R.id.fll_message);		
		list = (ListView) res.findViewById(R.id.fll_list);
		list.setChoiceMode(mListMode);
		adapter = new LoginAdapter();
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				callback.showLoginFrom(adapter.getItem(position));
			}
		});
		addAccount = (Button) res.findViewById(R.id.fll_add_account);
		addAccount.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				callback.showUserDialog();
			}
		});
		addAccountWrap = res.findViewById(R.id.fll_add_account_wrap);
		updateUI();
		return res;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(0, null, new LoaderListener());
	}
	
	private void onUsersUpdate(List<User> users){
		adapter.updateUsers(users);
	}
	
	public void updateUI(){
		Log.e("LLF","UPDATE UI");
		final Context ctx = getActivity();
		if(ctx!=null){								
			if(mAdmin){
				if(UserContext.getCurrentUser()==null){
					Toast.makeText(ctx, "Wystąpił błąd, zaloguj się ponownie", Toast.LENGTH_LONG).show();
					return;
				}
				if(UserContext.getCurrentUser().getAccountType()==User.ACCOUNT_TYPE_INITIAL){
					message.setText("Aby zarządzać użytkownikami utwórz konto administratora.");
					list.setVisibility(View.INVISIBLE);
					addAccount.setText("Utwórz konto administratora");
				}else{
					message.setText("Wybierz nazwę użytkownika aby zmienić jego ustawienia.");
					list.setVisibility(View.VISIBLE);
					addAccount.setText("Dodaj konto");
				}
			}else{
				addAccountWrap.setVisibility(Util.isAccountCreationOnLoginScreenAllowed(ctx)?View.VISIBLE:View.GONE);
				message.setText("Wybierz nazwę użytkownika aby się zalogować.");
				list.setVisibility(View.VISIBLE);
				addAccount.setText("Dodaj konto");
			}	
		}		
	}
	
	private class LoaderListener implements LoaderCallbacks<Cursor>{
		
		private Cursor data;

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {					
			return new CursorLoader(getActivity(), 
					UsersProvider.UriHelper.USERS_URI, 
					UsersTable.COLUMNS, null, null, UsersTable.C_ACCOUNT_TYPE+" ASC, "+UsersTable.C_USER_NAME+" ASC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if(data!=null){
				this.data = data;
				final List<User> res = new ArrayList<User>();
				while(data.moveToNext()){
					User.Builder b = Util.getUserBuilderFromCursor(data, data.getPosition());
					if(b!=null){
						res.add(b.build());
					}
				}
				data.moveToPosition(-1);
				onUsersUpdate(res);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			if(data!=null){
				data.close();
				data = null;
			}
		}						
	}
	
	private class LoginAdapter extends BaseAdapter{

		private List<User> data = new ArrayList<User>();
		
		@Override
		public int getCount() {			
			return data==null?0:data.size();
		}

		@Override
		public User getItem(int position) {			
			return data!=null&&data.size()>position?data.get(position):null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final User user = getItem(position);
			TextView name;
			ImageButton delete;
			if(convertView == null){
				final LayoutInflater inflater = LayoutInflater.from(getActivity());
				convertView = inflater.inflate(R.layout.v_login_list_item, parent, false);
				name = (TextView) convertView.findViewById(R.id.vlli_name);
				delete = (ImageButton) convertView.findViewById(R.id.vlli_delete);				
				convertView.setTag(R.id.vlli_name, name);
				convertView.setTag(R.id.vlli_delete, delete);
			}else{
				name = (TextView) convertView.getTag(R.id.vlli_name);
				delete = (ImageButton) convertView.getTag(R.id.vlli_delete);
			}
			name.setText(user.getUserName());
			delete.setVisibility(mAdmin&&user.isRegularUser()?View.VISIBLE:View.GONE);
			delete.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Log.e("LLF","DELETE");
					showDeleteQuestion(user);
				}
			});
			
			return convertView;
		}
		
		private void showDeleteQuestion(final User user){
			Context ctx = getActivity();
			if(ctx!=null){
				AlertDialog.Builder builder = new AlertDialog.Builder(ctx);				
				builder.setMessage(String.format("Czy na pewno usunąć konto %s?", user.getUserName()));
				builder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new DeleteAccountTask(getActivity(), user).execute();
					}
				});
				builder.setNegativeButton("Nie", null);
				builder.show();
			}
		}
		
		public void updateUsers(List<User> users){
			data = users;
			notifyDataSetChanged();
		}
		
	}	
	
	static class DeleteAccountTask extends AsyncTask<Void,Void,Void>{
		
		private Context ctx;
		
		private final User user;
		
		DeleteAccountTask(Context ctx,User user){
			this.ctx=ctx.getApplicationContext();
			this.user=user;
		}

		@Override
		protected Void doInBackground(Void... params) {
			int uDeleted = ctx.getContentResolver().delete(UsersProvider.UriHelper.userByLocalUserId(user.getLocalUserId()), null, null);
			int nDeleted = ctx.getContentResolver().delete(NotesProvider.UriHelper.noteByLocalUser(user.getLocalUserId()), null, null);
			int eDeleted = ctx.getContentResolver().delete(ExerciseStatesProvider.UriHelper.exerciseStatesByLocalUserId(user.getLocalUserId()), null, null);
			Log.e("DEL_ACC","deleted notes: "+nDeleted);
			Log.e("DEL_ACC","deleted users: "+uDeleted);
			Log.e("DEL_ACC","deleted exercise states: "+eDeleted);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			SharedPreferences.Editor ed = prefs.edit();
			Map<String,?> all = prefs.getAll();
			String test = Util.LP_PREFIX+user.getLocalUserId()+":";
			for(String key : all.keySet()){
				if(key.startsWith(test)){
					Log.e("DEL_ACC", "removed key: "+key);
					ed.remove(key);
				}
			}
			ed.commit();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {		
			super.onPostExecute(result);
			ctx = null;
		}
		
		@Override
		protected void onCancelled(Void result) {		
			super.onCancelled(result);
			ctx = null;
		}
		
		
	}
	
}
