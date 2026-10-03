package com.example.ui.screens.about

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About App",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Cash Tracker App Icon
            CashTrackerAppIcon(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(24.dp), clip = false)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Name
            Text(
                text = "Cash Tracker",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Personal Expense & Asset Manager",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3 Major Features Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Features",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Transaction Tracking Feature
            MajorFeatureCard(
                icon = Icons.Default.ReceiptLong,
                title = "Smart Expense & Income Tracking",
                description = "Effortlessly record and categorize daily spending, income, and account-to-account transfers with an integrated calculator keypad and daily group breakdowns."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Financial Analytics & Budgeting Feature
            MajorFeatureCard(
                icon = Icons.Default.BarChart,
                title = "Visual Analytics & Budgeting",
                description = "Understand cash flow with intuitive visual category charts, monthly trend analysis, and dynamic budget goal tracking to stay on top of your financial health."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 100% Offline & Private Security Feature
            MajorFeatureCard(
                icon = Icons.Default.Security,
                title = "100% Offline & Private Security",
                description = "All financial records stay stored locally on your device with zero data collection, optional PIN lock protection, and complete CSV spreadsheet backup and restore."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 100% native Compose vector app icon for Cash Tracker.
 * Safe from any resource loading or XML parser crashes.
 */
@Composable
fun CashTrackerAppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val w = size.width
            val h = size.height

            // 1. BACK FOLD / FLAP OF WALLET (Dark Royal Blue)
            drawRoundRect(
                color = Color(0xFF1E40AF),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.58f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
            )

            // 2. MAIN BLUE WALLET BODY
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF4285F4), Color(0xFF2563EB)),
                    start = Offset(w * 0.10f, h * 0.22f),
                    end = Offset(w * 0.78f, h * 0.85f)
                ),
                topLeft = Offset(w * 0.08f, h * 0.22f),
                size = Size(w * 0.70f, h * 0.60f),
                cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
            )

            // 3. WALLET STRAP / CLASP
            drawRoundRect(
                color = Color(0xFF1D4ED8),
                topLeft = Offset(w * 0.05f, h * 0.42f),
                size = Size(w * 0.30f, h * 0.18f),
                cornerRadius = CornerRadius(w * 0.05f, h * 0.05f)
            )
            // Snap Button
            drawCircle(
                color = Color.White,
                radius = w * 0.045f,
                center = Offset(w * 0.26f, h * 0.51f)
            )

            // 4. GOLDEN RUPEE COIN
            val coinCenter = Offset(w * 0.74f, h * 0.32f)
            val coinRadius = w * 0.21f

            // White Halo
            drawCircle(
                color = Color.White,
                radius = coinRadius + (w * 0.035f),
                center = coinCenter
            )
            // Golden Coin Body
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFB300), Color(0xFFF57C00)),
                    start = Offset(coinCenter.x - coinRadius, coinCenter.y - coinRadius),
                    end = Offset(coinCenter.x + coinRadius, coinCenter.y + coinRadius)
                ),
                radius = coinRadius,
                center = coinCenter
            )

            // Draw Rupee (₹) Symbol
            drawRupeeSymbol(
                center = coinCenter,
                scale = w * 0.0035f
            )

            // 5. THREE-BAR FINANCIAL GROWTH CHART (Bottom-Right)
            val barW = w * 0.11f
            val baseBottom = h * 0.88f

            // Bar 1: Green (Lowest)
            drawGrowthBar(
                color = Color(0xFF22C55E),
                topLeft = Offset(w * 0.46f, baseBottom - h * 0.24f),
                size = Size(barW, h * 0.24f),
                cornerRadius = w * 0.035f
            )

            // Bar 2: Cyan (Medium)
            drawGrowthBar(
                color = Color(0xFF38BDF8),
                topLeft = Offset(w * 0.61f, baseBottom - h * 0.34f),
                size = Size(barW, h * 0.34f),
                cornerRadius = w * 0.035f
            )

            // Bar 3: Royal Blue (Tallest)
            drawGrowthBar(
                color = Color(0xFF1D4ED8),
                topLeft = Offset(w * 0.76f, baseBottom - h * 0.45f),
                size = Size(barW, h * 0.45f),
                cornerRadius = w * 0.035f
            )
        }
    }
}

private fun DrawScope.drawGrowthBar(
    color: Color,
    topLeft: Offset,
    size: Size,
    cornerRadius: Float
) {
    val borderPadding = 2.5f
    // White border outline
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(topLeft.x - borderPadding, topLeft.y - borderPadding),
        size = Size(size.width + borderPadding * 2, size.height + borderPadding * 2),
        cornerRadius = CornerRadius(cornerRadius + borderPadding, cornerRadius + borderPadding)
    )
    // Colored Bar
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
}

private fun DrawScope.drawRupeeSymbol(center: Offset, scale: Float) {
    val strokeW = 4.2f * scale
    val color = Color.White

    // Top horizontal bar
    drawLine(
        color = color,
        start = Offset(center.x - 12f * scale, center.y - 12f * scale),
        end = Offset(center.x + 12f * scale, center.y - 12f * scale),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )

    // Second horizontal bar
    drawLine(
        color = color,
        start = Offset(center.x - 12f * scale, center.y - 5f * scale),
        end = Offset(center.x + 8f * scale, center.y - 5f * scale),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )

    // Upper curve of R
    val path = Path().apply {
        moveTo(center.x - 5f * scale, center.y - 12f * scale)
        lineTo(center.x - 5f * scale, center.y + 1f * scale)
        cubicTo(
            center.x + 8f * scale, center.y + 1f * scale,
            center.x + 8f * scale, center.y - 12f * scale,
            center.x - 5f * scale, center.y - 12f * scale
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeW, cap = StrokeCap.Round)
    )

    // Downward leg
    drawLine(
        color = color,
        start = Offset(center.x - 2f * scale, center.y + 1f * scale),
        end = Offset(center.x + 11f * scale, center.y + 14f * scale),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )
}

@Composable
private fun MajorFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
