package net.guyor.freemacrotracker.javascript.delegates

import net.guyor.freemacrotracker.model.FMTAlertLevel

interface FMTAndroidDelegate {
    fun FMTShowAlert(msg: String, level: FMTAlertLevel)
}