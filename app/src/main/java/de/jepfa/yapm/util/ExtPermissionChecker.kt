package de.jepfa.yapm.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object ExtPermissionChecker {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_RW_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val PERMISSIONS_READ_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun verifyRWStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_RW_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    fun verifyReadStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_READ_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
