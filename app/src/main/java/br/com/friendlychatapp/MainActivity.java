package br.com.friendlychatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final String FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length";
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private ListView messageListView;
    private MessageAdapter messageAdapter;
    private ProgressBar progressBar;
    private ImageButton photoPicker;
    private EditText messageEditText;
    private Button sendButton;

    private String userName;

    // Firebase variables
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private FirebaseRemoteConfig firebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userName = ANONYMOUS;

        firebaseStorage = FirebaseStorage.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    onSignedInInitialize(user.getDisplayName());
                    Toast.makeText(MainActivity.this,"Logged in",Toast.LENGTH_LONG).show();
                }else{
                    onSignedOutCleanup();
                    // Cria a lista de provides de opçoes de login
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());
                    // Cria a tela de login
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .setLogo(R.mipmap.ic_launcher)
                                    .setTosAndPrivacyPolicyUrls(
                                            "https://example.com/terms.html",
                                            "https://example.com/privacy.html")
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("messages");
        storageReference = firebaseStorage.getReference().child("chat_photos");

        progressBar = findViewById(R.id.progressBar);
        messageListView = findViewById(R.id.messageListView);
        photoPicker = findViewById(R.id.photoPickerButton);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this,R.layout.item_message,friendlyMessages);
        messageListView.setAdapter(messageAdapter);

        progressBar.setVisibility(View.INVISIBLE);

        // Adiciona o ImagePicker para selecionar uma foto para mensagem
        photoPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,"Complete action using"),RC_PHOTO_PICKER);
            }
        });

        // Libera o botão SEND quando há texto
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0){
                    sendButton.setEnabled(true);
                }else{
                    sendButton.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(messageEditText.getText().toString(),userName,null);
                // Sem o push() sobrescreve todos os dados do nó (.child()) messages
                databaseReference.push().setValue(friendlyMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this,"Success",Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,"Fail",Toast.LENGTH_LONG).show();
                    }
                });
                messageEditText.setText("");
            }
        });
        /* Teste Crashlytics
        Button crashButton = new Button(this);
        crashButton.setText("Crash!");
        crashButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                throw new RuntimeException("Test Crash"); // Force a crash
            }
        });
        addContentView(crashButton, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));*/
        FirebaseRemoteConfigSettings remoteConfig = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(remoteConfig);
        Map<String,Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY,DEFAULT_MSG_LENGTH_LIMIT);
        firebaseRemoteConfig.setDefaultsAsync(defaultConfigMap);
        fetchConfig();
    }


    private void onSignedOutCleanup() {
        userName = ANONYMOUS;
        messageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void detachDatabaseReadListener() {
        // Remove the listener, so the user cannot read messages anymore
        if(childEventListener != null) {
            databaseReference.removeEventListener(childEventListener);
            childEventListener = null;
        }
    }

    private void onSignedInInitialize(String displayName) {
        // Set the username to retrieve info from the right user
        userName = displayName;
        attachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        // Set the listener to retrieve info from the Firebase
        // if the listener is not created yet
        if (childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    FriendlyMessage fm = dataSnapshot.getValue(FriendlyMessage.class);
                    messageAdapter.add(fm);
                    //Log.e(MainActivity.class.getSimpleName() + " Snapshot data", "" + fm.toString());
                    messageListView.smoothScrollToPosition(messageListView.getCount());
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            databaseReference.addChildEventListener(childEventListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            if (resultCode == RESULT_OK){
                Toast.makeText(MainActivity.this,"Logged in",Toast.LENGTH_LONG).show();
            }else if(resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this,"Sign in canceled",Toast.LENGTH_LONG).show();
                finish();
            }
        }else if(requestCode == RC_PHOTO_PICKER){
            if (resultCode == RESULT_OK){
                boolean lessThanMaxSize = false;
                if(data!=null){
                    Uri uri = data.getData();
                    lessThanMaxSize = checkSize(uri);
                }
                if (lessThanMaxSize){
                    uploadPhoto(data);
                }else {
                    Snackbar.make(findViewById(R.id.main_activity),"File to big",BaseTransientBottomBar.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(MainActivity.this,"Error uploading image.",Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Check image size (only internal storage) */
    private boolean checkSize(Uri uri){
        long sizeToMatch = 3 * 1024 * 1024;
        //Log.e(TAG,""+uri);
        Cursor queryCursor = getContentResolver().query(uri,null,null,null,null);
        if (queryCursor == null){
            //Log.e(TAG,""+uri.getPath());
            return false;
        }
        queryCursor.moveToFirst();
        long imgSize = queryCursor.getLong(queryCursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE));
        //Log.e(TAG,"ImageSize from queryCursor: "+imgSize);
        queryCursor.close();

        // Method 2
        // Try to get the file from the uri
        /*if (imgSize <= sizeToMatch) {
            try {
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                if (parcelFileDescriptor != null) {
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    Show the image in a ImageView
//                    ImageView imageView =new ImageView(this);
//                    imageView.setImageBitmap(image);
//                    addContentView(imageView, new ViewGroup.LayoutParams(
//                            ViewGroup.LayoutParams.MATCH_PARENT,
//                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    parcelFileDescriptor.close();
                    uploadPhoto(image, uri);
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Error file not found");
            } catch (NullPointerException npe) {
                Log.e(TAG, "Error null pointer exception");
            }
        }*/
        return imgSize <= sizeToMatch;
    }

    private void uploadPhoto(Bitmap image,Uri uri){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();
        final StorageReference photoRef = storageReference.child(uri.getLastPathSegment());
        UploadTask uploadTask = photoRef.putBytes(imageData);
        uploadTask.addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.main_activity),"Fail", BaseTransientBottomBar.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(MainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Snackbar.make(findViewById(R.id.main_activity),"Success",BaseTransientBottomBar.LENGTH_LONG).show();
                Log.e(TAG,"MetaData.getPath: "+taskSnapshot.getMetadata().getPath());
                Log.e(TAG,"MetaData.getName: "+taskSnapshot.getMetadata().getName());
                Log.e(TAG,"MetaData.Size: "+taskSnapshot.getMetadata().getSizeBytes());
                Log.e(TAG,"BytesTransferred: "+taskSnapshot.getBytesTransferred());
                Log.e(TAG,"TotalByteCount: "+taskSnapshot.getTotalByteCount());
            }
        });
        Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if(!task.isSuccessful()){
                    //throw task.getException();
                    Log.e(TAG,"Exception: "+task.getException());
                }
                return photoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(MainActivity.this, new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (!task.isSuccessful()){
                    Toast.makeText(MainActivity.this,"No download Url",Toast.LENGTH_LONG).show();
                }
                Uri downloadUri = task.getResult();
                if (downloadUri != null) {
                    FriendlyMessage friendlyMessage = new FriendlyMessage(null, userName, downloadUri.toString());
                    databaseReference.push().setValue(friendlyMessage);
                }else {
                    Toast.makeText(MainActivity.this,"No download Url",Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.main_activity),"Fail",BaseTransientBottomBar.LENGTH_LONG).show();
            }
        });
    }

    private void uploadPhoto(Intent data){
        Uri selectedImageUri = data.getData();
        final StorageReference photoRef = storageReference.child(selectedImageUri.getLastPathSegment());
        UploadTask uploadTask = photoRef.putFile(selectedImageUri);
        uploadTask.addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.main_activity),"Fail", BaseTransientBottomBar.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(MainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Snackbar.make(findViewById(R.id.main_activity),"Success",BaseTransientBottomBar.LENGTH_LONG).show();
                //Log.e(TAG,"BytesTransferred: "+taskSnapshot.getBytesTransferred());
                //Log.e(TAG,"TotalByteCount: "+taskSnapshot.getTotalByteCount());
            }
        });
        Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if(!task.isSuccessful()){
                    //throw task.getException();
                    Log.e(TAG,"Exception: "+task.getException());
                }
                return photoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(MainActivity.this, new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (!task.isSuccessful()){
                    Toast.makeText(MainActivity.this,"No download Url",Toast.LENGTH_LONG).show();
                }
                Uri downloadUri = task.getResult();
                if (downloadUri != null) {
                    FriendlyMessage friendlyMessage = new FriendlyMessage(null, userName, downloadUri.toString());
                    databaseReference.push().setValue(friendlyMessage);
                }else {
                    Toast.makeText(MainActivity.this,"No download Url",Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.main_activity),"Fail",BaseTransientBottomBar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(MainActivity.this);
                return true;
            case R.id.clear_cache:
                clearCache(MainActivity.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearCache(Context context) {
        try {
            boolean deleted = context.getCacheDir().delete();
            Toast.makeText(MainActivity.this,"Deleted: "+deleted,Toast.LENGTH_LONG).show();
        }catch (Exception e){
            Log.e(TAG,"CODE: "+e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG,"onPause called");
        super.onPause();
        if(authStateListener != null)
            firebaseAuth.removeAuthStateListener(authStateListener);
        detachDatabaseReadListener();
        messageAdapter.clear();
    }

    @Override
    protected void onResume() {
        Log.e(TAG,"onResume called");
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    private void fetchConfig() {
        long cacheExpiration = 3600;
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }
        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        firebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG,"Error fetching config:::\t",e);
                applyRetrievedLengthLimit();
            }
        });
    }

    private void applyRetrievedLengthLimit() {
        long friendlyMessageLength = firebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter((int) friendlyMessageLength)});
        Log.e(TAG,FRIENDLY_MSG_LENGTH_KEY+" = "+friendlyMessageLength);
    }
}
