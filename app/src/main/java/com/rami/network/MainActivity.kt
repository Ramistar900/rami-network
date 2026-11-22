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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.network.data.AdminApi
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
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
                        label = { Text("كلمة المرور") }, singleLine = true)
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

@Composable
private fun LibraryContent(token: String) {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf(JSONArray()) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var sourceUrl by rememberSaveable { mutableStateOf("") }
    var rightsNote by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("جارٍ تحميل المصادر...") }
    var busy by remember { mutableStateOf(false) }
    suspend fun loadSources() {
        runCatching { AdminApi.call(token, "list_sources") }
            .onSuccess { sources = it.optJSONArray("data") ?: JSONArray(); message = "تم تحديث المصادر." }
            .onFailure { message = it.message ?: "تعذر تحميل المصادر." }
    }
    LaunchedEffect(token) { loadSources() }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("إضافة مصدر HLS", color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(displayName, { displayName = it }, Modifier.fillMaxWidth(),
                label = { Text("اسم المصدر") }, singleLine = true)
            OutlinedTextField(sourceUrl, { sourceUrl = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("رابط HLS بصيغة HTTPS") }, singleLine = true)
            OutlinedTextField(rightsNote, { rightsNote = it }, Modifier.fillMaxWidth(),
                label = { Text("بيان حقوق التوزيع أو الإذن") }, minLines = 2)
            Button(onClick = {
                busy = true
                scope.launch {
                    runCatching { AdminApi.call(token, "create_source", JSONObject()
                        .put("display_name", displayName.trim()).put("source_kind", "external_url")
                        .put("source_type", "hls").put("source_url", sourceUrl.trim())
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
    Text("لا تعتمد مصدراً إلا إذا كنت تملك حق بثه وتوزيعه. اعتماد المصدر مسؤولية المالك.",
        color = RoyalMuted, fontSize = 11.sp)
}

@Composable
private fun ScheduleContent(token: String) {
    val scope = rememberCoroutineScope()
    var channelId by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("live") }
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("اختر قناة مسجلة وأدخل بيانات البث عند توفرها.") }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("حالة البث المركزية", color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(channelId, { channelId = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("معرّف القناة UUID") }, singleLine = true)
            OutlinedTextField(mode, { mode = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("النمط: live أو playlist أو stopped") }, singleLine = true)
            OutlinedTextField(streamUrl, { streamUrl = it.trim() }, Modifier.fillMaxWidth(),
                label = { Text("رابط HLS النهائي للبث المباشر") }, singleLine = true)
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
    Text("هذا يحدّث توقيت وحالة التشغيل المركزية فقط. لا ينشئ خادم فيديو أو يحوّل MP4 إلى HLS؛ يلزم رابط HLS نهائي من مزود بث مرخّص.",
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
