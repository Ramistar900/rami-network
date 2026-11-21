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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider

private val RoyalBlack = Color(0xFF090909)
private val RoyalPanel = Color(0xFF171717)
private val RoyalGold = Color(0xFFD6B36A)
private val RoyalMuted = Color(0xFFB9B3A6)

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
                    DashboardScreen()
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RoyalBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalBlack)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RAMİ NETWORK",
                    color = RoyalGold,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "مركز إدارة شبكة RAMİ TV",
                    color = RoyalMuted,
                    fontSize = 15.sp
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalPanel)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مرحباً بك في مركز الشبكة",
                        color = RoyalGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "من هنا ستتم إدارة القنوات والمكتبات وجداول البث مركزياً. تطبيقات المشاهدة ستبقى منفصلة عن أدوات الإدارة.",
                        color = RoyalMuted,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("حالة المشروع", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "قيد التأسيس",
                            color = RoyalGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "أقسام الإدارة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            AdminSectionCard(
                title = "إدارة القنوات",
                description = "إنشاء القنوات وترقيمها تلقائياً وتعديل أسمائها وشعاراتها."
            )
            AdminSectionCard(
                title = "مكتبة المحتوى",
                description = "إضافة روابط الفيديو وترتيب قوائم التشغيل الخاصة بكل قناة."
            )
            AdminSectionCard(
                title = "جدول البث",
                description = "إعداد مواعيد التشغيل والبث المتزامن لجميع المشاهدين."
            )

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "RAMİ TV  •  لوحة الإدارة",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color(0xFF77736B),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AdminSectionCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = RoyalGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = RoyalMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
