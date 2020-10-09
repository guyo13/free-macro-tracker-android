package net.guyor.freemacrotracker.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.guyor.freemacrotracker.R
import net.guyor.freemacrotracker.databinding.MainFragmentBinding
import net.guyor.freemacrotracker.model.FMTAlertLevel
import net.guyor.freemacrotracker.model.FMTFileData

private const val TAG = "MainFragment"
private const val FMT_EXPORT_USER_DATA = 0xf5ee
private const val FMT_IMPORT_USER_DATA = 0xf5ef
private const val FMT_DATA_EXPORT_TYPE = "application/json"
private const val FMT_URL = "https://free-macro-tracker--test-preview1-iwwp86yn.web.app"

// For local debugging
//private const val FMT_URL = "http://10.0.2.2:3333"

class MainFragment : Fragment() {

    private lateinit var webView: WebView
    private val webViewClient =  WebViewClient()
    private lateinit var binding: MainFragmentBinding
    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = MainFragmentBinding.inflate(inflater, container, false)
        webView = binding.webview
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        // For local development
         webView.settings.cacheMode = LOAD_NO_CACHE
        // Production cache mode
//        webView.settings.cacheMode = LOAD_CACHE_ELSE_NETWORK
        // Set web view client
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(viewModel.platformObject, "FMTPlatformInterface")
        webView.loadUrl(FMT_URL)

        // When JS code calling show alert
        viewModel.fmtShowAlertEvent.observe(viewLifecycleOwner) {
            if (it != null) {
                this@MainFragment.context?.let { ctx ->
                    val dialog = MaterialAlertDialogBuilder(ctx)
                        .setMessage(it.msg)
                        .setPositiveButton(R.string.alert_ok, null)
                    when (it.level) {
                        FMTAlertLevel.danger -> dialog.setTitle(R.string.alert_danger)
                        FMTAlertLevel.warning -> dialog.setTitle(R.string.alert_warning)
                        FMTAlertLevel.success -> dialog.setTitle(R.string.alert_success)
                        else -> {}
                    }
                    dialog.show()
                }
                viewModel.onFmtShowAlertEventEnded()
            }
        }

        // When JS indicating that the app finished loading
        viewModel.fmtFinishedLoadingEvent.observe(viewLifecycleOwner) {
            if (it == true) {
                lifecycleScope.launch(Dispatchers.Main) {
//                    Log.d(TAG, "fmtFinishedLoadingEvent")
                    binding.webview.visibility = VISIBLE
                    activity?.setTheme(R.style.AppTheme)
                    viewModel.onFmtFinishedLoadingEventEnded()
                }
            }
        }

        // When JS code is asking to show a Snackbar
        viewModel.fmtShowSnackBarEvent.observe(viewLifecycleOwner, {
            if (it != null) {
                val snackbar = Snackbar.make(binding.webview, it, LENGTH_SHORT)
                snackbar.show()
                viewModel.onFmtShowSnackbarEventEnded()
            }
        })

