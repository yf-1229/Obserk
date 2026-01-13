package com.obserk

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obserk.ui.HomeScreen
import com.obserk.ui.HomeViewModel
import com.obserk.ui.LogScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObserkApp(viewModel: HomeViewModel = viewModel()) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetContent = {
            LogScreen(uiState = uiState, viewModel = viewModel)
        },
        modifier = Modifier.safeDrawingPadding(),
    ) { innerPadding ->
        HomeScreen(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview
@Composable
fun ObserkAppPreview() {
    ObserkApp()
}
