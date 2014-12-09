package oly.netpowerctrl.utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.FavCollection;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.main.MainActivity;

/**
 * Show a permanent notification in the android statusbar and add favourite scenes and devicePorts as actions.
 */
public class AndroidStatusBarService extends Service implements RecognitionListener, onDataLoaded {
    public static final int REQUEST_CODE = 8978;
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static final int MSG_TIMEOUT_COMMAND_MODE = 3;
    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECOGNIZER_START_LISTENING:
                    startListen();
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    cancelListen();
                    break;

                case MSG_TIMEOUT_COMMAND_MODE:
//                    timeout();
            }
        }
    };
    protected final Messenger mServerMessenger = new Messenger(handler);
    private static final String TAG = "SpeechService";
    public static AndroidStatusBarService instance;
    protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    //protected MediaPlayer mMediaPlayerCommandOn = new MediaPlayer();
    protected MediaPlayer mMediaPlayerCommandOK = new MediaPlayer();
    protected MediaPlayer mMediaPlayerCommandTimeout = new MediaPlayer();
    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(15000, 15000) {

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            mIsCountDownOn = false;
            cancelListen();
            startListen();
        }
    };
    NotificationManager mNotificationManager;
    private onCollectionUpdated<FavCollection, FavCollection.FavItem> collectionUpdateListener =
            new onCollectionUpdated<FavCollection, FavCollection.FavItem>() {
                @Override
                public boolean updated(@NonNull FavCollection collection, FavCollection.FavItem item, @NonNull ObserverUpdateActions action, int position) {
                    createNotification();
                    return true;
                }
            };
    private boolean mIsStreamSolo;
    //    private boolean timeoutRunning = false;
    private CharSequence command_word;

    public static void startOrStop(Context context) {
        if (SharedPrefs.isNotification(context))
            context.startService(new Intent(context, AndroidStatusBarService.class));
        else
            context.stopService(new Intent(context, AndroidStatusBarService.class));
    }

    private void createNotification() {
        Context context = this;
        Intent startMainIntent = new Intent(context, MainActivity.class);
        startMainIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent startMainPendingIntent =
                PendingIntent.getActivity(context, (int) System.currentTimeMillis(), startMainIntent, 0);

        Notification.Builder b;

        AppData appData = AppData.getInstance();
        FavCollection g = appData.favCollection;
        int maxLength = 0;
        List<FavCollection.FavItem> items = new ArrayList<>(g.getItems());

        if (items.size() == 0) {
            b = new Notification.Builder(context)
                    .setContentText(context.getString(R.string.statusbar_no_favourites))
                    .setContentTitle(context.getString(R.string.app_name))
                    .setSmallIcon(R.drawable.netpowerctrl)
                    .setContentIntent(startMainPendingIntent)
                    .setOngoing(true);
        } else {
            RemoteViews remoteViewsRoot = new RemoteViews(context.getPackageName(), R.layout.statusbar_container);

            for (FavCollection.FavItem favItem : items) {
                Executable executable = appData.findExecutable(favItem.executable_uid);

                if (executable == null) {
                    // No executable found for the given uid. We remove the item from the favCollection now.
                    g.setFavourite(favItem.executable_uid, false);
                    continue;
                }

                if (maxLength > 3) break;
                ++maxLength;

                // This intent will be executed by a click on the widget
                Intent clickIntent = AndroidShortcuts.createShortcutExecutionIntent(context, favItem.executable_uid, false, true);
                if (clickIntent == null)
                    continue;
                clickIntent.setAction(Intent.ACTION_MAIN);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), clickIntent, 0);

                Bitmap bitmap = LoadStoreIconData.loadBitmap(context, executable.getUid(), LoadStoreIconData.IconState.OnlyOneState, null);
                RemoteViews remoteViewsWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
                remoteViewsWidget.setTextViewText(R.id.widget_name, executable.getTitle());
                remoteViewsWidget.setImageViewBitmap(R.id.widget_image, bitmap);
                remoteViewsWidget.setViewVisibility(R.id.widget_status, View.GONE);
                remoteViewsWidget.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);
                remoteViewsRoot.addView(R.id.notifications, remoteViewsWidget);
            }

            b = new Notification.Builder(context)
                    .setContent(remoteViewsRoot)
                    .setSmallIcon(R.drawable.netpowerctrl)
                    .setContentIntent(startMainPendingIntent)
                    .setOngoing(true);
        }

        mNotificationManager.cancel(1);
        //noinspection deprecation
        mNotificationManager.notify(1, b.getNotification());

        AppData.useAppData();
        AppData.observersOnDataLoaded.register(this);
    }

    @Override
    public boolean onDataLoaded() {
        AppData.getInstance().favCollection.registerObserver(collectionUpdateListener);
        return false;
    }

    private void loadMediaFile(String file, MediaPlayer mediaPlayer) {
        try {
            AssetFileDescriptor afd = getAssets().openFd(file);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            //Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            //mMediaPlayerCommandOn.setDataSource(this, soundUri);
            if (mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.setLooping(false);
                mediaPlayer.prepare();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            //Bundle results = data.getExtras();
            //Log.w(TAG, "Result: ");
            List<String> list = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //List<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (list != null) {
                //float[] value = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                for (int i = 0; i < list.size(); ++i) {
                    String t = list.get(i).toLowerCase();
                    //Log.w(TAG, String.valueOf(value[i]) + ":" + list.get(i));
                    Log.w(TAG, t);
                    if (doCommand(t)) {
                        mMediaPlayerCommandOK.start();
                        return;
                    }
                }
            }
        }
