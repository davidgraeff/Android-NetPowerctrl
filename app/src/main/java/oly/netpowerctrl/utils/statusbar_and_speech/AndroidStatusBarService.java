package oly.netpowerctrl.utils.statusbar_and_speech;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
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
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.FavCollection;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.utils.AndroidShortcuts;
import oly.netpowerctrl.utils.WakeLocker;

/**
 * Show a permanent notification in the android statusbar and add favourite scenes and devicePorts as actions.
 */
public class AndroidStatusBarService extends Service implements RecognitionListener, onDataLoaded, TextToSpeech.OnInitListener {
    public static final int REQUEST_CODE = 8978;
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static final int MSG_TIMEOUT_COMMAND_MODE = 3;
    static final int MSG_RECOGNIZER_START_LISTENING_COMMAND_MODE = 4;
    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECOGNIZER_START_LISTENING:
                    startListen(false);
                    break;

                case MSG_RECOGNIZER_START_LISTENING_COMMAND_MODE:
                    startListen(true);
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
    protected Intent mSpeechRecognizerIntentExternal;
    protected MediaPlayer mMediaPlayerCommandOn;
    protected MediaPlayer mMediaPlayerCommandOK;
    protected MediaPlayer mMediaPlayerCommandTimeout;
    protected ProgressDialog progressDialog;
    protected boolean mIsListening;
    protected boolean mCommandMode = false;
    protected volatile boolean mIsCountDownOn;
    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(60000, 60000) {

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            mIsCountDownOn = false;
            cancelListen();
            startListen(false);
        }
    };
    NotificationManager mNotificationManager;
    private TextToSpeech mTts;
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
    private String command_word;
    private Handler externalSpeechTimeout = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            WakeLocker.release();
        }
    };

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
        // WakeLocker
        externalSpeechTimeout.removeMessages(0);
        WakeLocker.release();

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

