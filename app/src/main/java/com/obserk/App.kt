package com.obserk

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obserk.ui.HomeScreen
import com.obserk.ui.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObserkApp(viewModel: HomeViewModel = viewModel()) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true // 非表示（Hidden）状態をスキップする = 閉じられない
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetContent = {
            Text(text = "Bottom Sheet", modifier = Modifier.fillMaxSize(), fontSize = 20.sp)
        },
        modifier = Modifier.safeDrawingPadding(),
    ) { innerPadding ->
        HomeScreen(
            uiState = uiState,
            onCardPressed = { uiState.isStudying },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview
@Composable
fun ObserkAppPreview() {
    ObserkApp()
}