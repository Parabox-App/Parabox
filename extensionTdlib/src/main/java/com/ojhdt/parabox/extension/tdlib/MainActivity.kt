package com.ojhdt.parabox.extension.tdlib

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ojhdt.parabox.extension.tdlib.core.TdAccountMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TdAccountManagerScreen()
            }
        }
    }
}

private data class AccountRowUi(
    val meta: TdAccountMeta,
    val active: Boolean,
    val storageBytes: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TdAccountManagerScreen() {
    val context = LocalContext.current
    val coordinator = remember { TdExtensionContainer.getCoordinator(context) }
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AccountRowUi>>(emptyList()) }
    var pendingLogout by remember { mutableStateOf<AccountRowUi?>(null) }

    fun refresh() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                coordinator.storage.listAccounts().map { meta ->
                    AccountRowUi(
                        meta = meta,
                        active = coordinator.isAccountActive(meta.accountId),
                        storageBytes = coordinator.storage.computeAccountBytes(meta.accountId),
                    )
                }
            }
            accounts = loaded
        }
    }

    LaunchedEffect(Unit) { refresh() }

    pendingLogout?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingLogout = null },
            title = { Text("登出 Telegram 账号") },
            text = {
                Text("确定要登出 ${target.meta.displayName.ifBlank { target.meta.accountId }} 并清除其本地数据库与缓存吗？其他账号不受影响。")
            },
            confirmButton = {
                TextButton(onClick = {
                    val accountId = target.meta.accountId
                    pendingLogout = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            coordinator.logoutAndPurgeAccount(accountId)
                        }
                        Toast.makeText(context, "已登出账号", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                }) {
                    Text("确定登出")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLogout = null }) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parabox TDLib 多账号管理") },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无已登录的 Telegram 账号",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请在 Parabox 主应用的「设置 → 连接与扩展」中授权并新建 Telegram (TDLib) 连接，可多次新建以同时登录多个账号。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(accounts, key = { it.meta.accountId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.meta.displayName.ifBlank { item.meta.accountId },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = if (item.active) "运行中" else "未绑定",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            if (item.meta.phoneNumber.isNotBlank()) {
                                Text(
                                    text = "手机号：${item.meta.phoneNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            val mb = String.format("%.2f MB", item.storageBytes / (1024.0 * 1024.0))
                            Text(
                                text = "账号 ID：${item.meta.accountId} · 本地占用：$mb",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val freed = withContext(Dispatchers.IO) {
                                            coordinator.storage.clearMediaCache(item.meta.accountId)
                                        }
                                        val freedMb = String.format("%.2f MB", freed / (1024.0 * 1024.0))
                                        Toast.makeText(context, "已清理 $freedMb 缓存", Toast.LENGTH_SHORT).show()
                                        refresh()
                                    }
                                }) {
                                    Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("清理缓存")
                                }
                                OutlinedButton(onClick = { pendingLogout = item }) {
                                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("登出账号")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
