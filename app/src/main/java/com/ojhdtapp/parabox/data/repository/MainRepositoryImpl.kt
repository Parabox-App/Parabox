import android.content.Context
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.buildChatEntity
import com.ojhdtapp.parabox.data.local.buildContactEntity
import com.ojhdtapp.parabox.data.local.buildMessageEntity
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import javax.inject.Inject
import kotlinx.coroutines.*

class MainRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : MainRepository {
    override suspend fun receiveMessage(msg: ReceiveMessage, ext: ExtensionInfo): ParaboxResult {
        coroutineScope {
            val allowForegroundNotification = context.getDataStoreValue(DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION, false)
            val chatEntity = buildChatEntity(msg, ext)
            val contactEntity = buildContactEntity(msg, ext)
            val chatIdDeferred = async {
                db.chatDao.insertChat(chatEntity)
            }
            val contactIdDeferred = async {
                db.contactDao.insertContact(contactEntity)
            }
            val messageEntity = buildMessageEntity(msg, chatIdDeferred.await(), contactIdDeferred.await())
            db.messageDao.insertMessage(messageEntity)
        }

        return ParaboxResult(1, "")
    }
}