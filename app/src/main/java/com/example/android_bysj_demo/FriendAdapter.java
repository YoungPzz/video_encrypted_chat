package com.example.android_bysj_demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class FriendAdapter extends BaseAdapter {

    private Context context;
    private List<Friend> friendList;
    private OnSecurityStatusClickListener listener;

    public interface OnSecurityStatusClickListener {
        void onSecurityStatusClick(Friend friend, int position);
    }

    public FriendAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList = friendList;
    }

    public void setOnSecurityStatusClickListener(OnSecurityStatusClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return friendList.size();
    }

    @Override
    public Object getItem(int position) {
        return friendList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        }

        Friend friend = friendList.get(position);

        TextView tvName = convertView.findViewById(R.id.tv_friend_name);
        TextView tvUserId = convertView.findViewById(R.id.tv_friend_user_id);
        TextView tvSecurityStatus = convertView.findViewById(R.id.tv_security_status);
        View statusIndicator = convertView.findViewById(R.id.status_indicator);
        LinearLayout llSecurityStatusContainer = convertView.findViewById(R.id.ll_security_status_container);

        tvName.setText(friend.getName());
        tvUserId.setText(friend.getId());
        tvSecurityStatus.setText(friend.getSecurityStatus().getDisplayName());
        tvSecurityStatus.setTextColor(friend.getSecurityStatus().getColor());
        statusIndicator.setBackgroundColor(friend.getSecurityStatus().getColor());

        // 安全状态点击事件
        llSecurityStatusContainer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSecurityStatusClick(friend, position);
            }
        });

        return convertView;
    }
}
