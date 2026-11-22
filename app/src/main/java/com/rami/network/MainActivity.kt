package com.rami.network

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.network.data.AdminApi
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val RoyalBlack = Color(0xFF090909)
private val RoyalPanel = Color(0xFF171717)
private val RoyalGold = Color(0xFFD6B36A)
private val RoyalMuted = Color(0xFFB9B3A6)
private val RoyalLine = Color(0xFF39342A)

private enum class AdminSection(val title: String) {
    DASHBOARD("الرئيسية"), CHANNELS("القنوات"), LIBRARY("مكتبة المحتوى"),
    SCHEDULE("جدول البث"), BRANDING("هوية الشبكة"), SETTINGS("الإعدادات")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = RoyalBlack, surface = RoyalPanel, primary = RoyalGold,
                onBackground = Color(0xFFF5F1E8), onSurface = Color(0xFFF5F1E8)
            )) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RoyalNetworkRoot()
                }
            }
        }
    }
}

@Composable
private fun RoyalNetworkRoot() {
    var token by rememberSaveable { mutableStateOf<String?>(null) }
    if (token == null) LoginScreen { token = it }
    else RoyalNetworkApp(token = token!!, onLogout = { token = null })
}

@Composable
private fun LoginScreen(onSignedIn: (String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = RoyalBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("RAMİ NETWORK", color = RoyalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("مركز إدارة شبكة RAMİ TV", color = RoyalMuted, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("دخول المالك", color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
                        label = { Text("البريد الإلكتروني") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation())
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) message = "أدخل البريد وكلمة المرور."
                            else {
                                busy = true
                                scope.launch {
                                    runCatching { AdminApi.signIn(email, password) }
                                        .onSuccess(onSignedIn)
                                        .onFailure { message = it.message ?: "تعذر تسجيل الدخول." }
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
                    ) { Text(if (busy) "جارٍ تسجيل الدخول..." else "تسجيل الدخول", fontWeight = FontWeight.Bold) }
                    if (message.isNotBlank()) Text(message, color = RoyalGold, fontSize = 12.sp)
                    Text("الجلسة مؤقتة داخل التطبيق. لا تُحفظ كلمة المرور أو مفاتيح الخادم السرية.",
                        color = RoyalMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RoyalNetworkApp(token: String, onLogout: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(AdminSection.DASHBOARD.name) }
    val current = AdminSection.valueOf(selected)
    Surface(Modifier.fillMaxSize(), color = RoyalBlack) {
        Column(
            Modifier.fillMaxSize().background(RoyalBlack).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("RAMİ NETWORK", color = RoyalGold, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("مركز إدارة شبكة RAMİ TV", color = RoyalMuted, fontSize = 14.sp)
                    Text("متصل بالخادم عبر جلسة المالك", color = Color(0xFF8FC6A0), fontSize = 12.sp)
                }
                OutlinedButton(onClick = onLogout) { Text("خروج", color = RoyalMuted) }
            }
            SectionNavigation(current) { selected = it.name }
            when (current) {
                AdminSection.DASHBOARD -> DashboardContent { selected = it.name }
                AdminSection.CHANNELS -> ChannelsContent(token)
                AdminSection.LIBRARY -> LibraryContent(token)
                AdminSection.SCHEDULE -> ScheduleContent(token)
                AdminSection.BRANDING -> EmptySection("هوية RAMİ TV", "إعداد موحد للشعار داخل مشغلات الشبكة.",
                    listOf("اختيار الشعار المعتمد.", "تحديد الحجم والشفافية والموقع.", "مزامنة إعدادات الهوية مع تطبيقات المشاهدة."))
                AdminSection.SETTINGS -> EmptySection("إعدادات الشبكة", "إعدادات الإدارة والاتصال والأمان.",
                    listOf("جلسة المالك مؤقتة.", "لا توجد مفاتيح سرية داخل التطبيق.", "اللغات المخطط لها: العربية والتركية والإنجليزية."))
            }
            Text("RAMİ TV • مركز الإدارة الآمن", Modifier.fillMaxWidth(),
                color = Color(0xFF77736B), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SectionNavigation(selected: AdminSection, onSelect: (AdminSection) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigationButton(AdminSection.DASHBOARD, selected, onSelect, Modifier.weight(1f))
            NavigationButton(AdminSection.CHANNELS, selected, onSelect, Modifier.weight(1f))
            NavigationButton(AdminSection.LIBRARY, selected, onSelect, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigationButton(AdminSection.SCHEDULE, selected, onSelect, Modifier.weight(1f))
            NavigationButton(AdminSection.BRANDING, selected, onSelect, Modifier.weight(1f))
            NavigationButton(AdminSection.SETTINGS, selected, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavigationButton(section: AdminSection, selected: AdminSection,
    onSelect: (AdminSection) -> Unit, modifier: Modifier) {
    if (section == selected) {
        Button(onClick = { onSelect(section) }, modifier = modifier, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
        ) { Text(section.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(onClick = { onSelect(section) }, modifier = modifier,
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, RoyalLine)
        ) { Text(section.title, fontSize = 11.sp, color = RoyalMuted, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun DashboardContent(onOpen: (AdminSection) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("حالة المنصة", color = RoyalGold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            StatusLine("خادم الإدارة", "متصل")
            StatusLine("قاعدة بيانات الشبكة", "مهيأة ومحمية")
            StatusLine("واجهة المشاهدين", "قراءة القنوات المنشورة فقط")
            StatusLine("البث الفعلي", "يتطلب مصدر HLS معتمداً")
        }
    }
    SectionTitle("أقسام الإدارة")
    DashboardLink("إدارة القنوات", "إنشاء القنوات وتعديلها ونشرها.", AdminSection.CHANNELS, onOpen)
    DashboardLink("مكتبة المحتوى", "إضافة المصادر واعتماد حقوق التوزيع.", AdminSection.LIBRARY, onOpen)
    DashboardLink("جدول البث", "تحديد حالة التشغيل والتوقيت المركزي.", AdminSection.SCHEDULE, onOpen)
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Text(value, color = RoyalMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DashboardLink(title: String, description: String, destination: AdminSection,
    onOpen: (AdminSection) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = RoyalGold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = RoyalMuted, fontSize = 12.sp)
            OutlinedButton(onClick = { onOpen(destination) }) { Text("فتح القسم", color = RoyalGold, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ChannelsContent(token: String) {
    val scope = rememberCoroutineScope()
    var channels by remember { mutableStateOf(JSONArray()) }
    var name by rememberSaveable { mutableStateOf("") }
    var slug by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("جارٍ تحميل القنوات...") }
    var busy by remember { mutableStateOf(false) }
    suspend fun loadChannels() {
        runCatching { AdminApi.call(token, "list_channels") }
            .onSuccess { channels = it.optJSONArray("data") ?: JSONArray(); message = "تم تحديث قائمة القنوات." }
            .onFailure { message = it.message ?: "تعذر تحميل القنوات." }
    }
    LaunchedEffect(token) { loadChannels() }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("إنشاء قناة", color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("اسم القناة") }, singleLine = true)
            OutlinedTextField(slug, { slug = it.lowercase().replace(" ", "-") }, Modifier.fillMaxWidth(),
                label = { Text("معرّف القناة بالإنجليزية") }, placeholder = { Text("rami-tv") }, singleLine = true)
            Button(onClick = {
                busy = true
                scope.launch {
                    runCatching { AdminApi.call(token, "create_channel", JSONObject()
                        .put("name", name.trim()).put("slug", slug.trim()).put("playback_mode", "vod")) }
                        .onSuccess { name = ""; slug = ""; message = "تم إنشاء القناة وحجز رقمها من الخادم."; loadChannels() }
                        .onFailure { message = it.message ?: "فشل إنشاء القناة." }
                    busy = false
                }
            }, enabled = !busy && name.isNotBlank() && slug.matches(Regex("[a-z0-9][a-z0-9-]{1,62}")),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text(if (busy) "جارٍ الحفظ..." else "إنشاء القناة") }
            Text(message, color = RoyalMuted, fontSize = 12.sp)
        }
    }
    SectionTitle("القنوات المسجلة")
    if (channels.length() == 0) Text("لا توجد قنوات بعد. أنشئ أول قناة أعلاه.", color = RoyalMuted, fontSize = 12.sp)
    for (index in 0 until channels.length()) {
        val channel = channels.optJSONObject(index) ?: continue
        val id = channel.optString("id")
        val enabled = channel.optBoolean("is_enabled")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("القناة " + channel.optLong("channel_number") + " • " + channel.optString("name"),
                    color = RoyalGold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("ID: " + id, color = RoyalMuted, fontSize = 10.sp)
                Text(if (enabled) "منشورة للمشاهدين" else "غير منشورة", color = RoyalMuted, fontSize = 11.sp)
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { AdminApi.call(token, "update_channel",
                            JSONObject().put("channel_id", id).put("is_enabled", !enabled)) }
                            .onSuccess { message = "تم تحديث حالة القناة."; loadChannels() }
                            .onFailure { message = it.message ?: "تعذر تحديث القناة." }
                    }
                }) { Text(if (enabled) "إيقاف النشر" else "نشر القناة", color = RoyalGold) }
            }
        }
    }
}

private data class CommonsTestSample(
    val title: String,
    val sourceUrl: String,
    val sourcePage: String,
    val durationMs: Long,
    val sizeLabel: String,
    val description: String,
    val rightsNote: String
)

private val commonsTestSamples = listOf(
    CommonsTestSample(
        title = "Forest walking and outdoor cooking – Slovenia",
        sourceUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Forest_walking_and_outdoor_cooking_slovenia.webm",
        sourcePage = "https://commons.wikimedia.org/wiki/File:Forest_walking_and_outdoor_cooking_slovenia.webm",
        durationMs = 4_463_000L,
        sizeLabel = "1 ساعة و14 دقيقة • 3.1 GB",
        description = "جولة طويلة في غابات سلوفينيا مع مشاهد الطبيعة والطيور والجداول والطهي في الهواء الطلق.",
        rightsNote = "CC BY 3.0. Attribution: Snicel 69. Source: https://commons.wikimedia.org/wiki/File:Forest_walking_and_outdoor_cooking_slovenia.webm . License: https://creativecommons.org/licenses/by/3.0 . No edits. Wikimedia Commons records reviewer confirmation on 30 December 2025."
    ),
    CommonsTestSample(
        title = "The Most Beautiful Drone Footage – Cross Country Bicycle Tour",
        sourceUrl = "https://upload.wikimedia.org/wikipedia/commons/3/36/The_Most_Beautiful_Drone_Footage_From_Our_Cross_Country_Bicycle_Tour.webm",
        sourcePage = "https://commons.wikimedia.org/wiki/File:The_Most_Beautiful_Drone_Footage_From_Our_Cross_Country_Bicycle_Tour.webm",
        durationMs = 951_000L,
        sizeLabel = "15 دقيقة و51 ثانية • 1.32 GB",
        description = "مشاهد جوية للطبيعة والمناظر على امتداد رحلة بالدراجة عبر الولايات المتحدة.",
        rightsNote = "CC BY 3.0. Attribution: Ryan Van Duzer. Source: https://commons.wikimedia.org/wiki/File:The_Most_Beautiful_Drone_Footage_From_Our_Cross_Country_Bicycle_Tour.webm . License: https://creativecommons.org/licenses/by/3.0 . No edits. Wikimedia Commons records reviewer confirmation on 17 July 2024."
    ),
    CommonsTestSample(
        title = "50 Kilometers of Brazilian Forest Canopy",
        sourceUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ec/50_Kilometers_of_Brazilian_Forest_Canopy.webm",
        sourcePage = "https://commons.wikimedia.org/wiki/File:50_Kilometers_of_Brazilian_Forest_Canopy.webm",
        durationMs = 602_000L,
        sizeLabel = "10 دقائق و2 ثانية • 86.28 MB",
        description = "تصوير علمي لامتداد بيانات الليدار فوق الغابات المطيرة البرازيلية.",
        rightsNote = "Public domain in the United States. Creator: NASA Scientific Visualization Studio. Source: https://commons.wikimedia.org/wiki/File:50_Kilometers_of_Brazilian_Forest_Canopy.webm . Do not use NASA logos or imply NASA endorsement."
    ),
    CommonsTestSample(
        title = "Universe (1976)",
        sourceUrl = "https://upload.wikimedia.org/wikipedia/commons/0/00/Universe_%281976%29.webm",
        sourcePage = "https://commons.wikimedia.org/wiki/File:Universe_(1976).webm",
        durationMs = 1_612_000L,
        sizeLabel = "26 دقيقة و52 ثانية • 38.06 MB",
        description = "فيلم علمي وثائقي عن الكون من إنتاج وكالة ناسا، بتعليق صوتي باللغة الإنجليزية.",
        rightsNote = "Public domain in the United States. Creator: National Aeronautics and Space Administration. Source: https://commons.wikimedia.org/wiki/File:Universe_(1976).webm . Do not use NASA logos or imply NASA endorsement."
    ),
    CommonsTestSample(
        title = "The Challenges of Outer Space – NASA 1955",
        sourceUrl = "https://upload.wikimedia.org/wikipedia/commons/6/66/The_Challenges_of_Outer_Space_NASA_1955.webm",
        sourcePage = "https://commons.wikimedia.org/wiki/File:The_Challenges_of_Outer_Space_NASA_1955.webm",
        durationMs = 1_453_000L,
        sizeLabel = "24 دقيقة و13 ثانية • 196.14 MB",
        description = "وثائقي تاريخي علمي عن بدايات استكشاف الفضاء، من أرشيف ناسا.",
        rightsNote = "Public domain in the United States. Creator: NASA. Source: https://commons.wikimedia.org/wiki/File:The_Challenges_of_Outer_Space_NASA_1955.webm . Do not use NASA logos or imply NASA endorsement."
    )
)

@Composable
private fun LibraryContent(token: String) {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf(JSONArray()) }
    var mediaItems by remember { mutableStateOf(JSONArray()) }
    var confirmCommonsImport by remember { mutableStateOf(false) }
    var sourceType by rememberSaveable { mutableStateOf("hls") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var sourceUrl by rememberSaveable { mutableStateOf("") }
    var rightsNote by rememberSaveable { mutableStateOf("") }
    var publicSourceId by rememberSaveable { mutableStateOf("") }
    var publicViewerUrl by rememberSaveable { mutableStateOf("") }
    var mediaTitle by rememberSaveable { mutableStateOf("") }
    var mediaSourceId by rememberSaveable { mutableStateOf("") }
    var durationSeconds by rememberSaveable { mutableStateOf("1800") }
    var message by remember { mutableStateOf("جارٍ تحميل المصادر...") }
    var busy by remember { mutableStateOf(false) }
    suspend fun loadSources() {
        runCatching {
            AdminApi.call(token, "list_sources") to AdminApi.call(token, "list_media")
        }.onSuccess { pair ->
            sources = pair.first.optJSONArray("data") ?: JSONArray()
            mediaItems = pair.second.optJSONArray("data") ?: JSONArray()
            message = "تم تحديث المصادر ومكتبة الفيديوهات."
        }.onFailure { message = it.message ?: "تعذر تحميل المكتبة." }
    }

    fun importCommonsLibrary() {
        if (busy) return
        busy = true
        scope.launch {
            var completed = 0
            try {
                val channelResponse = AdminApi.call(token, "list_channels")
                val channelArray = channelResponse.optJSONArray("data") ?: JSONArray()
                var vodChannel: JSONObject? = null
                for (index in 0 until channelArray.length()) {
                    val candidate = channelArray.optJSONObject(index) ?: continue
                    if (candidate.optString("slug") == "rami-vod-test") {
                        vodChannel = candidate
                        break
                    }
                }
                if (vodChannel == null) {
                    vodChannel = AdminApi.call(token, "create_channel", JSONObject()
                        .put("slug", "rami-vod-test")
                        .put("name", "RAMI VOD TEST")
                        .put("playback_mode", "scheduled"))
                        .optJSONObject("data")
                        ?: throw IllegalStateException("تعذر إنشاء قناة اختبار الفيديو.")
                }
                val channelRecord = vodChannel
                    ?: throw IllegalStateException("تعذر تحميل قناة اختبار الفيديو.")
                val channelId = channelRecord.optString("id")
                if (channelId.isBlank()) throw IllegalStateException("معرّف قناة VOD غير صالح.")

                val sourceArray = AdminApi.call(token, "list_sources")
                    .optJSONArray("data") ?: JSONArray()
                val mediaArray = AdminApi.call(token, "list_media")
                    .optJSONArray("data") ?: JSONArray()
                val playlistArray = AdminApi.call(token, "list_playlist",
                    JSONObject().put("channel_id", channelId))
                    .optJSONArray("data") ?: JSONArray()

                for (sample in commonsTestSamples) {
                    message = "جارٍ تجهيز: " + sample.title
                    var source: JSONObject? = null
                    for (index in 0 until sourceArray.length()) {
                        val candidate = sourceArray.optJSONObject(index) ?: continue
                        if (candidate.optString("source_url") == sample.sourceUrl) {
                            source = candidate
                            break
                        }
                    }
                    if (source == null) {
                        source = AdminApi.call(token, "create_source", JSONObject()
                            .put("display_name", sample.title)
                            .put("source_kind", "external_url")
                            .put("source_type", "webm")
                            .put("source_url", sample.sourceUrl)
                            .put("rights_note", sample.rightsNote))
                            .optJSONObject("data")
                            ?: throw IllegalStateException("تعذر تسجيل مصدر: " + sample.title)
                        sourceArray.put(source)
                    }

                    var sourceRecord = source
                        ?: throw IllegalStateException("تعذر تحميل بيانات المصدر.")
                    val sourceId = sourceRecord.optString("id")
                    if (sourceId.isBlank()) throw IllegalStateException("معرّف المصدر مفقود.")
                    val rightsStatus = sourceRecord.optString("rights_status")
                    if (rightsStatus == "rejected" || rightsStatus == "unknown") {
                        throw IllegalStateException("حالة حقوق المصدر تتطلب مراجعة يدوية: " + sample.title)
                    }
                    if (rightsStatus != "approved" || !sourceRecord.optBoolean("is_enabled")) {
                        sourceRecord = AdminApi.call(token, "set_source_rights", JSONObject()
                            .put("source_id", sourceId)
                            .put("rights_status", "approved")
                            .put("rights_note", sample.rightsNote))
                            .optJSONObject("data") ?: sourceRecord
                    }
                    if (sourceRecord.optString("viewer_url") != sample.sourceUrl) {
                        sourceRecord = AdminApi.call(token, "set_source_viewer_url", JSONObject()
                            .put("source_id", sourceId)
                            .put("viewer_url", sample.sourceUrl))
                            .optJSONObject("data") ?: sourceRecord
                    }

                    var mediaId = ""
                    for (index in 0 until mediaArray.length()) {
                        val candidate = mediaArray.optJSONObject(index) ?: continue
                        if (candidate.optString("source_id") == sourceId &&
                            candidate.optString("title") == sample.title) {
                            mediaId = candidate.optString("id")
                            break
                        }
                    }
                    if (mediaId.isBlank()) {
                        val media = AdminApi.call(token, "create_media", JSONObject()
                            .put("source_id", sourceId)
                            .put("title", sample.title)
                            .put("description", sample.description + " المصدر والترخيص: " + sample.sourcePage)
                            .put("duration_ms", sample.durationMs))
                            .optJSONObject("data")
                            ?: throw IllegalStateException("تعذر إضافة الفيديو: " + sample.title)
                        mediaArray.put(media)
                        mediaId = media.optString("id")
                    }

                    var alreadyInPlaylist = false
                    for (index in 0 until playlistArray.length()) {
                        val entry = playlistArray.optJSONObject(index) ?: continue
                        if (entry.optString("media_item_id") == mediaId) {
                            alreadyInPlaylist = true
                            break
                        }
                    }
                    if (!alreadyInPlaylist) {
                        val entry = AdminApi.call(token, "append_playlist_entry", JSONObject()
                            .put("channel_id", channelId)
                            .put("media_item_id", mediaId))
                            .optJSONObject("data")
                            ?: throw IllegalStateException("تعذر إدخال الفيديو في قائمة التشغيل.")
                        playlistArray.put(entry)
                    }
                    completed++
                }

                val scheduleArray = AdminApi.call(token, "list_schedule",
                    JSONObject().put("channel_id", channelId))
                    .optJSONArray("data") ?: JSONArray()
                val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                val now = Date()
                val nowKey = utcFormat.format(now).take(19)
                var hasActivePlaylist = false
                for (index in 0 until scheduleArray.length()) {
                    val block = scheduleArray.optJSONObject(index) ?: continue
                    val startKey = block.optString("starts_at").take(19)
                    val endKey = block.optString("ends_at").take(19)
                    if (block.optString("block_type") == "playlist" &&
                        block.optBoolean("is_enabled") &&
                        startKey.isNotBlank() && endKey.isNotBlank() &&
                        startKey <= nowKey && endKey > nowKey) {
                        hasActivePlaylist = true
                        break
                    }
                }
                if (!hasActivePlaylist) {
                    val endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        time = now
                        add(Calendar.DAY_OF_YEAR, 30)
                    }
                    AdminApi.call(token, "create_schedule_block", JSONObject()
                        .put("channel_id", channelId)
                        .put("block_type", "playlist")
                        .put("starts_at", utcFormat.format(now))
                        .put("ends_at", utcFormat.format(endCalendar.time))
                        .put("priority", 100))
                }
                AdminApi.call(token, "update_channel", JSONObject()
                    .put("channel_id", channelId)
                    .put("playback_mode", "scheduled")
                    .put("is_enabled", true))
                loadSources()
                message = "اكتمل تجهيز " + completed + " مواد في قناة RAMI VOD TEST. ستظهر للمشاهدين بعد تحديث حالة الجدولة خلال دقيقة تقريباً."
            } catch (error: Exception) {
                loadSources()
                message = "توقف الاستيراد بعد " + completed + "/" + commonsTestSamples.size +
                    " مواد: " + (error.message ?: "خطأ غير معروف")
            } finally {
                busy = false
            }
        }
    }

    if (confirmCommonsImport) {
        AlertDialog(
            onDismissRequest = { confirmCommonsImport = false },
            title = { Text("استيراد مكتبة RAMİ TV التجريبية") },
            text = {
                Text("سيُنشأ أو يُحدّث مسار RAMI VOD TEST المستقل، وتُضاف " +
                    commonsTestSamples.size + " مواد طويلة إلى قائمته وتُنشر القناة للاختبار. " +
                    "لن تتغير قناة RAMI TEST LIVE. بعض الملفات كبيرة جداً وتُبث مباشرة من Wikimedia Commons.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCommonsImport = false
                    importCommonsLibrary()
                }) { Text("ابدأ الاستيراد", color = RoyalGold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCommonsImport = false }) {
                    Text("إلغاء", color = RoyalMuted)
                }
            },
            containerColor = RoyalPanel,
            titleContentColor = RoyalGold,
            textContentColor = RoyalMuted
        )
    }

    LaunchedEffect(token) { loadSources() }

    SectionTitle("مكتبة Wikimedia Commons التجريبية")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("دفعة الاختبار الأولى • 5 مقاطع", color = RoyalGold,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
            for (sample in commonsTestSamples) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(sample.title, color = Color(0xFFF5F1E8), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(sample.sizeLabel, color = RoyalMuted, fontSize = 10.sp)
                }
            }
            Text("المقاطع تُشغّل مباشرة من Wikimedia Commons ولا تُخزّن في Supabase. " +
                "ملفا الغابة والطيران الجوي كبيران جداً؛ يفضّل الاختبار عبر Wi-Fi.",
                color = RoyalMuted, fontSize = 11.sp)
            Button(onClick = { confirmCommonsImport = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text(if (busy) "جارٍ تجهيز المكتبة..." else "استيراد المقاطع وإنشاء قناة VOD TEST") }
        }
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("إضافة مصدر HLS أو فيديو WebM/MP4", color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sourceType == "hls", onClick = { sourceType = "hls" },
                    label = { Text("HLS") })
                FilterChip(selected = sourceType == "mp4", onClick = { sourceType = "mp4" },
                    label = { Text("MP4") })
                FilterChip(selected = sourceType == "webm", onClick = { sourceType = "webm" },
                    label = { Text("WebM") })
            }
            OutlinedTextField(displayName, { displayName = it }, Modifier.fillMaxWidth(),
                label = { Text("اسم المصدر") }, singleLine = true)
            OutlinedTextField(sourceUrl, { sourceUrl = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("رابط " + sourceType.uppercase(Locale.US) + " بصيغة HTTPS") }, singleLine = true)
            OutlinedTextField(rightsNote, { rightsNote = it }, Modifier.fillMaxWidth(),
                label = { Text("بيان حقوق التوزيع أو الإذن") }, minLines = 2)
            Button(onClick = {
                busy = true
                scope.launch {
                    runCatching { AdminApi.call(token, "create_source", JSONObject()
                        .put("display_name", displayName.trim()).put("source_kind", "external_url")
                        .put("source_type", sourceType).put("source_url", sourceUrl.trim())
                        .put("rights_note", rightsNote.trim())) }
                        .onSuccess {
                            displayName = ""; sourceUrl = ""; rightsNote = ""
                            message = "أُضيف المصدر بحالة انتظار اعتماد الحقوق."
                            loadSources()
                        }.onFailure { message = it.message ?: "تعذر إضافة المصدر." }
                    busy = false
                }
            }, enabled = !busy && displayName.isNotBlank() && sourceUrl.startsWith("https://"),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text(if (busy) "جارٍ الحفظ..." else "إضافة المصدر") }
            Text(message, color = RoyalMuted, fontSize = 12.sp)
        }
    }
    SectionTitle("مصادر المحتوى")
    if (sources.length() == 0) Text("لا توجد مصادر مسجلة بعد.", color = RoyalMuted, fontSize = 12.sp)
    for (index in 0 until sources.length()) {
        val source = sources.optJSONObject(index) ?: continue
        val id = source.optString("id")
        val approved = source.optString("rights_status") == "approved"
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(source.optString("display_name"), color = RoyalGold, fontSize = 14.sp)
                Text("الحالة: " + source.optString("rights_status"), color = RoyalMuted, fontSize = 11.sp)
                Text("معرّف المصدر: " + id, color = RoyalMuted, fontSize = 10.sp)
                Text(
                    if (source.optString("viewer_url").isBlank()) "رابط المشاهدة العام غير مضبوط"
                    else "رابط المشاهدة العام جاهز",
                    color = RoyalMuted, fontSize = 10.sp
                )
                if (!approved) OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { AdminApi.call(token, "set_source_rights", JSONObject()
                            .put("source_id", id).put("rights_status", "approved")
                            .put("rights_note", source.optString("rights_note"))) }
                            .onSuccess { message = "تم اعتماد المصدر."; loadSources() }
                            .onFailure { message = it.message ?: "تعذر اعتماد المصدر." }
                    }
                }) { Text("أؤكد امتلاك الإذن واعتماد المصدر", color = RoyalGold) }
            }
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("رابط المشاهدة العام", color = RoyalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(publicSourceId, { publicSourceId = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("معرّف المصدر المعتمد UUID") }, singleLine = true)
            OutlinedTextField(publicViewerUrl, { publicViewerUrl = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("رابط HLS/MP4/WebM عام HTTPS دون رموز وصول") }, singleLine = true)
            Button(onClick = {
                scope.launch {
                    runCatching {
                        AdminApi.call(token, "set_source_viewer_url", JSONObject()
                            .put("source_id", publicSourceId).put("viewer_url", publicViewerUrl))
                    }.onSuccess {
                        publicViewerUrl = ""
                        message = "تم حفظ رابط المشاهدة العام."
                        loadSources()
                    }.onFailure { message = it.message ?: "تعذر حفظ رابط المشاهدة." }
                }
            }, enabled = publicSourceId.isNotBlank() && publicViewerUrl.startsWith("https://"),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text("حفظ رابط المشاهدة") }
        }
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("إضافة فيديو إلى المكتبة", color = RoyalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(mediaTitle, { mediaTitle = it }, Modifier.fillMaxWidth(),
                label = { Text("اسم الفيديو أو البرنامج") }, singleLine = true)
            OutlinedTextField(mediaSourceId, { mediaSourceId = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("معرّف المصدر المعتمد UUID") }, singleLine = true)
            OutlinedTextField(durationSeconds, { durationSeconds = it.filter(Char::isDigit) },
                Modifier.fillMaxWidth(), label = { Text("المدة بالثواني") }, singleLine = true)
            Button(onClick = {
                val duration = durationSeconds.toLongOrNull()
                if (duration == null || duration <= 0L) {
                    message = "أدخل مدة صحيحة بالثواني."
                } else {
                    busy = true
                    scope.launch {
                        runCatching {
                            AdminApi.call(token, "create_media", JSONObject()
                                .put("source_id", mediaSourceId)
                                .put("title", mediaTitle.trim())
                                .put("duration_ms", duration * 1000L))
                        }.onSuccess {
                            mediaTitle = ""; message = "تمت إضافة الفيديو إلى المكتبة."
                            loadSources()
                        }.onFailure { message = it.message ?: "تعذر إضافة الفيديو." }
                        busy = false
                    }
                }
            }, enabled = !busy && mediaTitle.isNotBlank() && mediaSourceId.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text("حفظ الفيديو") }
        }
    }

    SectionTitle("الفيديوهات المسجلة")
    if (mediaItems.length() == 0) Text("لا توجد فيديوهات بعد.", color = RoyalMuted, fontSize = 12.sp)
    for (index in 0 until mediaItems.length()) {
        val media = mediaItems.optJSONObject(index) ?: continue
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(media.optString("title"), color = RoyalGold, fontSize = 14.sp)
                Text("معرّف الفيديو: " + media.optString("id"), color = RoyalMuted, fontSize = 10.sp)
                Text("معرّف المصدر: " + media.optString("source_id"), color = RoyalMuted, fontSize = 10.sp)
            }
        }
    }
    Text("لا تعتمد مصدراً إلا إذا كنت تملك حق بثه وتوزيعه. لا يُعرض رابط المصدر الخاص للمشاهدين؛ رابط المشاهدة العام فقط هو الذي يُنشر.",
        color = RoyalMuted, fontSize = 11.sp)
}

