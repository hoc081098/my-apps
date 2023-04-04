@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.hoc081098.myapps

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.hoc081098.myapps.ui.theme.MyAppsTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
  private val vm by viewModels<MainVM>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MyAppsTheme {
        // A surface container using the 'background' color from the theme
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          Scaffold(
            topBar = {
              CenterAlignedTopAppBar(
                title = {
                  Text(text = "My Apps")
                }
              )
            }
          ) { innerPadding ->
            Column(
              modifier = Modifier
                .padding(innerPadding)
                .consumedWindowInsets(innerPadding),
            ) {
              val searchTerm by vm.searchTerm.collectAsState()
              val appInfoList by vm.filteredAppInfoList.collectAsState()

              TextField(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp),
                value = searchTerm,
                onValueChange = vm::onSearchTermChange,
              )

              Spacer(modifier = Modifier.height(16.dp))

              ListContent(
                modifier = Modifier.weight(1f),
                appInfoList = appInfoList,
              )
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()

    val mode = getSystemService<AppOpsManager>()!!
      .unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName
      )
    val granted = mode == AppOpsManager.MODE_ALLOWED

    if (!granted) {
      startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    } else {
      vm.getInstalledApps()
    }
  }
}

@Composable
private fun ListContent(
  appInfoList: List<MainVM.AppInfo>?,
  modifier: Modifier = Modifier,
) {
  if (appInfoList == null) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
    return
  }

  if (appInfoList.isEmpty()) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "Empty",
        style = MaterialTheme.typography.bodyLarge
      )
    }
    return
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(items = appInfoList) { appInfo ->
      AppInfoItem(
        modifier = Modifier.fillParentMaxWidth(),
        appInfo = appInfo,
      )
    }
  }
}

@Composable
private fun AppInfoItem(
  appInfo: MainVM.AppInfo,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val bitmap = remember(appInfo.icon) {
      appInfo.icon
        .toBitmap(
          width = with(density) { 48.dp.roundToPx() },
          height = with(density) { 48.dp.roundToPx() },
        )
        .asImageBitmap()
    }

    Image(
      modifier = Modifier.sizeIn(48.dp),
      bitmap = bitmap,
      contentDescription = null,
    )

    Spacer(modifier = Modifier.width(16.dp))

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.Start,
    ) {
      Text(
        text = appInfo.name,
        style = MaterialTheme.typography.bodyLarge,
      )

      Text(
        text = appInfo.packageName,
        style = MaterialTheme.typography.bodyMedium,
      )

      Text(
        text = "Last time used: " + Formatter.format(appInfo.lastTimeUsed),
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
      )

      Text(
        text = "Last time visible: " + Formatter.format(appInfo.lastTimeVisible),
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
      )
    }
  }
}

private val Formatter by lazy {
  DateTimeFormatter.ofPattern(
    "HH:mm:ss dd/MM/yyyy",
    Locale.getDefault(),
  )!!
}

@Composable
fun Greeting(name: String) {
  Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  MyAppsTheme {
    Greeting("Android")
  }
}
