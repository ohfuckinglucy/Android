package com.example.visprog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import com.example.visprog.TrackItem as TrackItem

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(LocalContext.current))
//
//            Column {
//                RequestStoragePermission {
//                    viewModel.loadAudioFiles()
//                }
//                MusicPlayerScreen(viewModel)
//            }
//        }
//    }
//}

@Composable
fun MusicPlayerScreen(viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFC9E3FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LazyColumn(
            modifier = Modifier.height(300.dp)
        ) {
            itemsIndexed(viewModel.tracks) { index, track ->
                TrackItem(
                    title = track.displayName,
                    isPlaying = index == viewModel.trackIndex,
                    onClick = { viewModel.playTrack(index) }
                )
            }
        }

        Slider(
            value = viewModel.progress,
            onValueChange = { viewModel.seekTo(it) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF62A2FF),
                activeTrackColor = Color(0xFF84BEFF),
                inactiveTrackColor = Color(0xFFACD6FF)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.previousTrack() }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.Black
                )
            }
            IconButton(onClick = { viewModel.playPause() }) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black
                )
            }
            IconButton(onClick = { viewModel.nextTrack() }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.Black
                )
            }
        }

    }
}

@Composable
fun TrackItem(title: String, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) Color(0xFF99CBFF) else Color(0xFFC6E4FF))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(text = title, color = Color.Black)
    }
}
