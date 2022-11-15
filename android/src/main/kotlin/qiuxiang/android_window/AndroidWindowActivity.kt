package qiuxiang.android_window

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity

open class AndroidWindowActivity : FlutterActivity() {
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        app?.mainApi?.onActivityResult()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStart() {
        super.onStart()
        app?.activity = this
        var view = TextView(this)
        view.setOnDragListener(dragListener);
    }
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
}
