package com.coursework.mp3player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

/**
 * Created by pszmdf on 06/11/16.
 */
public class MP3Player extends Service {

    private MediaPlayer mPlayer;
    protected MP3PlayerState state = MP3PlayerState.STOPPED;
    protected String filePath;
    private int length = 0;
    private String channelID = "channelID";
    private Notification notification;
    private CharSequence name = "Channel 1";
    private String description = "Channel Description";
    private boolean songCheck = false;
    private final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";

    public final static String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public final static String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    public final static String ACTION_PAUSE = "ACTION_PAUSE";

    public final static String ACTION_PLAY = "ACTION_PLAY";

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        MP3Player getService() {
            // Return this instance of LocalService so clients can call public methods
            return MP3Player.this;
        }
    }

    private void startForegroundService(String artist, String title) {
        Log.d(TAG_FOREGROUND_SERVICE, "Start foreground service.");
        if (!songCheck) {
            // Create notification default intent.
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
            Intent playIntent = new Intent(this, MP3Player.class);
            playIntent.setAction(ACTION_PLAY);
            PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);
            NotificationCompat.Action playAction = new NotificationCompat.Action(R.drawable.ic_play_arrow_black_24dp, "Play", pendingPlayIntent);


            // Add Pause button intent in notification.
            Intent pauseIntent = new Intent(this, MP3Player.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pendingPrevIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
            NotificationCompat.Action prevAction = new NotificationCompat.Action(R.drawable.ic_pause_black_24dp, "Pause", pendingPrevIntent);

            notification = new NotificationCompat.Builder(this, channelID).setOnlyAlertOnce(true)
                    .setContentTitle(title).setAutoCancel(true).setColor(Color.GREEN).setContentIntent(pendingIntent)
                    .setContentText(artist).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).addAction(playAction).addAction(prevAction).setSmallIcon(R.drawable.ic_launcher_foreground).build();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelID, name, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(description);
                channel.enableVibration(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
            }
            startForeground(1, notification);
        }
    }

    public enum MP3PlayerState {
        ERROR,
        PLAYING,
        PAUSED,
        STOPPED,
    }

    private void initiateMediaPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                state=MP3PlayerState.STOPPED;
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.ACTION_MEDIA_COMPLETE);
                sendBroadcast(broadcastIntent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String musicpath = intent.getStringExtra("musicpath");
            String artist = intent.getStringExtra("artist");
            String title = intent.getStringExtra("title");
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    loadfromitem(musicpath); //load music file path, and if music player is currently playing, set variable songcheck to true, so that playmusic() wont be playing music again
                    playMusic(); //play music load by musicpath if songcheck is false
                    startForegroundService(artist, title); //start foreground service by displaying artist name, song title name, Play Button, Pause Button.
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService(); //stop foreground service and dismiss notification
                    break;
                case ACTION_PLAY:
                    resume(); //Play music
                    break;
                case ACTION_PAUSE:
                    pause(); //Pause music
                    break;
            }
        }

        return START_NOT_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    private void stopForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");
        stop();
        // Stop foreground service and remove the notification.
        stopForeground(false);

        // Stop the foreground service.
        stopSelf();
    }

    private void playMusic() {
        if (!songCheck) {
            initiateMediaPlayer();  //initiate new Music Player to play different music
            this.state = MP3PlayerState.PLAYING;
            try {
                mPlayer.setDataSource(this.filePath);
                mPlayer.prepare();
            } catch (IOException e) {
                Log.e("MP3Player", e.toString());
                e.printStackTrace();
                this.state = MP3PlayerState.ERROR;
                return;
            } catch (IllegalArgumentException e) {
                Log.e("MP3Player", e.toString());
                e.printStackTrace();
                this.state = MP3PlayerState.ERROR;
                return;
            }
            mPlayer.start();
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("duration",getDuration());
            broadcastIntent.setAction(MainActivity.ACTION_MEDIA_START);
            sendBroadcast(broadcastIntent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public MP3PlayerState getState() {
        return this.state;
    }


    private void loadfromitem(String filePath) {
        if (mPlayer != null) {
            if (state == MP3PlayerState.STOPPED) {
                songCheck = false;
                this.filePath = filePath;
                return;
            } else if (state == MP3PlayerState.PLAYING && filePath.equals(this.filePath)) {
                songCheck = true;
                return;
            } else {
                stop(); //stop the mp3 player if music path loaded is not the same as current playing music
            }
            songCheck = false;
        }
        this.filePath = filePath;
    }

    public int getProgress() {
        if (mPlayer != null) {
            if (this.state == MP3PlayerState.PAUSED || this.state == MP3PlayerState.PLAYING)
                return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mPlayer != null)
            if (this.state == MP3PlayerState.PAUSED || this.state == MP3PlayerState.PLAYING)
                return mPlayer.getDuration();
        return 0;
    }

    public void pause() {
        if (state == MP3PlayerState.PLAYING) {
            mPlayer.pause();
            state = MP3PlayerState.PAUSED;
            length = mPlayer.getCurrentPosition();
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.ACTION_MEDIA_PAUSE);
            sendBroadcast(broadcastIntent);
        }
    }

    private void stop() {
        if (mPlayer != null) {
            if (state == MP3PlayerState.PLAYING) {
                mPlayer.stop();
                state = MP3PlayerState.STOPPED;
                mPlayer.reset();
                mPlayer.release();
            }
        }
    }

    public void resume() {
        if (mPlayer != null) {
            if (state != MP3PlayerState.PLAYING) {
                this.state = MP3PlayerState.PLAYING;
                mPlayer.seekTo(length);
                mPlayer.start();
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.ACTION_MEDIA_RESUME);
                sendBroadcast(broadcastIntent);
            }
        }
    }

    public void seekTo(int i) {
        if (mPlayer != null) {  //dont care state
            length = i;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            try {
                stop();
            } finally {
                mPlayer = null;
            }
        }
    }
}
