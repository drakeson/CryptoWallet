package com.crypto.wallet.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.crypto.wallet.R
import com.crypto.wallet.data.models.Coin
import com.crypto.wallet.databinding.FragmentCoinDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Fragment displaying detailed coin information with secure WebView integration
 *
 * Security Features:
 * - URL whitelisting
 * - SSL error handling
 * - Input validation on JS bridge
 * - Disabled file access
 * - Mixed content blocking
 */
@AndroidEntryPoint
class CoinDetailFragment : Fragment(R.layout.fragment_coin_detail) {

    private var _binding: FragmentCoinDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var coin: Coin

    companion object {
        private const val ARG_COIN = "arg_coin"
        private const val TAG = "CoinDetailFragment"

        // Whitelisted domains for security
        private val ALLOWED_DOMAINS = listOf(
            "google.com",
            "coingecko.com",
            "coinmarketcap.com"
        )

        fun newInstance(coin: Coin): CoinDetailFragment {
            val fragment = CoinDetailFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARG_COIN, coin)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCoinDetailBinding.bind(view)

        coin = if (android.os.Build.VERSION.SDK_INT >= 33) {
            requireArguments().getParcelable(ARG_COIN, Coin::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(ARG_COIN)
        } ?: run {
            Timber.tag(TAG).e("Coin argument is null")
            parentFragmentManager.popBackStack()
            return
        }

        setupUI()
        setupWebView()
        loadCoinInfo()
    }


    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        // Load coin image with Glide
        Glide.with(binding.root.context)
            .load(coin.image)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .circleCrop()
            .into(binding.coinImage)

        // Display coin details
        binding.coinName.text = coin.name
        binding.coinSymbol.text = "Symbol: ${coin.symbol.uppercase()}"
        binding.coinPrice.text = "Price: $${formatNumber(coin.current_price)}"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                // Enable JavaScript for dynamic content
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false

                // Security Settings
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                // Performance
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            // Add JavaScript Interface for bidirectional communication
            addJavascriptInterface(WebAppInterface(), "Android")

            // Set WebViewClient for page events and security
            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return true

                    // Check if URL is from allowed domain
                    val isAllowed = ALLOWED_DOMAINS.any { domain ->
                        url.contains(domain, ignoreCase = true)
                    }

                    return if (isAllowed) {
                        Timber.tag(TAG).d("Loading allowed URL: $url")
                        false  // Let WebView handle it
                    } else {
                        Timber.tag(TAG).w("Blocked URL from untrusted domain: $url")
                        showError("URL not allowed for security reasons")
                        true  // Block the URL
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    showLoading()
                    Timber.tag(TAG).d("Page started loading: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    hideLoading()
                    Timber.tag(TAG).d("Page finished loading: $url")

                    // Inject custom JavaScript to enhance the page
                    // Sanitize coin name to prevent JS injection
                    val sanitizedName = coin.name.replace("'", "\\'").replace("\"", "\\\"")

                    view?.evaluateJavascript("""
                        (function() {
                            try {
                                if (window.Android) {
                                    Android.onPageLoaded('$sanitizedName', document.title);
                                }
                            } catch(e) {
                                console.error('Error calling Android interface:', e);
                            }
                        })();
                    """.trimIndent(), null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)

                    // Only handle main frame errors
                    if (request?.isForMainFrame == true) {
                        Timber.tag(TAG).e("WebView error: ${error?.description}")
                        showError("Failed to load page: ${error?.description}")
                        loadErrorPage()
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    // TODO: Never call handler.proceed() in production!
                    // This would bypass SSL security
                    handler?.cancel()

                    Timber.tag(TAG).e("SSL Error: ${error?.toString()}")
                    showError("SSL Error: Connection not secure")
                    loadErrorPage()
                }

                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Timber.tag(TAG).e("WebView error (legacy): $description")
                    showError("Error loading page: $description")
                    loadErrorPage()
                }
            }

            // Set WebChromeClient for progress and console logs
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    Timber.tag(TAG).d("Loading progress: $newProgress%")
                    // Could update progress bar here if needed
                }

                override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                    Timber.tag(TAG)
                        .d("WebView Console: $message at line $lineNumber from $sourceID")
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatNumber(number: Double): String {
        return String.format("%,.2f", number)
    }

    private fun loadCoinInfo() {
        // Build search query
        val query = "${coin.name} ${coin.symbol} cryptocurrency"
        val encodedQuery = Uri.encode(query)
        val url = "https://www.google.com/search?q=$encodedQuery"

        try {
            Timber.tag(TAG).d("Loading URL: $url")
            binding.webView.loadUrl(url)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error loading URL: ${e.message}")
            showError("Failed to load coin information")
            loadErrorPage()
        }
    }

