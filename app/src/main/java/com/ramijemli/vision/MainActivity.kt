package com.ramijemli.vision


import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.a.b.a.a.a.e
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import permissions.dispatcher.*


@RuntimePermissions
class MainActivity : AppCompatActivity() {


    private var handler: Handler? = null
    private var runnable: Runnable? = null
    // Set to true ensures requestInstall() triggers installation if necessary.
    private var mUserRequestedInstall = true
    private var mSession:Session? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkARAvailability()
    }

    private fun checkARAvailability() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            handler = Handler()
            runnable = Runnable {
                checkARAvailability()
            }

            handler?.postDelayed(runnable, 200)
        }

        if (availability.isSupported) {
            Toast.makeText(this, "AR available on this device", Toast.LENGTH_LONG).show()
            // indicator on the button.
        } else { // Unsupported or unknown.
            Toast.makeText(this, "AR not supported on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRationaleDialog(message: String, request: PermissionRequest) {
        AlertDialog.Builder(this)
            .setPositiveButton("Allow") { _, _ -> request.proceed() }
            .setNegativeButton("Deny") { _, _ -> request.cancel() }
            .setCancelable(false)
            .setMessage(message)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated function
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    fun showCamera() {
        try {
            if (mSession == null) {
                when(ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)){
                    // Success, create the AR session.
                    ArCoreApk.InstallStatus.INSTALLED-> mSession =  Session(this)

                    // Ensures next invocation of requestInstall() will either return
                    // INSTALLED or throw an exception.
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> mUserRequestedInstall = false
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "Handle exception $e", Toast.LENGTH_LONG).show()
            return
        }
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        showRationaleDialog("AR requires camera feature", request)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        Toast.makeText(this,"AR requires camera feature", Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    fun onCameraNeverAskAgain() {
        Toast.makeText(this, "AR requires camera feature", Toast.LENGTH_SHORT).show()
    }

}
