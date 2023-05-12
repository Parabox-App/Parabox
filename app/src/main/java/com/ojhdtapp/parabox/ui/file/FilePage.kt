
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.parabox.ui.file.FilePageViewModel
import com.ojhdtapp.parabox.ui.common.*
import com.ramcosta.composedestinations.annotation.Destination
@Destination
@FileNavGraph(start = true)
@Composable
fun FilePage(
    modifier: Modifier = Modifier,
) {
    val viewModel: FilePageViewModel = hiltViewModel()


}

