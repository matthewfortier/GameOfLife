package com.matthewfortier.gameoflife;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LifeFragment extends Fragment implements RecyclerViewAdapter.ItemClickListener {

    private static final String COLOR_ALIVE = "alive";
    private static final String COLOR_DEAD = "dead";
    private static final String GRID = "data";
    private static final String IMAGE_DIRECTORY = "images";
    private static final String FILENAME = "filename";
    private static final int ROWS = 20;
    private static final int COLS = 20;
    RecyclerView mRecyclerView;
    RecyclerViewAdapter mAdapter;
    boolean[][] mData;
    int mGeneration = 0;
    Button mSpeedButton;
    Button mStartStopButton;
    Button mDeadButton;
    Button mAliveButton;
    Button mCloneButton;
    Button mNextButton;
    Button mClearButton;
    int mDeadCellColor = 0;
    int mAliveCellColor = 0;
    int mRefreshInterval = 1000;
    int mCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_life, container, false);

        // Get the data and colors if added to the intent
        if (getArguments() != null) {
            mData = (boolean[][]) getArguments().getSerializable(GRID);
            if (getArguments().getInt(COLOR_ALIVE) != 0)
                mAliveCellColor = getArguments().getInt(COLOR_ALIVE);

            if (getArguments().getInt(COLOR_DEAD) != 0)
                mDeadCellColor = getArguments().getInt(COLOR_DEAD);
        }

        // Just in case there are arguments and mData is still null
        if (mData == null) {
            mData = new boolean[ROWS][COLS];
            // data to populate the RecyclerView with
            for (int i = 0; i < ROWS; i++)
                for (int j = 0; j < COLS; j++)
                    mData[i][j] = false;
        }

        // set up the RecyclerView
        mRecyclerView = v.findViewById(R.id.grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), COLS));
        mAdapter = new RecyclerViewAdapter(getContext(), mData);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

        // Set the colors in the grid if they are not the default
        if (mAliveCellColor != 0) mAdapter.setAliveCellColor(mAliveCellColor);
        if (mDeadCellColor != 0) mRecyclerView.setBackgroundColor(mDeadCellColor);

        // Setting up the handler to repeat the generations when the start button is clicked
        final Handler handler = new Handler();
        final Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                nextGeneration();
                handler.postDelayed(this, mRefreshInterval); // using editable refresh interval
            }
        };

        // Initialize all buttons
        mStartStopButton = v.findViewById(R.id.start_stop);
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If start, stop. If stop, start
                if (mStartStopButton.getText().toString().equals(getString(R.string.start)))
                {
                    handler.postDelayed(myRunnable, mRefreshInterval);
                    mStartStopButton.setText(getString(R.string.stop));
                }
                else
                {
                    handler.removeCallbacks(myRunnable);
                    mStartStopButton.setText(getString(R.string.start));
                }
            }
        });

        mNextButton = v.findViewById(R.id.next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextGeneration();
            }
        }); // Manually go to next generation

        mClearButton = v.findViewById(R.id.clear);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear by setting all values to false
                for (int i = 0; i < ROWS; i++)
                    for (int j = 0; j < COLS; j++)
                        mData[i][j] = false;
                mAdapter.notifyDataSetChanged();

                // Reset generation and subtitle
                mGeneration = 0;
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(getString(R.string.initial_generation_value));
            }
        });

        // Open up color picker intents from https://github.com/QuadFlask/colorpicker
        mDeadButton = v.findViewById(R.id.dead);
        mDeadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPickerDialogBuilder
                        .with(getContext())
                        .setTitle(getString(R.string.choose_color))
                        .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                        .density(12)
                        .setPositiveButton(getString(R.string.ok), new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                // Set the color based on dialog result
                                mDeadCellColor = selectedColor;
                                mRecyclerView.setBackgroundColor(selectedColor);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();
            }
        });

        mAliveButton = v.findViewById(R.id.alive);
        mAliveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPickerDialogBuilder
                        .with(getContext())
                        .setTitle(getString(R.string.choose_color))
                        .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                        .density(12)
                        .setPositiveButton(getString(R.string.ok), new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                mAliveCellColor = selectedColor;
                                mAdapter.setAliveCellColor(selectedColor);
                                mAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();
            }
        });

        mCloneButton = v.findViewById(R.id.clone);
        mCloneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clone activity by creating new activity and passing in data and colors
                Intent i = new Intent(getContext(), MainActivity.class);
                i.putExtra(GRID, mData);
                i.putExtra(COLOR_ALIVE, mAliveCellColor);
                i.putExtra(COLOR_DEAD, mDeadCellColor);
                startActivity(i);
            }
        });

        // Opens custom speed dialog for both animation timing and refresh interval
        mSpeedButton = v.findViewById(R.id.speed);
        mSpeedButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.animation_speed_text);
                builder.setMessage(R.string.animation_speed_interval);

                // Inflate custom view with input boxes for speeds
                View dialog = View.inflate(getContext(), R.layout.speed_dialog, null);

                // Set the text in the boxes to the current values
                final EditText animation = dialog.findViewById(R.id.animation_speed);
                animation.setText(Integer.toString(mAdapter.getAnimationSpeed()));
                final EditText refresh = dialog.findViewById(R.id.animation_refresh);
                refresh.setText(Integer.toString(mRefreshInterval));

                // Set the view and assign the buttons
                builder.setView(dialog);

                builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        // Apply new speeds and refresh the grid
                        String interval = refresh.getText().toString();
                        mRefreshInterval = Integer.parseInt(interval);

                        String speed = animation.getText().toString();
                        mAdapter.setAnimationSpeed(Integer.parseInt(speed));

                        mAdapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.cancel();
                    }
                }).show();
            }
        });


        return v;
    }

    // The brains of the operation
    // Taken directly from code provided by David Kopec
    private void nextGeneration() {

        int rows = ROWS;
        int columns = COLS;

        byte[][] livingNeighborsCount = new byte[ROWS][COLS];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // Variables to save positions left and right of row and column
                int leftOfRow = i + rows - 1;
                int rightOfRow = i + 1;
                int leftOfColumn = j + columns - 1;
                int rightOfColumn = j + 1;

                // Checks to see if the cells are alive or dead. If they are alive
                // it increments the count for living neighbors.
                if (mData[i][j]) {
                    livingNeighborsCount[leftOfRow % rows][leftOfColumn % columns]++;
                    livingNeighborsCount[leftOfRow % rows][j % columns]++;
                    livingNeighborsCount[(i + rows - 1) % rows][rightOfColumn % columns]++;
                    livingNeighborsCount[i % rows][leftOfColumn % columns]++;
                    livingNeighborsCount[i % rows][rightOfColumn % columns]++;
                    livingNeighborsCount[rightOfRow % rows][leftOfColumn % columns]++;
                    livingNeighborsCount[rightOfRow % rows][j % columns]++;
                    livingNeighborsCount[rightOfRow % rows][rightOfColumn % columns]++;
                }
            }
        }

        // Changes the status of the cell based on the number of living
        // neighbors it has.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // If the cell has 4 or more living neighbors, it dies
                // by overcrowding.
                if (livingNeighborsCount[i][j] >= 4) {
                    mData[i][j] = false;
                }

                // A cell dies by exposure if it has 0 or 1 living neighbors.
                if (livingNeighborsCount[i][j] < 2) {
                    mData[i][j] = false;
                }

                // A cell is born if it has 3 living neighbors.
                if (livingNeighborsCount[i][j] == 3) {
                    mData[i][j] = true;
                }
            }
        }

        // Increment generation, set subtitle, refresh grid
        mGeneration++;
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(getString(R.string.generation_subtitle_prefix) + " " + mGeneration);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(View view, int position) {
        // Gets the 2d position from 1d position
        int col = position % COLS;
        int row = (position - col) / ROWS;

        // Inverses the boolean to turn on/off
        mData[col][row] = !mData[col][row];
        // Refresh
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu
        inflater.inflate(R.menu.life_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.list:
                // Go to list view
                Intent i = new Intent(getContext(), ListActivity.class);
                startActivity(i);
                break;
            case R.id.save:
                // Save the grid to disk or upload
                saveGrid();
                break;
            case R.id.share:
                // Save and share grid image
                shareGrid();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void shareGrid() {
        // Allow for the view to be captured
        mRecyclerView.setDrawingCacheEnabled(true);

        // Select the directory to place image
        File cachePath = new File(getContext().getCacheDir(), IMAGE_DIRECTORY);
        cachePath.mkdirs();
        FileOutputStream stream;
        try {
            // Save image to PNG with default filename in cache directory
            stream = new FileOutputStream(cachePath + "/" + getString(R.string.default_filename));
            Bitmap b = mRecyclerView.getDrawingCache();
            b.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            Toast.makeText(getContext(), getString(R.string.save_success), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the Uri for the newly saved file for sharing
        File imagePath = new File(getContext().getCacheDir(), IMAGE_DIRECTORY);
        File newFile = new File(imagePath, getString(R.string.default_filename));
        Uri contentUri = FileProvider.getUriForFile(getContext(), getString(R.string.file_provider), newFile);

        if (contentUri != null) {
            // Use share intent to share to any app
            // NOTE: This does not work on the emulator
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContext().getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.app_choose)));
        }

        // Disables the cache so a new image can be captured
        mRecyclerView.setDrawingCacheEnabled(false);
    }

    private void saveGrid() {
        mRecyclerView.setDrawingCacheEnabled(true);

        File cachePath = new File(getContext().getCacheDir(), IMAGE_DIRECTORY);
        cachePath.mkdirs();
        FileOutputStream stream;
        String filename = getString(R.string.filename_prefix) + mCount;
        try {
            stream = new FileOutputStream(cachePath + "/" + filename + ".png");
            Bitmap b = mRecyclerView.getDrawingCache();
            b.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCount++;

        // Instead of sending to share intent, send image, grid, and colors to custom save intent
        Intent i = new Intent(getContext(), SaveActivity.class);
        i.putExtra(FILENAME, filename);
        i.putExtra(GRID, mData);
        i.putExtra(COLOR_ALIVE, mAliveCellColor);
        i.putExtra(COLOR_DEAD, mDeadCellColor);
        startActivity(i);

        mRecyclerView.setDrawingCacheEnabled(false);
    }
}
