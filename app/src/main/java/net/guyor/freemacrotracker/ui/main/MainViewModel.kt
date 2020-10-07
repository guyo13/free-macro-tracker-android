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
import net.guyor.freemacrotracker.model.FMTFileData

class MainViewModel : ViewModel(), FMTAndroidDelegate {
    private val _platformObject: FMTPlatformInterface = FMTPlatformInterface(this)
    val platformObject get() = _platformObject

    private val _fmtShowAlertEvent = MutableLiveData<FMTAlertEvent?>()
    val fmtShowAlertEvent: LiveData<FMTAlertEvent?> get() = _fmtShowAlertEvent

    private val _fmtShowSnackBarEvent = MutableLiveData<String?>()
    val fmtShowSnackBarEvent: LiveData<String?> get() = _fmtShowSnackBarEvent

    private val _fmtExportDataEvent = MutableLiveData<FMTFileData?>()
    val fmtExportDataEvent: LiveData<FMTFileData?> get() = _fmtExportDataEvent

    private val _fmtImportDataEvent = MutableLiveData<Boolean?>()
    val fmtImportDataEvent: LiveData<Boolean?> get() = _fmtImportDataEvent

    private val _fmtShowPromptEvent = MutableLiveData<FMTAlertEvent?>()
    val fmtShowPromptEvent: LiveData<FMTAlertEvent?> get() = _fmtShowPromptEvent

    override fun FMTShowAlert(msg: String, level: FMTAlertLevel) {
        viewModelScope.launch(context = Dispatchers.Main, block = {
            _fmtShowAlertEvent.value = FMTAlertEvent(msg, level)
        })
    }

    override fun FMTShowAlertBar(msg: String) {
        viewModelScope.launch(context = Dispatchers.Main, block = {
            _fmtShowSnackBarEvent.value = msg
        })
    }

    override fun FMTExportData(data: FMTFileData) {
        viewModelScope.launch(context = Dispatchers.Main, block = {
            _fmtExportDataEvent.value = data
        })
    }

    override fun FMTImportData() {
        viewModelScope.launch(context = Dispatchers.Main, block = {
            _fmtImportDataEvent.value = true
        })
    }

    override fun FMTShowPrompt(msg: String, level: FMTAlertLevel) {
        viewModelScope.launch(context = Dispatchers.Main, block = {
            _fmtShowPromptEvent.value = FMTAlertEvent(msg, level)
        })
    }

    fun onFmtShowAlertEventEnded() {
        _fmtShowAlertEvent.value = null
    }

    fun onFmtShowSnackbarEventEnded() {
        _fmtShowSnackBarEvent.value = null
    }

    fun onFmtExportEventEnded() {
        _fmtExportDataEvent.value = null
    }

    fun onFmtImportEventEnded() {
        _fmtImportDataEvent.value = null
    }

    fun onFmtShowPromptEventEnded() {
        _fmtShowPromptEvent.value = null
    }
}

data class FMTAlertEvent constructor(val msg: String, val level: FMTAlertLevel)