@Composable
private fun ScheduleContent(token: String) {
    val scope = rememberCoroutineScope()
    var channelId by rememberSaveable { mutableStateOf("") }
    var mediaId by rememberSaveable { mutableStateOf("") }
    var sourceId by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("live") }
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var blockType by rememberSaveable { mutableStateOf("playlist") }
    var startsAt by rememberSaveable {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()))
    }
    var endsAt by rememberSaveable {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }.time))
    }
    var priority by rememberSaveable { mutableStateOf("100") }
    var editingBlockId by rememberSaveable { mutableStateOf("") }
    var editStartsAt by rememberSaveable { mutableStateOf("") }
    var editEndsAt by rememberSaveable { mutableStateOf("") }
    var editPriority by rememberSaveable { mutableStateOf("100") }
    var editEnabled by rememberSaveable { mutableStateOf(true) }
    var message by remember { mutableStateOf("أدخل معرّف القناة لإدارة قوائمها وجدولها المركزي.") }
    var playlistEntries by remember { mutableStateOf(JSONArray()) }
    var scheduleBlocks by remember { mutableStateOf(JSONArray()) }

    fun reloadPlaylist() {
        scope.launch {
            runCatching {
                AdminApi.call(token, "list_playlist", JSONObject().put("channel_id", channelId))
            }.onSuccess {
                playlistEntries = it.optJSONArray("data") ?: JSONArray()
                message = "تم تحديث قائمة التشغيل."
            }.onFailure { message = it.message ?: "تعذر تحميل قائمة التشغيل." }
        }
    }

    fun reloadSchedule() {
        scope.launch {
            runCatching {
                AdminApi.call(token, "list_schedule", JSONObject().put("channel_id", channelId))
            }.onSuccess {
                scheduleBlocks = it.optJSONArray("data") ?: JSONArray()
                message = "تم تحديث جدول القناة."
            }.onFailure { message = it.message ?: "تعذر تحميل جدول القناة." }
        }
    }

    fun reorderPlaylist(from: Int, to: Int) {
        if (to !in 0 until playlistEntries.length()) return
        val ids = mutableListOf<String>()
        for (index in 0 until playlistEntries.length()) {
            ids += playlistEntries.optJSONObject(index)?.optString("id").orEmpty()
        }
        if (ids.any { it.isBlank() }) return
        val moved = ids.removeAt(from)
        ids.add(to, moved)
        val orderedIds = JSONArray()
        ids.forEach { orderedIds.put(it) }
        scope.launch {
            runCatching {
                AdminApi.call(token, "reorder_playlist_entries",
                    JSONObject().put("channel_id", channelId).put("entry_ids", orderedIds))
            }.onSuccess {
                playlistEntries = it.optJSONArray("data") ?: JSONArray()
                message = "تم حفظ الترتيب الجديد للقائمة."
            }.onFailure { message = it.message ?: "تعذر إعادة ترتيب القائمة." }
        }
    }

    SectionTitle("قائمة التشغيل المتزامنة")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("إدارة عناصر القناة", color = RoyalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(channelId, { channelId = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("معرّف القناة UUID") }, singleLine = true)
            OutlinedTextField(mediaId, { mediaId = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("معرّف الفيديو UUID") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching {
                            AdminApi.call(token, "append_playlist_entry",
                                JSONObject().put("channel_id", channelId).put("media_item_id", mediaId))
                        }.onSuccess {
                            reloadPlaylist()
                            message = "تمت إضافة الفيديو إلى القائمة."
                        }.onFailure { message = it.message ?: "تعذرت إضافة الفيديو." }
                    }
                }, enabled = channelId.isNotBlank() && mediaId.isNotBlank()) {
                    Text("إضافة فيديو", color = RoyalGold)
                }
                OutlinedButton(onClick = { reloadPlaylist() }, enabled = channelId.isNotBlank()) {
                    Text("تحديث القائمة", color = RoyalGold)
                }
            }
            if (playlistEntries.length() == 0) {
                Text("لا توجد عناصر محمّلة في القائمة.", color = RoyalMuted, fontSize = 12.sp)
            }
            for (index in 0 until playlistEntries.length()) {
                val entry = playlistEntries.optJSONObject(index) ?: continue
                val entryId = entry.optString("id")
                val media = entry.optJSONObject("media")
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBlack)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${index + 1}. " + (media?.optString("title") ?: entry.optString("media_item_id")),
                            color = RoyalGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("معرّف العنصر: $entryId", color = RoyalMuted, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { reorderPlaylist(index, index - 1) },
                                enabled = index > 0) { Text("↑", color = RoyalGold) }
                            OutlinedButton(onClick = { reorderPlaylist(index, index + 1) },
                                enabled = index < playlistEntries.length() - 1) { Text("↓", color = RoyalGold) }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        AdminApi.call(token, "remove_playlist_entry",
                                            JSONObject().put("channel_id", channelId).put("entry_id", entryId))
                                    }.onSuccess {
                                        playlistEntries = it.optJSONArray("data") ?: JSONArray()
                                        message = "تم حذف العنصر وإعادة ترقيم القائمة."
                                    }.onFailure { message = it.message ?: "تعذر حذف العنصر." }
                                }
                            }) { Text("حذف", color = RoyalGold) }
                        }
                    }
                }
            }
        }
    }

    SectionTitle("الجدولة التلقائية")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("إنشاء فترة بث", color = RoyalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(blockType, { blockType = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("النوع: playlist أو live_source أو media_item") }, singleLine = true)
            OutlinedTextField(startsAt, { startsAt = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("بداية الفترة UTC (ISO-8601)") }, singleLine = true)
            OutlinedTextField(endsAt, { endsAt = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("نهاية الفترة UTC (ISO-8601)") }, singleLine = true)
            OutlinedTextField(priority, { priority = it.filter { ch -> ch.isDigit() || ch == '-' } },
                Modifier.fillMaxWidth(), label = { Text("الأولوية (الأعلى يفوز)") }, singleLine = true)
            if (blockType == "live_source") {
                OutlinedTextField(sourceId, { sourceId = it.trim() }, Modifier.fillMaxWidth(),
                    label = { Text("معرّف مصدر البث المعتمد UUID") }, singleLine = true)
            }
            if (blockType == "media_item") {
                OutlinedTextField(mediaId, { mediaId = it.trim() }, Modifier.fillMaxWidth(),
                    label = { Text("معرّف الفيديو المعتمد UUID") }, singleLine = true)
            }
            Button(onClick = {
                scope.launch {
                    val payload = JSONObject()
                        .put("channel_id", channelId)
                        .put("block_type", blockType)
                        .put("starts_at", startsAt)
                        .put("ends_at", endsAt)
                        .put("priority", priority.toIntOrNull() ?: 100)
                    if (blockType == "live_source") payload.put("source_id", sourceId)
                    if (blockType == "media_item") payload.put("media_item_id", mediaId)
                    runCatching { AdminApi.call(token, "create_schedule_block", payload) }
                        .onSuccess {
                            reloadSchedule()
                            message = "تمت إضافة فترة الجدولة."
                        }
                        .onFailure { message = it.message ?: "تعذر إنشاء فترة الجدولة." }
                }
            }, enabled = channelId.isNotBlank() && blockType in setOf("playlist", "live_source", "media_item") &&
                startsAt.isNotBlank() && endsAt.isNotBlank() &&
                (blockType != "live_source" || sourceId.isNotBlank()) &&
                (blockType != "media_item" || mediaId.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text("إضافة إلى جدول القناة") }
            OutlinedButton(onClick = { reloadSchedule() }, enabled = channelId.isNotBlank()) {
                Text("تحميل جدول القناة", color = RoyalGold)
            }
            if (scheduleBlocks.length() == 0) {
                Text("لا توجد فترات مجدولة محمّلة.", color = RoyalMuted, fontSize = 12.sp)
            }
            for (index in 0 until scheduleBlocks.length()) {
                val block = scheduleBlocks.optJSONObject(index) ?: continue
                val blockId = block.optString("id")
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBlack)) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(block.optString("block_type"), color = RoyalGold, fontWeight = FontWeight.Bold)
                        Text(block.optString("starts_at") + " → " + block.optString("ends_at"),
                            color = RoyalMuted, fontSize = 10.sp)
                        Text("الأولوية: " + block.optString("priority") +
                            " | مفعّل: " + (if (block.optBoolean("is_enabled")) "نعم" else "لا"),
                            color = RoyalMuted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                editingBlockId = blockId
                                editStartsAt = block.optString("starts_at")
                                editEndsAt = block.optString("ends_at")
                                editPriority = block.optString("priority", "100")
                                editEnabled = block.optBoolean("is_enabled", true)
                            }) { Text("تعديل", color = RoyalGold) }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        AdminApi.call(token, "delete_schedule_block",
                                            JSONObject().put("channel_id", channelId).put("block_id", blockId))
                                    }.onSuccess {
                                        if (editingBlockId == blockId) editingBlockId = ""
                                        reloadSchedule()
                                        message = "تم حذف فترة الجدولة."
                                    }.onFailure { message = it.message ?: "تعذر حذف الفترة." }
                                }
                            }) { Text("حذف", color = RoyalGold) }
                        }
                    }
                }
            }
            if (editingBlockId.isNotBlank()) {
                Divider(color = RoyalLine)
                Text("تعديل الفترة المحددة", color = RoyalGold, fontWeight = FontWeight.Bold)
                OutlinedTextField(editStartsAt, { editStartsAt = it.trim() }, Modifier.fillMaxWidth(),
                    label = { Text("بداية الفترة UTC") }, singleLine = true)
                OutlinedTextField(editEndsAt, { editEndsAt = it.trim() }, Modifier.fillMaxWidth(),
                    label = { Text("نهاية الفترة UTC") }, singleLine = true)
                OutlinedTextField(editPriority, { editPriority = it.filter { ch -> ch.isDigit() || ch == '-' } },
                    Modifier.fillMaxWidth(), label = { Text("الأولوية") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = editEnabled, onCheckedChange = { editEnabled = it })
                    Text("الفترة مفعّلة", color = RoyalMuted, fontSize = 12.sp)
                }
                Button(onClick = {
                    scope.launch {
                        runCatching {
                            AdminApi.call(token, "update_schedule_block", JSONObject()
                                .put("channel_id", channelId).put("block_id", editingBlockId)
                                .put("starts_at", editStartsAt).put("ends_at", editEndsAt)
                                .put("priority", editPriority.toIntOrNull() ?: 100)
                                .put("is_enabled", editEnabled))
                        }.onSuccess {
                            editingBlockId = ""
                            reloadSchedule()
                            message = "تم حفظ تعديلات فترة الجدولة."
                        }.onFailure { message = it.message ?: "تعذر تعديل فترة الجدولة." }
                    }
                }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
                ) { Text("حفظ التعديلات") }
            }
        }
    }

    SectionTitle("التحكم اليدوي بالبث")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("حالة البث المركزية", color = RoyalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(mode, { mode = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("النمط: live أو playlist أو stopped") }, singleLine = true)
            OutlinedTextField(streamUrl, { streamUrl = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("رابط HLS العام النهائي HTTPS (دون رموز وصول)") }, singleLine = true)
            Button(onClick = {
                scope.launch {
                    val anchor = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                    runCatching { AdminApi.call(token, "set_playback_state", JSONObject()
                        .put("channel_id", channelId).put("playback_mode", mode).put("anchor_at", anchor)
                        .put("stream_url", if (streamUrl.isBlank()) JSONObject.NULL else streamUrl)) }
                        .onSuccess { message = "تم تحديث حالة التشغيل المركزية ورقم التسلسل." }
                        .onFailure { message = it.message ?: "تعذر تحديث حالة البث." }
                }
            }, enabled = channelId.isNotBlank() && mode in setOf("live", "playlist", "stopped") &&
                (mode != "live" || streamUrl.startsWith("https://")),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
            ) { Text("حفظ حالة البث") }
            Text(message, color = RoyalMuted, fontSize = 12.sp)
        }
    }
    Text("الجدولة تعمل تلقائياً كل دقيقة من توقيت الخادم. لا ينشئ هذا المحرك خادم فيديو أو يحوّل MP4 إلى HLS؛ يلزم رابط مشاهدة عام معتمد، ومخرج HLS فعلي للبث المباشر.",
        color = RoyalMuted, fontSize = 12.sp)
}

@Composable
private fun EmptySection(title: String, description: String, requirements: List<String>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(description, color = RoyalMuted, fontSize = 13.sp)
            requirements.forEach { Text("•  $it", color = Color(0xFFE8E0D0), fontSize = 12.sp) }
        }
    }
}
