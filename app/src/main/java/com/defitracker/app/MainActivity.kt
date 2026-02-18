package com.defitracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.defitracker.app.presentation.crypto_detail.CryptoDetailScreen
import com.defitracker.app.presentation.crypto_list.CryptoListScreen
import androidx.compose.ui.res.painterResource
import com.defitracker.app.presentation.wallet.WalletScreen
import com.defitracker.app.presentation.transactions.TransactionsScreen
import com.defitracker.app.ui.theme.DeFiTrackerTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Pairs : Screen("crypto_list", "Pairs", Icons.Default.List)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.Wallet)
    object Transactions : Screen("transactions", "History", Icons.Default.History)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            DeFiTrackerTheme {
                val navController = rememberNavController()
                val screens = listOf(Screen.Pairs, Screen.Wallet, Screen.Transactions)
                
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DeFi Tracker",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Black
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            tonalElevation = 0.dp
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            screens.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0ECB81),
                                        selectedTextColor = Color(0xFF0ECB81),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Navigation(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun Navigation(navController: androidx.navigation.NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Pairs.route
    ) {
        composable(Screen.Pairs.route) {
            CryptoListScreen(
                onNavigateToDetail = { symbol, source ->
                    navController.navigate("crypto_detail/$symbol/$source")
                }
            )
        }
        composable(Screen.Wallet.route) {
            WalletScreen()
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen()
        }
        composable(
            route = "crypto_detail/{symbol}/{source}",
            arguments = listOf(
                navArgument("symbol") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType }
            )
        ) {
            CryptoDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