        // When JS code is asking to export user data
        viewModel.fmtExportDataEvent.observe(viewLifecycleOwner, {
            it?.let { data ->
                exportDataToFile(data)
            }
        })
        viewModel.fmtImportDataEvent.observe(viewLifecycleOwner, { b ->
            if (b == true) {
                openFile(FMT_IMPORT_USER_DATA, FMT_DATA_EXPORT_TYPE, null)
            }
        })
        viewModel.fmtShowPromptEvent.observe(viewLifecycleOwner) {
            if (it != null) {
                this@MainFragment.context?.let { ctx ->
                    val dialog = MaterialAlertDialogBuilder(ctx)
                        .setMessage(it.msg)
                        .setPositiveButton(R.string.prompt_yes) { dialog, which ->
                            webView.evaluateJavascript("fmtAppInstance.userPromptPlatformCallback(true);", null)
                        }
                        .setNegativeButton(R.string.prompt_no) { dialog, which ->
                            webView.evaluateJavascript("fmtAppInstance.userPromptPlatformCallback(false);", null)
                        }
                    when (it.level) {
                        FMTAlertLevel.warning -> dialog.setTitle(R.string.alert_warning)
                        else -> dialog.setTitle(R.string.alert_confirm)
                    }
                    dialog.show()
                }
                viewModel.onFmtShowPromptEventEnded()
            }
        }
        return binding.root
    }

    private fun exportDataToFile(data: FMTFileData) {
        lifecycleScope.launch(Dispatchers.IO) {
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED != state) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.webview, R.string.export_storage_fail_msg, LENGTH_LONG).show()
                }
                return@launch
            }
            createFile(data.fileName, FMT_EXPORT_USER_DATA, FMT_DATA_EXPORT_TYPE, null)
        }
    }

    /** Based on https://developer.android.com/training/data-storage/shared/documents-files
     * */
    private fun openFile(requestCode: Int, mimeType: String, pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pickerInitialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent, requestCode)
    }

    private fun createFile(fileName: String, requestCode: Int, mimeType: String, pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pickerInitialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent, requestCode)
    }

    private suspend fun writeData(createDocumentResult: Intent?) {
        withContext(Dispatchers.IO) {
            createDocumentResult?.data?.let {
                Log.d(TAG, "Opening Uri ${createDocumentResult.data} for writing")
                activity?.contentResolver?.openOutputStream(it)?.use { outputStream ->
                    val writer = outputStream.bufferedWriter()
                    writer.write(viewModel.fmtExportDataEvent.value?.data)
                    writer.flush()
                }
            }
        }
    }
    private suspend fun importData(openDocumentResult: Intent?) {
        var rawData = ""
        withContext(Dispatchers.IO) {
            openDocumentResult?.data?.let {
                Log.d(TAG, "Opening Uri ${openDocumentResult.data} for reading")
                activity?.contentResolver?.openInputStream(it)?.use { stream ->
                    val reader = stream.bufferedReader()
                    rawData = reader.readText()
                    reader.close()
                }
                if (rawData.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        webView.evaluateJavascript("FMTImportData(`$rawData`);", null)
                    }
                }
            }
        }
    }


        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        context?.let {
            when (requestCode) {
                FMT_EXPORT_USER_DATA -> {
                    lifecycleScope.launch {
                        if (resultCode == Activity.RESULT_OK) {
                            writeData(data)
                            MaterialAlertDialogBuilder(it)
                                .setTitle(R.string.alert_success)
                                .setMessage(R.string.export_success_msg).show()
                        } else {
                            if (resultCode != Activity.RESULT_CANCELED)
                                MaterialAlertDialogBuilder(it)
                                    .setTitle(R.string.alert_danger)
                                    .setMessage(R.string.export_error_msg).show()
                        }
                        viewModel.onFmtExportEventEnded()
                    }
                }
                FMT_IMPORT_USER_DATA -> {
                    if (resultCode == Activity.RESULT_OK) {
                        MaterialAlertDialogBuilder(it).
                            setTitle(R.string.alert_confirm)
                            .setMessage(R.string.import_confirm_msg)
                            .setPositiveButton(R.string.prompt_yes) { _, _ ->
                                lifecycleScope.launch {
                                    importData(data)
                                }
                            }
                            .setNegativeButton(R.string.prompt_no) { _, _ ->
                                MaterialAlertDialogBuilder(it)
                                    .setMessage(R.string.import_abort_msg).show()
                            }.show()
                        // Leave it to the JS side to send any success messages
                    } else {
                        if (resultCode != Activity.RESULT_CANCELED)
                            MaterialAlertDialogBuilder(it)
                                .setTitle(R.string.alert_danger)
                                .setMessage(R.string.import_error_msg).show()
                    }
                    viewModel.onFmtImportEventEnded()
                }
                else -> {}
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}