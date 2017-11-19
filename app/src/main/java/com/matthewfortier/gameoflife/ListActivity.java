package com.matthewfortier.gameoflife;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    final int REQUEST_CODE = 0;
    private static final String COLOR_ALIVE = "alive";
    private static final String COLOR_DEAD = "dead";
    private static final String GRID = "data";
    private static final int ROWS = 20;
    private static final int COLS = 20;

    final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    DatabaseReference mRef = mDatabase.getReference();
    List<GamePattern> mPatterns = new ArrayList<GamePattern>();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_list);

        // Set up list recycler view
        mRecyclerView = findViewById(R.id.pattern_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        // Get all of the patterns from Firebase database
        mRef.child(getString(R.string.database_pattern)).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Loop through all patterns, converting them to GamePatterns
                for (DataSnapshot entryDataSnapshot : dataSnapshot.getChildren()) {
                    GamePattern tmp = entryDataSnapshot.getValue(GamePattern.class);
                    mPatterns.add(tmp);
                }

                // Set the adapter if not done already
                if (mAdapter == null) {
                    mAdapter = new PatternAdapter(mPatterns);
                    mRecyclerView.setAdapter(mAdapter);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Upload .data file from external storage
        switch (item.getItemId()) {
            case R.id.load:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // If success, get Uri of file selected
        if (requestCode == REQUEST_CODE) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                try {
                    // Uses ObjectInputStream to put/get serializable data into file
                    InputStream stream = getContentResolver().openInputStream(uri);
                    ObjectInputStream ois = new ObjectInputStream(stream);
                    boolean[][] data = (boolean[][]) ois.readObject();

                    // Start new intent with the data from the file to load
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    i.putExtra(GRID, data);
                    startActivity(i);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class PatternHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        ImageView mPatternImage;
        TextView mPatternTitle;
        private GamePattern mPattern;

        public PatternHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_pattern, parent, false));
            itemView.setOnClickListener(this);

            mPatternImage = itemView.findViewById(R.id.pattern_image);
            mPatternTitle = itemView.findViewById(R.id.pattern_title);
        }

        public void bind(GamePattern pattern) {
            mPattern = pattern;
            mPatternTitle.setText(pattern.getTitle());

            StorageReference imageRef = storageRef.child(getString(R.string.database_directory) + pattern.getFilename());

            final long ONE_MEGABYTE = 1024 * 1024;
            imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    mPatternImage.setImageBitmap(b);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        }

        @Override
        public void onClick(View v) {
            boolean[][] tmp = new boolean[ROWS][COLS];


            for (int i = 0; i < ROWS; i++)
                for (int j = 0; j < COLS; j++)
                    tmp[j][i] = mPattern.getData().get((j * COLS) + i);

            // Start Intent
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.putExtra(GRID, tmp);
            i.putExtra(COLOR_ALIVE, mPattern.getAlive());
            i.putExtra(COLOR_DEAD, mPattern.getDead());
            startActivity(i);
        }

    }

    private class PatternAdapter extends RecyclerView.Adapter<PatternHolder> {

        private List<GamePattern> mGamePatterns;

        PatternAdapter(List<GamePattern> patterns) {
            this.mGamePatterns = patterns;
        }

        @Override
        public PatternHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            return new PatternHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(PatternHolder holder, int position) {
            GamePattern pattern = mGamePatterns.get(position);
            holder.bind(pattern);
        }

        @Override
        public int getItemCount() {
            return mGamePatterns.size();
        }

    }

}
