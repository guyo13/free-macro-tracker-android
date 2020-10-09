package com.freemacrotracker.app.javascript.delegates

import com.freemacrotracker.app.model.FMTAlertLevel
import com.freemacrotracker.app.model.FMTFileData

interface FMTAndroidDelegate {
    fun FMTShowAlert(msg: String, level: FMTAlertLevel)
    fun FMTShowAlertBar(msg: String)
    fun FMTExportData(data: FMTFileData)
    fun FMTImportData()
    fun FMTShowPrompt(msg: String, level: FMTAlertLevel)
    fun FMTFinishedLoading()
}