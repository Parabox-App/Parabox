import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ojhdtapp.parabox.ui.common.UnderConstructionPage

@Composable
fun FilePage(
    modifier: Modifier = Modifier,
) {
    Scaffold {
        UnderConstructionPage(modifier = Modifier.padding(it))
    }
}

