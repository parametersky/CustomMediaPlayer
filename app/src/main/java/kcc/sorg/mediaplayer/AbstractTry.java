package kcc.sorg.mediaplayer;

import android.content.Context;

/**
 * Created by ford-pro2 on 15/12/25.
 */
public class AbstractTry {
    private Context mContext;
    private String TAG = "AbstractTry";
    public AbstractTry(Context context){
        mContext = context;
    }
    public String getTAG(){
        return TAG;
    }

}
