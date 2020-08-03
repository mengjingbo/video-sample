## 前言
ExoPlayer是Google开源的一款Android应用程序级的媒体播放器。它提供了Android MediaPlayer API的替代方法，可以在本地和Internet上播放音频和视频。ExoPlayer支持Android MediaPlayer API当前不支持的功能，包括DASH和SmoothStreaming自适应播放。与MediaPlayer API不同，ExoPlayer易于自定义和扩展。这里主要使用 ***ExoPlayer*** + ***AndroidVideoCache*** 实现边播放边缓存。下面点击可查看对应库和文档。

>- [ExoPlayer](https://github.c/ooogle/ExoPlayer)
>- [ExoPlayer Javadoc](https://exoplayer.dev/)
>- [ExoPlayer Release](https://github.com/google/ExoPlayer/blob/release-v2/RELEASENOTES.md)
>- [AndroidVideoCache](https://github.com/danikula/AndroidVideoCache)

---

## 效果
![在这里插入图片描述](https://github.com/mengjingbo/video-sample/blob/master/screenshot/20200731202822289.gif)

## ExoPlayer使用
添加ExoPlayer与AndroidVideoCache依赖，我这边使用的是ExoPlayer v2.10.5和AndroidVideoCache v2.7.1版本，可根据自身需求升级或降级。

```kotlin
// ExoPlayer
implementation 'com.google.android.exoplayer:exoplayer-core:2.10.5'
implementation 'com.google.android.exoplayer:exoplayer-ui:2.10.5'
// AndroidVideoCache
implementation 'com.danikula:videocache:2.7.1'
```
依赖添加完成后在Layout中使用ExoPlayer库中的PlayerView组件。

```kotlin
    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <com.google.android.exoplayer2.ui.PlayerView
            android:id="@+id/media_player_view"
            android:layout_width="0dp"
            android:layout_height="0dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <androidx.core.widget.ContentLoadingProgressBar
            android:id="@+id/media_video_progress"
            style="?android:attr/progressBarStyleLarge"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:indeterminateTint="@color/colorAccent"
            android:indeterminateTintMode="src_atop"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
```

具体使用，使用了DataBinding，不明白的朋友可在评论区评论，尽量及时给你回复。

```kotlin
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
     * 播放视频
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
     * 视频播放事件监听
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
```

## MediaPlayerManager定义
主要使用AndroidVideoCache中的HttpProxyCacheServer来缓存数据资源，使用ExoPlayer中的DefaultDataSourceFactory来决定数据加载策略。

```kotlin
class MediaPlayerManager {

    private var mUserAgent = this.javaClass.simpleName

    // 视频加载代理
    @Volatile
    private var mProxyCacheServer: HttpProxyCacheServer? = null

    companion object{

        private const val DISK_CACHE_DIR_NAME = "Video"

        @Volatile
        private var INSTANCES: MediaPlayerManager? = null

        fun getDefault(): MediaPlayerManager = INSTANCES ?: synchronized(this){ MediaPlayerManager().also  { INSTANCES = it }}
    }

    fun init(context: Context, userAgent: String) {
        mProxyCacheServer = createProxyCacheServer(context)
        mUserAgent = Util.getUserAgent(context, userAgent)
    }

    /**
     * 将传入的uri构建为一个规媒体资源
     *
     * DashMediaSource         DASH.
     * SsMediaSource           SmoothStreaming.
     * HlsMediaSource          HLS.
     * ProgressiveMediaSource  常规媒体文件.
     *
     * @return 返回一个常规媒体资源
     */
    fun buildDataSource(context: Context, uri: String): MediaSource {
        // 构建一个默认的Http数据资源处理工厂
        val mHttpDataSourceFactory = DefaultHttpDataSourceFactory(mUserAgent)
        // DefaultDataSourceFactory决定数据加载模式，是从网络加载还是本地缓存加载
        val mDataSourceFactory = DefaultDataSourceFactory(context, mHttpDataSourceFactory)
        // AndroidVideoCache库不支持DASH, SS(Smooth Streaming：平滑流媒体，如直播流), HLS数据格式，所以这里使用一个常见媒体转换数据资源工厂
        return ProgressiveMediaSource.Factory(mDataSourceFactory).createMediaSource(Uri.parse(getProxyUrl(uri)))
    }

    /**
     * 创建视频加载代理
     */
    private fun createProxyCacheServer(context: Context): HttpProxyCacheServer {
        return HttpProxyCacheServer.Builder(context)
            .cacheDirectory(getDiskCacheDirectory(context)) // 设置磁盘存储地址
            .maxCacheSize(1024 * 1024 * 1024)     // 设置可存储1G资源
            .build()
    }

    /**
     * 获取代理地址
     */
    fun getProxyUrl(url: String): String? = mProxyCacheServer?.getProxyUrl(url)

    /**
     * 是否缓存
     * @return true:已经缓存
     */
    fun isCached(url: String) = mProxyCacheServer?.isCached(url) ?: false

    /**
     * 视频磁盘缓存地址
     */
    @SuppressLint("SdCardPath")
    fun getDiskCacheDirectory(context: Context): File {
        var cacheDir: File? = null
        if (Environment.MEDIA_MOUNTED == getExternalStorageState()) {
            cacheDir = getExternalCacheDir(context)
        }
        if (cacheDir == null) {
            cacheDir = context.cacheDir
        }
        if (cacheDir == null) {
            val cacheDirPath = "/data/data/${context.packageName}/cache/"
            cacheDir = File(cacheDirPath)
        }
        return File(cacheDir, DISK_CACHE_DIR_NAME)
    }

    private fun getExternalStorageState(): String {
        return try {
            Environment.getExternalStorageState()
        } catch (e: NullPointerException) {
            ""
        }
    }

    private fun getExternalCacheDir(context: Context): File? {
        val cacheDir = context.getExternalFilesDir("Cache")
        if (!cacheDir?.exists()!!) {
            if (!cacheDir.mkdirs()) {
                return null
            }
        }
        return cacheDir
    }

    /**
     * 删除所有视频缓存
     */
    @Throws(IOException::class)
    fun deleteAllCache(context: Context) {
        val mFile = getDiskCacheDirectory(context)
        if (!mFile.exists()) return
        val mFiles = mFile.listFiles()
        if (!mFiles.isNullOrEmpty() && mFiles.isNotEmpty()) {
            mFiles.forEach {
                deleteVideoCache(it)
            }
        }
    }

    /**
     * 删除视频缓存
     */
    @Throws(IOException::class)
    private fun deleteVideoCache(file: File) {
        if (file.isFile && file.exists()) {
            val isDeleted = file.delete()
            Log.e(javaClass.simpleName, "删除视频缓存：${file.path}\t删除状态：$isDeleted")
        }
    }

    /**
     * 获取磁盘缓存的数据大小，单位：KB
     */
    fun getDiskCacheSize(context: Context): Long {
        val file = getDiskCacheDirectory(context)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) getFileSizes(file) else getFileSize(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return blockSize
    }

    private fun getFileSizes(file: File): Long {
        var size = 0L
        file.listFiles()?.forEach {
            if (it.isDirectory) {
                size += getFileSizes(it)
            } else {
                try {
                    size += getFileSize(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return size
    }

    private fun getFileSize(file: File): Long {
        var size = 0L
        if (file.exists()) {
            FileInputStream(file).use {
                size = it.available().toLong()
            }
        }
        return size
    }
}
```
## Tips: 
- MediaPlayerManager 类中定义了缓存函数 getDiskCacheDirectory 
- 缓存目标默认地址为：/storage/emulated/0/Android/data/应用包名/files/Cache/Video/目标文件

对Android本地磁盘有兴趣的可以看看我这篇：[Android存储路径探索](https://blog.csdn.net/mjb00000/article/details/90117016)
