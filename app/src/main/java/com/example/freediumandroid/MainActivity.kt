package com.example.freediumandroid

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.freediumandroid.ui.theme.FreediumAndroidTheme
import com.google.accompanist.flowlayout.FlowRow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI
import java.net.URLDecoder


val Context.dataStore by preferencesDataStore("saved_articles")

//data class ArticleInfo(
//    val url: String,
//    val title: String,
//    val author: String,
//    val timestamp: Long = System.currentTimeMillis()
//)

object SavedArticlesStore {
    private val SAVED_ARTICLES_KEY = stringPreferencesKey("saved_articles_list")
    private val gson = Gson()

    fun getSavedArticles(context: Context): Flow<List<ArticleInfo>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[SAVED_ARTICLES_KEY]
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<ArticleInfo>>() {}.type
                try {
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun saveArticle(context: Context, article: ArticleInfo) {
        context.dataStore.edit { preferences ->
            val current = preferences[SAVED_ARTICLES_KEY]?.let {
                val type = object : TypeToken<MutableList<ArticleInfo>>() {}.type
                gson.fromJson<MutableList<ArticleInfo>>(it, type)
            } ?: mutableListOf()

            if (current.none { it.url == article.url }) {
                current.add(article)
                preferences[SAVED_ARTICLES_KEY] = gson.toJson(current)
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val deepLinkUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkUrl.value = intent?.data?.toString()

        setContent {
            FreediumAndroidTheme {
                val navController = rememberNavController()

                // Handle deep links
                LaunchedEffect(deepLinkUrl.value) {
                    deepLinkUrl.value?.let { Log.d("PRINT", it) }
                    deepLinkUrl.value?.let {
                        val relativeUrl = it.removePrefix("https://medium.com/")
                        navController.navigate(Screen.Article.createRoute(relativeUrl))
                        deepLinkUrl.value = null // prevent repeat nav
                    }
                }

                MainApp(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        deepLinkUrl.value = intent?.data?.toString()
    }
}


// Navigation routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Article : Screen("article/{url}") {
        fun createRoute(url: String): String = "article/${Uri.encode(url)}"
    }
}

@Composable
fun MainApp(navController: NavHostController, modifier: Modifier = Modifier) {


    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(onChangeScreen = { url ->
                navController.navigate(Screen.Article.createRoute(url))
            })
        }

        composable(
            route = Screen.Article.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://medium.com/{url}/"
                    action = Intent.ACTION_VIEW
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
            ArticleScreen(
                decodedUrl,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    }
}


@Composable
fun SearchBar(
    text: MutableState<String>, isValid: MutableState<Boolean>
) {
    val mediumLinkRegex = remember {
        Regex("""^https?://([a-zA-Z0-9\-]+\.)*medium\.com/.+""")

    }

    isValid.value = mediumLinkRegex.matches(text.value.trim())

    OutlinedTextField(
        value = text.value,
        onValueChange = { newText ->
            text.value = newText
            isValid.value = mediumLinkRegex.matches(newText.trim())
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        isError = text.value.isNotEmpty() && !isValid.value,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search Icon"
            )
        },
        placeholder = { Text("Enter article link") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Medium Article URL") },
        supportingText = {
            if (text.value.isNotEmpty() && !isValid.value) {
                Text("Please enter a valid Medium article link.")
            }
        }
    )
}

data class ArticleInfo(
    val title: String,
    val mainImage: String,
    val authorName: String,
    val authorPfp: String,
    val publishDate: String,
    val subtitle: String,
    val url: String
)

@Composable
fun Modifier.verticalColumnScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    showScrollBarTrack: Boolean = false,
    scrollBarTrackColor: Color = MaterialTheme.colorScheme.background,
    scrollBarColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    scrollBarCornerRadius: Float = 4f,
    endPadding: Float = 12f
): Modifier {
    return drawWithContent {
        // Draw the column's content
        drawContent()
        // Dimensions and calculations
        val viewportHeight = this.size.height
        val totalContentHeight = scrollState.maxValue.toFloat() + viewportHeight
        val scrollValue = scrollState.value.toFloat()
        // Compute scrollbar height and position
        val scrollBarHeight =
            (viewportHeight / totalContentHeight) * viewportHeight
        val scrollBarStartOffset =
            (scrollValue / totalContentHeight) * viewportHeight
        // Draw the track (optional)
        if (showScrollBarTrack) {
            drawRoundRect(
                cornerRadius = CornerRadius(scrollBarCornerRadius),
                color = scrollBarTrackColor,
                topLeft = Offset(this.size.width - endPadding, 0f),
                size = Size(width.toPx(), viewportHeight),
            )
        }
        // Draw the scrollbar
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = scrollBarColor,
            topLeft = Offset(this.size.width - endPadding, scrollBarStartOffset),
            size = Size(width.toPx(), scrollBarHeight)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ArticleScreen(
    url: String, canNavigateBack: Boolean,
    navigateUp: () -> Unit = {}, modifier: Modifier = Modifier
) {
    var doc: Document
    var medUrl = remember { mutableStateOf("https://medium.com") }
    var authorUrl = remember { mutableStateOf("https://medium.com") }
    val state = rememberScrollState()
    var appBarVisible by remember { mutableStateOf(true) }
    var tagList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var previousScroll by remember { mutableStateOf(0) }
    var scrollDelta by remember { mutableStateOf(0f) }
    val animatedHeight by animateDpAsState(
        targetValue = if (appBarVisible) 90.dp else 0.dp,
        label = "AppBarAnimation"
    )
    val threshold = with(LocalDensity.current) { 20.dp.toPx() }
    var mainContent = remember { mutableStateOf("h1") }
    var loaded by remember { mutableStateOf(false) }
    var articleInfo =
        remember {
            mutableStateOf(
                ArticleInfo(
                    "DATA is uspposed to be here",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                )
            )
        }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val client = HttpClient(CIO)
        val response: HttpResponse = client.get("https://freedium.cfd/$url")
        Log.d("PRINT", response.status.value.toString())
        client.close()
        doc = Jsoup.parse(response.body())
        articleInfo.value =
            ArticleInfo(
                doc.select("h1")[0].text(),
                doc.select("img")[0].attr("src").toString(),
                doc.select("a")[11].text(),
                doc.select("a img")[0].attr("src").toString(),
                doc.select("div.flex span.text-gray-500")[1].text(),
                doc.select("h2")[0].text(),
                url,
            )
        medUrl.value = doc.select("a.text-green-500")[1].attr("href")
        authorUrl.value = doc.select("button a")[0].attr("href")
        mainContent.value = doc.select("div.main-content").html()
        loaded = true
        val tagDiv = doc.selectFirst("div.flex.flex-wrap.gap-2.mt-5")
        val newTagsList: List<Pair<String, String>> = if (tagDiv != null) {
            tagDiv.select("a[href]").map { a ->
                val href = a.attr("href")
                val text = a.text()
                href to text
            }
        } else {
            emptyList()
        }
        tagList = newTagsList
        SavedArticlesStore.saveArticle(context, articleInfo.value)
        snapshotFlow { state.value }
            .map { current ->
                val delta = current - previousScroll
                previousScroll = current
                delta
            }
            .collect { delta ->
                scrollDelta += delta
                if (scrollDelta > threshold) {
                    appBarVisible = false
                    scrollDelta = 0f
                } else if (scrollDelta < -threshold) {
                    appBarVisible = true
                    scrollDelta = 0f
                }
            }
    }
    Scaffold(
        topBar = {
            Box(Modifier.height(animatedHeight)) {
                TopAppBar(
                    title = { Text("") },
                    actions = {
                        Row {
                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, medUrl.value)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share via")
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.share)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = navigateUp) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_button)
                                )
                            }
                        }
                    }

                )
            }
        },

        content = { padding ->
            if (!loaded) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = 35.dp, vertical = 125.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()

                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalColumnScrollbar(state)
                        .verticalScroll(state)
                        .padding(horizontal = 20.dp, vertical = 125.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {

                    GlideImage(
                        model = articleInfo.value.mainImage,
                        contentDescription = "Main Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.FillWidth,

                        )

                    Column(Modifier.padding(top = 15.dp)) {
                        Text(
                            articleInfo.value.title,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            articleInfo.value.subtitle,
                            Modifier.padding(top = 20.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Box(
                        Modifier
                            .padding(top = 30.dp)
                            .height(72.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Box(
                                Modifier.padding(bottom = 17.dp)
                            ) {
                                GlideImage(
                                    model = articleInfo.value.authorPfp,
                                    contentDescription = articleInfo.value.authorName + "'s profile picture",
                                    modifier = Modifier
                                        .clip(CircleShape),

                                    )
                            }
                            Column(
                                Modifier
                                    .weight(0.9F)
                                    .padding(start = 15.dp)
                            ) {
                                Text(
                                    articleInfo.value.authorName,
                                )
                                Text(
                                    articleInfo.value.publishDate,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.weight(0.11F))
                            Box(Modifier.padding(bottom = 17.dp)) {
                                FilledTonalButton(onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(authorUrl.value)
                                    ).apply {
                                        addCategory(Intent.CATEGORY_BROWSABLE)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        setPackage(null) // Do NOT set a specific package
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Text("Follow")
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 15.dp)
                    ) {
                        HtmlText(mainContent.value)
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        mainAxisSpacing = 8.dp,
                        crossAxisSpacing = 8.dp
                    ) {
                        for ((url, label) in tagList) {
                            if (label.isNotEmpty()) {
                                AssistChip(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        })
}

val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val SHOW_WARNING_KEY = booleanPreferencesKey("show_medium_warning")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalGlideComposeApi::class
)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onChangeScreen: (url: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var text = remember { mutableStateOf("") }
    var validLink = remember { mutableStateOf(true) }

    val articleButtonHeight by animateDpAsState(
        if (text.value.isNotEmpty() && validLink.value) 96.dp else 0.dp
    )

    val savedArticles = remember { mutableStateListOf<ArticleInfo>() }
    val isMyAppPreferred = remember { mutableStateOf(true ) }
    var showWarningCard by remember { mutableStateOf(true) }

    // Load DataStore preference
    LaunchedEffect(Unit) {
        context.dataStore.data.collect { prefs ->
            showWarningCard = prefs[SHOW_WARNING_KEY] ?: true
        }
    }

    // Lifecycle observer to detect if app is preferred for medium.com
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://medium.com/@xxx/xxx"))
                val resolveInfoList = context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                val myPackageName = context.packageName
                isMyAppPreferred.value = resolveInfoList.any {
                    it.activityInfo.packageName == myPackageName
                }
                Log.d("PRINT", "IS APP PREFERRED? ${isMyAppPreferred.value}")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load saved articles (replace with your own flow)
    LaunchedEffect(Unit) {
        SavedArticlesStore.getSavedArticles(context).collect { articles ->
            savedArticles.clear()
            savedArticles.addAll(
                articles.asReversed().take(30).map {
                    ArticleInfo(
                        title = it.title,
                        url = it.url,
                        authorName = it.authorName,
                        authorPfp = it.authorPfp,
                        mainImage = it.mainImage,
                        subtitle = it.subtitle,
                        publishDate = it.publishDate
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Freedium") }) },
        content = { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 35.dp)
                    .padding(padding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🔔 Show warning card if app is not preferred and user hasn't dismissed it
                if (!isMyAppPreferred.value && showWarningCard) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        Box {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "We can't open Medium links yet!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Please go to this app's settings and enable medium.com under \"Open by default\" to allow this app to open Medium links.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ElevatedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Go to Settings")
                                }
                                Box(modifier = Modifier.height(10.dp))
                                OutlinedButton (
                                    onClick = {
                                        scope.launch {
                                            context.settingsDataStore.edit { prefs ->
                                                prefs[SHOW_WARNING_KEY] = false
                                            }
                                            showWarningCard = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                     "Dismiss"
                                    )
                                }
                            }
                        }
                    }

                }

                SearchBar(text, validLink)

                Column(modifier = Modifier.height(96.dp)) {
                    Spacer(modifier = Modifier.height(15.dp))
                    Box(modifier = Modifier.height(articleButtonHeight)) {
                        FilledTonalButton(
                            onClick = { onChangeScreen(text.value) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Open Article",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .offset(x = 15.dp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Article Icon",
                                modifier = Modifier.padding(end = 15.dp)
                            )
                        }
                    }
                }

                if (savedArticles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(15.dp))
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "No recent articles",
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            "No Recent Articles",
                            modifier = Modifier.padding(top = 30.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Text(
                        "Recent Articles",
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedArticles.size) { index ->
                            val article = savedArticles[index]
                            Card(
                                onClick = { onChangeScreen(article.url) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    GlideImage(
                                        model = article.mainImage,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            article.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            article.subtitle,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            article.authorName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun parseHtmlChildren(
    nodes: List<Node>,
    linkColor: Color = Color(0xFF6200EE)
): AnnotatedString {
    return buildAnnotatedString {
        for (child in nodes) {
            when (child) {
                is TextNode -> append(child.text())
                is Element -> {
                    when (child.tagName()) {
                        "strong" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(child.text())
                        }

                        "em" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(child.text())
                        }

                        "a" -> {
                            val link = child.attr("href")
                            val tag = "url_$link"
                            pushStringAnnotation(tag, link)
                            withStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(child.text())
                            }
                            pop()
                        }

                        else -> append(child.text())
                    }
                }
            }
        }
    }
}

data class LinkPreviewData(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val url: String
)

fun parseLinkPreviewElement(element: Element): LinkPreviewData? {
    val url = element.attr("href")
    val title = element.selectFirst("h2")?.text() ?: return null
    val subtitle = element.selectFirst("h3")?.text() ?: ""
    val imgStyle = element.selectFirst("[style*=background-image]")?.attr("style") ?: ""
    val imageUrl = Regex("""url\(['"]?(.*?)['"]?\)""").find(imgStyle)?.groupValues?.get(1) ?: ""

    return LinkPreviewData(
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        url = url
    )
}

@OptIn(ExperimentalGlideComposeApi::class)

@Composable
fun LinkPreviewCard(preview: LinkPreviewData, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hostname = remember(preview.url) {
        try {
            URI(preview.url).host?.removePrefix("www.") ?: preview.url
        } catch (e: Exception) {
            preview.url
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(preview.url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        GlideImage(
            model = preview.imageUrl,
            contentDescription = preview.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Text(preview.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(preview.subtitle, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(hostname, fontSize = 12.sp, color = Color.Gray)
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HtmlText(html: String, linkColor: Color = MaterialTheme.colorScheme.tertiary) {
    val doc = Jsoup.parse(html)
    val elements = doc.body().children()
    val context = LocalContext.current

    // State to track fullscreen image URL
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullsceenImageCaption by remember { mutableStateOf<String?>(null) }

    SelectionContainer {
        Column(modifier = Modifier.padding(5.dp)) {
            for (el in elements) {
                when (el.tagName().lowercase()) {
                    in listOf("h1", "h2", "h3", "h4", "h5", "h6") -> {
                        val level = el.tagName().substring(1).toIntOrNull() ?: 6
                        val style = when (level) {
                            1 -> TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            2 -> TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            3 -> TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            4 -> TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            5 -> TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            else -> TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
                        }
                        Text(
                            text = el.text(),
                            style = style,
                            modifier = Modifier.padding(top = 35.dp, bottom = 15.dp)
                        )
                    }

                    "pre", "code" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text(
                                text = el.text(),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
//                                    color = Color(white)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                                    .horizontalScroll(rememberScrollState()),
                                softWrap = false
                            )
                        }
                    }


                    "ul" -> {
                        val items = el.select("li")
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            for (item in items) {
                                val annotated = parseHtmlChildren(item.childNodes(), linkColor)

                                ClickableText(
                                    text = buildAnnotatedString {
                                        append("• ")
                                        append(annotated)
                                    },
                                    style = TextStyle(
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp,
                                        color = LocalContentColor.current
                                    ),
                                    modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                                    onClick = { offset ->
                                        annotated.getStringAnnotations(start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                if (annotation.tag.startsWith("url_")) {
                                                    val intent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(annotation.item)
                                                    )
                                                    context.startActivity(intent)
                                                }
                                            }
                                    }
                                )
                            }
                        }
                    }

                    "p" -> {
                        val annotated = parseHtmlChildren(el.childNodes(), linkColor)

                        ClickableText(
                            text = annotated,
                            style = TextStyle(
                                fontSize = 17.sp,
                                lineHeight = 26.sp
                            ).copy(color = LocalContentColor.current),
                            modifier = Modifier.padding(vertical = 10.dp),
                            onClick = { offset ->
                                annotated.getStringAnnotations(start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        if (annotation.tag.startsWith("url_")) {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(annotation.item)
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                            }
                        )
                    }

                    "div" -> {
                        // Check if div contains a link preview
                        val link = el.selectFirst("a[href]")
                        val preview = link?.let { parseLinkPreviewElement(it) }

                        if (preview != null) {
                            LinkPreviewCard(preview)
                        } else {
                            // Image inside div
                            val img = el.selectFirst("img")
                            val caption =
                                el.nextElementSibling()?.takeIf { it.tagName() == "figcaption" }
                                    ?.text()

                            img?.attr("src")?.let { url ->
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp)
                                    .clickable {
                                        fullscreenImageUrl = url
                                        fullsceenImageCaption = caption
                                    }
                                ) {
                                    GlideImage(
                                        model = url,
                                        contentDescription = "HTML Image",
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }

                            caption?.let {
                                Text(
                                    text = it,
                                    style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

//                    else -> {
//                        Text(
//                            text = el.text(),
//                            modifier = Modifier.padding(vertical = 5.dp)
//                        )
//                    }
                }
            }
        }
    }

    // Fullscreen image dialog
    if (fullscreenImageUrl != null) {
        ZoomableImageDialog(
            imageUrl = fullscreenImageUrl ?: "",
            caption = fullsceenImageCaption,
            onDismiss = { fullscreenImageUrl = null })
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ZoomableImageDialog(
    imageUrl: String,
    caption: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        ) {
            val scale = remember { mutableStateOf(1f) }
            val offsetX = remember { mutableStateOf(0f) }
            val offsetY = remember { mutableStateOf(0f) }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                GlideImage(
                    model = imageUrl,
                    contentDescription = "Fullscreen Zoomable Image",
                    modifier = Modifier
                        .weight(9F)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale.value = (scale.value * zoom).coerceIn(1f, 5f)
                                offsetX.value += pan.x
                                offsetY.value += pan.y
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = offsetX.value,
                            translationY = offsetY.value
                        ),
                    contentScale = ContentScale.Fit
                )
                Text(
                    caption ?: "",
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 320, heightDp = 320)
@Composable
fun HomeScreenPreview() {
    FreediumAndroidTheme {
        HomeScreen(onChangeScreen = {})
    }
}
