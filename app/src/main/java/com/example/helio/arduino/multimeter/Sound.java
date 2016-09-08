/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.helio.arduino.multimeter;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

public class Sound {

    private static final String TAG = "Sound";
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private boolean isPaused;

    public Sound(Context context){
        this.mContext = context;
    }

    public void stop(){
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            isPaused = true;
        }
    }

    public synchronized void pause(){
        if (isPlaying()) {
            mMediaPlayer.pause();
            isPaused = true;
        }
    }

    public synchronized void resume(){
        if (!isPlaying()) {
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
            isPaused = false;
        }
    }

    public synchronized void start(){
        if (!isPlaying()) {
            mMediaPlayer.start();
            isPaused = false;
        }
    }

    public boolean isPaused(){
        return isPaused;
    }

    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    public void prepareMelody() throws IOException {
        AssetFileDescriptor afd = mContext.getAssets().openFd("beep.mp3");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(false);
        try {
            mMediaPlayer.prepareAsync();
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
    }
}