//        if (!timeoutRunning)
        mMediaPlayerCommandTimeout.start();
        handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 500);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotification();
    }

//    private void timeout() {
//        Log.w(TAG, "timeout");
//        if (commandMode)
//            mMediaPlayerCommandTimeout.start();
//        timeoutRunning = false;
//        commandMode = false;
//    }

    private void startListen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // turn off beep sound
            if (!mIsStreamSolo) {
                mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                mIsStreamSolo = true;
            }
        }
        if (!mIsListening) {
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            mIsListening = true;
            //Log.d(TAG, "message start listening"); //$NON-NLS-1$
        }
    }

    private void cancelListen() {
        if (mIsStreamSolo) {
            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
            mIsStreamSolo = false;
        }
        mSpeechRecognizer.cancel();
        mIsListening = false;
        //Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
    }

    @Override
    public void onDestroy() {
        instance = null;
        mNotificationManager.cancel(1);
        Log.w(TAG, "onDestroy");
        Toast.makeText(this, "STOP", Toast.LENGTH_SHORT).show();
        if (mIsCountDownOn)
            mNoSpeechCountDown.cancel();
        if (mSpeechRecognizer != null)
            mSpeechRecognizer.destroy();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (SharedPrefs.isSpeechEnabled(this)) {
            command_word = SharedPrefs.getSpeechCommandWord(this);
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(this);
            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            //mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    this.getPackageName());

            //loadMediaFile("sounds/computerbeep_75.mp3", mMediaPlayerCommandOn);
            loadMediaFile("sounds/computerbeep_74.mp3", mMediaPlayerCommandTimeout);
            loadMediaFile("sounds/computerbeep_16.mp3", mMediaPlayerCommandOK);

            mMediaPlayerCommandOK.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 100);
                }
            });

            Log.w(TAG, "onCreate");
            mMediaPlayerCommandOK.start();
        } else if (mAudioManager != null) {
            if (mIsCountDownOn)
                mNoSpeechCountDown.cancel();
            if (mSpeechRecognizer != null)
                mSpeechRecognizer.destroy();
            mAudioManager = null;
            mSpeechRecognizer = null;
            //mMediaPlayerCommandOn = null;
            mMediaPlayerCommandTimeout = null;
            mMediaPlayerCommandOK = null;
            mSpeechRecognizerIntent = null;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }

    @Override
    public void onBeginningOfSpeech() {
        // speech input will be processed, so there is no need for count down anymore
        if (mIsCountDownOn) {
            mIsCountDownOn = false;
            mNoSpeechCountDown.cancel();
        }
        Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
    }

    @Override
    public void onError(int error) {
        if (mIsCountDownOn) {
            mIsCountDownOn = false;
            mNoSpeechCountDown.cancel();
        }
        mIsListening = false;
        if (error == 9) {
            Log.w(TAG, "No permissions!");
            return;
        }
        if (error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SERVER) {
            Log.w(TAG, "ERROR: " + error);
            mMediaPlayerCommandTimeout.start();
            return;
        }

        startListen();
    }

    private void startPopupListener() {
        // Start intent
        Activity activity = MainActivity.instance;
        if (activity != null)
            activity.startActivityForResult(mSpeechRecognizerIntent, REQUEST_CODE);
        else
            startListen();
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//        {
//            mIsCountDownOn = true;
//            mNoSpeechCountDown.start();
//            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false); //NEW
//        }
        //Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
    }

    @Override
    public void onResults(Bundle results) {
        cancelListen();
        //Log.w(TAG, "Result: ");
        List<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < list.size(); ++i) {
            String t = list.get(i).toLowerCase();
            if (t.contains(command_word)) {
                startPopupListener();
                //mMediaPlayerCommandOn.start();
                return;
            }
        }
//        if (!timeoutRunning)
        handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 100);
    }

    private boolean doCommand(String command) {
        boolean precise = command.startsWith("starte");
        command = command.replace("starte ", "");
        Executable executable = AppData.getInstance().findFirstExecutableByName(command.substring(0).trim(), !precise);
        if (executable != null) {
            Log.w(TAG, "Starte " + executable.getTitle());
            handler.removeMessages(MSG_TIMEOUT_COMMAND_MODE);
            AppData.getInstance().executeToggle(executable, null);
            return true;
        }

        return false;
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }
}
