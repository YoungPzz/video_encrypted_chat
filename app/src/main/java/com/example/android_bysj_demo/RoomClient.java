package com.example.android_bysj_demo;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.webrtc.AudioTrack;
import org.webrtc.HCCrypto;
import org.webrtc.HCCryptoDecryptor;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;
import org.mediasoup.droid.MediasoupException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

public class RoomClient {
    private static final String TAG = "RoomClient";

    private final Context context;
    private Socket socket;
    private Device mediasoupDevice;
    private SendTransport sendTransport;
    private RecvTransport recvTransport;
    private Producer videoProducer;
    private Producer audioProducer;
    private String roomId;
    private String userId;
    private Boolean isSender = false;
    private String rtpCapabilities;
    private MediasoupClientListener listener;
    private ArrayList<String> consumingTransports = new ArrayList<>();
    private List<ConsumerInfo> consumerTransports = new ArrayList<>();
    private boolean toUserIsReady = false;
    private static String ServerURL = "http://52.80.120.124:3000/mediasoup";
    
    // 加密配置参数
    private boolean useSM4Encryption = true; // 默认启用 SM4 加密
    private byte[] sm4Key = new byte[]{
        0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xAB, (byte)0xCD, (byte)0xEF,
        (byte)0xFE, (byte)0xDC, (byte)0xBA, 0x76, 0x54, 0x32, 0x10
    };
    private byte[] sm4CTR = new byte[]{
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
    };

    public interface MediasoupClientListener {
        void onConnected();
        void onConnectionFailed(String error);
        void onRemoteTrackAdded(VideoTrack videoTrack, AudioTrack audioTrack);
        void onParticipantLeft(String participantId);
    }

    public RoomClient(Context context, String userId, String roomId) {
        this.context = context;
        MediasoupClient.initialize(context);
        this.roomId = roomId;
        this.userId = userId;
    }

    public void setMediasoupClientListener(MediasoupClientListener listener) {
        this.listener = listener;
    }

    public void connect() {
        try {
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionAttempts = 3;
            opts.reconnectionDelay = 1000;
            opts.forceNew = true;
            opts.secure = false; // 启用 HTTPS
//            opts.sslContext = SSLContext.getDefault(); // 忽略 SSL 验证
            opts.transports = new String[]{"websocket", "polling"}; // 允许使用 WebSocket 和 HTTP 长轮询
//            opts.secure = false;

            socket = IO.socket(ServerURL, opts);
            setupSocketListeners();
            socket.connect();

        } catch (Exception e) {
            Log.e(TAG, "Error connecting to server: " + e.getMessage());
            if (listener != null) {
                listener.onConnectionFailed(e.getMessage());
            }
        }
    }

