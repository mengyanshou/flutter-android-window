package qiuxiang.android_window

import android.annotation.SuppressLint
import android.app.Service
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterSurfaceView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AndroidWindow(
    val service: Service,
    focusable: Boolean,
    width: Int,
    height: Int,
    private val x: Int,
    private val y: Int,
    private val engine: FlutterEngine
) {
    private var startX = 0f
    private var startY = 0f
    private var initialX = 0
    private var initialY = 0
    private var dragging = false
    private lateinit var flutterView: FlutterView
    private var windowManager = service.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    private val inflater =
        service.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val metrics = DisplayMetrics()

    @SuppressLint("InflateParams")
    private var rootView = inflater.inflate(R.layout.floating, null) as ViewGroup
    private val layoutParams = WindowManager.LayoutParams(
        width,
        height,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("Deprecation")
            WindowManager.LayoutParams.TYPE_TOAST
        },
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
        PixelFormat.TRANSLUCENT
    )

    @RequiresApi(Build.VERSION_CODES.N)
    private val dragListener = View.OnDragListener { _, event ->
//        val channel = channel ?: return@OnDragListener false
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {

                Log.e("Nightmare", "ACTION_DRAG_ENTERED");
//                channel.invokeMethod("entered", listOf(event.x, event.y))
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                Log.e("Nightmare", "ACTION_DRAG_LOCATION");
//                channel.invokeMethod("updated", listOf(event.x, event.y))
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                Log.e("Nightmare", "ACTION_DRAG_EXITED");
//                channel.invokeMethod("exited", null)
            }
            DragEvent.ACTION_DROP -> {
                app?.activity?.requestDragAndDropPermissions(event) ;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.e("Nightmare", "drag");
//                    handleDrop(event, channel, activity!!)
                }
            }
        }
        return@OnDragListener true
    }

//    @RequiresApi(Build.VERSION_CODES.N)
//    private fun handleDrop(event: DragEvent, channel: MethodChannel, activity: Activity) {
//        val permission = activity.requestDragAndDropPermissions(event) ?: return
//
//        val result = mutableListOf<String>()
//        for (i in 0 until event.clipData.itemCount) {
//            event.clipData.getItemAt(i)?.uri?.let {
//                result.add(it.toString())
//            }
//        }
//        permission.release()
//
//        channel.invokeMethod("performOperation", result)
//    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun open() {
        engine.platformViewsController.attach(
            inflater.context,
            engine.renderer,
            engine.dartExecutor
        )
        val floatingApi = AndroidWindowApi(this)
        Pigeon.AndroidWindowApi.setup(engine.dartExecutor.binaryMessenger, floatingApi)
        layoutParams.gravity = Gravity.START or Gravity.TOP
        layoutParams.x = x
        layoutParams.y = y
        windowManager.addView(rootView, layoutParams)
        @Suppress("Deprecation")
        windowManager.defaultDisplay.getMetrics(metrics)
        flutterView = FlutterView(inflater.context, FlutterSurfaceView(inflater.context, true))
        flutterView.attachToFlutterEngine(engine)
        @Suppress("ClickableViewAccessibility")
        flutterView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (dragging) {
                        setPosition(
                            initialX + (event.rawX - startX).roundToInt(),
                            initialY + (event.rawY - startY).roundToInt()
                        )
                    } else {
                        startX = event.rawX
                        startY = event.rawY
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    layoutParams.flags =
                        layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    windowManager.updateViewLayout(rootView, layoutParams)
                }
            }
            false
        }
        @Suppress("ClickableViewAccessibility")
        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowManager.updateViewLayout(rootView, layoutParams)
                    true
                }
                else -> false
            }
        }
        flutterView.setOnDragListener(dragListener);
        rootView.setOnDragListener(dragListener);
        engine.lifecycleChannel.appIsResumed()
        rootView.findViewById<LinearLayout>(R.id.floating_window)
            .addView(
                flutterView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
    }

    fun dragStart() {
        dragging = true
    }

    fun dragEnd() {
        dragging = false
    }

    fun close() {
        flutterView.detachFromFlutterEngine()
        windowManager.removeView(rootView)
    }

    fun updateLayout() {
        @Suppress("Deprecation")
        windowManager.defaultDisplay.getMetrics(metrics)
        setPosition(layoutParams.x, layoutParams.y)
    }

    fun setLayout(width: Int, height: Int) {
        layoutParams.width = width
        layoutParams.height = height
        windowManager.updateViewLayout(rootView, layoutParams)
    }

    fun setPosition(x: Int, y: Int) {
        layoutParams.x = min(max(0, x), metrics.widthPixels - layoutParams.width)
        layoutParams.y = min(max(0, y), metrics.heightPixels - layoutParams.height)
        windowManager.updateViewLayout(rootView, layoutParams)
    }
}

val AndroidWindow.app: AndroidWindowApplication?
    get() {
        return service.application as? AndroidWindowApplication
    }
