package com.hoc081098.myapps

import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

suspend fun getUsageStatsList(
  usm: UsageStatsManager,
  packageManager: PackageManager,
  clock: Clock,
  zoneId: ZoneId = ZoneId.systemDefault(),
): List<AppInfo> = withContext(Dispatchers.IO) {
  val startInstant = Instant.now(clock)
  val endInstant = startInstant.minus(Duration.ofDays(6 * 30))

  val endTime = startInstant.toEpochMilli()
  val startTime = endInstant.toEpochMilli()

  usm
    .queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
    .mapIndexedNotNull { index, stats ->
      currentCoroutineContext().ensureActive()
      Log.d("getUsageStatsList", ">>> $index")

      val packageName = stats.packageName
      val packageInfo = runCatching { packageManager.getPackageInfoCompat(packageName) }
        .onFailure {
          if (it !is PackageManager.NameNotFoundException) {
            throw it
          }
        }
        .getOrNull()

      val lastTimeUsed = Instant.ofEpochMilli(stats.lastTimeUsed)!!
      val lastTimeVisible = Instant.ofEpochMilli(stats.lastTimeVisible)!!
      val label = packageInfo?.applicationInfo?.loadLabel(packageManager)?.toString()
      val drawable = packageInfo?.applicationInfo?.loadIcon(packageManager)

      AppInfo(
        packageName = packageName,
        name = label,
        icon = drawable,
        lastTimeUsed = lastTimeUsed.atZone(zoneId).toLocalDateTime(),
        lastTimeVisible = lastTimeVisible.atZone(zoneId).toLocalDateTime(),
      )
    }
    .sortedByDescending { it.lastTimeUsed }
    .also { Log.d("getUsageStatsList", "Result: ${it.size}") }
}
