package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {
    
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var closeButton: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        // 隐藏状态栏和导航栏，实现全屏效果
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        
        playerView = findViewById(R.id.player_view)
        closeButton = findViewById(R.id.close_button)
        
        // 获取视频URL
        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl != null) {
            initializePlayer(videoUrl)
        }
        
        // 设置关闭按钮
        closeButton.setOnClickListener {
            finish()
        }
    }
    
    private fun initializePlayer(videoUrl: String) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            
            // 创建媒体项
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            
            // 准备播放器
            exoPlayer.prepare()
            
            // 开始播放
            exoPlayer.playWhenReady = true
        }
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
} 