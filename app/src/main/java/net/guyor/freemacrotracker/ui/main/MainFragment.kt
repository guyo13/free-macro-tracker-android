package net.guyor.freemacrotracker.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import net.guyor.freemacrotracker.R
import net.guyor.freemacrotracker.databinding.MainFragmentBinding
import net.guyor.freemacrotracker.model.FMTAlertLevel

private const val TAG = "MainFragment"

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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
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
        viewModel.fmtShowSnackBarEvent.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                val snackbar = Snackbar.make(binding.webview, it, LENGTH_SHORT)
                snackbar.show()
                viewModel.onFmtShowSnackbarEventEnded()
            }
        })
        return binding.root
    }

}