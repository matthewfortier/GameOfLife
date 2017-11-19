package com.matthewfortier.gameoflife;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SaveActivity extends AppCompatActivity {

    // https://stackoverflow.com/questions/1657193/java-code-library-for-generating-slugs-for-use-in-pretty-urls
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final String COLOR_ALIVE = "alive";
    private static final String COLOR_DEAD = "dead";
    private static final String GRID = "data";
    private static final String FILENAME = "filename";
    StorageReference mStorageRef;
    ImageView mSavedImage;
    Button mUploadButton;
    EditText mImageTitle;
    boolean[][] mData;
    int mAliveColor;
    int mDeadColor;

    // https://stackoverflow.com/questions/1657193/java-code-library-for-generating-slugs-for-use-in-pretty-urls
    // This function turns a pattern title into a valid filename
    public static String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_activity);

        // Initialize FirebaseStorage object based on included config file
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // Gte the data from the intent
        final String filename = getIntent().getExtras().getString(FILENAME);

        mData = (boolean[][]) getIntent().getExtras().getSerializable(GRID);
        mAliveColor = getIntent().getExtras().getInt(COLOR_ALIVE);
        mDeadColor = getIntent().getExtras().getInt(COLOR_DEAD);

        final File cachePath = new File(getCacheDir() + getString(R.string.database_directory_prefix) + filename + ".png");

        mSavedImage = findViewById(R.id.save_image);
        mSavedImage.setImageResource(0);

        // Load the image into the image view from the extras if it exists
        if (cachePath.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(cachePath.getAbsolutePath());
            mSavedImage.setImageBitmap(myBitmap);
        } else {
            Toast.makeText(getBaseContext(), R.string.not_found, Toast.LENGTH_SHORT).show();
        }

        mImageTitle = findViewById(R.id.image_title);

        mUploadButton = findViewById(R.id.upload);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Build an alert dialog that allows the user to save pattern to database or disk
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(SaveActivity.this);
                builder.setTitle(R.string.save_location)
                        .setPositiveButton(R.string.to_disk, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Make new directory Patterns in external storage if it does not already exists
                                String root = Environment.getExternalStorageDirectory().toString();
                                File myDir = new File(root + getString(R.string.patterns_directory));
                                myDir.mkdirs();

                                File file = new File(myDir, toSlug(mImageTitle.getText().toString()) + ".data");

                                // If a file with the same name exists, overwrite it
                                if (file.exists())
                                    file.delete();
                                try {

                                    // Send the 2s array into a file using ObjectOutput Stream
                                    FileOutputStream fos = new FileOutputStream(file);
                                    ObjectOutputStream oos = new ObjectOutputStream(fos);

                                    oos.writeObject(mData);
                                    oos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    Toast.makeText(getApplicationContext(), R.string.disk_success, Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                        })
                        .setNegativeButton(R.string.upload, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Get the Uri of the saved file
                                Uri file = Uri.fromFile(cachePath);
                                // Make an image reference in the Firebase Database
                                StorageReference imageRef = mStorageRef.child(getString(R.string.database_directory) + toSlug(mImageTitle.getText().toString()) + ".png");
                                // Upload the file
                                imageRef.putFile(file)
                                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                // If the file was uploaded, upload the objects that go with it
                                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                                DatabaseReference myRef = database.getReference(getString(R.string.pattern));

                                                // Convert 2d array to List because Firebase does not support serializable data
                                                List<Boolean> tmp = new ArrayList<Boolean>();
                                                for (int i = 0; i < mData.length; i++)
                                                    for (int j = 0; j < mData.length; j++)
                                                        tmp.add(mData[i][j]);

                                                // Make a GamePattern object with data, title, colors and filename of the image
                                                GamePattern pattern = new GamePattern(tmp, mImageTitle.getText().toString(), mAliveColor, mDeadColor, toSlug(mImageTitle.getText().toString()) + ".png");
                                                // Upload the object to the database
                                                myRef.push().setValue(pattern);

                                                // Go to the list activity to see newly uploaded pattern
                                                Intent i = new Intent(getApplicationContext(), ListActivity.class);
                                                startActivity(i);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception exception) {
                                                // Handle unsuccessful uploads
                                                // ...
                                                Toast.makeText(getApplicationContext(), R.string.upload_unsuccessful, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        })
                        .show();
            }
        });
    }
}
