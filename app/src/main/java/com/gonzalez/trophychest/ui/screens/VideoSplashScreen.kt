package com.gonzalez.trophychest.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.FirebaseManager
import com.gonzalez.trophychest.navigation.Screen

@OptIn(UnstableApi::class)//API MEDIA 3 INSTABLE --------------------------------------
@Composable
fun VideoSplashScreen(navController: NavHostController) {
    val context = LocalContext.current//CNTEXTO ANDROID ---------------------------------
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { //EXOPLAYER
            val videoUri = "android.resource://${context.packageName}/${R.raw.splash_video}"// RUTA DE VIDEO
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true//REPRODUCE EL VIDEO
        }
    }

    DisposableEffect(Unit) { //EFECTO PARA CAMBIAR DE PANTALLA
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) { //ESTADO DE REPRODUCCION
                if (playbackState == Player.STATE_ENDED) { //SI EL VIDEO HA TERMINADO
                    val destination = if (FirebaseManager.currentUser != null) { //REVISA QUE ES USUARIO SEA NULO O NO
                        Screen.Principal.route
                    } else {
                        Screen.Login.route
                    }

                    navController.navigate(destination) { //CAMBIA DE PANTLLA
                        popUpTo("video_splash") { inclusive = true } //INCLUSIVE ELIMINA LA PANTALLA DEL HISTORIAL
                        launchSingleTop = true
                    }
                }
            }
        }

        exoPlayer.addListener(listener)//LISTENER AL RESPRODUCTOR ----------------------------

        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }//CIERRA RECURSO DE VIDEO (REPRODUCTOR)--------------------------------------
    }

    //VISTA TRADICIONAL DENTRO DE CMPOSE
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer //UNIMOS LA VISTA A EL REPRODUCTOR
                useController = false
                //PARAMETROS DEL VIDEO
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
