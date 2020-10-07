package net.guyor.freemacrotracker.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
import java.io.File

private const val TAG = "MainFragment"
private const val STORAGE_WRITE_PERMISSIONS_CODE_FOR_EXPORT = 0xf5ee
private const val STORAGE_READ_PERMISSIONS_CODE_FOR_IMPORT = 0xf5ee7

class MainFragment : Fragment() {

    private lateinit var webView: WebView
    private val webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
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
        // remove in production
        webView.settings.cacheMode = LOAD_NO_CACHE
        webView.webViewClient = webViewClient
        // Change to public url in production
        webView.addJavascriptInterface(viewModel.platformObject, "FMTPlatformInterface")
        webView.loadUrl("http://10.0.2.2:3333")

        // When JS code calling show alert
        viewModel.fmtShowAlertEvent.observe(viewLifecycleOwner) {
            if (it != null) {
                // TODO Show alert
                this@MainFragment.context?.let { ctx ->
                    val dialog = MaterialAlertDialogBuilder(ctx)
                        .setMessage(it.msg)
                        .setPositiveButton(R.string.alert_ok, null)
                    when (it.level) {
                        FMTAlertLevel.danger -> dialog.setTitle(R.string.alert_danger)
                        FMTAlertLevel.warning -> dialog.setTitle(R.string.alert_warning)
                        FMTAlertLevel.success -> dialog.setTitle(R.string.alert_success)
                    }
                    dialog.show()
                }
                viewModel.onFmtShowAlertEventEnded()
            }
        }

        // When JS code is asking to show a Snackbar
        viewModel.fmtShowSnackBarEvent.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                val snackbar = Snackbar.make(binding.webview, it, LENGTH_SHORT)
                snackbar.show()
                viewModel.onFmtShowSnackbarEventEnded()
            }
        })

        // When JS code is asking to export user data
        viewModel.fmtExportDataEvent.observe(viewLifecycleOwner, Observer {
            it?.let { data ->
                when {
                    checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                        exportDataToFile(data) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                viewModel.onFmtExportEventEnded()
                            }
                        }
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                        this@MainFragment.context?.let { ctx ->
                            MaterialAlertDialogBuilder(ctx)
                                .setTitle(R.string.export_write_perm_title)
                                .setMessage(R.string.export_write_perm_msg)
                                .setPositiveButton(
                                    R.string.alert_ok,
                                    DialogInterface.OnClickListener { _, _ ->
                                        requestStorageWritePermissions()
                                    })
                                .setNegativeButton(
                                    R.string.alert_cancel,
                                    DialogInterface.OnClickListener { _, _ ->
                                        Log.i(
                                            TAG,
                                            "User rejected data export"
                                        )
                                    })
                        }
                    }
                    else -> {
                        requestStorageWritePermissions()
                    }
                }
            }
        })
        return binding.root
    }

    private fun requestStorageWritePermissions(code: Int = STORAGE_WRITE_PERMISSIONS_CODE_FOR_EXPORT) {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), code)
    }

    fun requestStorageReadPermissions(code: Int = STORAGE_READ_PERMISSIONS_CODE_FOR_IMPORT) {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), code)
    }

    private fun checkPermission(permission: String) : Boolean {
        var hasPermissions = false;
        this.context?.let {
            val res = ContextCompat.checkSelfPermission(it, permission)
            hasPermissions = res == PERMISSION_GRANTED
        }
        return hasPermissions
    }

    private fun exportDataToFile(data: FMTFileData, successCallback: (() -> Unit)?) {
        lifecycleScope.launch(Dispatchers.IO) {
            // TODO - export
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED != state) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.webview, R.string.export_storage_fail_msg, LENGTH_LONG).show()
                }
                return@launch
            }
            val basePath = Environment.DIRECTORY_DOCUMENTS + File.pathSeparator + "FreeMacroTracker"
            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, data.fileName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, basePath)
                }
            }
            successCallback?.let { it() }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE_WRITE_PERMISSIONS_CODE_FOR_EXPORT -> {
                if (grantResults[permissions.indexOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)] == PERMISSION_GRANTED) {
                    viewModel.fmtExportDataEvent.value?.let {
                        exportDataToFile(it) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                viewModel.onFmtExportEventEnded()
                            }
                        }
                    }
                } else {
                    viewModel.onFmtExportEventEnded()
                    Snackbar.make(binding.webview, R.string.export_perm_fail_msg, LENGTH_LONG).show()
                }
            }
            STORAGE_READ_PERMISSIONS_CODE_FOR_IMPORT -> {
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}