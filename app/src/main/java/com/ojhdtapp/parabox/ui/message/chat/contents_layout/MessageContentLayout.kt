package com.ojhdtapp.parabox.ui.message.chat.contents_layout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement

@Composable
fun PlainTextLayout(
    modifier: Modifier = Modifier,
    text: AnnotatedString
){
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = text
    )
}

@Composable
fun ImageLayout(
    modifier: Modifier = Modifier,
    model: Any?
){

}

@Composable
fun AudioLayout(
    modifier: Modifier = Modifier,
){

}

@Composable
fun FileLayout(
    modifier: Modifier = Modifier
){

}

@Composable
fun LocationLayout(
    modifier: Modifier = Modifier
){

}