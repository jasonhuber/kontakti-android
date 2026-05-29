package com.kontakti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.kontakti.ui.auth.AuthState
import com.kontakti.ui.auth.AuthViewModel
import com.kontakti.ui.auth.LoginScreen
import com.kontakti.ui.auth.OnboardingScreen
import com.kontakti.ui.screens.GmailImportScreen
import com.kontakti.ui.screens.ImportContactsScreen
import com.kontakti.ui.screens.LinkedInImportScreen
import com.kontakti.ui.screens.companies.AddCompanyScreen
import com.kontakti.ui.screens.companies.CompaniesListScreen
import com.kontakti.ui.screens.companies.CompanyDetailScreen
import com.kontakti.ui.screens.discussions.DiscussionDetailScreen
import com.kontakti.ui.screens.discussions.DiscussionsListScreen
import com.kontakti.ui.screens.discussions.LogDiscussionScreen
import com.kontakti.ui.screens.duplicates.DuplicatesScreen
import com.kontakti.ui.screens.feed.FeedScreen
import com.kontakti.ui.screens.groups.GroupImportWizard
import com.kontakti.ui.screens.groups.SocialGroupsScreen
import com.kontakti.ui.screens.people.AddPersonScreen
import com.kontakti.ui.screens.people.PeopleListScreen
import com.kontakti.ui.screens.people.PersonDetailScreen
import com.kontakti.ui.screens.people.PersonEditScreen
import com.kontakti.ui.screens.quiz.QuizSessionScreen
import com.kontakti.ui.screens.search.NaturalSearchScreen
import com.kontakti.ui.screens.settings.SettingsScreen
import com.kontakti.ui.screens.today.TodayScreen
import com.kontakti.ui.screens.voice.VoiceRecorderScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { KontaktiAppRoot() }
            }
        }
    }
}

// ── Routes ───────────────────────────────────────────────────────────────────

object Routes {
    const val TODAY = "today"
    const val PEOPLE = "people"
    const val COMPANIES = "companies"
    const val DISCUSSIONS = "discussions"
    const val FEED = "feed"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val PERSON_DETAIL = "person/{personId}"
    const val PERSON_EDIT = "person/{personId}/edit"
    const val PERSON_ADD = "person/add"

    const val COMPANY_DETAIL = "company/{companyId}"
    const val COMPANY_ADD = "company/add"
    const val DISCUSSION_DETAIL = "discussion/{discussionId}"
    const val DISCUSSION_LOG = "discussion/log"

    const val GROUPS = "groups"
    const val GROUPS_IMPORT = "groups/import"
    const val DUPLICATES = "duplicates"

    const val QUIZ = "quiz"

    const val VOICE = "voice"
    const val VOICE_FOR = "voice?personId={personId}"

    const val IMPORT_PHONE = "import/phone"
    const val IMPORT_GMAIL = "import/gmail"
    const val IMPORT_LINKEDIN = "import/linkedin"

