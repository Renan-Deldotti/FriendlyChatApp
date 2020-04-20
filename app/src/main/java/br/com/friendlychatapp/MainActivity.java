package br.com.friendlychatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView messageListView;
    private MessageAdapter messageAdapter;
    private ProgressBar progressBar;
    private ImageButton photoPicker;
    private EditText messageEditText;
    private Button sendButton;

    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userName = ANONYMOUS;

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
                // Selecionar a foto
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
        
    }
}
