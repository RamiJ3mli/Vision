package com.ramijemli.vision


import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.google.ar.core.ArCoreApk
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import permissions.dispatcher.*


@RuntimePermissions
class MainActivity : AppCompatActivity() {


    private var handler: Handler? = null
    private var runnable: Runnable? = null
    // Set to true ensures requestInstall() triggers installation if necessary.
    private var mUserRequestedInstall = true
    private var mSession: Session? = null

    private var arFragment: ArFragment? = null
    private var astronautObj: ModelRenderable? = null
    private var fishObj: ModelRenderable? = null
    private var mechaObj: ModelRenderable? = null
    private var light: Light? = null
    private var astro: TransformableNode? = null
    private var i = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupAR()
    }

    private fun setupAR() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            handler = Handler()
            runnable = Runnable {
                setupAR()
            }

            handler?.postDelayed(runnable, 200)
            return
        }

        if (availability.isSupported) {
            Toast.makeText(this, "AR available on this device", Toast.LENGTH_LONG).show()
            initSession()
        } else {
            // Unsupported or unknown.
            Toast.makeText(this, "AR not supported on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun initSession() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?

        ModelRenderable.builder()
                .setSource(this, Uri.parse("Astronaut.sfb"))
                .build()
                .thenAccept { obj -> astronautObj = obj }
                .exceptionally {
                    val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        ModelRenderable.builder()
                .setSource(this, Uri.parse("fish.sfb"))
                .build()
                .thenAccept { obj -> fishObj = obj }
                .exceptionally {
                    val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        ModelRenderable.builder()
                .setSource(this, Uri.parse("mecha.sfb"))
                .build()
                .thenAccept { obj -> mechaObj = obj }
                .exceptionally {
                    val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        light = Light.builder(Light.Type.POINT)
                .setColor(com.google.ar.sceneform.rendering.Color(Color.WHITE))
                .setShadowCastingEnabled(true)
                .setIntensity(120000F)
                .build()

        arFragment?.setOnTapArPlaneListener { hitresult: HitResult, _: Plane, _: MotionEvent ->
            if (astronautObj == null || fishObj == null || mechaObj == null) {
                return@setOnTapArPlaneListener
            }


            val anchor = hitresult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment?.arSceneView?.scene)


            astro = TransformableNode(arFragment?.transformationSystem)
            astro?.scaleController?.minScale = .06f
            astro?.scaleController?.maxScale = .061f
            astro?.setParent(anchorNode)
            astro?.renderable = astronautObj
            astro?.light = light
            astro?.select()
            setupRotationAnimation()
            setupColorAnimation()
        }
    }


    private fun setupRotationAnimation() {
        val anim = ValueAnimator.ofFloat(0f, 359f)
        anim.addUpdateListener { astro?.localRotation = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), it.animatedValue as Float) }
        anim.repeatCount = ObjectAnimator.INFINITE
        anim.repeatMode = ObjectAnimator.REVERSE
        anim.interpolator = AnticipateOvershootInterpolator()
        anim.duration = 4000
        anim.start()
    }

    private fun setupColorAnimation() {
        val color = com.google.ar.sceneform.rendering.Color(Color.WHITE)
        val colors = arrayOf(Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)

        val anim = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, Color.RED)
        anim.addUpdateListener {
            color.set(it.animatedValue as Int)
            astro?.light?.color = color
        }
        anim.doOnEnd {
            try {
                i++
                anim.setIntValues(Color.parseColor("#${color.a}${color.a}${color.r}${color.r}${color.g}${color.g}${color.b}${color.b}"), colors[i])
            } catch (e: Exception) {
                i = 0
            }
        }
        anim.repeatCount = ObjectAnimator.INFINITE
        anim.repeatMode = ObjectAnimator.REVERSE
        anim.duration = 1000
        anim.start()
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
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        showRationaleDialog("AR requires camera feature", request)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        Toast.makeText(this, "AR requires camera feature", Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    fun onCameraNeverAskAgain() {
        Toast.makeText(this, "AR requires camera feature", Toast.LENGTH_SHORT).show()
    }

}