    private void setupSocketListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Connected to signaling server");
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            if (args != null && args.length > 0) {
                // 打印详细的错误信息
                for (int i = 0; i < args.length; i++) {
                    Log.e(TAG, "Connection error [" + i + "]: " + args[i]);
                }
            } else {
                Log.e(TAG, "Connection error: No additional details provided.");
            }
        });

        // 监听你的自定义事件 'connection-success'
        socket.on("connection-success", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String socketId = data.getString("socketId");

                Log.d(TAG, "Socket connection success with ID: " + socketId);
                joinRoom();

            } catch (JSONException e) {
                Log.e(TAG, "Socket error parsing connection-success data", e);
            }
        });

        // 监听new-producer事件
        socket.on("new-producer", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String producerId = data.getString("producerId");

                Log.d(TAG, "New producer with ID: " + producerId);

                signalNewConsumerTransport(producerId);
                // 通知监听器
//                if (listener != null) {
//                    listener.onNewProducer(producerId);
//                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing new-producer data", e);
            }
        });

        // 监听producer-closed事件
        socket.on("producer-closed", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String remoteProducerId = data.getString("remoteProducerId");

                // 提取userInfo，可能是JSONObject或String
                JSONObject userInfo;
                if (data.has("userInfo")) {
                    Object userInfoObj = data.get("userInfo");
                    if (userInfoObj instanceof JSONObject) {
                        userInfo = (JSONObject) userInfoObj;
                    } else if (userInfoObj instanceof String) {
                        // 如果是字符串，尝试解析为JSONObject
                        userInfo = new JSONObject((String) userInfoObj);
                    } else {
                        userInfo = new JSONObject(); // 空对象作为默认值
                    }
                } else {
                    userInfo = new JSONObject(); // 如果没有userInfo字段
                }

                Log.d(TAG, "Producer closed: " + remoteProducerId +
                        ", userInfo: " + userInfo.toString());
//                handleProducerClosed(remoteProducerId, userInfo);

                // 通知监听器
//                if (listener != null) {
//                    listener.onProducerClosed(remoteProducerId, userInfo);
//                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing producer-closed data", e);
            }
        });
    }

    private void joinRoom() {
        JSONObject data = new JSONObject();
        try {
            data.put("roomName", roomId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("joinRoom", data, new Ack() {
            @Override
            public void call(Object... args) {
                JSONObject response = (JSONObject) args[0];
                try {
                    // 这里直接获取rtpCapabilities字段
                    JSONObject rtpCapabilitiesJson = response.getJSONObject("rtpCapabilities");
                    String rtpCapabilities = rtpCapabilitiesJson.toString();

                    createDevice(rtpCapabilities);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createDevice(String rtpCapabilities) {
        try {
            mediasoupDevice = new Device();
            mediasoupDevice.load(rtpCapabilities, null);
            createSendTransport();
        } catch (MediasoupException e) {
            Log.e(TAG, "Error creating device: " + e.getMessage());
        }
    }

    // 获取用户信息的方法
    private JSONObject getUserInfo() {
        JSONObject userInfo = new JSONObject();
        try {
            // 在这里添加用户信息，例如：
            userInfo.put("name", "syw");
            userInfo.put("id", "111");
            // 其他用户信息...
        } catch (JSONException e) {
            Log.e(TAG, "创建用户信息JSON失败", e);
        }
        return userInfo;
    }

    private void createSendTransport() {
        try {
            JSONObject data = new JSONObject();
            data.put("consumer", false);

            /**
             * 1. 客户端创建 SendTransport → 触发 onConnect → 和服务端完成 DTLS 加密握手；
             * 2. 客户端开启摄像头/麦克风，往 SendTransport 推流 → 触发 onProduce；
             * 3. onProduce 给服务端发注册指令 → 服务端创建 Producer；
             * 4. 服务端返回 Producer ID 和其他生产者信息 → 客户端获取其他用户的流（getProducers()）；
             * 5. 至此，客户端的音视频流通过加密通道，持续发送到服务端，服务端再转发给其他客户端。
             */
            SendTransport.Listener sendTransportListener = new SendTransport.Listener() {
                @Override
                public String onProduce(Transport transport, String kind, String rtpParameters, String appData) {
                    try {
                        /**
                         * 当客户端要向 SendTransport 中 “注入” 音视频流（比如开启摄像头 / 麦克风后），SendTransport 会触发 onProduce，这里的逻辑是：
                         * 收集音视频流的关键信息：
                         * kind：媒体类型（audio / 视频 video）；
                         * rtpParameters：RTP 传输参数（编码格式、端口、时钟频率等，就是之前讲的 rtpCapabilities 协商后的结果）；
                         * appData：自定义业务数据（这里合并了原始业务数据 + 用户信息，比如谁的视频流）；
                         * 通过 Socket.IO 给服务端发 transport-produce 指令，把这些信息传给服务端；
                         * 服务端收到后，创建 Producer（生产者）对象，准备接收客户端的音视频数据；
                         * 服务端返回响应（包含 Producer ID、是否有其他生产者），客户端拿到后如果有其他生产者，就去获取（比如显示其他用户的视频）。
                         */
                        Log.d(TAG, "onProduce");

                        JSONObject params = new JSONObject();
                        params.put("kind", kind);
                        params.put("rtpParameters", rtpParameters);
                        Log.d("youngptest", rtpParameters);
                        // 添加应用数据和用户信息
                        JSONObject appDataObject = new JSONObject(appData);

                        // 添加用户信息到appData
                        JSONObject userInfo = getUserInfo(); // 获取用户信息的方法
                        JSONObject combinedAppData = new JSONObject();
                        // 合并原始appData和userInfo
                        Iterator<String> keys = appDataObject.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            combinedAppData.put(key, appDataObject.get(key));
                        }
                        combinedAppData.put("userInfo", userInfo);
                        params.put("appData", combinedAppData);
                        Log.d(TAG, "produce 的数据");
                        Log.d(TAG, params.toString());
                        socket.emit("transport-produce", params, new Ack() {
                            @Override
                            public void call(Object... args) {
                                Log.e(TAG, "transport-produce");
                                if (args.length > 0 && args[0] instanceof JSONObject) {
                                    try {
                                        JSONObject response = (JSONObject) args[0];
                                        String id = response.getString("id");
                                        boolean producersExist = response.optBoolean("producersExist", false);
                                        Log.d(TAG, "处理produce响应成功");
                                        // 如果存在其他生产者，获取它们
                                        if (producersExist) {
                                            getProducers();
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "处理produce响应失败", e);
                                    }
                                }
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "producer connect error", e);
                        // 连接失败
                        transport.close();
                    }

                    return "";
                }

                @Override
                public String onProduceData(Transport transport, String sctpStreamParameters, String label, String protocol, String appData) {
                    return "";
                }

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                    /*
                    当 SendTransport 需要和服务端建立 DTLS 加密连接 时触发（DTLS 是 WebRTC 传输层加密的核心），这里的逻辑是：
                    拿到本地的 DTLS 参数（dtlsParameters）；
                    通过 Socket.IO 给服务端发 transport-connect 指令，把 DTLS 参数传给服务端；
                    服务端拿到参数后，和客户端完成 DTLS 握手，建立加密的传输通道。
                     */
                    Log.i(TAG, "producer connect");

                    try {
                        JSONObject params = new JSONObject();
                        params.put("dtlsParameters", new JSONObject(dtlsParameters));

                        // 发送 transport-connect 事件
                        socket.emit("transport-connect", params, new Ack() {
                            @Override
                            public void call(Object... args) {
                                // 成功连接后调用回调
                                Log.i(TAG, "producer connect success");
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "producer connect error", e);
                        // 连接失败
                        transport.close();
                    }
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {

                }
            };

            socket.emit("createWebRtcTransport", data, new Ack() {
                @Override
                public void call(Object... args) {
                    try {

                        Log.d(TAG, "args[0] content: " + args[0]);
                        Log.d(TAG, "args[0] type: " + args[0].getClass().getName());
                        JSONObject info = (JSONObject) args[0];
                        JSONObject params = info.optJSONObject("params");
                        String id = params.optString("id");
                        String iceParameters = params.optString("iceParameters");
                        String iceCandidates = params.optString("iceCandidates");
                        String dtlsParameters = params.optString("dtlsParameters");

                        sendTransport = mediasoupDevice.createSendTransport(
                                sendTransportListener,
                                id,
                                iceParameters,
                                iceCandidates,
                                dtlsParameters
                        );

                        if (listener != null) {
                            listener.onConnected();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating send transport: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error requesting send transport: " + e.getMessage());
        }
    }

    private void getProducers() {
        socket.emit("getProducers", new Ack() {
            @Override
            public void call(Object... args) {
                if (args.length > 0 && args[0] instanceof JSONArray) {
                    JSONArray producerIds = (JSONArray) args[0];

                    Log.i(TAG, "getProducers");
                    Log.i(TAG, "Producer IDs: " + producerIds.toString());

                    // 遍历生产者ID数组
                    for (int i = 0; i < producerIds.length(); i++) {
                        try {
                            String producerId = producerIds.getString(i);
                            // 对每个生产者ID调用signalNewConsumerTransport方法
                            signalNewConsumerTransport(producerId);
                        } catch (JSONException e) {
                            Log.e(TAG, "处理生产者ID失败", e);
                        }
                    }
                }
            }
        });
    }

    private void signalNewConsumerTransport(String remoteProducerId) {
        // 检查用户是否准备好

        // 检查是否已经为这个生产者创建了传输通道
        if (consumingTransports.contains(remoteProducerId)) {
            return; // 已存在，直接返回
        }

        // 将这个生产者ID添加到正在消费的传输列表中
        consumingTransports.add(remoteProducerId);

        // 创建WebRTC传输通道
        JSONObject data = new JSONObject();
        try {
            data.put("consumer", true); // 标记这是一个消费者传输通道请求
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        socket.emit("createWebRtcTransport", data, new Ack() {
            @Override
            public void call(Object... args) {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    JSONObject response = (JSONObject) args[0];
                    try {
                        JSONObject params = response.getJSONObject("params");

                        // 检查错误
                        if (params.has("error")) {
                            String error = params.getString("error");
                            Log.e(TAG, "创建消费者WebRTC传输错误: " + error);
                            return;
                        }

                        Log.d(TAG, "consumer接收端params");
                        Log.d(TAG, params.toString());

                        JSONObject info = params;
                        String id = info.optString("id");
                        String iceParameters = info.optString("iceParameters");
                        String iceCandidates = info.optString("iceCandidates");
                        String dtlsParameters = info.optString("dtlsParameters");


//                        String sctpParameters = info.optString("sctpParameters");


                        RecvTransport.Listener recvTransportListener = new RecvTransport.Listener() {
                            @Override
                            public void onConnect(Transport transport, String dtlsParameters) {
                                try {
                                    JSONObject params = new JSONObject();
                                    params.put("dtlsParameters", new JSONObject(dtlsParameters));
                                    params.put("serverConsumerTransportId", id);
                                    Log.d(TAG, "recvlistener connect 进来了");

                                    socket.emit("transport-recv-connect", params, new Ack() {
                                        @Override
                                        public void call(Object... args) {
                                            // 通知传输连接成功
                                            Log.d(TAG, "连接消费者传输成功");
                                        }
                                    });
                                } catch (JSONException e) {
                                    Log.e(TAG, "连接消费者传输失败", e);
                                    // 传输连接失败
                                    transport.close();
                                }
                            }

                            @Override
                            public void onConnectionStateChange(Transport transport, String connectionState) {
                                Log.d(TAG, "Consumer transport connection state changed to: " + connectionState + " for transport: " + transport.getId());

                                if (connectionState.equals("connected") || connectionState.equals("completed")) {
                                    Log.d(TAG, "WebRTC connection successfully established!");
                                } else if (connectionState.equals("failed")) {
                                    Log.e(TAG, "WebRTC connection failed - ICE negotiation failed.");
                                } else if (connectionState.equals("disconnected")) {
                                    Log.e(TAG, "WebRTC connection disconnected - temporary network issue?");
                                } else if (connectionState.equals("closed")) {
                                    Log.d(TAG, "WebRTC connection closed.");
                                }
                            }
                        };

                        RecvTransport consumerTransport = mediasoupDevice.createRecvTransport(
                                recvTransportListener,
                                id,
                                iceParameters,
                                iceCandidates,
                                dtlsParameters);
                        String severConsumerId = id;
                        connectRecvTransport(consumerTransport, remoteProducerId, severConsumerId);


                    } catch (JSONException | MediasoupException e) {
                        Log.e(TAG, "解析传输参数失败", e);
                    }
                }
            }
        });
    }


    private void connectRecvTransport(RecvTransport consumerTransport, String remoteProducerId, String serverConsumerTransportId) {
        try {
            JSONObject params = new JSONObject();
            String rtpCapabilities = mediasoupDevice.getRtpCapabilities();
            params.put("rtpCapabilities", new JSONObject(rtpCapabilities));
            params.put("remoteProducerId", remoteProducerId);
            params.put("serverConsumerTransportId", serverConsumerTransportId);
            params.put("enableRtx", true);

            socket.emit("consume", params, new Ack() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0 && args[0] instanceof JSONObject) {
                        try {
                            JSONObject response = (JSONObject) args[0];
                            JSONObject responseParams = response.getJSONObject("params");

                            // 检查错误
                            if (responseParams.has("error")) {
                                Log.e(TAG, "Cannot consume: " + responseParams.getString("error"));
                                return;
                            }

                            Log.i(TAG, "consume的返回消息");
                            Log.i(TAG, responseParams.toString());

                            // 从响应中提取所需参数
                            final String id = responseParams.getString("id");
                            final String producerId = responseParams.getString("producerId");
                            final String kind = responseParams.getString("kind");
                            final JSONObject rtpParameters = responseParams.getJSONObject("rtpParameters");
                            final JSONObject appData = responseParams.getJSONObject("userInfo");
                            final String serverConsumerId = responseParams.getString("serverConsumerId");



                            // mediasoup-client-android可能不支持encodedInsertableStreams，需要检查API
                            // consumerOptions.put("encodedInsertableStreams", true);

                            // 创建消费者(这里的实现取决于mediasoup-client-android的API)
                            Consumer consumer = consumerTransport.consume(
                                    new Consumer.Listener() {
                                        @Override
                                        public void onTransportClose(Consumer consumer) {
                                            Log.d(TAG, "Consumer transport closed: " + consumer.getId());
                                        }
                                    },
                                    id,
                                    producerId,
                                    kind,
                                    rtpParameters.toString(),
                                    appData.toString()
                            );

                            if (consumer != null) {
                                Log.d(TAG, "=== 开始设置 " + kind + " 解密器 ===");
                                Log.d(TAG, "Consumer ID: " + consumer.getId());
                                Log.d(TAG, "Producer ID: " + producerId);
                                try {
                                    HCCryptoDecryptor decryptor = new HCCryptoDecryptor();
                                    // 配置解密器
                                    decryptor.enableSM4Decryption(useSM4Encryption);
                                    if (useSM4Encryption) {
//                                        decryptor.setSM4Key(sm4Key);
//                                        decryptor.setSM4CTR(sm4CTR);
                                        Log.d(TAG, kind + "解密器已设置（SM4-CTR）");
                                    } else {
                                        Log.d(TAG, kind + "解密器已设置（XOR）");
                                    }
                                    
                                    Log.d(TAG, "HCCryptoDecryptor Java 对象创建成功");
                                    Log.d(TAG, "Native decryptor 指针: " + decryptor.getNativeHCCryptoDecryptor());

                                    consumer.setFrameDecryptor(decryptor);
                                    Log.d(TAG, kind + "解密器已设置成功");
                                    Log.d(TAG, "=== 解密器设置完成 ===");
                                } catch (Exception e) {
                                    Log.e(TAG, "设置解密器失败: " + e.getMessage(), e);
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e(TAG, "Consumer 为 null，无法设置解密器");
                            }

                            // 在Android中处理RTP接收器
                            // 注意：Android的WebRTC API可能与Web不同，需要检查是否支持编码流处理
//                            handleRtpReceiver(consumer);

                            // 保存消费者传输信息
                            ConsumerInfo consumerInfo = new ConsumerInfo();
                            consumerInfo.consumerTransport = consumerTransport;
                            consumerInfo.serverConsumerTransportId = id;
                            consumerInfo.producerId = remoteProducerId;
                            consumerInfo.consumer = consumer;
                            consumerInfo.kind = kind;
                            consumerTransports.add(consumerInfo);

                            // 处理媒体流和参与者
//                            handleMediaStreamAndParticipant(consumer, userInfo);

                            // 通知服务器恢复消费者的媒体流
                            resumeConsumer(serverConsumerId);
                        } catch (JSONException | MediasoupException e) {
                            Log.e(TAG, "解析consume响应失败", e);
                        }
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "创建consume请求失败", e);
        } catch (MediasoupException e) {
            throw new RuntimeException(e);
        }
    }


    // 恢复消费者媒体流
    private void resumeConsumer(String serverConsumerId) {
        try {
            JSONObject params = new JSONObject();
            params.put("serverConsumerId", serverConsumerId);

            socket.emit("consumer-resume", params, new Ack() {
                @Override
                public void call(Object... args) {

                }
            });
            Log.d(TAG, "消费者恢复成功: " + serverConsumerId);
            MediaStreamTrack videoTrack = null;
            MediaStreamTrack audioTrack = null;
            if (listener != null) {
                for(ConsumerInfo consumerInfo: consumerTransports){
                    Log.d(TAG, "consumerInfo");
                    Log.d(TAG, consumerInfo.serverConsumerTransportId);
                    Log.d(TAG, consumerInfo.kind);
                    Log.d(TAG, consumerInfo.producerId);
                    Log.d(TAG, String.valueOf(consumerInfo.consumer.getTrack() == null));
                    if(consumerInfo.kind.equals("video")){
                        videoTrack =  consumerInfo.consumer.getTrack();
                        if(videoTrack != null){
                            Log.d(TAG, "Video track found: " + videoTrack.id());
                            Log.d(TAG, "Track state: " + videoTrack.state());
                            Log.d(TAG, "Track enabled: " + videoTrack.enabled());
                            Log.d(TAG, "videoTrack不为空: " + serverConsumerId);
                        }
                    }else {
                        audioTrack = consumerInfo.consumer.getTrack();
                        if(audioTrack != null){
                            Log.d(TAG, "audioTrack不为空: " + serverConsumerId);
                        }
                    }
                }
                listener.onRemoteTrackAdded((VideoTrack)videoTrack, (AudioTrack)audioTrack);
            }
        } catch (JSONException e) {
            Log.e(TAG, "创建consumer-resume请求失败", e);
        }
    }


    public void addTrack(VideoTrack videoTrack) {
        if (sendTransport != null && videoTrack != null) {
            try {
                videoProducer = sendTransport.produce(
                        producer -> Log.d(TAG, "Video producer transport closed"),
                        videoTrack,
                        null,
                        null,
                        null
                );
                // 添加加密器设置
                if (videoProducer != null) {
                    HCCrypto encryptor = new HCCrypto();
                    // 配置加密器
                    encryptor.enableSM4Encryption(useSM4Encryption);
                    if (useSM4Encryption) {
//                        encryptor.setSM4Key(sm4Key);
//                        encryptor.setSM4CTR(sm4CTR);
                        Log.d(TAG, "视频加密器已设置（SM4-CTR）");
                    } else {
                        Log.d(TAG, "视频加密器已设置（XOR）");
                    }
                    videoProducer.setFrameEncryptor(encryptor);
                }
                Log.d(TAG, "produce video track success: ");
            } catch (MediasoupException e) {
                Log.e(TAG, "Error adding video track: " + e.getMessage());
            }
        }
    }

    public void addTrack(AudioTrack audioTrack) {
        if (sendTransport != null && audioTrack != null) {
            try {
                audioProducer = sendTransport.produce(
                        producer -> Log.d(TAG, "Audio producer transport closed"),
                        audioTrack,
                        null,
                        null,
                        null
                );
                if (audioProducer != null) {
                    HCCrypto encryptor = new HCCrypto();
                    // 配置加密器
                    encryptor.enableSM4Encryption(useSM4Encryption);
                    if (useSM4Encryption) {
                        encryptor.setSM4Key(sm4Key);
                        encryptor.setSM4CTR(sm4CTR);
                        Log.d(TAG, "音频加密器已设置（SM4-CTR）");
                    } else {
                        Log.d(TAG, "音频加密器已设置（XOR）");
                    }
                    audioProducer.setFrameEncryptor(encryptor);
                }
            } catch (MediasoupException e) {
                Log.e(TAG, "Error adding audio track: " + e.getMessage());
            }
        }
    }




    public void close() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
        if (sendTransport != null) {
            sendTransport.close();
        }
        if (recvTransport != null) {
            recvTransport.close();
        }
        if (mediasoupDevice != null) {
            mediasoupDevice.dispose();
        }
    }

    // ========== 加密配置方法 ==========

    /**
     * 设置是否使用 SM4 加密（默认为 true）
     * @param enable true 使用 SM4-CTR 加密，false 使用 XOR 加密
     */
    public void setSM4Encryption(boolean enable) {
        this.useSM4Encryption = enable;
        Log.d(TAG, "SM4 加密设置为: " + (enable ? "启用" : "禁用"));
    }

    /**
     * 设置 SM4 密钥（16字节）
     * @param key 16字节的密钥数组
     */
    public void setSM4Key(byte[] key) {
        if (key == null || key.length != 16) {
            Log.e(TAG, "SM4 密钥必须是 16 字节");
            return;
        }
        this.sm4Key = key;
        Log.d(TAG, "SM4 密钥已更新");
    }

    /**
     * 设置 SM4 CTR 计数器（16字节）
     * @param ctr 16字节的 CTR 数组
     */
    public void setSM4CTR(byte[] ctr) {
        if (ctr == null || ctr.length != 16) {
            Log.e(TAG, "SM4 CTR 必须是 16 字节");
            return;
        }
        this.sm4CTR = ctr;
        Log.d(TAG, "SM4 CTR 已更新");
    }

    /**
     * 获取当前是否启用 SM4 加密
     */
    public boolean isSM4EncryptionEnabled() {
        return useSM4Encryption;
    }
}