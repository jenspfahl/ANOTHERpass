package de.jepfa.yapm.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat

object PermissionChecker {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_RW_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val PERMISSIONS_READ_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun hasRWStoragePermissions(context: Context): Boolean {
        return hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun verifyRWStoragePermissions(activity: Activity) {
        if (!hasRWStoragePermissions(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_RW_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    fun hasReadStoragePermissions(context: Context): Boolean {
        return hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun verifyReadStoragePermissions(activity: Activity) {
        if (! hasReadStoragePermissions(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_READ_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return hasPermissions(context, Manifest.permission.CAMERA)
    }

    private fun hasPermissions(context: Context, permissionName: String): Boolean {
        val permission = ActivityCompat.checkSelfPermission(context, permissionName)
        return permission == PackageManager.PERMISSION_GRANTED
    }

}
