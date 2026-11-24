import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ojhdtapp.parabox.ui.common.UnderConstructionPage
import com.ojhdtapp.parabox.ui.navigation.suite.LocalBottomBarClearance
import com.ojhdtapp.parabox.ui.navigation.suite.withBottomBarClearance

@Composable
fun FilePage(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(
            WindowInsets.displayCutout.only(WindowInsetsSides.Bottom),
        ),
    ) { scaffoldPadding ->
        UnderConstructionPage(
            modifier = Modifier.padding(
                scaffoldPadding.withBottomBarClearance(LocalBottomBarClearance.current),
            ),
        )
    }
}

