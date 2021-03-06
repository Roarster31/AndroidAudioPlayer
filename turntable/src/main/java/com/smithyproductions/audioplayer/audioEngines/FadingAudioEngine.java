package com.smithyproductions.audioplayer.audioEngines;

import android.util.Log;

import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;

/**
 * Created by rory on 09/01/16.
 */
public class FadingAudioEngine extends PreloadingAudioEngine {


    private static final int FADE_DURATION = 5000;
    private boolean currentlyFading;
    private float globalVolume = 1.0f;

    public FadingAudioEngine(Class<? extends BasePlayerEngine> mediaPlayerClass) {
        super(mediaPlayerClass);
    }

    @Override
    public void onProgress(float progress) {
        if(!playerArray[0].isPreparing()) {
            int timeRemaining = (int) ((1 - progress) * playerArray[0].getDuration());

            if (timeRemaining < FADE_DURATION) {
                if (!playerArray[1].isPreparing()) {
                    currentlyFading = true;
                    float fadeProgress = timeRemaining / (float) FADE_DURATION;
                    playerArray[0].setVolume(globalVolume * fadeProgress);
                    playerArray[1].setVolume(globalVolume * (1 - fadeProgress));
                    playerArray[1].play();
                }
            }
        }

        super.onProgress(progress);
    }

    @Override
    public void onTrackFinished() {
        if(currentlyFading) {
            Log.d("FadingAudioEngine", "just finished: " + playerArray[0].getTrack());
            movePlayersToNextTrack();
            playerArray[0].play();
            playerArray[1].setVolume(1);
            currentlyFading = false;
            Log.d("FadingAudioEngine", "now playing: " + playerArray[0].getTrack());
        } else {
            super.onTrackFinished();
        }
    }

    @Override
    public void setVolume(float volume) {
        globalVolume = volume;
        if(!currentlyFading) {
            playerArray[0].setVolume(globalVolume);
            playerArray[1].setVolume(globalVolume);
        }
    }

    @Override
    public void pause() {
        if(currentlyFading) {
            movePlayersToNextTrack();
            playerArray[0].setVolume(1);
            playerArray[1].setVolume(1);
            currentlyFading = false;
            parentCallbacks.onProgress(playerArray[0].getProgress());
        }
        super.pause();
    }
}
