package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.theme.Theme

@Composable
fun EmptyChatNoticeBlock(
    modifier: Modifier = Modifier,
    state: MainSharedState,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(
                    when {
                        state.datastore.enableDynamicColor -> R.drawable.empty_dynamic
                        state.datastore.theme == Theme.WILLOW -> R.drawable.empty_willow
                        state.datastore.theme == Theme.PURPLE -> R.drawable.empty_purple
                        state.datastore.theme == Theme.SAKURA -> R.drawable.empty_sakura
                        state.datastore.theme == Theme.GARDENIA -> R.drawable.empty_gardenia
                        state.datastore.theme == Theme.WATER -> R.drawable.empty_water
                        else -> R.drawable.empty_willow
                    }
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(180.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(R.string.contact_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onClick) {
            Text(text = "新增连接")
        }
    }
}