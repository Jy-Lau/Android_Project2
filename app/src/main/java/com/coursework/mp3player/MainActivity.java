package com.coursework.mp3player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ItemClickListener, SeekBar.OnSeekBarChangeListener {
    private RecyclerView recyclerView;
    private SeekBar seekBar;
    private ImageButton btn_playpause;
    private ImageButton btn_stop;
    private final List<MusicModel> musicModels = new ArrayList<>(); //list of music file(can be shown in recyclerview)
    private final String globalPath = Environment.getExternalStorageDirectory().getPath() + "/Download/"; //change the path to access to different directory
    private final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    private Intent music;
    private MP3Player mService; //MP3 Player service
    private TextView textView_currentPos;
    private TextView textview_totalduration;
    private BroadcastReceiver mBroadcastReceiver; //Broadcast receiver to receive broadcast from MP3 Player
    public static final String ACTION_MEDIA_START = "ACTION_MEDIA_START";
    public static final String ACTION_MEDIA_COMPLETE = "ACTION_MEDIA_COMPLETE";
    public static final String ACTION_MEDIA_RESUME = "ACTION_MEDIA_RESUME";
    public static final String ACTION_MEDIA_PAUSE = "ACTION_MEDIA_PAUSE";
    private static final int SB_FACTOR = 1000;   //seek bar has maximum value of 1000, therefore have to divide milliseconds by factor 1000 before input to seek bar
    private static final String[] PERMISSION_STRING = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE = 1;
    private CountDownTimer mCountDownTimer;
    private long mTimeLeftInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView_currentPos = findViewById(R.id.textview_currentpos);
        textview_totalduration = findViewById(R.id.textview_totalduration);
        btn_playpause = findViewById(R.id.btn_playpause);
        btn_stop = findViewById(R.id.btn_stop);
        seekBar = findViewById(R.id.seekbar_progress);
        seekBar.setOnSeekBarChangeListener(this);seekBar.setEnabled(false);
        music = new Intent(this, MP3Player.class);
        recyclerView = findViewById(R.id.recyclerview_main);
        if (!hasPermission(PERMISSION_STRING)) {
            requestPermissions(PERMISSION_STRING, REQUEST_CODE);
        } else readfromDirecotry();
    }

    private void readfromDirecotry() {
        File musicDir = new File(globalPath);
        File[] newFile = musicDir.listFiles(new AudioFilter()); //Filter only music file found in global path
        if (newFile == null || newFile.length == 0) {
            Toast.makeText(this, "No music file found in " + globalPath, Toast.LENGTH_LONG).show();
        } else setUpRecyclerViewwithFile(newFile); //set up recycler view
    }

    private boolean hasPermission(String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readfromDirecotry();
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    //Bind MP3 Player service to MainActivity
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MP3Player.LocalBinder binder = (MP3Player.LocalBinder) service;
            mService = binder.getService();
            Log.d("durationasdfasdf: ", "" + mService.getDuration());
            Log.d("progress: ", "" + mService.getProgress());
            setUpTimer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    private void setUpTimer() {
        seekBar.setEnabled(true);
        mTimeLeftInMillis = mService.getDuration() - mService.getProgress();
        seekBar.setMax(mService.getDuration() / SB_FACTOR);
        long min=getCalculatedMin(mService.getDuration());
        long seconds=getCalculatedSec(mService.getDuration());
        textview_totalduration.setText("/" + min + ":" + String.format("%02d", seconds));
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                long min = getCalculatedMin((int)millisUntilFinished);
                long seconds = getCalculatedSec((int)millisUntilFinished);
                textView_currentPos.setText(min + ":" + String.format("%02d", seconds));
                seekBar.setProgress(mService.getProgress() / SB_FACTOR);
                mTimeLeftInMillis = millisUntilFinished;
            }
            public void onFinish() {}
        }.start();
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mService.pause();
        btn_playpause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
    }

    private void resumeTimer() {
        mService.resume();
        setUpTimer();
        btn_playpause.setImageResource(R.drawable.ic_pause_black_24dp);
    }

    private void stopTimer() {
        mCountDownTimer.cancel();
        seekBar.setProgress(0);
        seekBar.setEnabled(false);
        btn_playpause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        textview_totalduration.setText("/0.00");
        textView_currentPos.setText("0.00");
    }
    //callbacks from MP3Player service
    private void registerBroadcast() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                switch (action) {
                    case ACTION_MEDIA_START:    //receive broadcast from MP3 Player where music is started
                        try{
                            mCountDownTimer.cancel();
                            setUpTimer();
                        }
                        catch(NullPointerException e){
                            e.printStackTrace();
                        }
                        break;
                    case ACTION_MEDIA_COMPLETE: //receive broadcast from MP3 Player where music is completed
                        stopTimer();
                        stopServices();
                        break;
                    case ACTION_MEDIA_RESUME: //receive broadcast from MP3 Player where music is resume (clicked in notification)
                        resumeTimer();
                        break;
                    case ACTION_MEDIA_PAUSE: //receive broadcast from MP3 Player where music is pause (clicked in notification)
                        pauseTimer();
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_MEDIA_START));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_MEDIA_COMPLETE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_MEDIA_PAUSE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_MEDIA_RESUME));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerBroadcast();
        if (isMyServiceRunning(MP3Player.class)) {//if MP3 Player service is running, set up buttons and seek bar and start handler timer
            bindService(music, connection, BIND_AUTO_CREATE);
        }
    }

    private void setUpRecyclerViewwithFile(File[] list) {
        if (list != null) {
            for (File f :
                    list) {
                mediaMetadataRetriever.setDataSource(f.getAbsolutePath());
                String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                musicModels.add(new MusicModel(title, artist, f.getAbsolutePath()) {
                });
            }
            final RecyclerViewAdapter myAdapter = new RecyclerViewAdapter(this, musicModels, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(myAdapter);
        }
    }

    public void onClick(View v) {
        if (mService == null) {
            Toast.makeText(this, "Select a song from the list", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.btn_playpause: //play&pause button is pressed
                switch (mService.getState()) {
                    case PAUSED:    //User resume music during pause state
                        resumeTimer();
                        break;
                    case PLAYING: //User pause music during playing state
                        pauseTimer();
                        break;
                }
                break;
            case R.id.btn_stop: //stop button is pressed
                //if current state is already stop, then return
                if (mService.getState() == MP3Player.MP3PlayerState.STOPPED) {
                    return;
                }
                stopTimer();
                stopServices();
                btn_playpause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                break;
        }
    }

    //play the music item clicked in recyclerview
    @Override
    public void onItemClick(View view, int position, boolean isLongClick) {
        btn_playpause.setImageResource(R.drawable.ic_pause_black_24dp);
        seekBar.setEnabled(true);
        music.putExtra("musicpath", musicModels.get(position).getFilePath());
        mediaMetadataRetriever.setDataSource(musicModels.get(position).getFilePath());
        String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        music.putExtra("artist", artist);
        music.putExtra("title", title);
        music.setAction(MP3Player.ACTION_START_FOREGROUND_SERVICE);
        startService(music);
        bindService(music, connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mService != null && fromUser && mService.getState() != MP3Player.MP3PlayerState.STOPPED) {
            long min=getCalculatedMin((seekBar.getMax() - progress) * SB_FACTOR);
            long seconds=getCalculatedSec((seekBar.getMax() - progress) * SB_FACTOR);
            textView_currentPos.setText(min + ":" + String.format("%02d", seconds));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        pauseTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mService.seekTo(seekBar.getProgress() * SB_FACTOR); //move to desire music duration after user stop tracking
        resumeTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(connection);
            unregisterReceiver(mBroadcastReceiver);
        }
    }

    private long getCalculatedMin(int min){
        long minutes = TimeUnit.MILLISECONDS.toMinutes(min);
        return minutes;
    }
    private long getCalculatedSec(int sec){
        long seconds = TimeUnit.MILLISECONDS.toSeconds(sec) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sec));
        return seconds;
    }
    private void stopServices(){
        music.setAction(MP3Player.ACTION_STOP_FOREGROUND_SERVICE);
        startService(music);
    }
    //check if MP3 Player service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
