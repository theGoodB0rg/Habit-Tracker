package com.habittracker.legal.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.habittracker.ui.navigation.Screen

/**
 * Modern About screen with comprehensive app information
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: LegalViewModel = hiltViewModel()
) {
    val appVersionState by viewModel.appVersionState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAppVersionInfo()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Icon and Name Header
            item {
                AppHeaderSection()
            }
            
            // Version Information Card
            item {
                AnimatedVersionCard(appVersionState)
            }
            
            // Developer Information Card
            item {
                DeveloperInfoCard(
                    developerInfo = viewModel.developerInfo,
                    onEmailClick = { viewModel.sendFeedback() },
                    onWebsiteClick = { url -> viewModel.openUrl(url) }
                )
            }
            
            // Action Buttons
            item {
                ActionButtonsSection(
                    onRateAppClick = { viewModel.rateApp() },
                    onFeedbackClick = { viewModel.sendFeedback() },
                    onPrivacyPolicyClick = { 
                        navController.navigate(Screen.HelpWebView.createRoute("privacy_policy")) 
                    },
                    onTermsClick = { 
                        navController.navigate(Screen.HelpWebView.createRoute("terms_of_service")) 
                    },
                    onTutorialClick = {
                        navController.navigate(Screen.Tutorial.route)
                    },
                    onTipsClick = {
                        navController.navigate(Screen.Tips.route)
                    }
                )
            }
            
            // Additional Information
            item {
                AdditionalInfoSection()
            }
        }
    }
}

@Composable
private fun AppHeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ),
                    shape = CircleShape
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "App Icon",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Habit Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Build better habits, one day at a time",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVersionCard(versionState: UiState<com.habittracker.legal.domain.AppVersionInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Version Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedContent(
                targetState = versionState,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "version_content"
            ) { state ->
                when (state) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    is UiState.Success -> {
                        VersionInfoContent(state.data)
                    }
                    is UiState.Error -> {
                        Text(
                            text = "Failed to load version information",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionInfoContent(versionInfo: com.habittracker.legal.domain.AppVersionInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VersionInfoRow("Version", "${versionInfo.versionName} (${versionInfo.versionCode})")
        VersionInfoRow("Build Type", versionInfo.buildType)
        VersionInfoRow("Target SDK", versionInfo.targetSdk.toString())
        VersionInfoRow("Minimum SDK", versionInfo.minSdk.toString())
        VersionInfoRow("Build Date", versionInfo.buildDate)
    }
}

@Composable
private fun VersionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeveloperInfoCard(
    developerInfo: com.habittracker.legal.domain.DeveloperInfo,
    onEmailClick: () -> Unit,
    onWebsiteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = developerInfo.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Contact Information
            ContactInfoRow(
                icon = Icons.Default.Email,
                text = developerInfo.email,
                onClick = onEmailClick
            )
            
            developerInfo.website?.let { website ->
                ContactInfoRow(
                    icon = Icons.Default.Language,
                    text = website,
                    onClick = { onWebsiteClick(website) }
                )
            }
            
            developerInfo.githubProfile?.let { github ->
                ContactInfoRow(
                    icon = Icons.Default.Code,
                    text = "GitHub Profile",
                    onClick = { onWebsiteClick(github) }
                )
            }
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ActionButtonsSection(
    onRateAppClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onTutorialClick: () -> Unit,
    onTipsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Star,
                    text = "Rate App",
                    onClick = onRateAppClick,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Feedback,
                    text = "Feedback",
                    onClick = onFeedbackClick,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.School,
                    text = "Tutorial",
                    onClick = onTutorialClick,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Lightbulb,
                    text = "Tips",
                    onClick = onTipsClick,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Legal Links
            LegalLinkRow("Privacy Policy", onPrivacyPolicyClick)
            LegalLinkRow("Terms of Service", onTermsClick)
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LegalLinkRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdditionalInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "About This App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Habit Tracker is a privacy-first, fully offline habit tracking application designed to help you build and maintain positive habits.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                FeatureRow(Icons.Default.Lock, "Complete privacy - all data stays on your device")
                FeatureRow(Icons.Default.Smartphone, "Beautiful, modern interface")
                FeatureRow(Icons.Default.TrackChanges, "Smart streak tracking and motivation") // Using TrackChanges as 'Target' proxy
                FeatureRow(Icons.Default.DarkMode, "Dark and light themes") // Using DarkMode as Moon proxy
                FeatureRow(Icons.Default.Notifications, "Customizable reminders")
                FeatureRow(Icons.Default.Analytics, "Progress analytics")
                FeatureRow(Icons.Default.ExitToApp, "Export your data")
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
