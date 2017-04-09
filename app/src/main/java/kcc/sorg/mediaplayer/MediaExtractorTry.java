package kcc.sorg.mediaplayer;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

/**
 * Created by ford-pro2 on 15/12/25.
 */
public class MediaExtractorTry extends AbstractTry{
    private   MediaExtractor mMediaExtractor = null;
    private String mMediaPath = null;
    public MediaExtractorTry(Context context) {
        super(context);
        init();
    }
    private void init(){
        mMediaExtractor = new MediaExtractor();
    }

    public void setDataSource(String path){
        mMediaPath = path;
    }

    public void start(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Log.e(getTAG(),"SDK version < 16");
            return;
        }
        if (mMediaPath == null){
            Log.e(getTAG(),"Media Path not set");
            return;
        }
        try {
            mMediaExtractor.setDataSource(mMediaPath);
            int numTracks = mMediaExtractor.getTrackCount();
            int numCodec = MediaCodecList.getCodecCount();
            MediaCodecInfo info;
            for ( int j = 0; j < numCodec; ++j){
                info = MediaCodecList.getCodecInfoAt(j);
                Log.d(getTAG(), "Codec  info "+ j+ " is "+info.getName());
                info.isEncoder();
                for(String type: info.getSupportedTypes()){
                    Log.d(getTAG(),"Support Type: "+type);
                }
            }
            for (int i = 0; i < numTracks; ++i){
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(getTAG(),"find mime type : "+ mime);

//                list.findDecoderForFormat(format);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void release(){
        mMediaExtractor.release();
        mMediaExtractor = null;
    }

/*

 int numTracks = extractor.getTrackCount();
 for (int i = 0; i < numTracks; ++i) {
   MediaFormat format = extractor.getTrackFormat(i);
   String mime = format.getString(MediaFormat.KEY_MIME);
   if (weAreInterestedInThisTrack) {
     extractor.selectTrack(i);
   }
 }
 ByteBuffer inputBuffer = ByteBuffer.allocate(...)
 while (extractor.readSampleData(inputBuffer, ...) >= 0) {
   int trackIndex = extractor.getSampleTrackIndex();
   long presentationTimeUs = extractor.getSampleTime();
   ...
   extractor.advance();
 }

 extractor.release();
 extractor = null;
 */
}
