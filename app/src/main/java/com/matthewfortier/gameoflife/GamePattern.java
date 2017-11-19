package com.matthewfortier.gameoflife;

import android.net.Uri;

import java.security.PublicKey;
import java.util.List;

/**
 * Created by matthewfortier on 11/8/17.
 */

public class GamePattern {
    private List<Boolean> mData;
    private int mAlive;
    private int mDead;
    private String mTitle;
    private String mFilename;

    public GamePattern() {}

    public GamePattern(List<Boolean> data, String title, int alive, int dead, String imageUrl) {
        this.mData = data;
        this.mAlive = alive;
        this.mDead = dead;
        this.mFilename = imageUrl;
        this.mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public List<Boolean> getData() {
        return mData;
    }

    public void setData(List<Boolean> data) {
        this.mData = data;
    }

    public int getAlive() {
        return mAlive;
    }

    public void setAlive(int alive) {
        this.mAlive = alive;
    }

    public int getDead() {
        return mDead;
    }

    public void setDead(int dead) {
        this.mDead = dead;
    }

    public String getFilename() {
        return mFilename;
    }

    public void setFilename(String mFilename) {
        this.mFilename = mFilename;
    }
}
