package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TermsActivity extends Activity {

	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_terms);

		Button btn_accButton = (Button) findViewById(R.id.at_btn_acceptTerms);
			
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(!Util.termsAccepted(this)){
			btn_accButton.setVisibility(View.VISIBLE);
			btn_accButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(TermsActivity.this)
							.setTitle(R.string.dialog_title_terms_confirm)
							.setMessage(R.string.dialog_message_terms_confirm)
							.setPositiveButton(R.string.dialog_ans_yes,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {											
											SharedPreferences.Editor edit = prefs
													.edit();											
											edit.putInt(Constants.PREF_ACCEPTED_VERSION, Util.getAppVersion(TermsActivity.this));											
											edit.apply();

											Intent i = new Intent(TermsActivity.this,CheckLoginActivity.class);
											i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
											startActivity(i);
											finish();											

										}
									}).setNegativeButton(R.string.dialog_ans_no, null)
							.show();
				}
			});
		} else {
			btn_accButton.setVisibility(View.GONE);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayUseLogoEnabled(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		}
		return (super.onOptionsItemSelected(item));
	}

}
