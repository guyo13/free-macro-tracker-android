package com.freemacrotracker.app.javascript.interfaces

import android.util.Log
import android.webkit.JavascriptInterface
import com.freemacrotracker.app.javascript.delegates.FMTAndroidDelegate
import com.freemacrotracker.app.model.FMTAlertLevel
import com.freemacrotracker.app.model.FMTFileData

private const val TAG = "FMTPlatformInterface"
class FMTPlatformInterface constructor(private val delegate: FMTAndroidDelegate) {
    @JavascriptInterface
    fun FMTShowAlert(msg: String?, level: String?) {
//        Log.v(TAG, "FMTShowAlert msg $msg, level $level")
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
    @JavascriptInterface
    fun FMTShowAlertBar(msg: String?) {
//        Log.v(TAG, "FMTShowAlertBar msg $msg")
        msg?.let {
            delegate.FMTShowAlertBar(msg)
        }
    }
    @JavascriptInterface
    fun FMTExportData(data: String?, filename: String?) {
//        Log.v(TAG, "FMTExportData called")
        data?.let {
            delegate.FMTExportData(FMTFileData(data, filename ?: ""))
        }
    }
    @JavascriptInterface
    fun FMTImportData() {
//        Log.v(TAG, "FMTImportData called")
        delegate.FMTImportData()
    }
    @JavascriptInterface
    fun FMTShowPrompt(msg: String?, level: String?) {
//        Log.v(TAG, "FMTShowPrompt msg $msg, level $level")
        if (msg != null && level != null) {
            val alertLevel: FMTAlertLevel = try {
                FMTAlertLevel.valueOf(level)
            } catch (t: Throwable) {
                Log.e(TAG, t.message.toString())
                Log.w(TAG, "Invalid alert level specified (${level})")
                FMTAlertLevel.unspecified
            }
            delegate.FMTShowPrompt(msg, alertLevel);
        }
    }
    @JavascriptInterface
    fun FMTFinishedLoading() {
//        Log.v(TAG, "on FMTFinishedLoading")
        delegate.FMTFinishedLoading()
    }
}