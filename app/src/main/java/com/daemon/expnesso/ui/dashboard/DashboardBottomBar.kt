package com.daemon.expnesso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardBottomBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    onAddClick: () -> Unit
) {
    val bottomBarColor = Color(0xFF0F172A) // Dark slate color from image
    val activeColor = Color(0xFF06B6D4) // Cyan color from image
    val inactiveColor = Color(0xFF94A3B8) // Slate gray from image

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bottomBarColor)
            .navigationBarsPadding()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (Activity) Tab
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Dashboard,
                label = "Home",
                isSelected = selectedTab == DashboardTab.Activity,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.Activity) }
            )

            // Books Tab
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LibraryBooks,
                label = "Books",
                isSelected = selectedTab == DashboardTab.Books,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.Books) }
            )

            // Spacer for center FAB
            Spacer(modifier = Modifier.width(64.dp))

            // Split Stats Tab
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PieChart,
                label = "Split Stats",
                isSelected = selectedTab == DashboardTab.SplitStats,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.SplitStats) }
            )

            // History Tab
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                label = "History",
                isSelected = selectedTab == DashboardTab.History,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.History) }
            )
        }

        // Center FAB
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(64.dp)
                .shadow(8.dp, CircleShape)
                .background(activeColor, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Expense",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BottomBarItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val color by androidx.compose.animation.animateColorAsState(if (isSelected) activeColor else inactiveColor, label = "color")
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "scale")
    
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
