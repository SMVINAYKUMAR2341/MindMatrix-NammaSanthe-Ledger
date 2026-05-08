package com.nammasanthe.ledger

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nammasanthe.ledger.data.entity.TxnEntity
import com.nammasanthe.ledger.sync.FirebaseAuthManager
import com.nammasanthe.ledger.ui.nav.Routes
import com.nammasanthe.ledger.ui.screens.*
import com.nammasanthe.ledger.ui.theme.NammaSantheTheme
import com.nammasanthe.ledger.util.LocaleManager
import com.nammasanthe.ledger.util.PhotoProofManager
import com.nammasanthe.ledger.viewmodel.ConfirmationViewModel
import com.nammasanthe.ledger.viewmodel.LedgerViewModel
import com.nammasanthe.ledger.viewmodel.LedgerViewModelFactory
import com.nammasanthe.ledger.viewmodel.OcrViewModel
import com.nammasanthe.ledger.viewmodel.ProfileViewModel
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// DataStore for onboarding preference
private val Context.dataStore by preferencesDataStore(name = "app_prefs")
private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lang = runCatching {
            runBlocking {
                NammaSantheApp.instance.profileStore.profile.first().language
            }
        }.getOrDefault("en")
        super.attachBaseContext(LocaleManager.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NammaSantheTheme { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val factory = LedgerViewModelFactory()
    val ledger: LedgerViewModel = viewModel(factory = factory)
    val ocr: OcrViewModel = viewModel(factory = factory)
    val profileVm: ProfileViewModel = viewModel(factory = factory)
    val confirmVm: ConfirmationViewModel = viewModel(factory = factory)

    val authManager = remember { FirebaseAuthManager.getInstance(context) }

    // Read onboarding completed state
    val onboardingCompleted by context.dataStore.data
        .map { it[ONBOARDING_COMPLETED] ?: false }
        .collectAsState(initial = false)

    // Check if user is signed in
    val isSignedIn by remember { mutableStateOf(authManager.isSignedIn()) }

    // PIN lock state
    val profile by profileVm.profile.collectAsState()
    var unlocked by remember { mutableStateOf(false) }
    val pinHash = profile.pinHash

    // Determine start destination
    val startDestination = when {
        !onboardingCompleted -> Routes.Onboarding
        !isSignedIn -> Routes.Login
        else -> Routes.Home
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = current in listOf(Routes.Home, Routes.Customers, Routes.Reports, Routes.Scanner)

    // Show PIN gate if needed (but only after auth flow)
    if (isSignedIn && !pinHash.isNullOrEmpty() && !unlocked) {
        PinGateScreen(expectedHash = pinHash, onUnlock = { unlocked = true })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current == Routes.Home,
                        onClick = { nav.navigate(Routes.Home) { popUpTo(Routes.Home) { inclusive = true } } },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = current == Routes.Customers,
                        onClick = { nav.navigate(Routes.Customers) },
                        icon = { Icon(Icons.Default.Group, null) },
                        label = { Text("Customers") }
                    )
                    NavigationBarItem(
                        selected = current == Routes.Scanner,
                        onClick = { nav.navigate(Routes.Scanner) },
                        icon = { Icon(Icons.Default.DocumentScanner, null) },
                        label = { Text("Scan") }
                    )
                    NavigationBarItem(
                        selected = current == Routes.Reports,
                        onClick = { nav.navigate(Routes.Reports) },
                        icon = { Icon(Icons.Default.QueryStats, null) },
                        label = { Text("Reports") }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = nav, startDestination = startDestination) {
                // ── Onboarding ───────────────────────────────────────────────────────────
                composable(Routes.Onboarding) {
                    OnboardingScreen(
                        onFinish = {
                            // Mark onboarding as completed
                            runBlocking {
                                context.dataStore.edit { prefs ->
                                    prefs[ONBOARDING_COMPLETED] = true
                                }
                            }
                            nav.navigate(Routes.Login) {
                                popUpTo(Routes.Onboarding) { inclusive = true }
                            }
                        },
                        onSkip = {
                            // Skip to login
                            runBlocking {
                                context.dataStore.edit { prefs ->
                                    prefs[ONBOARDING_COMPLETED] = true
                                }
                            }
                            nav.navigate(Routes.Login) {
                                popUpTo(Routes.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Auth ─────────────────────────────────────────────────────────────────
                composable(Routes.Login) {
                    LoginScreen(
                        onLoginSuccess = {
                            nav.navigate(Routes.Home) {
                                popUpTo(Routes.Login) { inclusive = true }
                            }
                        },
                        onNavigateToSignup = {
                            nav.navigate(Routes.Signup)
                        },
                        onSkip = {
                            // Continue without account - go to home
                            nav.navigate(Routes.Home) {
                                popUpTo(Routes.Login) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.Signup) {
                    SignupScreen(
                        onSignupSuccess = {
                            nav.navigate(Routes.Home) {
                                popUpTo(Routes.Signup) { inclusive = true }
                                popUpTo(Routes.Login) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            nav.popBackStack()
                        },
                        onBack = {
                            nav.popBackStack()
                        }
                    )
                }

                // ── Main App ─────────────────────────────────────────────────────────────
                composable(Routes.Home) {
                    HomeScreen(
                        viewModel = ledger,
                        profileViewModel = profileVm,
                        onQuickEntry = { nav.navigate(Routes.QuickEntry) },
                        onCustomers = { nav.navigate(Routes.Customers) },
                        onReports = { nav.navigate(Routes.Reports) },
                        onScanner = { nav.navigate(Routes.Scanner) },
                        onProfile = { nav.navigate(Routes.Profile) },
                        onCustomer = { nav.navigate(Routes.customer(it)) }
                    )
                }
                composable(Routes.Customers) {
                    CustomersScreen(viewModel = ledger, onCustomer = { nav.navigate(Routes.customer(it)) })
                }
                composable(Routes.Customer) { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    CustomerLedgerScreen(
                        viewModel = ledger,
                        profileViewModel = profileVm,
                        confirmationVm = confirmVm,
                        customerId = id,
                        onBack = { nav.popBackStack() },
                        onShowQr = { txnId -> nav.navigate(Routes.qrDisplay(txnId)) },
                        onTransactionClick = { txnId -> nav.navigate(Routes.transactionDetail(txnId)) }
                    )
                }
                composable(Routes.QuickEntry) {
                    QuickEntryScreen(ledger, onBack = { nav.popBackStack() })
                }
                composable(Routes.Reports) { ReportsScreen(ledger, onBack = { nav.popBackStack() }) }
                composable(Routes.Scanner) {
                    ScannerScreen(
                        ocrViewModel = ocr,
                        onBack = { nav.popBackStack() },
                        onResult = { nav.navigate(Routes.ScanResult) }
                    )
                }
                composable(Routes.ScanResult) {
                    ScanResultScreen(
                        ocrViewModel = ocr,
                        onBack = { nav.popBackStack(Routes.Home, false) },
                        onRetry = { nav.popBackStack() }
                    )
                }
                composable(Routes.Profile) {
                    ProfileScreen(
                        viewModel = profileVm,
                        ledgerViewModel = ledger,
                        onBack = { nav.popBackStack() },
                        onGeminiSettings = { nav.navigate(Routes.GeminiSettings) }
                    )
                }
                // ── QR Confirmation ─────────────────────────────────────────────────────
                composable(Routes.QrDisplay) { entry ->
                    val txnId = entry.arguments?.getString("txnId")?.toLongOrNull()
                        ?: return@composable
                    QrDisplayScreen(
                        txnId = txnId,
                        confirmationVm = confirmVm,
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(Routes.QrScanConfirm) {
                    QrScannerConfirmScreen(
                        confirmationVm = confirmVm,
                        onBack = { nav.popBackStack() },
                        onDone = { nav.popBackStack(Routes.Customer, false) }
                    )
                }
                // ── Settings ─────────────────────────────────────────────────────────────
                composable(Routes.GeminiSettings) {
                    GeminiSettingsScreen(
                        onBack = { nav.popBackStack() }
                    )
                }
                // ── Transaction Detail ─────────────────────────────────────────────────────
                composable(Routes.TransactionDetail) { entry: NavBackStackEntry ->
                    val txnId: Long = entry.arguments?.getString("txnId")?.toLongOrNull() ?: 0L
                    if (txnId == 0L) return@composable

                    val scope = rememberCoroutineScope()
                    var transaction: TxnEntity? by remember { mutableStateOf(null) }
                    var customerName: String by remember { mutableStateOf("") }

                    LaunchedEffect(key1 = txnId) {
                        transaction = ledger.getTransactionById(txnId)
                        transaction?.let { txn: TxnEntity ->
                            val customer = ledger.getCustomer(txn.customerId)
                            customerName = customer?.name ?: "Unknown"
                        }
                    }

                    val txn = transaction
                    if (txn != null) {
                        TransactionDetailScreen(
                            transaction = txn,
                            customerName = customerName,
                            confirmationVm = confirmVm,
                            onBack = { nav.popBackStack() },
                            onAddPhoto = { id: Long, uri: Uri ->
                                scope.launch {
                                    val photoPath = PhotoProofManager.saveTransactionPhoto(
                                        context,
                                        uri,
                                        id
                                    )
                                    photoPath?.let { path: String ->
                                        ledger.updateTransactionPhoto(id, path)
                                        transaction = ledger.getTransactionById(txnId)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
