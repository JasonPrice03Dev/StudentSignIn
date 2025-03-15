package com.mad.studentsignin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.mad.studentsignin.ui.AttendanceViewingPage
import com.mad.studentsignin.ui.CaptureFacePage
import com.mad.studentsignin.ui.FacialRecognitionPage
import com.mad.studentsignin.ui.HubForLecturer
import com.mad.studentsignin.ui.HubPage
import com.mad.studentsignin.ui.QRCodePage
import com.mad.studentsignin.ui.QRCodeScanner
import com.mad.studentsignin.ui.StartPageViewModel
import com.mad.studentsignin.ui.StartPage
import com.mad.studentsignin.ui.StatisticsPage
import com.mad.studentsignin.ui.theme.StudentSignInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase on app start
        FirebaseApp.initializeApp(this)

        super.onCreate(savedInstanceState)

        setContent {
            // Creating a NavController for navigation between screens
            val navController = rememberNavController()

            StudentSignInTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(navController)
                }
            }
        }
    }
}

// Creating the NavGraph composable to move from screen to screen
@Composable
fun NavGraph(navController: NavHostController) {
    // Retrieving the ViewModel which can be passed through screens
    val viewModel: StartPageViewModel = viewModel()

    // Set up the navigation with a start destination of StartPage
    NavHost(navController, startDestination = "startPage") {
        composable("startPage") {
            StartPage(navController, viewModel)
        }
        composable("hubPage/{course}") { backStackEntry ->
            val course = backStackEntry.arguments?.getString("course") ?: ""
            HubPage(navController, viewModel, course)
        }
        composable("hubForLecturer") {
            HubForLecturer(navController, viewModel)
        }
        composable("captureFacePage") {
            CaptureFacePage(navController, viewModel)
        }
        composable("facialRecognitionPage/{course}/{scannedData}/{classId}") { backStackEntry ->
            val scannedData = backStackEntry.arguments?.getString("scannedData") ?: ""
            val course = backStackEntry.arguments?.getString("course") ?: ""
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            FacialRecognitionPage(navController, viewModel, course, scannedData, classId)
        }
        composable("qrCodePage/{uniqueIdentifier}") { backStackEntry ->
            val uniqueIdentifier = backStackEntry.arguments?.getString("uniqueIdentifier") ?: ""
            QRCodePage(uniqueIdentifier = uniqueIdentifier, viewModel = viewModel, navController)
        }
        composable("qrCodeScanner/{course}/{currentClassId}") { backStackEntry ->
            val course = backStackEntry.arguments?.getString("course") ?: ""
            val currentClassId = backStackEntry.arguments?.getString("currentClassId") ?: ""
            QRCodeScanner(navController, course, currentClassId, viewModel = viewModel)
        }
        composable("attendanceViewingPage/{id}/{course}/{classId}") { backStackEntry ->
            val course = backStackEntry.arguments?.getString("course") ?: ""
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            AttendanceViewingPage(
                navController, course = course, classId = classId, viewModel = viewModel
            )
        }
        composable(
            route = "statisticsPage/{studentId}/{email}/{course}/{moduleName}", arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType },
                navArgument("course") { type = NavType.StringType },
                navArgument("moduleName") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val course = backStackEntry.arguments?.getString("course") ?: ""
            val moduleName = backStackEntry.arguments?.getString("moduleName") ?: ""

            StatisticsPage(
                studentId = studentId,
                email = email,
                moduleName = moduleName,
                course = course,
                viewModel = viewModel
            )
        }
    }
}
