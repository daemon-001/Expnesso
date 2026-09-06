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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
            .height(72.dp)
            .background(bottomBarColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Books Tab
            BottomBarItem(
                icon = Icons.Default.Dashboard,
                label = "Books",
                isSelected = selectedTab == DashboardTab.Books,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.Books) }
            )

            // Activity Tab
            BottomBarItem(
                icon = Icons.Default.Receipt,
                label = "Activity",
                isSelected = selectedTab == DashboardTab.Activity,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.Activity) }
            )

            // Spacer for center FAB
            Spacer(modifier = Modifier.width(64.dp))

            // Groups Tab
            BottomBarItem(
                icon = Icons.Default.People,
                label = "Groups",
                isSelected = selectedTab == DashboardTab.Groups,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.Groups) }
            )

            // Split Stats Tab
            BottomBarItem(
                icon = Icons.Default.PieChart,
                label = "Split Stats",
                isSelected = selectedTab == DashboardTab.SplitStats,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { onTabSelected(DashboardTab.SplitStats) }
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
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val color = if (isSelected) activeColor else inactiveColor
    
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
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
