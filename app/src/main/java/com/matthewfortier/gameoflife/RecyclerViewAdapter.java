package com.matthewfortier.gameoflife;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    // Set size of grid
    private static final int ROWS = 20;
    private static final int COLS = 20;
    private boolean[][] mData = new boolean[0][0];
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private int mAliveCellColor = Color.WHITE;
    private int mAnimationSpeed = 1500;

    // data is passed into the constructor
    RecyclerViewAdapter(Context context, boolean[][] data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cell_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Get 2d position from 1d position
        int col = position % COLS;
        int row = (position - col) / ROWS;

        // If the cell is alive, enable the animation using given colors and timing
        if (mData[col][row]) {
            ObjectAnimator anim = ObjectAnimator.ofInt(holder.myImageView, "backgroundColor", mAliveCellColor, Color.TRANSPARENT,
                    mAliveCellColor);
            anim.setDuration(mAnimationSpeed);
            anim.setEvaluator(new ArgbEvaluator());
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            anim.start();
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.length * mData.length;
    }

    public void setAliveCellColor(int color) {
        mAliveCellColor = color;
    }

    public int getAnimationSpeed() {
        return mAnimationSpeed;
    }

    public void setAnimationSpeed(int speed) {
        mAnimationSpeed = speed;
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView myImageView;

        ViewHolder(View itemView) {
            super(itemView);
            myImageView = itemView.findViewById(R.id.info_text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
}