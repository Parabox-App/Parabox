package cn.evole.onebot.client.util;

import cn.evole.onebot.client.core.Bot;
import cn.evole.onebot.sdk.entity.ArrayMsg;
import cn.evole.onebot.sdk.entity.MsgChainBean;
import cn.evole.onebot.sdk.util.BotUtils;
import cn.evole.onebot.sdk.util.RegexUtils;
import cn.evole.onebot.sdk.util.json.GsonUtil;
import cn.evole.onebot.sdk.util.json.JsonsObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Name: onebot-client / TransUtils
 * Author: cnlimiter
 * CreateTime: 2023/11/22 11:24
 * Description:
 */

public class TransUtils {

    public static JsonsObject arrayToMsg(JsonsObject json){
        if (json.has("message") && json.get().get("message").isJsonArray()){
            List<ArrayMsg> msg = GsonUtil.getGson().fromJson(json.get().getAsJsonArray("message"), new TypeToken<List<ArrayMsg>>() {
            }.getType());
            StringBuilder code = new StringBuilder();
            for(ArrayMsg msgItem : msg){
                code.append(BotUtils.arrayMsgToCode(msgItem));
            }
            json.get().addProperty("message", code.toString());
        }
        return json;
    }

    public static List<MsgChainBean> stringToMsgChain(String msg) {
        JsonArray array = new JsonArray();
        Gson json = new Gson();

        try {
            Arrays.stream(msg.split("(?<=\\[CQ:[^]]{1,99999}])|(?=\\[CQ:[^]]{1,99999}])")).filter((s) -> {
                return !s.isEmpty();
            }).forEach((s) -> {
                Matcher matcher = RegexUtils.regexMatcher("\\[CQ:([^,\\[\\]]+)((?:,[^,=\\[\\]]+=[^,\\[\\]]*)*)]", s);
                JsonObject object = new JsonObject();
                JsonObject params = new JsonObject();
                if (matcher == null) {
                    object.addProperty("type", "text");
                    params.addProperty("text", s);
                } else {
                    object.addProperty("type", matcher.group(1));
                    Arrays.stream(matcher.group(2).split(",")).filter((args) -> {
                        return !args.isEmpty();
                    }).forEach((args) -> {
                        String k = args.substring(0, args.indexOf("="));
                        String v = unescape(args.substring(args.indexOf("=") + 1));
                        params.addProperty(k, v);
                    });
                }

                object.add("data", params);
                array.add(object);
            });
        } catch (Exception var4) {
            log.error("Raw message convert failed: {}", var4.getMessage());
            return null;
        }

        return (List)json.fromJson(array, ArrayList.class);
    }
}
