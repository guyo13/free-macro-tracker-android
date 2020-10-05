package net.guyor.freemacrotracker.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.guyor.freemacrotracker.javascript.delegates.FMTAndroidDelegate
import net.guyor.freemacrotracker.javascript.interfaces.FMTPlatformInterface
import net.guyor.freemacrotracker.model.FMTAlertLevel

class MainViewModel : ViewModel(), FMTAndroidDelegate {
    private val _platformObject: FMTPlatformInterface = FMTPlatformInterface(this)
    val platformObject get() = _platformObject

    private val _fmtShowAlertEvent = MutableLiveData<FMTAlertEvent?>()
    val fmtShowAlertEvent: LiveData<FMTAlertEvent?> get() = _fmtShowAlertEvent

    override fun FMTShowAlert(msg: String, level: FMTAlertLevel) {
        viewModelScope.launch(context = Dispatchers.Main, block = {
            _fmtShowAlertEvent.value = FMTAlertEvent(msg, level)
        })
    }

    public fun onFmtShowAlertEventEnded() {
        _fmtShowAlertEvent.value = null
    }
}

data class FMTAlertEvent constructor(val msg: String, val level: FMTAlertLevel)