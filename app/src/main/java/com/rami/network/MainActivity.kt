package com.rami.network

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.network.domain.ContentSource
import com.rami.network.domain.ContentSourceKind
import com.rami.network.domain.SourceType
import com.rami.network.domain.Validation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection

private val RoyalBlack = Color(0xFF090909)
private val RoyalPanel = Color(0xFF171717)
private val RoyalGold = Color(0xFFD6B36A)
private val RoyalMuted = Color(0xFFB9B3A6)
private val RoyalLine = Color(0xFF39342A)

private enum class AdminSection(val title: String) {
    DASHBOARD("الرئيسية"),
    CHANNELS("القنوات"),
    LIBRARY("مكتبة المحتوى"),
    SCHEDULE("جدول البث"),
    PLAYBACK("مشغل القناة"),
    BRANDING("هوية الشبكة"),
    SETTINGS("الإعدادات")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = RoyalBlack,
                    surface = RoyalPanel,
                    primary = RoyalGold,
                    onBackground = Color(0xFFF5F1E8),
                    onSurface = Color(0xFFF5F1E8)
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RoyalNetworkApp()
                }
            }
        }
    }
}

@Composable
private fun RoyalNetworkApp() {
    var selectedSection by rememberSaveable { mutableStateOf(AdminSection.DASHBOARD.name) }
    val current = AdminSection.valueOf(selectedSection)

    Surface(modifier = Modifier.fillMaxSize(), color = RoyalBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalBlack)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header()
            SectionNavigation(
                selected = current,
                onSelect = { selectedSection = it.name }
            )
            when (current) {
                AdminSection.DASHBOARD -> DashboardContent(
                    onOpen = { selectedSection = it.name }
                )
                AdminSection.CHANNELS -> ChannelsContent()
                AdminSection.LIBRARY -> LibraryContent()
                AdminSection.SCHEDULE -> ScheduleContent()
                AdminSection.PLAYBACK -> PlaylistPlayerScreen()
                AdminSection.BRANDING -> BrandingContent()
                AdminSection.SETTINGS -> SettingsContent()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RAMİ TV  •  مركز الإدارة",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF77736B),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = "RAMİ NETWORK",
            color = RoyalGold,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "مركز إدارة شبكة RAMİ TV",
            color = RoyalMuted,
            fontSize = 14.sp
        )
        Text(
            text = "إدارة مركزية للقنوات والمحتوى والبث",
            color = Color(0xFFE8E0D0),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionNavigation(
    selected: AdminSection,
    onSelect: (AdminSection) -> Unit
) {
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
        NavigationButton(AdminSection.PLAYBACK, selected, onSelect, Modifier.fillMaxWidth())
    }
}

@Composable
private fun NavigationButton(
    section: AdminSection,
    selected: AdminSection,
    onSelect: (AdminSection) -> Unit,
    modifier: Modifier
) {
    val active = section == selected
    if (active) {
        Button(
            onClick = { onSelect(section) },
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoyalGold,
                contentColor = RoyalBlack
            )
        ) {
            Text(section.title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = { onSelect(section) },
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalLine)
        ) {
            Text(
                section.title,
                fontSize = 11.sp,
                color = RoyalMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DashboardContent(onOpen: (AdminSection) -> Unit) {
    StatusCard()
    SectionTitle("أقسام الإدارة")
    DashboardLink(
        "إدارة القنوات",
        "إنشاء القنوات وتعديل الاسم والشعار وحالة النشر.",
        AdminSection.CHANNELS,
        onOpen
    )
    DashboardLink(
        "مكتبة المحتوى",
        "تسجيل مصادر المحتوى وربطها بالقنوات مع توثيق حقوق التوزيع.",
        AdminSection.LIBRARY,
        onOpen
    )
    DashboardLink(
        "جدول البث",
        "تنظيم قوائم التشغيل ومواعيد البرامج لكل قناة.",
        AdminSection.SCHEDULE,
        onOpen
    )
    DashboardLink(
        "هوية RAMİ TV",
        "إعداد الشعار الموحد ومظهره داخل مشغلات الشبكة.",
        AdminSection.BRANDING,
        onOpen
    )
}

@Composable
private fun StatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                "حالة المنصة",
                color = RoyalGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            StatusLine("تطبيق الإدارة", "الواجهة الأساسية جاهزة")
            StatusLine("الخادم المركزي", "لم يتم ربطه بعد")
            StatusLine("البث المتزامن", "بانتظار خدمة البث")
            Text(
                "لن يتم عرض أي قناة على أنها منشورة قبل تأكيد الخادم.",
                color = RoyalMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, color = RoyalMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(
        value,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun DashboardLink(
    title: String,
    description: String,
    destination: AdminSection,
    onOpen: (AdminSection) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = RoyalGold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = RoyalMuted, fontSize = 12.sp)
            OutlinedButton(
                onClick = { onOpen(destination) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("فتح القسم", color = RoyalGold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyManagementSection(
    title: String,
    description: String,
    requirements: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = RoyalGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(description, color = RoyalMuted, fontSize = 13.sp)
            Text(
                "حالة القسم: بانتظار ربط الخادم",
                color = RoyalGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            requirements.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = RoyalGold, fontSize = 15.sp)
                    Text(item, color = Color(0xFFE8E0D0), fontSize = 12.sp)
                }
            }
            Text(
                "لن تُحفظ التغييرات أو تُنشر للمشاهدين قبل اتصال آمن بالخادم المركزي.",
                color = RoyalMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ChannelsContent() {
    EmptyManagementSection(
        title = "إدارة القنوات",
        description = "كل قناة لها رقم ثابت واسم وشعار وحالة نشر مستقلة. حذف قناة لا يغيّر أرقام القنوات الأخرى.",
        requirements = listOf(
            "إنشاء قناة جديدة وترقيمها تلقائياً من الخادم.",
            "تعديل اسم القناة وشعارها وتفعيلها أو إيقافها.",
            "نشر القناة بعد التحقق من إعداداتها.",
            "الاحتفاظ بسجل أرقام القنوات المحذوفة لمنع إعادة استخدامها."
        )
    )
}

@Composable
private fun LibraryContent() {
    var displayName by rememberSaveable { mutableStateOf("") }
    var sourceUrl by rememberSaveable { mutableStateOf("") }
    var rightsNote by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }
    val draftSources = remember { mutableStateListOf<ContentSource>() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(
                "إضافة مصدر HLS",
                color = RoyalGold,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "أدخل رابط قائمة HLS بصيغة m3u8 مع اسم المصدر وبيان حق التوزيع.",
                color = RoyalMuted,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم المصدر") },
                singleLine = true
            )
            OutlinedTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رابط HLS") },
                placeholder = { Text("https://server.example/channel/index.m3u8") },
                singleLine = true
            )
            OutlinedTextField(
                value = rightsNote,
                onValueChange = { rightsNote = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("حقوق التوزيع / الإذن") },
                minLines = 2
            )
            Button(
                onClick = {
                    val source = ContentSource(
                        id = "draft-${draftSources.size + 1}-${displayName.hashCode()}",
                        kind = ContentSourceKind.EXTERNAL_URL,
                        displayName = displayName.trim(),
                        sourceType = SourceType.HLS,
                        sourceUrl = sourceUrl.trim(),
                        rightsNote = rightsNote.trim()
                    )
                    if (Validation.isValidContentSource(source)) {
                        draftSources.add(source)
                        displayName = ""
                        sourceUrl = ""
                        rightsNote = ""
                        feedback = "تمت إضافة المصدر إلى قائمة المعاينة المؤقتة."
                    } else {
                        feedback = "تحقق من الاسم ورابط HTTPS بصيغة m3u8 وبيان حقوق التوزيع."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = RoyalBlack
                )
            ) {
                Text("التحقق وإضافة المصدر", fontWeight = FontWeight.Bold)
            }
            if (feedback.isNotBlank()) {
                Text(feedback, color = RoyalGold, fontSize = 12.sp)
            }
            Text(
                "مهم: هذه قائمة معاينة داخل جلسة التطبيق فقط. لم تُحفظ على الخادم ولم تُنشر للمشاهدين بعد.",
                color = RoyalMuted,
                fontSize = 11.sp
            )
        }
    }

    SectionTitle("مصادر HLS في المعاينة")
    if (draftSources.isEmpty()) {
        Text("لا توجد مصادر مضافة بعد.", color = RoyalMuted, fontSize = 12.sp)
    } else {
        draftSources.forEach { source ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalPanel)
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(source.displayName, color = RoyalGold, fontSize = 14.sp)
                        Text("HLS • رابط تم التحقق من بنيته", color = RoyalMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { draftSources.remove(source) }) {
                        Text("حذف", color = RoyalMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    Text(
        "قبل اعتماد المصدر على الشبكة، سيتحقق الخادم من الرابط وحقوق التوزيع، ثم يربطه بالقناة المختارة.",
        color = RoyalMuted,
        fontSize = 12.sp
    )
}

@Composable
private fun ScheduleContent() {
    EmptyManagementSection(
        title = "جدول البث",
        description = "الخادم هو المرجع الوحيد لتوقيت البث، حتى يشاهد الجميع البرنامج نفسه في اللحظة نفسها.",
        requirements = listOf(
            "إنشاء قوائم تشغيل مستقلة لكل قناة.",
            "تحديد مواعيد البرامج والمنطقة الزمنية.",
            "تكرار قائمة التشغيل تلقائياً عند نهايتها.",
            "نشر حالة التشغيل ووقت البداية من الخادم."
        )
    )
}

@Composable
private fun BrandingContent() {
    EmptyManagementSection(
        title = "هوية RAMİ TV",
        description = "إعداد موحد لشعار الشبكة ليظهر داخل مشغلات تطبيقات المشاهدة المتصلة.",
        requirements = listOf(
            "اختيار ملف الشعار المعتمد.",
            "تحديد الحجم والشفافية ومكان الظهور.",
            "حفظ إعداد موحد تستخدمه تطبيقات المشاهدة.",
            "دراسة طبقة عائمة اختيارية على Android مع مراعاة قيود النظام والتطبيقات."
        )
    )
}

@Composable
private fun SettingsContent() {
    EmptyManagementSection(
        title = "إعدادات الشبكة",
        description = "إعدادات الإدارة والاتصال والأمان، وليست إعدادات للمشاهدين.",
        requirements = listOf(
            "تسجيل دخول إداري وصلاحيات على الخادم.",
            "اختيار اللغة: العربية أو التركية أو الإنجليزية.",
            "عرض حالة الخادم وسجل العمليات.",
            "عدم وضع مفاتيح الإدارة أو أسرار البث داخل التطبيق."
        )
    )
}
