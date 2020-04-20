package br.com.friendlychatapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    public MessageAdapter(@NonNull Context context, int resource, @NonNull List<FriendlyMessage> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_message,parent,false);
        }
        ImageView photoImageView = convertView.findViewById(R.id.photoImageView);
        TextView messageTextView = convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = convertView.findViewById(R.id.nameTextView);

        FriendlyMessage message = getItem(position);

        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto){
            messageTextView.setVisibility(View.GONE);
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext()).load(message.getPhotoUrl()).into(photoImageView);
        }else{
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
        }
        authorTextView.setText(message.getName());
        return convertView;
    }
}
