package pl.epodreczniki.util;

import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

public final class ConnectionDetector {
	
	private ConnectionDetector(){		
	}
	
	private static volatile boolean dbEmpty = false;
	
	public static boolean isDbEmpty(){
		synchronized(ConnectionDetector.class){			
			return dbEmpty;
		}
	}
	
	public static boolean isConnected(Context ctx){
		ConnectivityManager connectivity = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null){
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++){
                	if (info[i].getState() == NetworkInfo.State.CONNECTED){
                        return true;
                    }                	
                }                    
        }
        return false;
	}
			
	public static void checkDb(Context ctx){
		new Thread(new DbChecker(ctx)).start();
	}
	
	public static boolean isExternalStorageAvailable(){
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}				
	
	static class DbChecker extends ContextAwareRunnable{

		public DbChecker(Context ctx) {
			super(ctx);
		}

		@Override
		protected void doWork(Context ctx) {
			Cursor c = ctx.getContentResolver().query(BooksProvider.BOOKS_URI, BooksTable.COLUMNS, null, null, null);
			if(c!=null){
				synchronized (ConnectionDetector.class) {
					dbEmpty=c.getCount()<1;	
				}				
				c.close();
			}
		}
		
	}

}
