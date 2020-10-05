package net.guyor.freemacrotracker.javascript.interfaces

import android.util.Log
import android.webkit.JavascriptInterface
import net.guyor.freemacrotracker.javascript.delegates.FMTAndroidDelegate
import net.guyor.freemacrotracker.model.FMTAlertLevel

private const val TAG = "FMTPlatformInterface"
class FMTPlatformInterface constructor(private val delegate: FMTAndroidDelegate) {
    @JavascriptInterface
    public fun FMTShowAlert(msg: String?, level: String?) {
        Log.v(TAG, "FMTShowAlert msg $msg, level $level")
        if (msg != null && level != null) {
            val alertLevel: FMTAlertLevel = try {
                FMTAlertLevel.valueOf(level)
            } catch (t: Throwable) {
                Log.e(TAG, t.message.toString())
                Log.w(TAG, "Invalid alert level specified (${level})")
                FMTAlertLevel.unspecified
            }
            delegate.FMTShowAlert(msg, alertLevel);
        }
    }
}