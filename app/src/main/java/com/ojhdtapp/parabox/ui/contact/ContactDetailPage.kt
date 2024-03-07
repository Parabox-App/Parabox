package com.ojhdtapp.parabox.ui.contact

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailPage(
    modifier: Modifier = Modifier,
    viewModel: ContactPageViewModel,
    mainSharedState: MainSharedState,
    layoutType: ContactLayoutType,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    Crossfade(targetState = state.contactDetail.shouldDisplay, label = "") {
        if (it) {
            Scaffold(topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    }
                )
            }) { innerPadding ->
                Column(modifier = modifier.padding(innerPadding)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CommonAvatar(
                            model = CommonAvatarModel(
                                model = state.contactDetail.contactWithExtensionInfo?.contact?.avatar?.getModel(),
                                name = state.contactDetail.contactWithExtensionInfo?.contact?.name ?: "null"
                            )
                        )
                    }
                    Row {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("发起会话")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {}
                            ) {
                                Box(modifier = Modifier.size(30.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Message,
                                        contentDescription = "message"
                                    )
                                }
                            }
                        }
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("添加至联系人")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = {}
                            ) {
                                Box(modifier = Modifier.size(30.dp)) {
                                    Icon(imageVector = Icons.Outlined.PersonAddAlt, contentDescription = "add_contact")
                                }
                            }
                        }
                        Card(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(text = "信息")
                            Row {
                                Text(text = state.contactDetail.contactWithExtensionInfo?.extensionInfo?.name ?: "")
                            }
                        }
                        Card(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(text = "加入的群组")
                        }
                    }
                }
            }
        }
    }
}