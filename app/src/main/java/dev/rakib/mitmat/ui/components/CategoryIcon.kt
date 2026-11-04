package dev.rakib.mitmat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardTravel
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rakib.mitmat.ui.theme.DarkBlueCard
import dev.rakib.mitmat.ui.theme.DarkLavenderCard
import dev.rakib.mitmat.ui.theme.DarkMintCard
import dev.rakib.mitmat.ui.theme.DarkPeachCard
import dev.rakib.mitmat.ui.theme.DarkPinkCard
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.LocalIsDarkTheme
import dev.rakib.mitmat.ui.theme.MintAccent
import dev.rakib.mitmat.ui.theme.PastelLavender
import dev.rakib.mitmat.ui.theme.PastelMint
import dev.rakib.mitmat.ui.theme.PastelPeach
import dev.rakib.mitmat.ui.theme.PastelPink
import dev.rakib.mitmat.ui.theme.PastelPowderBlue
import dev.rakib.mitmat.ui.theme.PeachAccent
import dev.rakib.mitmat.ui.theme.PinkAccent
import dev.rakib.mitmat.ui.theme.PowderBlueAccent

enum class ExpenseCategory(
    val title: String,
    val icon: ImageVector,
    val lightBg: Color,
    val darkBg: Color,
    val accentColor: Color
) {
    FOOD("Food", Icons.Rounded.Restaurant, PastelPeach, DarkPeachCard, PeachAccent),
    GROCERIES("Groceries", Icons.Rounded.ShoppingCart, PastelMint, DarkMintCard, MintAccent),
    TRAVEL("Travel", Icons.Rounded.CardTravel, PastelPowderBlue, DarkBlueCard, PowderBlueAccent),
    RENT("Rent", Icons.Rounded.Home, PastelLavender, DarkLavenderCard, LavenderAccent),
    ENTERTAINMENT("Entertainment", Icons.Rounded.Movie, PastelPink, DarkPinkCard, PinkAccent),
    UTILITIES("Utilities", Icons.Rounded.WaterDrop, PastelPowderBlue, DarkBlueCard, PowderBlueAccent),
    GENERAL("General", Icons.Rounded.Receipt, PastelLavender, DarkLavenderCard, LavenderAccent);

    companion object {
        fun fromString(name: String): ExpenseCategory {
            return entries.firstOrNull { it.title.equals(name, ignoreCase = true) } ?: GENERAL
        }
    }
}

@Composable
fun CategoryBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    val category = ExpenseCategory.fromString(categoryName)
    val isDark = LocalIsDarkTheme.current
    val bgColor = if (isDark) category.darkBg else category.lightBg

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.title,
            tint = category.accentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
