import android.content.Context
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    val context: Context,
) : MainRepository {

}