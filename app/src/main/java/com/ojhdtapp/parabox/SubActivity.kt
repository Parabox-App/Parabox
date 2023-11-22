package com.ojhdtapp.parabox

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SubActivity  : AppCompatActivity()  {
    @Inject
    lateinit var extensionManager: ExtensionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}