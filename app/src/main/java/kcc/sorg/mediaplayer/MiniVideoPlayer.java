package kcc.sorg.mediaplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ford-pro2 on 15/12/25.
 */
public class MiniVideoPlayer {

    private MediaCodec mVCodec = null;
    private MediaCodec mACodec = null;
    private String mPath = null;
    private Context mContext = null;
    private Surface mOutputSurface = null;
    private MediaExtractor mExtractor = null;
    private String TAG = "MiniVideoPlayer";

    private int mAudioTrack = -1;
    private int mVideoTrack = -1;
    private String mVideoMIME = null;
    private String mAudioMIME = null;

    private ReadThread mReader = null;
    private boolean mInputDone = false;
    private boolean mOutputDone = false;
    private LinkedBlockingQueue<CustomByteBuffer> mVideoQueue = null;

    public MiniVideoPlayer(Context context) {
        mContext = context;
        mExtractor = new MediaExtractor();

    }

    public void setPath(String path) {
        mPath = path;
    }

    public void setSurface(SurfaceTexture texture) {
        mOutputSurface = new Surface(texture);
    }

    public void prepare() {
        try {
            mExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "prepare failed, cannot found mPath: " + mPath);
            return;
        }
        int numTrack = mExtractor.getTrackCount();
        for (int i = 0; i < numTrack; ++i) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mediatype = format.getString(MediaFormat.KEY_MIME);
            if (mediatype.startsWith("video")) {
                mVideoTrack = i;
                mVideoMIME = mediatype;
                mVCodec = createCodecFrom(mediatype);
                Log.d(TAG,"MIME:"+mVideoMIME);
                Log.d(TAG,"Height:"+format.getInteger(MediaFormat.KEY_HEIGHT));
                Log.d(TAG,"Width:"+format.getInteger(MediaFormat.KEY_WIDTH));
                Log.d(TAG,"Length:"+format.getLong(MediaFormat.KEY_DURATION));
//                Log.d(TAG,"FPS:"+format.getFloat(MediaFormat.KEY_FRAME_RATE));
//                mOutputSurface.
            } else if (mediatype.startsWith("audio")) {
                mAudioTrack = i;
                mAudioMIME = mediatype;
                mACodec = createCodecFrom(mediatype);
            }
        }
        if (mVCodec != null && mOutputSurface != null) {
            mVCodec.configure(mExtractor.getTrackFormat(mVideoTrack), mOutputSurface, null, 0);
        }
        if (mACodec != null) {
            mACodec.configure(mExtractor.getTrackFormat(mAudioTrack), null, null, 0);
        }
        mVideoQueue = new LinkedBlockingQueue<CustomByteBuffer>();
        mReader = new ReadThread("read thread", mExtractor, mVideoTrack, mVideoQueue);
    }

    public void start() {
//        mReader.start();
        mVCodec.start();
        mExtractor.selectTrack(mVideoTrack);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = mVCodec.getInputBuffers();
        long startMs = System.currentTimeMillis();
        for (; ; ) {
            if (!mInputDone) {
                int inputBufferId = mVCodec.dequeueInputBuffer(10000);
                if (inputBufferId >= 0) {
                    // fill inputBuffers[inputBufferId] with valid data


                    int readcout = 0;
                    readcout = mExtractor.readSampleData(inputBuffers[inputBufferId], 0);
                    if (readcout > 0) {
                        Log.d(TAG, "get input buffer:readcount "+readcout);
                        mVCodec.queueInputBuffer(inputBufferId, 0, readcout, mExtractor.getSampleTime(), MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        mExtractor.advance();
                    } else {
                        Log.d(TAG, "get input buffer end "+readcout);
                        mVCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mInputDone = true;
                    }


                } else {
                    break;
                }
            }
//            int outputBufferId = mVCodec.dequeueOutputBuffer(info, 10000);
            if (!mOutputDone) {
                int decoderStatus = mVCodec.dequeueOutputBuffer(info, 10000);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    Log.d(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mVCodec.getOutputFormat();
                    Log.d(TAG, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    Log.d(TAG, "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                            " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "output EOS: "+info.flags);
                        mOutputDone = true;
                    }
                    while(info.presentationTimeUs/1000 > System.currentTimeMillis()-startMs){
                        try{
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    boolean doRender = (info.size != 0);

                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                    // that the texture will be available before the call returns, so we
                    // need to wait for the onFrameAvailable callback to fire.
                    mVCodec.releaseOutputBuffer(decoderStatus, doRender);
                }
            } else {
                break;
            }
        }
        mVCodec.stop();
        mVCodec.release();
        mExtractor.release();

    }

    public void stop() {

    }

    public void release() {

    }

    public class CustomByteBuffer {
        public ByteBuffer mBuffer;
        public int mCount;
        public long mTime;

        public CustomByteBuffer(ByteBuffer buffer, int count, long time) {
            mBuffer = buffer;
            mCount = count;
            mTime = time;
        }

    }


    private class ReadThread extends Thread {
        private MediaExtractor mME = null;
        private int mTrack = -1;
        private LinkedBlockingQueue<CustomByteBuffer> mQueue = null;
        private boolean read = true;

        public ReadThread(String name, MediaExtractor me, int track, LinkedBlockingQueue<CustomByteBuffer> queue) {
            setName(name);
            mME = me;
            mTrack = track;
            mQueue = queue;
        }

        public void run() {
            //read only video track now, add audio track in the feature
            mME.selectTrack(mTrack);
            ByteBuffer inputBuffer = ByteBuffer.allocate(8 * 1024);
            inputBuffer.clear();
            int readcout = 0;
            while ((readcout = mME.readSampleData(inputBuffer, 0)) >= 0) {
                Log.d(TAG, "read buffer: " + readcout);

                try {
                    mQueue.put(new CustomByteBuffer(inputBuffer, readcout, mME.getSampleTime()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                inputBuffer.clear();
                mME.advance();
            }
            mInputDone = true;
        }


    }

    public MediaCodec createCodecFrom(String mime) {
        int numCodec = MediaCodecList.getCodecCount();
        MediaCodecInfo info;
//        String type = null;
        for (int j = 0; j < numCodec; ++j) {
            info = MediaCodecList.getCodecInfoAt(j);
            if (!info.isEncoder()) {
                for (String type : info.getSupportedTypes()) {
                    if (type.equals(mime)) {
                        try {
                            MediaCodec codec = MediaCodec.createByCodecName(info.getName());
                            return codec;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }


}
