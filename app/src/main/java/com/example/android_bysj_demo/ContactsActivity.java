package com.example.android_bysj_demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private ListView lvFriends;
    private ListView lvFriendRequests;
    private Button btnAddFriend;
    private TextView tvFriendRequestsCount;

    private List<Friend> friendList;
    private List<FriendRequest> friendRequestList;
    private FriendAdapter friendsAdapter;
    private FriendRequestAdapter requestsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        initViews();
        initData();
        setupListeners();
    }

    private void initViews() {
        lvFriends = findViewById(R.id.lv_friends);
        lvFriendRequests = findViewById(R.id.lv_friend_requests);
        btnAddFriend = findViewById(R.id.btn_add_friend);
        tvFriendRequestsCount = findViewById(R.id.tv_friend_requests_count);
    }

    private void initData() {
        // 初始化好友列表（写死数据）
        friendList = new ArrayList<>();
        friendList.add(new Friend("13800138002", "李老师", Friend.SecurityStatus.SAFE));
        friendList.add(new Friend("13800138003", "车同学", Friend.SecurityStatus.WARNING));
        friendList.add(new Friend("13800138004", "吴同学（2）", Friend.SecurityStatus.SAFE));
        friendList.add(new Friend("13800138005", "李同学", Friend.SecurityStatus.DANGER));

        // 初始化好友申请列表（写死数据）
        friendRequestList = new ArrayList<>();
        friendRequestList.add(new FriendRequest("req_001", "13900139001", "吴同学", "我是吴同学，想加你为好友"));

        // 设置适配器
        friendsAdapter = new FriendAdapter(this, friendList);
        friendsAdapter.setOnSecurityStatusClickListener(this::showSecurityCheckDialog);
        lvFriends.setAdapter(friendsAdapter);

        requestsAdapter = new FriendRequestAdapter(this, friendRequestList);
        lvFriendRequests.setAdapter(requestsAdapter);

        // 更新申请数量显示
        updateFriendRequestsCount();
    }

    private void setupListeners() {
        // 添加好友按钮点击事件
        btnAddFriend.setOnClickListener(v -> showAddFriendDialog());

        // 好友列表项点击事件
        lvFriends.setOnItemClickListener((parent, view, position, id) -> {
            Friend friend = friendList.get(position);
            showFriendDetailDialog(friend);
        });

        // 好友申请列表项点击事件
        lvFriendRequests.setOnItemClickListener((parent, view, position, id) -> {
            FriendRequest request = friendRequestList.get(position);
            showFriendRequestDialog(request, position);
        });
    }

    private void showAddFriendDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_friend, null);
        EditText etUserId = dialogView.findViewById(R.id.et_friend_user_id);
        EditText etMessage = dialogView.findViewById(R.id.et_request_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加好友");
        builder.setView(dialogView);
        builder.setPositiveButton("发送申请", (dialog, which) -> {
            String userId = etUserId.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (userId.isEmpty()) {
                Toast.makeText(ContactsActivity.this, "请输入用户ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // 模拟查询用户安全状态（实际项目中应从服务器获取）
            Friend.SecurityStatus userStatus = checkUserSecurityStatus(userId);

            // 如果是高风险用户，弹出确认对话框
            if (userStatus == Friend.SecurityStatus.DANGER) {
                showHighRiskConfirmDialog(userId, message);
            } else {
                // 直接发送申请
                sendFriendRequest(userId, message);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 显示高风险用户确认对话框
     */
    private void showHighRiskConfirmDialog(String userId, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("安全警告");
        builder.setMessage("该用户的状态为高风险，确认添加吗？");
        builder.setPositiveButton("确认添加", (dialog, which) -> {
            sendFriendRequest(userId, message);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 发送好友申请
     */
    private void sendFriendRequest(String userId, String message) {
        // 模拟发送申请成功
        Toast.makeText(ContactsActivity.this, "好友申请已发送", Toast.LENGTH_SHORT).show();
    }

    /**
     * 查询用户安全状态（模拟）
     */
    private Friend.SecurityStatus checkUserSecurityStatus(String userId) {
        // 模拟：根据用户ID判断风险等级
        // 实际项目中应从服务器查询
        if (userId.contains("999")) {
            return Friend.SecurityStatus.DANGER;
        } else if (userId.contains("666")) {
            return Friend.SecurityStatus.WARNING;
        } else {
            return Friend.SecurityStatus.SAFE;
        }
    }

    private void showFriendDetailDialog(Friend friend) {
        String statusText = friend.getSecurityStatus().getDisplayName();
        String statusColor = getStatusColorHex(friend.getSecurityStatus());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("好友详情");
        builder.setMessage("用户ID: " + friend.getId() + "\n" +
                "昵称: " + friend.getName() + "\n" +
                "安全状态: " + statusText);

        builder.setPositiveButton("确定", null);
        builder.setNegativeButton("删除好友", (dialog, which) -> showDeleteConfirmDialog(friend));
        builder.setNeutralButton("邀请视频聊天", (dialog, which) -> inviteToVideoChat(friend));
        builder.show();
    }

    /**
     * 邀请好友进入视频聊天
     */
    private void inviteToVideoChat(Friend friend) {
        // 跳转到视频聊天界面，自动填入房间号（使用好友手机号作为房间号）
        Intent intent = new Intent(this, VideoChatActivity.class);
        intent.putExtra("USER_ID", "user_1"); // 当前用户ID
        intent.putExtra("ROOM_ID", friend.getId()); // 使用好友手机号作为房间号
        intent.putExtra("USE_SM4_ENCRYPTION", true); // 启用SM4加密
        intent.putExtra("OPEN_ENCRYPTION", true); // 启用加密
        startActivity(intent);

        Toast.makeText(this, "已邀请 " + friend.getName() + " 进入视频聊天", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示安全状态检测对话框
     */
    private void showSecurityCheckDialog(Friend friend, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重新检测安全状态");
        builder.setMessage("确定要重新检测好友 " + friend.getName() + " 的安全状态吗？");
        builder.setPositiveButton("确认检测", (dialog, which) -> {
            performSecurityCheck(friend, position);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 执行安全状态检测
     */
    private void performSecurityCheck(Friend friend, int position) {
        // 模拟检测过程，随机生成新的安全状态
        Friend.SecurityStatus[] statuses = Friend.SecurityStatus.values();
        Friend.SecurityStatus newStatus = statuses[(int) (Math.random() * statuses.length)];

        // 更新好友的安全状态
        friendList.get(position);
        friendList.set(position, new Friend(friend.getId(), friend.getName(), newStatus));
        friendsAdapter.notifyDataSetChanged();

        String statusText = newStatus.getDisplayName();
        Toast.makeText(this, friend.getName() + " 的安全状态已更新为：" + statusText, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示删除好友确认对话框
     */
    private void showDeleteConfirmDialog(Friend friend) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除好友 " + friend.getName() + " 吗？");
        builder.setPositiveButton("确定删除", (dialog, which) -> {
            // 从好友列表中移除
            friendList.remove(friend);
            friendsAdapter.notifyDataSetChanged();
            Toast.makeText(ContactsActivity.this, "已删除好友", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showFriendRequestDialog(FriendRequest request, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("好友申请");
        builder.setMessage("来自: " + request.getFromUserName() +
                "\n用户ID: " + request.getFromUserId() +
                "\n申请消息: " + request.getMessage());

        builder.setPositiveButton("同意", (dialog, which) -> {
            // 同意好友申请，将申请者添加到好友列表
            Friend newFriend = new Friend(
                    request.getFromUserId(),
                    request.getFromUserName(),
                    Friend.SecurityStatus.SAFE
            );
            friendList.add(newFriend);
            friendsAdapter.notifyDataSetChanged();

            // 从申请列表中移除
            friendRequestList.remove(position);
            requestsAdapter.notifyDataSetChanged();
            updateFriendRequestsCount();

            Toast.makeText(ContactsActivity.this, "已同意好友申请", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("拒绝", (dialog, which) -> {
            // 从申请列表中移除
            friendRequestList.remove(position);
            requestsAdapter.notifyDataSetChanged();
            updateFriendRequestsCount();

            Toast.makeText(ContactsActivity.this, "已拒绝好友申请", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("稍后处理", null);
        builder.show();
    }

    private void updateFriendRequestsCount() {
        if (friendRequestList.isEmpty()) {
            tvFriendRequestsCount.setText("暂无好友申请");
        } else {
            tvFriendRequestsCount.setText("好友申请 (" + friendRequestList.size() + ")");
        }
    }

    private String getStatusColorHex(Friend.SecurityStatus status) {
        return String.format("#%06X", (0xFFFFFF & status.getColor()));
    }
}
