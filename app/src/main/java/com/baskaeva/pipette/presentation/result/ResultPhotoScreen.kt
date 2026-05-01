package com.baskaeva.pipette.presentation.result

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.baskaeva.pipette.R
import com.baskaeva.pipette.presentation.components.ColorItemCompact
import com.baskaeva.pipette.presentation.components.ColorItemExpanded
import com.baskaeva.pipette.presentation.theme.BackgroundDark
import com.baskaeva.pipette.presentation.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultPhotoScreen(
    imageUri: Uri?,
    colorCount: Int,
    onBack: () -> Unit,
    viewModel: ResultPhotoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val favouritedHexes by viewModel.favouritedHexes.collectAsState()

    LaunchedEffect(imageUri, colorCount) {
        imageUri?.let { viewModel.processImage(context, it, colorCount) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.palette), color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Photo preview
            item {
                imageUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Фотография",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (val state = uiState) {
                is ResultUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.palette_loading),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                is ResultUiState.Success -> {
                    item {
                        Text(
                            text = "Доминирующие цвета (${state.colors.size})",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (colorCount <= 3) {
                        items(state.colors) { colorItem ->
                            ColorItemExpanded(
                                colorItem = colorItem,
                                isFavourited = colorItem.hex in favouritedHexes,
                                onToggleFavourite = { viewModel.toggleFavourite(it) }
                            )
                        }
                    } else {
                        items(state.colors) { colorItem ->
                            ColorItemCompact(
                                colorItem = colorItem,
                                isFavourited = colorItem.hex in favouritedHexes,
                                onToggleFavourite = { viewModel.toggleFavourite(it) }
                            )
                        }
                    }
                }

                is ResultUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚠️ ${state.message}", color = Color(0xFFFF6B6B), fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onBack) { Text(stringResource(R.string.back)) }
                            }
                        }
                    }
                }
            }
        }
    }
}