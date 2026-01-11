package com.obserk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    // navigationType: NavigationType,
    // contentType: ObserkContentType,
    onCardPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // val uiState by homeViewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        shape = MaterialTheme.shapes.medium,
        // colors = MaterialTheme.colorScheme.surfaceVariant,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // TODO: change with status
        border = CardDefaults.outlinedCardBorder(),
        onClick = { onCardPressed() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hi there!",
                style = MaterialTheme.typography.titleLarge
            )

        }
    }
}