    private fun loadErrorPage() {
        // Sanitize data before injecting into HTML
        val sanitizedName = coin.name
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;")

        val sanitizedSymbol = coin.symbol
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .uppercase()

        val errorHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Error Loading Content</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        margin: 0;
                        padding: 20px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        text-align: center;
                    }
                    
                    .container {
                        background: rgba(255, 255, 255, 0.1);
                        backdrop-filter: blur(10px);
                        -webkit-backdrop-filter: blur(10px);
                        border-radius: 20px;
                        padding: 40px;
                        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
                        max-width: 500px;
                        width: 100%;
                    }
                    
                    .error-icon {
                        font-size: 4em;
                        margin-bottom: 20px;
                    }
                    
                    .coin-name {
                        font-size: 2em;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    
                    .symbol {
                        display: inline-block;
                        background: rgba(255, 255, 255, 0.2);
                        padding: 10px 20px;
                        border-radius: 25px;
                        font-size: 1.2em;
                        margin: 10px 0;
                        font-weight: 600;
                    }
                    
                    p {
                        font-size: 1.1em;
                        line-height: 1.6;
                        margin: 20px 0;
                        opacity: 0.9;
                    }
                    
                    .button-group {
                        display: flex;
                        gap: 10px;
                        justify-content: center;
                        flex-wrap: wrap;
                        margin-top: 30px;
                    }
                    
                    button {
                        background: white;
                        color: #667eea;
                        border: none;
                        padding: 15px 30px;
                        font-size: 1.1em;
                        border-radius: 25px;
                        cursor: pointer;
                        font-weight: bold;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
                        transition: all 0.3s ease;
                        min-width: 120px;
                    }
                    
                    button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
                    }
                    
                    button:active {
                        transform: translateY(0);
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
                    }
                    
                    .info {
                        margin-top: 30px;
                        font-size: 0.9em;
                        opacity: 0.7;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="error-icon">⚠️</div>
                    <div class="coin-name">$sanitizedName</div>
                    <div class="symbol">$sanitizedSymbol</div>
                    <p>Unable to load external content</p>
                    <p class="info">
                        This could be due to network issues, content restrictions, or security policies.
                    </p>
                    <div class="button-group">
                        <button onclick="retry()">🔄 Retry</button>
                        <button onclick="goBack()">← Go Back</button>
                    </div>
                </div>
                
                <script>
                    function retry() {
                        try {
                            if (window.Android) {
                                Android.retryLoading();
                            } else {
                                console.error('Android interface not available');
                            }
                        } catch(e) {
                            console.error('Error calling retry:', e);
                        }
                    }
                    
                    function goBack() {
                        try {
                            if (window.Android) {
                                Android.goBack();
                            } else {
                                console.error('Android interface not available');
                            }
                        } catch(e) {
                            console.error('Error calling goBack:', e);
                        }
                    }
                    
                    // Notify Android that error page loaded
                    try {
                        if (window.Android) {
                            Android.onError('Page failed to load');
                        }
                    } catch(e) {
                        console.error('Error sending error notification:', e);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.webView.loadDataWithBaseURL(
            null,
            errorHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * JavaScript Interface for bidirectional communication between WebView and Native
     */
    inner class WebAppInterface {

        @JavascriptInterface
        fun onPageLoaded(coinName: String, pageTitle: String) {
            // Validate input
            if (coinName.isBlank() || pageTitle.isBlank()) {
                Timber.tag(TAG).w("Invalid input to onPageLoaded: empty strings")
                return
            }

            // Limit length to prevent abuse
            val sanitizedName = coinName.take(100)
            val sanitizedTitle = pageTitle.take(200)

            Timber.tag(TAG).d("Page loaded for $sanitizedName: $sanitizedTitle")

            activity?.runOnUiThread {
                // Could update UI here based on page load
            }
        }

        @JavascriptInterface
        fun onError(error: String) {
            val sanitizedError = error.take(500)

            Timber.tag(TAG).e("JS Error: $sanitizedError")

            activity?.runOnUiThread {
                showError(sanitizedError)
            }
        }

        @JavascriptInterface
        fun retryLoading() {
            Timber.tag(TAG).d("Retry loading requested from JS")

            activity?.runOnUiThread {
                loadCoinInfo()
            }
        }

        @JavascriptInterface
        fun goBack() {
            Timber.tag(TAG).d("Go back requested from JS")

            activity?.runOnUiThread {
                parentFragmentManager.popBackStack()
            }
        }

        @JavascriptInterface
        fun showToast(message: String) {
            if (message.isBlank()) {
                Timber.tag(TAG).w("Attempted to show empty toast")
                return
            }

            val sanitizedMessage = message.take(100)  // Limit length

            activity?.runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    sanitizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding.webView.apply {
            stopLoading()
            clearHistory()
            clearCache(true)
            removeAllViews()
            destroy()
        }

        _binding = null
    }
}