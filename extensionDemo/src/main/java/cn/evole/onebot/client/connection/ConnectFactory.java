package cn.evole.onebot.client.connection;

import cn.evole.onebot.client.config.BotConfig;
import cn.evole.onebot.client.handler.ActionHandler;

import java.net.URI;
import java.util.concurrent.BlockingQueue;


import android.util.Log;

/**
 * Description:
 * Author: cnlimiter
 * Date: 2022/10/1 17:01
 * Version: 1.0
 */
public class ConnectFactory {
    private final BotConfig config;
    private final BlockingQueue<String> queue;
    private final ActionHandler actionHandler;
    public WSClient ws;
    /**
     *
     * @param config 配置
     * @param queue 队列消息
     */
    public ConnectFactory(BotConfig config, BlockingQueue<String> queue){
        this.config = config;
        this.queue = queue;
        this.actionHandler = new ActionHandler(config);
        try {
            this.ws = createWebsocketClient();
        }catch (NullPointerException e){
            Log.e("demo", "▌ §c连接错误，请检查服务端是否开启 §a┈━═☆");
        }
    }



    /**
     * 创建websocket客户端(支持cqhttp和mirai类型)
     * @return 连接示例
     */
    public WSClient createWebsocketClient(){
        StringBuilder builder = new StringBuilder();
        WSClient ws = null;
        if (config.isMiraiHttp()){
            builder.append(config.getUrl());
            builder.append("/all");
            builder.append("?verifyKey=");
            if (config.isAccessToken()) {
                builder.append(config.getToken());
            }
            builder.append("&qq=");
            builder.append(config.getBotId());
        }
        else {
            builder.append(config.getUrl());
            if (config.isAccessToken()) {
                builder.append("?access_token=");
                builder.append(config.getToken());
            }
        }
        String url = builder.toString();
        try {
            ws = new WSClient(URI.create(url), queue, actionHandler);
            ws.connect();
        }catch (Exception e){
            Log.e("demo", "▌ §c{}连接错误，请检查服务端是否开启 §a┈━═☆" + url);
        }
        return ws;
    }

    public void stop(){
        ws.close();
    }


}
