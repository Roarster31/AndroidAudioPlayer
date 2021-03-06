package com.smithyproductions.audioplayer.audioEngines;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

/**
 * Created by rory on 07/01/16.
 */
public class PreloadingAudioEngine extends BaseAudioEngine {

    BasePlayerEngine[] playerArray = new BasePlayerEngine[2];

    public PreloadingAudioEngine(Class<? extends BasePlayerEngine> mediaPlayerClass) {
        super(mediaPlayerClass);
    }

    @Override
    public void init(Context context, @NonNull AudioEngineCallbacks callbacks) {
        this.parentCallbacks = callbacks;

        BasePlayerEngine engine1 = createBasePlayerEngine(mMediaPlayerClass, context);
        engine1.setCallbackHandler(this);

        BasePlayerEngine engine2 = createBasePlayerEngine(mMediaPlayerClass, context);

        playerArray[0] = engine1;
        playerArray[1] = engine2;


    }


    @Override
    public void reset() {
        playerArray[0].pause();
        setAutoPlay(false);
        playerArray[0].unloadCurrent();
        playerArray[1].unloadCurrent();
    }

    @Override
    public void setVolume(float volume) {
        playerArray[0].setVolume(volume);
        playerArray[1].setVolume(volume);
    }

    @Override
    public int getPlaybackPosition() {
        return (int) (playerArray[0].getProgress() * playerArray[0].getDuration());
    }

    @Override
    public void setPlaybackPosition(int position) {
        playerArray[0].seekTo(position);
    }

    @Override
    public void play() {
        if (playerArray[0].getTrack() != null) {
            if (playerArray[0].isFinished()) {
                movePlayersToNextTrack();
                playFromStart(playerArray[0]);
            } else {
                playerArray[0].play();
            }

        } else if (trackProvider != null && trackProvider.getTrackCount() > 0) {
            loadTracks(0);
        }

        //player will play immediately if it can, otehrwise it'll play when
        //it's ready
        playerArray[0].play();
        setAutoPlay(true);
    }


    protected void loadTracks(final int offset) {
        parentCallbacks.onTrackChange(null);
        if(trackProvider != null) {
            trackProvider.cancelAllTrackRequests();
            final int n = trackProvider.getCurrentTrackIndex() + offset;
            trackProvider.requestNthTrack(n, new TrackProvider.TrackCallback() {
                @Override
                public void onTrackRetrieved(AudioTrack track) {
                    Log.d("PreloadingAudioEngine", "applying to player 0 track for position " + n + ": " + track);
                    loadInTrack(track, playerArray[0]);
                    parentCallbacks.onTrackChange(track);

                    if (trackProvider != null) {
                        //todo make this cleaner
                        switch (offset) {
                            case 1:
                                trackProvider.incrementTrackIndex();
                                break;
                            case -1:
                                trackProvider.decrementTrackIndex();
                                break;
                        }

                        //now get the next track
                        final int n = trackProvider.getCurrentTrackIndex() + 1;
                        trackProvider.requestNthTrack(n, new TrackProvider.TrackCallback() {
                            @Override
                            public void onTrackRetrieved(AudioTrack track) {
                                Log.d("PreloadingAudioEngine", "applying to player 1 track for position " + n + ": " + track);
                                loadInTrack(track, playerArray[1]);
                            }

                            @Override
                            public void onError(String errorMsg) {
                                Log.d("PreloadingAudioEngine", "can't get next track: '" + errorMsg + "'");
                            }
                        });
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    Log.d("PreloadingAudioEngine", "can't get current track: '" + errorMsg + "'");
                }
            });
        }


    }

    private void loadInTrack(AudioTrack track, final BasePlayerEngine engine) {
        if (track != null) {
            AudioTrack loadedTrack = engine.getTrack();
            if (track.equals(loadedTrack)) {
                Log.d("PreloadingAudioEngine", "track already loaded");
            } else {
                engine.unloadCurrent();
                engine.loadTrack(track);
            }

        } else {
            throw new RuntimeException("No track here!");
        }
    }

    protected void swapEngines() {
        BasePlayerEngine tmp = playerArray[0];
        playerArray[0] = playerArray[1];
        playerArray[1] = tmp;

        playerArray[0].setCallbackHandler(this);
        playerArray[1].setCallbackHandler(null);
        playerArray[1].unloadCurrent();

        //we don't want the secondary player to be playing
        playerArray[1].pause();
    }

    private void playFromStart(BasePlayerEngine engine) {
        engine.seekTo(0);
        engine.play();
    }

    @Override
    public void pause() {
        playerArray[0].pause();
        setAutoPlay(false);
    }

    @Override
    public void next() {
        movePlayersToNextTrack();
        playFromStart(playerArray[0]);
        setAutoPlay(true);
    }

    protected void movePlayersToNextTrack() {
        swapEngines();
        loadTracks(1);
    }

    private void movePlayersToPreviousTrack() {
        swapEngines();
        loadTracks(-1);
    }

    @Override
    public void previous() {
        movePlayersToPreviousTrack();
        playerArray[0].play();
        setAutoPlay(true);
    }

    @Override
    public void onTrackFinished() {
        //this only happens when we're playing, so we should play the background engine
        Log.d("PreloadingAudioEngine", "just finished: " + playerArray[0].getTrack());
        movePlayersToNextTrack();
        playFromStart(playerArray[0]);
        Log.d("PreloadingAudioEngine", "trying to play from beginning: " + playerArray[0].getTrack());
    }

    @Override
    public void onProgress(float progress) {
        parentCallbacks.onProgress(progress);
    }

    @Override
    public void onGeneralError() {
        parentCallbacks.onError();
    }

    @Override
    public void onTrackUnplayable() {
        next();
    }

    @Override
    public void onTracksInvalidated() {
        if (trackProvider != null && trackProvider.getTrackCount() > 0) {
            loadTracks(0);
        } else {
            parentCallbacks.onTrackChange(null);
        }
    }
}
