package de.jepfa.yapm.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import de.jepfa.yapm.ui.BaseActivity

object PermissionChecker {

    const val PERMISSION_REQUEST_CODE = 163434
    private val PERMISSIONS_RW_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val PERMISSIONS_READ_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun hasRWStoragePermissions(context: Context): Boolean {
        return !isExtReadWritePermissionsNeeded() || hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun verifyRWStoragePermissions(activity: Activity) {
        if (!hasRWStoragePermissions(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_RW_STORAGE,
                    PERMISSION_REQUEST_CODE
            )
        }
    }

    fun hasReadStoragePermissions(context: Context): Boolean {
        return !isExtReadWritePermissionsNeeded() || hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun verifyReadStoragePermissions(activity: Activity) {
        if (! hasReadStoragePermissions(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_READ_STORAGE,
                    PERMISSION_REQUEST_CODE
            )
        }
    }

    fun verifyCameraPermissions(activity: BaseActivity): Boolean {
        if (! hasCameraPermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
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

    private fun isExtReadWritePermissionsNeeded() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

}
