package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CategoryIconResolver {
    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "restaurant", "food", "dining" -> Icons.Default.Restaurant
            "local_cafe", "cafe", "coffee" -> Icons.Default.LocalCafe
            "delivery_dining", "delivery" -> Icons.Default.DeliveryDining
            "shopping_cart", "grocery", "groceries" -> Icons.Default.ShoppingCart
            "directions_car", "transport", "transportation", "car" -> Icons.Default.DirectionsCar
            "local_gas_station", "gas", "fuel", "petrol" -> Icons.Default.LocalGasStation
            "shopping_bag", "shopping", "clothes" -> Icons.Default.ShoppingBag
            "receipt_long", "receipt", "bills", "utilities" -> Icons.Default.ReceiptLong
            "home", "housing", "rent" -> Icons.Default.Home
            "movie", "entertainment", "cinema" -> Icons.Default.Movie
            "medical_services", "health", "medical", "doctor" -> Icons.Default.MedicalServices
            "subscriptions", "subscription", "streaming" -> Icons.Default.Subscriptions
            "flight", "travel", "flight_takeoff", "holiday" -> Icons.Default.Flight
            "spa", "personal_care", "beauty" -> Icons.Default.Spa
            "school", "education", "books" -> Icons.Default.School
            "card_giftcard", "gift", "donation", "charity" -> Icons.Default.CardGiftcard
            "trending_up", "investment", "investments" -> Icons.Default.TrendingUp
            "payments", "salary", "wage" -> Icons.Default.Payments
            "redeem", "bonus" -> Icons.Default.Redeem
            "work", "freelance", "business" -> Icons.Default.Work
            "show_chart", "stocks", "mutual_funds" -> Icons.Default.ShowChart
            "savings", "interest", "deposit" -> Icons.Default.Savings
            "apartment", "rental", "real_estate" -> Icons.Default.Apartment
            "currency_exchange", "refund", "cashback" -> Icons.Default.CurrencyExchange
            "account_balance_wallet", "wallet", "cash" -> Icons.Default.AccountBalanceWallet
            "account_balance", "bank" -> Icons.Default.AccountBalance
            "credit_card" -> Icons.Default.CreditCard
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF0D9488)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            if (cleanHex.length == 6) {
                Color(android.graphics.Color.parseColor("#$cleanHex"))
            } else if (cleanHex.length == 8) {
                Color(android.graphics.Color.parseColor("#$cleanHex"))
            } else {
                fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }
}

@Composable
fun CategoryIconBadge(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    val originalColor = CategoryIconResolver.parseColor(colorHex)
    val bgColor = if (com.example.ui.theme.FinancialColors.refined && androidx.compose.material3.MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        androidx.compose.ui.graphics.lerp(originalColor, Color.White, 0.45f)
    } else originalColor
    val icon = CategoryIconResolver.getIcon(iconName)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.14f))
            .border(
                width = 0.8.dp,
                color = bgColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconName,
            tint = bgColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
