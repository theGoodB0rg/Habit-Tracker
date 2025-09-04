package com.habittracker.legal.presentation

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.habittracker.legal.domain.HelpPageType

/**
 * Modern WebView screen for displaying legal content with proper error handling
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HelpWebViewScreen(
    pageType: String,
    navController: NavController,
    viewModel: LegalViewModel = hiltViewModel()
) {
    val htmlContentState by viewModel.htmlContentState.collectAsState()
    
    // Convert string pageType to HelpPageType enum
    val helpPageType = remember(pageType) {
        when (pageType) {
            "privacy_policy" -> HelpPageType.PRIVACY_POLICY
            "terms_of_service" -> HelpPageType.TERMS_OF_SERVICE
            "about_us" -> HelpPageType.ABOUT
            else -> HelpPageType.ABOUT
        }
    }
    
    // State for WebView loading and errors
    var isWebViewLoading by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(helpPageType) {
        viewModel.loadHtmlContent(helpPageType)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(getPageTitle(helpPageType))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            viewModel.loadHtmlContent(helpPageType)
                            webViewError = null
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = htmlContentState,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "html_content"
            ) { state ->
                when (state) {
                    is UiState.Loading -> {
                        LoadingContent()
                    }
                    is UiState.Success -> {
                        WebViewContent(
                            htmlContent = state.data,
                            pageType = helpPageType,
                            onLoadingChange = { isWebViewLoading = it },
                            onError = { webViewError = it }
                        )
                    }
                    is UiState.Error -> {
                        ErrorContent(
                            error = state.message,
                            onRetry = { 
                                viewModel.loadHtmlContent(helpPageType)
                                webViewError = null
                            }
                        )
                    }
                }
            }
            
            // Show loading indicator for WebView
            AnimatedVisibility(
                visible = isWebViewLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Loading content...")
                    }
                }
            }
            
            // Show WebView error if any
            webViewError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "WebView Error",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = { webViewError = null }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading content...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "Failed to Load Content",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun WebViewContent(
    htmlContent: String,
    @Suppress("UNUSED_PARAMETER") pageType: HelpPageType, // Reserved for future page-specific configurations
    onLoadingChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val webViewContext = LocalContext.current
    
    AndroidView(
        factory = { _ ->
            WebView(webViewContext).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChange(true)
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChange(false)
                    }
                    
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        onLoadingChange(false)
                        error?.description?.toString()?.let { errorMsg ->
                            onError(errorMsg)
                        }
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = false // Security: disable JS for static content
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
            }
        },
        update = { webView ->
            // Create a complete HTML document with proper styling
            val styledHtml = createStyledHtml(htmlContent, webViewContext)
            webView.loadDataWithBaseURL(
                null,
                styledHtml,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun createStyledHtml(content: String, @Suppress("UNUSED_PARAMETER") context: android.content.Context): String {
    // Note: Context parameter reserved for future dynamic theme detection
    val isDarkTheme = android.content.res.Configuration().uiMode and 
        android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    
    val backgroundColor = if (isDarkTheme) "#121212" else "#FFFFFF"
    val textColor = if (isDarkTheme) "#E0E0E0" else "#212121"
    val linkColor = if (isDarkTheme) "#BB86FC" else "#6200EE"
    
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 20px;
                    background-color: $backgroundColor;
                    color: $textColor;
                    font-size: 16px;
                }
                
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 24px;
                    margin-bottom: 16px;
                    font-weight: 600;
                }
                
                h1 { font-size: 28px; }
                h2 { font-size: 24px; }
                h3 { font-size: 20px; }
                
                p {
                    margin: 16px 0;
                    text-align: justify;
                }
                
                a {
                    color: $linkColor;
                    text-decoration: none;
                }
                
                a:hover {
                    text-decoration: underline;
                }
                
                ul, ol {
                    margin: 16px 0;
                    padding-left: 24px;
                }
                
                li {
                    margin: 8px 0;
                }
                
                blockquote {
                    border-left: 4px solid $linkColor;
                    margin: 16px 0;
                    padding: 8px 16px;
                    background-color: ${if (isDarkTheme) "#1E1E1E" else "#F5F5F5"};
                }
                
                code {
                    background-color: ${if (isDarkTheme) "#2D2D2D" else "#F0F0F0"};
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: 'Courier New', monospace;
                }
                
                .section {
                    margin: 24px 0;
                }
                
                .highlight {
                    background-color: ${if (isDarkTheme) "#3D3D00" else "#FFFBCD"};
                    padding: 2px 4px;
                    border-radius: 2px;
                }
            </style>
        </head>
        <body>
            $content
        </body>
        </html>
    """.trimIndent()
}

private fun getPageTitle(pageType: HelpPageType): String {
    return when (pageType) {
        HelpPageType.ABOUT -> "About Us"
        HelpPageType.PRIVACY_POLICY -> "Privacy Policy"
        HelpPageType.TERMS_OF_SERVICE -> "Terms of Service"
    }
}
