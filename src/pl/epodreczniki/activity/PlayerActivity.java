package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import android.app.Activity;
import android.content.res.Configuration;

public class PlayerActivity extends Activity {
	
	public static final String EXTRA_VIDEO_URL = "ex_vid";

	private VideoView video;
	
	private ProgressBar pb;
	
	private int lastPosition = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_player);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		video = (VideoView) findViewById(R.id.ap_vv);
		pb = (ProgressBar) findViewById(R.id.ap_pb);
		final String url = getIntent().getStringExtra(EXTRA_VIDEO_URL);
		final MediaController ctl = new MediaController(this) {
			@Override
			public boolean dispatchKeyEvent(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
					((Activity) getContext()).finish();
				}
				return super.dispatchKeyEvent(event);
			}						
		};
		ctl.setAnchorView(video);		
		video.setMediaController(ctl);
		video.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {				
				mp.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
					@Override
					public void onBufferingUpdate(MediaPlayer mp, int percent) {						
						if (percent == 100) {
							pb.setVisibility(View.INVISIBLE);							
						}else{
							pb.setVisibility(View.VISIBLE);
						}
					}
				});				
			}
		});
		video.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {				
				mp.seekTo(0);
			}
		});
		video.setOnErrorListener(new OnErrorListener(){
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Toast.makeText(PlayerActivity.this, R.string.toast_cannot_play, Toast.LENGTH_LONG).show();
				return true;
			}			
		});
		video.setVideoURI(Uri.parse(url));		
	}

	@Override
	protected void onResume() {
		super.onResume();
		video.seekTo(lastPosition);
		video.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		lastPosition = video.getCurrentPosition();
		video.pause();
	}		
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
}
