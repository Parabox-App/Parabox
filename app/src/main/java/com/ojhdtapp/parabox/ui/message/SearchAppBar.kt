package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.ui.util.LocalFixedInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    modifier: Modifier = Modifier,
//    isActivated: Boolean = false,
    text: String,
    onTextChange: (text: String) -> Unit
) {
    var isActivated by remember {
        mutableStateOf(false)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                LocalFixedInsets.current.statusBarHeight + 64.dp
            )
            .padding(
                PaddingValues(
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 16.dp).value,
                    animateDpAsState(
                        targetValue = if (isActivated) 0.dp else 8.dp + LocalFixedInsets.current.statusBarHeight
                    ).value,
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 16.dp).value,
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 8.dp).value
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animateIntAsState(targetValue = if (isActivated) 0 else 50).value))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { isActivated = !isActivated },
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "search",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                BasicTextField(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    value = text,
                    onValueChange = onTextChange,
                    enabled = false,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true
                )
            }
        }
    }
}