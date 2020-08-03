package com.video.sample

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.Util
import com.video.sample.databinding.ActivityMainBinding
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MEDIA_URI: String = "http://wxsnsdy.tc.qq.com/105/20210/snsdyvideodownload?filekey=30280201010421301f0201690402534804102ca905ce620b1241b726bc41dcff44e00204012882540400&bizid=1023&hy=SH&fileparam=302c020101042530230204136ffd93020457e3c4ff02024ef202031e8d7f02030f42400204045a320a0201000400"
    }

    private lateinit var binding: ActivityMainBinding

    private var mStartPosition = 0
    private var mPlaybackProgressPosition = 0L

    // 自适应音轨
    private val mTrackSelectionFactory by lazy { AdaptiveTrackSelection.Factory() }

    // 创建播放器
    private val mPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(this),
            DefaultTrackSelector(mTrackSelectionFactory),
            DefaultLoadControl()
        )
    }

    private val mOnVideoEventListener by lazy { OnVideoEventListener() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.data = this
        binding.executePendingBindings()
        initView()
    }

    private fun initView() {
        // 将播放器连接到视图
        binding.mediaPlayerView.player = mPlayer
        mPlayer?.addListener(mOnVideoEventListener)
    }

    override fun onResume() {
        super.onResume()
        updatePlaybackProgress()
        playbackVideo()
    }

    override fun onPause() {
        super.onPause()
        mPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPlayer?.removeListener(mOnVideoEventListener)
    }

    /**
     * 播放广告视频
     */
    private fun playbackVideo() {
        clearPlaybackProgress()
        if (mPlayer != null) {
            // 设置播放进度
            val mHaveStartPosition = mStartPosition != C.INDEX_UNSET
            if (mHaveStartPosition){
                mPlayer.seekTo(mStartPosition, mPlaybackProgressPosition)
            }
            @C.ContentType
            val mMediaSourceType = Util.inferContentType(Uri.parse(MEDIA_URI), null)
            if(mMediaSourceType == C.TYPE_OTHER) {
                // 获取构建后的媒体资源
                val mMediaSource = MediaPlayerManager.getDefault().buildDataSource(this, MEDIA_URI)
                // 将媒体资源设置给播放器
                mPlayer.prepare(mMediaSource, !mHaveStartPosition, true)
                // 是否是自动播放
                mPlayer.playWhenReady = true
            } else {
                Log.e(javaClass.simpleName, "播放媒体资源出错，类型不支持，错误类型：$mMediaSourceType")
                return
            }
        }
    }

    /**
     * 视频播放时间监听
     */
    private inner class OnVideoEventListener: Player.EventListener{

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == ExoPlayer.STATE_BUFFERING){
                binding.mediaVideoProgress.show()
            }else {
                binding.mediaVideoProgress.hide()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val mPlayerChange = !isPlaying && mPlayer?.playbackState == Player.STATE_ENDED
            if (mPlayerChange) clearPlaybackProgress()
            if (mPlayerChange) {
                Toast.makeText(this@MainActivity, "播放完成！", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            binding.mediaVideoProgress.hide()
            clearPlaybackProgress()
            Toast.makeText(this@MainActivity, "播放出现错，${error?.message}！", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 更新播放进度
     */
    private fun updatePlaybackProgress() {
        mStartPosition = mPlayer?.currentWindowIndex ?: 0
        mPlaybackProgressPosition = max(0, (mPlayer?.contentPosition ?: 0))
    }

    /**
     * 清除播放位置
     */
    private fun clearPlaybackProgress() {
        mStartPosition = C.INDEX_UNSET
        mPlaybackProgressPosition = C.TIME_UNSET
        mPlayer?.stop()
    }
}