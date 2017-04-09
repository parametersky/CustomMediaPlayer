package kcc.sorg.mediaplayer;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;

public class PlayerActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{
    private static final String TAG = "PlayerActivity";
    private TextureView mView;
    private boolean surfaceReady = false;
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            String path ="rtsp://218.204.223.237:554/live/1/67A7572844E51A64/f68g2mj7wjua3la7.sdp";
            MiniVideoPlayer player = new MiniVideoPlayer(getBaseContext());
            player.setPath(path);
            player.setSurface(mView.getSurfaceTexture());
            player.prepare();
            player.start();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Log.i(TAG, "onCreate: ");

        mView = (TextureView)findViewById(R.id.texttureview);
        mView.setSurfaceTextureListener(this);
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(surfaceReady) {
                    String path = "/storage/emulated/0/DCIM/Camera/VID_20151216_102422.mp4";
                    final MiniVideoPlayer player = new MiniVideoPlayer(getBaseContext());
                    player.setPath(path);
                    player.setSurface(mView.getSurfaceTexture());
                    player.prepare();
                    new Thread(new Runnable() {
                        public void run() {
                            player.start();
                        }
                    }).start();
                }
            }
        });
        Log.i(TAG, "onCreate: end");

    }

    public void onResume(){
        super.onResume();
        Log.i(TAG, "onResume: ");
//        mHandler.sendEmptyMessageDelayed(0, 1000);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        surfaceReady = true;

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
