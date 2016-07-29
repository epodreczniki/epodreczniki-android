package pl.epodreczniki.util;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.model.User;

public final class UserContext {
	
	private static volatile User currentUser;
	
	private static volatile UserObserver observer;
	
	private static volatile boolean invalidateOptions = false;
	
	public static synchronized void login(User user, Context ctx){		
		if(observer!=null){
			ctx.getApplicationContext().getContentResolver().unregisterContentObserver(observer);
		}
		currentUser = user;
		observer = new UserObserver(new Handler(), ctx.getApplicationContext(), user.getLocalUserId());
		ctx.getApplicationContext().getContentResolver().registerContentObserver(UsersProvider.UriHelper.userByLocalUserId(user.getLocalUserId()), false, observer);
	}
	
	public static synchronized void logout(Context ctx){
		currentUser = null;
		ctx.getApplicationContext().getContentResolver().unregisterContentObserver(observer);
		observer = null;
	}
	
	public static synchronized void updateUser(User u){
		Log.e("UserContext","UPDATING USER "+u.getAccountType());
		currentUser = u;
	}
	
	public static synchronized User getCurrentUser(){
		return currentUser;
	}
	
	public static synchronized boolean isLoggedIn(){
		return currentUser!=null;
	}
	
	public static synchronized boolean shouldInvalidateOptions(){
		return invalidateOptions;
	}
	
	public static synchronized void setInvalidateOptions(boolean value){
		invalidateOptions = value;
	}
	
	private UserContext(){		
	}		
	
	static class UserObserver extends ContentObserver{
		
		private Context ctx;
		
		private final long userId;
		
		public UserObserver(Handler handler, Context ctx, long userId){
			super(handler);
			this.ctx = ctx.getApplicationContext();
			this.userId = userId;
		}
		
		@Override
		public boolean deliverSelfNotifications() {
			return false;
		}
		
		@Override
		public void onChange(boolean selfChange) {			
			new QueryUserTask(ctx, userId).execute();
		}
		
	}
	
	static class QueryUserTask extends AsyncTask<Void,Void,User>{

		private final WeakReference<Context> ctxRef;
		
		private final long userId;
		
		QueryUserTask(Context ctx, long userId){
			ctxRef = new WeakReference<Context>(ctx);
			this.userId = userId;
		}
		
		@Override
		protected User doInBackground(Void... params) {
			User res = null;
			final Context ctx = ctxRef.get();
			if(ctx!=null){
				final Cursor c = ctx.getContentResolver().query(UsersProvider.UriHelper.userByLocalUserId(userId), UsersTable.COLUMNS, null, null, null);
				if(c!=null){
					if(c.moveToFirst()){
						res = Util.getUserBuilderFromCursor(c, c.getPosition()).build();
					}
					c.close();
				}				
			}
			UserContext.updateUser(res);
			return res;
		}
		
	}

}
