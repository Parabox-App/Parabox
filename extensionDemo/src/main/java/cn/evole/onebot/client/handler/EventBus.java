package cn.evole.onebot.client.handler;

import android.util.Log;

import cn.evole.onebot.client.listener.EnableEventListener;
import cn.evole.onebot.client.listener.EventListener;
import cn.evole.onebot.client.util.ListenerUtils;
import cn.evole.onebot.client.util.TransUtils;
import cn.evole.onebot.sdk.event.Event;
import cn.evole.onebot.sdk.event.message.MessageEvent;
import cn.evole.onebot.sdk.util.json.GsonUtil;
import cn.evole.onebot.sdk.util.json.JsonsObject;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project: onebot-client
 * Author: cnlimiter
 * Date: 2023/3/19 15:45
 * Description:
 */
@SuppressWarnings("unused")
public class EventBus {
    //存储监听器对象
    protected List<EventListener<?>> eventlistenerlist = new ArrayList<>();
    //缓存类型与监听器的关系
    protected Map<Class<? extends Event>, List<EventListener<?>>> cache = new ConcurrentHashMap<>();
    //线程池 用于并发执行队列中的任务
    protected BlockingQueue<String> queue;
    private boolean close = false;

    public EventBus(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    public void addListener(EventListener<?> EventListener) {
        this.eventlistenerlist.add(EventListener);
    }

    public void stop() {
        this.close = true;
        this.cache.clear();
        this.eventlistenerlist.clear();
    }

    public void run() {
        try {
            while (!this.close) {
                this.runTask();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行任务
     */
    protected void runTask() {
        String message = this.getTask();//获取消息
        if (message.equals("null")) {
            Log.d("demo", "消息队列为空");
            return;
        }
        Log.d("demo", "处理前信息" + message);
        JsonsObject msg = TransUtils.arrayToMsg(new JsonsObject(message));
        Class<? extends Event> messageType = ListenerUtils.getMessageType(msg);//获取消息对应的实体类型
        if (messageType == null) {
            Log.d("demo", "类型不支持");
            return;
        }
        Log.d("demo", "接收到上报消息内容" + msg);
        Event bean = GsonUtil.strToJavaBean(msg.toString(), messageType);//将消息反序列化为对象
        List<EventListener<?>> executes = this.cache.get(messageType);
        if (this.cache.get(messageType) == null) {
            executes = getMethod(messageType);
            this.cache.put(messageType, executes);
        }
        Log.d("demo", "execute 3:" + executes.toString());
        for (EventListener eventListener : executes) {
            Log.d("demo", "监听器:" + executes.toString());
            eventListener.onMessage(bean);//调用监听方法
        }

    }

    /**
     * 从队列中获取任务
     *
     * @return
     */
    protected String getTask() {
        try {
            return this.queue.take();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    /**
     * 获取能处理消息类型的处理器
     *
     * @param messageType
     * @return
     */
    protected List<EventListener<?>> getMethod(Class<? extends Event> messageType) {
        List<EventListener<?>> eventListeners = new ArrayList<>();
        for (EventListener<?> eventListener : this.eventlistenerlist) {
            Log.d("demo", "eventListener:" + eventListener);
            try {
                try {
                    eventListener.getClass().getMethod("onMessage", messageType);//判断是否注册监听器
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    continue;//不支持则跳过
                }
                if (eventListener instanceof EnableEventListener) {
                    EnableEventListener<?> enableListener = (EnableEventListener<?>) eventListener;
                    if (!enableListener.enable()) {//检测是否开启该插件
                        continue;
                    }
                }
                eventListeners.add(eventListener);//开启后添加入当前类型的插件
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return eventListeners;
    }

    public List<EventListener<?>> getListenerList() {
        return this.eventlistenerlist;
    }

    /**
     * 清除类型缓存
     */
    public void cleanCache() {
        this.cache.clear();
    }


}
