package com.example.android_bysj_demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class FriendRequestAdapter extends BaseAdapter {

    private Context context;
    private List<FriendRequest> requestList;

    public FriendRequestAdapter(Context context, List<FriendRequest> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    @Override
    public int getCount() {
        return requestList.size();
    }

    @Override
    public Object getItem(int position) {
        return requestList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        }

        FriendRequest request = requestList.get(position);

        TextView tvUserName = convertView.findViewById(R.id.tv_request_user_name);
        TextView tvUserId = convertView.findViewById(R.id.tv_request_user_id);
        TextView tvMessage = convertView.findViewById(R.id.tv_request_message);

        tvUserName.setText(request.getFromUserName());
        tvUserId.setText(request.getFromUserId());
        tvMessage.setText(request.getMessage());

        return convertView;
    }
}
