package oly.netpowerctrl.utils.statusbar_and_speech;

import android.service.voice.AlwaysOnHotwordDetector;

/**
 * Started by the AndroidStatusBarService if user wants hotwords. Only available for Android 5.0+.
 */
public class SpeechHotwordDetector {
    private AndroidStatusBarService androidStatusBarService;
    private AlwaysOnHotwordDetector alwaysOnHotwordDetector;

    SpeechHotwordDetector(AndroidStatusBarService androidStatusBarService) {
        this.androidStatusBarService = androidStatusBarService;
    }
}
