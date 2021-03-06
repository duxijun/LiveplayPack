package com.hejunlin.liveplayback.evideoview;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/**
 * Created by chicunxiang on 2018/8/27.
 *
 * @史上最帅无敌创建者 ccx
 * @创建时间 2018/8/27 10:31
 */

public class EVideoView extends FrameLayout {


    /**
     * 由ijkplayer提供，用于播放视频，需要给他传入一个surfaceView
     */
    private             IMediaPlayer mMediaPlayer = null;
    public static final int          VIDEO_START  = 1;
    public static final int          VIDEO_PAUSE  = 2;
    /**
     * 视频文件地址
     */
    private             String       mPath        = "";
    private SurfaceView         surfaceView;
    private VideoPlayerListener listener;
    private Context             mContext;
    private boolean isInitMediaPlay = true;
    private Handler mHandler        = new Handler(Looper.getMainLooper());
    private Uri     mURI;
    private boolean isURi;

    public EVideoView(@NonNull Context context) {
        super(context);
        initVideoView(context);
    }

    public EVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public EVideoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        mContext = context;
        //获取焦点
        setFocusable(true);

    }

    /**
     * 设置视频地址。
     * 根据是否第一次播放视频，做不同的操作。
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        if (TextUtils.equals("", mPath)) {
            //如果是第一次播放视频，那就创建一个新的surfaceView
            mPath = path;
            createSurfaceView();
        } else {
            //否则就直接load
            mPath = path;
            release();
            load();
        }
    }

    /**
     * 设置视频地址。
     * 根据是否第一次播放视频，做不同的操作。
     *
     * @param path the path of the video.
     */
    public void setVideoPath(File path) {
        setVideoPath(path.getAbsolutePath());
    }

    /**
     * 新建一个surfaceview
     */
    private void createSurfaceView() {
        //生成一个新的surface view
        surfaceView = new SurfaceView(mContext);
        surfaceView.getHolder().addCallback(new SurfaceCallback());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT, Gravity.CENTER);
        surfaceView.setLayoutParams(layoutParams);
        // 如果有两个surface，则会冲突，需要设置永远置顶
        surfaceView.setZOrderOnTop(true);
        this.addView(surfaceView);
    }

    public int getVideoWidth() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getVideoWidth();
        }
        return 0;
    }

    public int getVideoHeight() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getVideoHeight();
        }
        return 0;
    }

    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();

        } else {
            return false;
        }

    }


    private void postProgress() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 如果停下，就不再更新进度
                if (isPlaying()) {
                    return;
                }
                if (mOnPlayStatusChangeListener != null) {
                    mOnPlayStatusChangeListener.onProgressChange(getCurrentPosition(), getDuration());
                }
                mHandler.postDelayed(this, 15);
            }
        }, 15);
    }

    public void setVideoPath(Uri parse) {
        isURi = true;
        //如果是第一次播放视频，那就创建一个新的surfaceView
        this.mURI = parse;
        createSurfaceView();

    }


    /**
     * surfaceView的监听器
     */
    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 其实这里只用对 MediaPlayer.setDisplay(surfaceView.getHolder());
            // 进行重新赋值就可以了，如果是重新创建，将会重新播放
            // 但是act被回收的时候，MediaPlayer可能会出现null,所以直接调用全部方法
            load();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // surfaceview创建成功后，加载视频
            start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // 因为每次变小都会将surface缩小。所以需要进行暂停，当回来的时候，会重新调用load方法。
            pause();
        }
    }

    /**
     * 加载视频
     */
    private void load() {
        // 每次都要重新创建IMediaPlayer
        createPlayer();
        // 理论上说是不允许修改多次的路径
        String dataSource = mMediaPlayer.getDataSource();
        try {
            if (dataSource == null) {
                if (isURi) {
                    mMediaPlayer.setDataSource(mContext, mURI, new HashMap<String, String>());
                } else {
                    mMediaPlayer.setDataSource(mPath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 给mediaPlayer设置视图
        mMediaPlayer.setDisplay(surfaceView.getHolder());
        if (isInitMediaPlay) {
            mMediaPlayer.prepareAsync();
            isInitMediaPlay = false;
        }
    }


    /**
     * 创建一个新的player，防止多次初始化，并且act被回收后，导致null问题.
     */
    private void createPlayer() {
        if (mMediaPlayer == null) {
//            IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
            // 开启硬解码
//            IjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            mMediaPlayer = new IjkMediaPlayer();
            mMediaPlayer.setScreenOnWhilePlaying(true);
            try {

                Method method = Class.forName("tv.danmaku.ijk.media.player.IjkMediaPlayer")
                        .getDeclaredMethod("setOption", int.class, String.class, long.class);
                // 硬编码
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);

                // 设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
                // 是否缓冲
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1);
                // 设置缓冲区,单位是kb
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 8 * 8 * 2 * 1024);
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 0);
//                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"max-buffer-size",maxCacheSize);

//                method.invoke(mMediaPlayer,IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 20000);
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "buffer_size", 8 * 8 * 2 * 1024);
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 1);  // 无限读
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
//                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 10240L);
                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L);
//  关闭播放器缓冲，这个必须关闭，否则会出现播放一段时间后，一直卡主，控制台打印 FFP_MSG_BUFFERING_START
//                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0L);
//                method.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L);

                // 另一个反射
//
                Method method1 = Class.forName("tv.danmaku.ijk.media.player.IjkMediaPlayer")
                        .getDeclaredMethod("setOption", int.class, String.class, String.class);
                method1.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
                method1.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp");
                method1.invoke(mMediaPlayer, IjkMediaPlayer.OPT_CATEGORY_FORMAT, "allowed_media_types", "video");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            mMediaPlayer.setLooping(false);
            isInitMediaPlay = true;
            if (listener != null) {
                mMediaPlayer.setOnPreparedListener(listener);
                mMediaPlayer.setOnInfoListener(listener);
                mMediaPlayer.setOnSeekCompleteListener(listener);
                mMediaPlayer.setOnBufferingUpdateListener(listener);
                mMediaPlayer.setOnErrorListener(listener);
                mMediaPlayer.setOnVideoSizeChangedListener(listener);
                mMediaPlayer.setOnTimedTextListener(listener);
                mMediaPlayer.setOnCompletionListener(listener);
            }

        }
    }


    public void setListener(VideoPlayerListener listener) {
        this.listener = listener;
    }

    /**
     * -------======--------- 下面封装了一下控制视频的方法
     */

    public void start() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            postProgress();
            if (mOnPlayStatusChangeListener != null) {
                mOnPlayStatusChangeListener.onStatusChange(VIDEO_START);
            }
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.setDisplay(null);
            mMediaPlayer.release();
            mMediaPlayer = null;
            surfaceView = null;
            this.removeAllViews();
        }
    }


    public void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            if (mOnPlayStatusChangeListener != null) {
                mOnPlayStatusChangeListener.onStatusChange(VIDEO_PAUSE);
            }
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }


    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
    }


    public long getDuration() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public void toggle() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                pause();
            } else {
                start();
            }
        }
    }


    public long getCurrentPosition() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }


    public void seekTo(long l) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(l);
        }
    }

    private onPlayStatusChangeListener mOnPlayStatusChangeListener;

    public void setOnPlayStatusChangeListener(onPlayStatusChangeListener onPlayStatusChangeListener) {
        mOnPlayStatusChangeListener = onPlayStatusChangeListener;
    }

    public interface onPlayStatusChangeListener {
        void onStatusChange(int status);

        void onProgressChange(long progress, long max);
    }

}
