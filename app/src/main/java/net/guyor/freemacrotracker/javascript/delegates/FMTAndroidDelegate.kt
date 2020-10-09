package net.guyor.freemacrotracker.javascript.delegates

import net.guyor.freemacrotracker.model.FMTAlertLevel
import net.guyor.freemacrotracker.model.FMTFileData

interface FMTAndroidDelegate {
    fun FMTShowAlert(msg: String, level: FMTAlertLevel)
    fun FMTShowAlertBar(msg: String)
    fun FMTExportData(data: FMTFileData)
    fun FMTImportData()
    fun FMTShowPrompt(msg: String, level: FMTAlertLevel)
    fun FMTFinishedLoading()
}