//    private void timeout() {
//        Log.w(TAG, "timeout");
//        if (commandMode)
//            mMediaPlayerCommandTimeout.start();
//        timeoutRunning = false;
//        commandMode = false;
//    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotification();
    }

    private void startListen(boolean commandMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // turn off beep sound
            if (!mIsStreamSolo) {
                mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                mIsStreamSolo = true;
            }
        }
        if (!mIsListening) {
            mCommandMode = commandMode;
            if (commandMode) {
                WakeLocker.acquire(this);
                externalSpeechTimeout.removeMessages(0);
                externalSpeechTimeout.sendEmptyMessageDelayed(0, 5000);
                //progressDialog.show();
                mMediaPlayerCommandOn.start();
            } else {
                mIsListening = true;
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            }
            //Log.d(TAG, "message start listening"); //$NON-NLS-1$
        }
    }

    private void cancelListen() {
        mCommandMode = false;
        if (mIsListening)
            Toast.makeText(this, "Speak finished", Toast.LENGTH_SHORT).show();

        if (progressDialog.isShowing())
            progressDialog.dismiss();

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

    void unload() {
        if (mIsCountDownOn)
            mNoSpeechCountDown.cancel();
        if (mSpeechRecognizer != null)
            mSpeechRecognizer.destroy();
        if (mMediaPlayerCommandOK != null) {
            mMediaPlayerCommandOK.reset();
            mMediaPlayerCommandOK.release();
        }
        if (mMediaPlayerCommandOn != null) {
            mMediaPlayerCommandOn.reset();
            mMediaPlayerCommandOn.release();
        }
        if (mMediaPlayerCommandTimeout != null) {
            mMediaPlayerCommandTimeout.reset();
            mMediaPlayerCommandTimeout.release();
        }
        if (mTts != null)
            mTts.shutdown();
        mTts = null;
        mAudioManager = null;
        mSpeechRecognizer = null;
        progressDialog = null;
        //mMediaPlayerCommandOn = null;
        mMediaPlayerCommandTimeout = null;
        mMediaPlayerCommandOK = null;
        mSpeechRecognizerIntent = null;
        mSpeechRecognizerIntentExternal = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (SharedPrefs.isSpeechEnabled(this)) {
            unload();
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Speak now...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                   @Override
                                                   public void onCancel(DialogInterface dialog) {
                                                       startListen(false);
                                                   }
                                               }
            );
            command_word = SharedPrefs.getSpeechCommandWord(this).toLowerCase();
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(this);
            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 200);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 200);
            //mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    this.getPackageName());

            mSpeechRecognizerIntentExternal = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntentExternal.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            //mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            mSpeechRecognizerIntentExternal.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntentExternal.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    this.getPackageName());

            mTts = new TextToSpeech(this, this);
            mTts.setLanguage(Locale.GERMAN);

            mMediaPlayerCommandOn = new MediaPlayer();
            mMediaPlayerCommandOK = new MediaPlayer();
            mMediaPlayerCommandTimeout = new MediaPlayer();

            loadMediaFile("sounds/computerbeep_75.mp3", mMediaPlayerCommandOn);
            loadMediaFile("sounds/computerbeep_74.mp3", mMediaPlayerCommandTimeout);
            loadMediaFile("sounds/computerbeep_16.mp3", mMediaPlayerCommandOK);

            mMediaPlayerCommandOK.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 100);
                }
            });
            mMediaPlayerCommandOn.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mIsListening = true;
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                }
            });

            //Log.w(TAG, "onCreate");
            handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 100);
            //mMediaPlayerCommandOK.start();
        } else if (mAudioManager != null) {
            unload();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }

    @Override
    public void onBeginningOfSpeech() {
        Toast.makeText(this, "Speak...", Toast.LENGTH_SHORT).show();

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
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            Log.w(TAG, "No permissions!");
            return;
        }
        if (error == SpeechRecognizer.ERROR_SERVER) {
            Log.w(TAG, "Speech Server busy, trying again in 60s");
            mNoSpeechCountDown.start();
            return;
        }
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            Log.e(TAG, "Speech busy. This should not happen!");
            mNoSpeechCountDown.start();
            return;
        }
        if (error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && error != SpeechRecognizer.ERROR_NO_MATCH) {
            Log.w(TAG, "ERROR: " + error);
            mMediaPlayerCommandTimeout.start();
            return;
        }

        startListen(false);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (!mCommandMode && scanForHotWord(partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))) {
            cancelListen();
            handler.sendEmptyMessage(MSG_RECOGNIZER_START_LISTENING_COMMAND_MODE);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//        {
        mIsCountDownOn = true;
        mNoSpeechCountDown.start();
//            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false); //NEW
//        }
        //Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
    }

    private boolean scanForHotWord(List<String> list) {
        for (int i = 0; i < list.size(); ++i) {
            String t = list.get(i).toLowerCase();
            //Log.w(TAG, "cmp "+t+" "+command_word);
            if (t.contains(command_word)) {
                return true;
            }
        }
        return false;
    }

    private boolean scanForCommands(List<String> list) {
        for (int i = 0; i < list.size(); ++i) {
            String t = list.get(i).toLowerCase();
            Log.w(TAG, "scan: " + t);
            if (doCommand(t)) {
                mMediaPlayerCommandOK.start();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResults(Bundle results) {
        boolean commandMode = mCommandMode;
        cancelListen();

        // WakeLocker
        externalSpeechTimeout.removeMessages(0);
        WakeLocker.release();

        if (commandMode) {
            if (!scanForCommands(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))) {
                mMediaPlayerCommandTimeout.start();
                handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 500);
            }
            return;
        } else if (scanForHotWord(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))) {
            handler.sendEmptyMessage(MSG_RECOGNIZER_START_LISTENING_COMMAND_MODE);
            return;
        }
        handler.sendEmptyMessageDelayed(MSG_RECOGNIZER_START_LISTENING, 100);
    }

    private boolean doCommand(String command) {
        boolean precise = command.startsWith("starte");
        if (precise)
            command = command.replace("starte ", "");
        boolean alarm = command.startsWith("alarm in");
        if (alarm) {
            Log.w(TAG, command);
            command = command.replace("alarm in ", "");
            int index = command.indexOf(' ');
            if (index == -1) return false;
            String timeStr = command.substring(0, index).replace("einer", "1").replace("eine", "1");
            int time = 0;
            try {
                time = Integer.valueOf(timeStr);
            } catch (NumberFormatException ignored) {
            }
            if (time == 0) return false;
            Log.w(TAG, "time " + time);
            command = command.substring(index + timeStr.length() - 1).replace("minuten", "");
            Log.w(TAG, "command " + command);
            if (command.length() == 0) return false;
            Executable executable = AppData.getInstance().findFirstExecutableByName(command.trim(), true);
            if (executable == null) return false;
            Log.w(TAG, "alarm executable " + executable.getTitle());

            HashMap<String, String> myHashAlarm = new HashMap<>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_ALARM));
            mTts.speak("Alarm eingerichtet. Ausführung in " + String.valueOf(time) + " Minuten", TextToSpeech.QUEUE_FLUSH, myHashAlarm);

            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, time);
            Timer timer = Timer.createNewOneTimeTimer(c, executable, DevicePort.TOGGLE);
            AppData.getInstance().timerCollection.addAlarm(timer);
            return true;
        }
        String[] commands = command.trim().split("und");
        boolean executed = false;
        for (String commandPart : commands) {
            Executable executable = AppData.getInstance().findFirstExecutableByName(commandPart.trim(), !precise);
            if (executable != null) {
                Log.w(TAG, "Starte " + executable.getTitle());
                handler.removeMessages(MSG_TIMEOUT_COMMAND_MODE);
                AppData.getInstance().executeToggle(executable, null);
                executed = true;
            }
        }

        return executed;
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    // Text to speach
    @Override
    public void onInit(int i) {
        HashMap<String, String> myHashAlarm = new HashMap<>();
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                String.valueOf(AudioManager.STREAM_ALARM));
        mTts.speak("Spracherkennung eingeschaltet", TextToSpeech.QUEUE_FLUSH, myHashAlarm);
    }
}
