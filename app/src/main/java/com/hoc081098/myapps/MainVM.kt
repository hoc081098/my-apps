@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.hoc081098.myapps

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface AppListItem

data class AppInfo(
  val packageName: String,
  val name: String?,
  val icon: Drawable?,
  val lastTimeUsed: LocalDateTime,
  val lastTimeVisible: LocalDateTime,
) : AppListItem


data class Header(val day: LocalDate) : AppListItem

class MainVM(
  private val application: Application,
  private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
  private val clock = Clock.systemUTC()!!
  private val usm by lazy { application.getSystemService<UsageStatsManager>()!! }
  private val packageManager get() = application.packageManager!!

  private val loadApps = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  private val _searchTerm = MutableStateFlow(savedStateHandle.get<String?>(SEARCH_TERM_KEY).orEmpty())

  val filteredAppInfoList: StateFlow<Map<LocalDate, List<AppInfo>>?>
  val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

  init {
    val appInfoListFlow = loadApps
      .flatMapLatest {
        suspend {
          getUsageStatsList(
            usm = usm,
            packageManager = packageManager,
            clock = clock
          )
        }
          .asFlow()
          .map {
            @Suppress("USELESS_CAST")
            it as List<AppInfo>?
          }
          .catch { e ->
            Log.e(TAG, "Error: ${e.message}", e)
            emit(null)
          }
      }

    filteredAppInfoList = combine(
      searchTerm
        .debounce(400)
        .distinctUntilChanged(),
      appInfoListFlow,
    ) { searchTerm, appInfoList ->
      if (searchTerm.isBlank()) {
        appInfoList
      } else {
        appInfoList?.filter {
          it.name?.contains(searchTerm, ignoreCase = true) == true
              || it.packageName.contains(searchTerm, ignoreCase = true)
        }
      }
    }
      .map { appInfos ->
        appInfos?.groupBy { it.lastTimeUsed.toLocalDate() }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
      )
  }

  fun getInstalledApps() {
    viewModelScope.launch { loadApps.emit(Unit) }
  }

  fun onSearchTermChange(value: String) {
    _searchTerm.value = value
    savedStateHandle[SEARCH_TERM_KEY] = value
  }

  private companion object {
    private const val SEARCH_TERM_KEY = "searchTerm"
    private const val TAG = "MainVM"
  }
}


fun ApplicationInfo.isSystemPackage(): Boolean = flags and ApplicationInfo.FLAG_SYSTEM != 0

fun PackageInfo.isSystemPackage(): Boolean = applicationInfo.isSystemPackage()

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
  } else {
    @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
  }