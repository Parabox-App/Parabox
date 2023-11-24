package cn.evolvefield.onebot.client.connection;

import cn.evolvefield.onebot.client.handler.ActionHandler;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static cn.evolvefield.onebot.client.connection.WSClient.log;

import android.util.Log;

/**
 * Description:
 * Author: cnlimiter
 * Date: 2022/10/1 17:01
 * Version: 1.0
 */
public class ConnectFactory {
    private final BlockingQueue<String> queue;
    private final ActionHandler actionHandler;
    public WSClient ws;
    /**
     *
     * @param queue 队列消息
     */
    public ConnectFactory(BlockingQueue<String> queue){
        this.queue = queue;
        this.actionHandler = new ActionHandler();
        try {
            this.ws = createWebsocketClient();
        }catch (NullPointerException e){
            Log.e("demo","▌ §c连接错误，请检查服务端是否开启 §a┈━═☆");
        }
    }



    /**
     * 创建websocket客户端(支持cqhttp和mirai类型)
     * @return 连接示例
     */
    public WSClient createWebsocketClient(){
        WSClient ws = null;
        String url = "ws://127.0.0.1:5800";
        try {
            ws = new WSClient(URI.create(url), queue, actionHandler);
            ws.connect();
        }catch (Exception e){
            Log.e("demo","▌ §c{}连接错误，请检查服务端是否开启 §a┈━═☆"+ url);
        }
        return ws;
    }

    public void stop(){
        ws.close();
    }
}