    fun personDetail(id: String) = "person/$id"
    fun personEdit(id: String) = "person/$id/edit"
    fun companyDetail(id: String) = "company/$id"
    fun discussionDetail(id: String) = "discussion/$id"
    fun voiceFor(id: String?) = if (id == null) VOICE else "voice?personId=$id"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.TODAY, "Today", Icons.Default.Today),
    BottomItem(Routes.PEOPLE, "People", Icons.Default.Group),
    BottomItem(Routes.COMPANIES, "Companies", Icons.Default.Business),
    BottomItem(Routes.DISCUSSIONS, "Discussions", Icons.AutoMirrored.Filled.Chat),
    BottomItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KontaktiAppRoot(authVm: AuthViewModel = hiltViewModel()) {
    val authState by authVm.state.collectAsState()
    when (authState) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.SignedOut -> LoginScreen(vm = authVm)
        is AuthState.NeedsOnboarding -> OnboardingScreen(onComplete = { authVm.completeOnboarding() })
        is AuthState.SignedIn -> MainNavigation(onSignedOut = { authVm.logout() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavigation(onSignedOut: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in bottomItems.map { it.route }
    val showVoiceFab = showBottomBar // visible on main tabs

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                nav.navigate(item.route) {
                                    launchSingleTop = true
                                    popUpTo(Routes.TODAY) { saveState = true }
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    item.label,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showVoiceFab) {
                FloatingActionButton(onClick = { nav.navigate(Routes.VOICE) }) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice memo")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(onOpenPerson = { nav.navigate(Routes.personDetail(it)) })
            }
            composable(Routes.PEOPLE) {
                PeopleListScreen(
                    onOpenPerson = { nav.navigate(Routes.personDetail(it)) },
                    onAddPerson = { nav.navigate(Routes.PERSON_ADD) }
                )
            }
            composable(Routes.SEARCH) {
                NaturalSearchScreen(onOpenPerson = { nav.navigate(Routes.personDetail(it)) })
            }
            composable(Routes.COMPANIES) {
                CompaniesListScreen(
                    onOpenCompany = { nav.navigate(Routes.companyDetail(it)) },
                    onAddCompany = { nav.navigate(Routes.COMPANY_ADD) }
                )
            }
            composable(
                Routes.COMPANY_DETAIL,
                arguments = listOf(navArgument("companyId") { type = NavType.StringType })
            ) {
                CompanyDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPerson = { nav.navigate(Routes.personDetail(it)) }
                )
            }
            composable(Routes.COMPANY_ADD) {
                AddCompanyScreen(
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.companyDetail(id))
                    }
                )
            }
            composable(Routes.DISCUSSIONS) {
                DiscussionsListScreen(
                    onOpenDiscussion = { nav.navigate(Routes.discussionDetail(it)) },
                    onLogDiscussion = { nav.navigate(Routes.DISCUSSION_LOG) }
                )
            }
            composable(
                Routes.DISCUSSION_DETAIL,
                arguments = listOf(navArgument("discussionId") { type = NavType.StringType })
            ) {
                DiscussionDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPerson = { nav.navigate(Routes.personDetail(it)) }
                )
            }
            composable(Routes.DISCUSSION_LOG) {
                LogDiscussionScreen(
                    onBack = { nav.popBackStack() },
                    onCreated = { nav.popBackStack() }
                )
            }
            composable(Routes.FEED) {
                FeedScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenGroups = { nav.navigate(Routes.GROUPS) },
                    onOpenDuplicates = { nav.navigate(Routes.DUPLICATES) },
                    onOpenImport = { nav.navigate(Routes.IMPORT_PHONE) },
                    onLinkGoogle = { /* TODO: launch Google sign-in for new account */ },
                    onSignedOut = onSignedOut,
                    onOpenActivity = { nav.navigate(Routes.FEED) }
                )
            }

            composable(Routes.PERSON_ADD) {
                AddPersonScreen(
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.personDetail(id))
                    }
                )
            }
            composable(
                Routes.PERSON_DETAIL,
                arguments = listOf(navArgument("personId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("personId").orEmpty()
                PersonDetailScreen(
                    personId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.personEdit(it)) }
                )
            }
            composable(
                Routes.PERSON_EDIT,
                arguments = listOf(navArgument("personId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("personId").orEmpty()
                PersonEditScreen(personId = id, onBack = { nav.popBackStack() })
            }

            composable(Routes.GROUPS) {
                SocialGroupsScreen(
                    onBack = { nav.popBackStack() },
                    onImport = { nav.navigate(Routes.GROUPS_IMPORT) }
                )
            }
            composable(Routes.GROUPS_IMPORT) {
                GroupImportWizard(onBack = { nav.popBackStack() })
            }
            composable(Routes.DUPLICATES) {
                DuplicatesScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.QUIZ) {
                QuizSessionScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.VOICE) {
                VoiceRecorderScreen(onBack = { nav.popBackStack() }, personId = null)
            }
            composable(
                Routes.VOICE_FOR,
                arguments = listOf(navArgument("personId") {
                    type = NavType.StringType
                    nullable = true
                })
            ) { entry ->
                VoiceRecorderScreen(onBack = { nav.popBackStack() }, personId = entry.arguments?.getString("personId"))
            }

            composable(Routes.IMPORT_PHONE) { ImportContactsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.IMPORT_GMAIL) { GmailImportScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.IMPORT_LINKEDIN) { LinkedInImportScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
