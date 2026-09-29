package com.rateio.app.ui.creategroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rateio.app.ui.theme.LocalRateioColors

/** The prototype's `#categorias`: category chips, always one selected. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryChipRow(
    selectedCategory: GroupCategory,
    onCategorySelected: (GroupCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRateioColors.current
    Column(modifier = modifier) {
        FieldLabel(text = "Category")
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GroupCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    label = { Text(text = category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.paperRaised,
                        labelColor = colors.ink,
                        selectedContainerColor = colors.brandInk,
                        selectedLabelColor = colors.onBrand,
                    ),
                )
            }
        }
    }
}

/** The prototype's `.field label`: a small, uppercase label in `--ink-soft`. Shared by all three fields. */
@Composable
internal fun FieldLabel(text: String) {
    val colors = LocalRateioColors.current
    Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.inkSoft)
}
