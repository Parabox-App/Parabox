package com.ojhdt.parabox.extension.demo.controller

import android.util.Log
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestParam
import com.yanzhenjie.andserver.annotation.RestController

@RestController
class OneBotController {
    @PostMapping("/")
    fun handle(@RequestParam("post_type") post_type: String): String {
        Log.d("ojhdt", "post_type: ${post_type}")
        return "ok"
    }
}