import android.content.Context
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessageResult
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : MainRepository {
    override suspend fun receiveMessage(msg: ReceiveMessage): ReceiveMessageResult {
        db
        return ReceiveMessageResult(1, "")
    }
}