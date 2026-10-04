package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.example.ui.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.about.AboutAppScreen
import com.example.ui.screens.accounts.AccountDetailScreen
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.bookmarks.BookmarksScreen
import com.example.ui.screens.budget.BudgetScreen
import com.example.ui.screens.categories.CategoryManagerScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.memos.DailyMemosScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.pcmanager.PCManagerScreen
import com.example.ui.screens.recurring.RecurringInstallmentsScreen
import com.example.ui.screens.search.SearchFilterScreen
import com.example.ui.screens.security.PinLockScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.transaction.AddEditTransactionScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Transactions", Icons.Default.ReceiptLong)
    object Stats : Screen("stats", "Stats", Icons.Default.PieChart)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalanceWallet)
    object More : Screen("more", "More", Icons.Default.Settings)
}

val allBottomNavItems = listOf(
    Screen.Home,
    Screen.Stats,
    Screen.Accounts,
    Screen.More
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MainAppNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.notices.collect { notice ->
            val result = snackbar.showSnackbar(notice.message, notice.actionLabel, withDismissAction = true)
            if (result == SnackbarResult.ActionPerformed) notice.onAction?.invoke()
        }
    }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val storedPin by viewModel.storedPin.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val showAccountsTab by viewModel.showAccountsTab.collectAsStateWithLifecycle()

    val currentNavItems = remember(showAccountsTab) {
        if (showAccountsTab) {
            listOf(Screen.Home, Screen.Stats, Screen.Accounts, Screen.More)
        } else {
            listOf(Screen.Home, Screen.Stats, Screen.More)
        }
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            viewModel = viewModel,
            isEditModeFromSettings = false,
            onFinishOnboarding = { /* Flow updates state */ }
        )
        return
    }

    if (isPinEnabled && !storedPin.isNullOrBlank() && !isAppUnlocked) {
        PinLockScreen(
            correctPin = storedPin!!,
            onUnlocked = { viewModel.unlockApp() }
        )
        return
    }

    val readErrors by viewModel.readErrors.collectAsStateWithLifecycle()
    val currentRoute = currentDestination?.route ?: Screen.Home.route

    LaunchedEffect(showAccountsTab, currentRoute) {
        if (!showAccountsTab && currentRoute == Screen.Accounts.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    val isBottomBarVisible = currentNavItems.any { it.route == currentRoute } || currentRoute == "about"

    com.example.ui.components.FullscreenChartHost(screenKey = currentRoute) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    currentNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route || (currentRoute == "about" && screen.route == Screen.More.route)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    maxLines = 1
                                )
                            },
                            selected = selected,
                            modifier = Modifier.testTag("nav_tab_${screen.route}"),
                            onClick = {
                                if (currentRoute == "about" && screen.route == Screen.More.route) {
                                    navController.popBackStack()
                                } else if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (it * 0.25f).toInt() },
                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -(it * 0.15f).toInt() },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -(it * 0.15f).toInt() },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (it * 0.25f).toInt() },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) {
            // Main Bottom Bar tabs - smooth subtle scale crossfade
            composable(
                route = Screen.Home.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
                }
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { navController.navigate("add_transaction/0") },
                    onNavigateToEditTransaction = { id -> navController.navigate("add_transaction/$id") },
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToMemos = { navController.navigate("memos") }
                )
            }

            composable(
                route = Screen.Stats.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
                }
            ) {
                StatsScreen(
                    viewModel = viewModel,
                    onNavigateToTransaction = { id -> navController.navigate("add_transaction/$id") }
                )
            }

            composable(
                route = Screen.Accounts.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
                }
            ) {
                AccountsScreen(
                    viewModel = viewModel,
                    onNavigateToAccountDetail = { id -> navController.navigate("account_detail/$id") }
                )
            }

            composable(
                route = "budget",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { (it * 0.25f).toInt() },
                        animationSpec = tween(340, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(280))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -(it * 0.15f).toInt() },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(220))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -(it * 0.15f).toInt() },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { (it * 0.25f).toInt() },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(220))
                }
            ) {
                BudgetScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.More.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
                }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToCategoryManager = { navController.navigate("categories") },
                    onNavigateToBudget = { navController.navigate("budget") },
                    onNavigateToOnboarding = { navController.navigate("onboarding") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToPCManager = { navController.navigate("pc_manager") }
                )
            }

            // Sub screens
            composable(
                route = "add_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = 0L }),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { (it * 0.95f).toInt() },
                        animationSpec = tween(360, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { (it * 0.95f).toInt() },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(240))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(260))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { (it * 0.95f).toInt() },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(240))
                }
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                AddEditTransactionScreen(
                    viewModel = viewModel,
                    transactionId = txId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "account_detail/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accId = backStackEntry.arguments?.getLong("accountId") ?: 1L
                AccountDetailScreen(
                    viewModel = viewModel,
                    accountId = accId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditTransaction = { id -> navController.navigate("add_transaction/$id") }
                )
            }

            composable("search") {
                SearchFilterScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditTransaction = { id -> navController.navigate("add_transaction/$id") }
                )
            }

            composable("categories") {
                CategoryManagerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("recurring") {
                RecurringInstallmentsScreen(
                    viewModel = viewModel
                )
            }

            composable("bookmarks") {
                BookmarksScreen(
                    viewModel = viewModel,
                    onUseBookmark = { bm ->
                        navController.navigate("add_transaction/0")
                    }
                )
            }

            composable("memos") {
                DailyMemosScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("onboarding") {
                OnboardingScreen(
                    viewModel = viewModel,
                    isEditModeFromSettings = true,
                    onFinishOnboarding = { navController.popBackStack() }
                )
            }

            composable(
                route = "about",
                enterTransition = { fadeIn(animationSpec = tween(180)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) },
                popEnterTransition = { fadeIn(animationSpec = tween(180)) },
                popExitTransition = { fadeOut(animationSpec = tween(150)) }
            ) {
                AboutAppScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "pc_manager",
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) },
                popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                popExitTransition = { fadeOut(animationSpec = tween(150)) }
            ) {
                PCManagerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        if (readErrors.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Could not load records") },
                text = { Text(readErrors.values.first()) },
                confirmButton = { TextButton(onClick = viewModel::retryReads) { Text("Retry") } }
            )
        }
    }
    }
}
