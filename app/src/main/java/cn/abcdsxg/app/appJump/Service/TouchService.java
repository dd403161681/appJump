package cn.abcdsxg.app.appJump.Service;


import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.Toast;
import java.util.List;
import cn.abcdsxg.app.appJump.Data.Adapter.PanelItemAdapter;
import cn.abcdsxg.app.appJump.Data.Constant;
import cn.abcdsxg.app.appJump.Data.Utils.SpUtil;
import cn.abcdsxg.app.appJump.Data.Utils.SuUtils;
import cn.abcdsxg.app.appJump.Data.Utils.ToolUtils;
import cn.abcdsxg.app.appJump.Data.greenDao.AppInfo;
import cn.abcdsxg.app.appJump.Data.greenDao.DBManager;
import cn.abcdsxg.app.appJump.Data.greenDao.LancherInfo;
import cn.abcdsxg.app.appJump.R;
import cn.abcdsxg.app.appJump.View.TouchToSelectView;

/**
 * Author : 时小光
 * Email  : abcdsxg@gmail.com
 * Blog   : http://www.abcdsxg.cn
 * Date   : 16-11-14 14:10
 */

public class TouchService extends Service implements View.OnTouchListener{

    private int mStartX,mCurrentX,mCurrentY;
    private int mScreenHeight;
    //mFloatView的宽高属性
    private int mViewWidth,mViewHeight;

    //触发显示touchView需要移动的最小距离
    private int mMinDistant=20;

    private WindowManager mWindowManager;

    //透明的侧边view用于触发显示touchView
    private View mFloatView;
    //滑动时显示的touchView
    private TouchToSelectView mTouchToSelectView;

    //是否已经显示达到触摸条件时出现view
    private boolean hasShowTouchView;
    //当前是否选中某个item
    private boolean hasSelected;
    //选中item的位置
    private int pos;
    private GridView gridView;
    private boolean showBound;
    private int[] location;
    //由于权限问题启动失败
    private boolean failStart;

    private void initParams(){
        getScreenParams();
    }

    private void getScreenParams() {
        DisplayMetrics metrics=getResources().getDisplayMetrics();
        mScreenHeight=metrics.heightPixels;
    }

    private int getDefaultViewWidht(){
        String minDistantString=SpUtil.getStringSp(this,Constant.MINDISTANT);
        if(minDistantString!=null) {
            mMinDistant = Integer.valueOf(minDistantString);
            return mMinDistant;
        }
        return 20;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        initParams();
        createView();
        initWinodwManager();
        showFloatView();
    }


    private void createView(){
        mFloatView=new View(this);
        ViewGroup.LayoutParams params=new ViewGroup.LayoutParams(mViewWidth,mViewHeight);
        mFloatView.setLayoutParams(params);
        mFloatView.setBackgroundColor(Color.TRANSPARENT);
        mFloatView.setOnTouchListener(this);

        mTouchToSelectView=new TouchToSelectView(this);
        mTouchToSelectView.setOnTouchListener(this);
        //初始化子view相关变量
        View view=mTouchToSelectView.getChildAt(0);
        gridView= (GridView) view.findViewById(R.id.gridView);
        PanelItemAdapter adapter=new PanelItemAdapter(this,ToolUtils.getLancherInfoList(this),true);
        gridView.setAdapter(adapter);
        location = new int[2];
    }

    private void initWinodwManager() {
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        getFloatViewParams();
        if(showBound){
            mFloatView.setBackgroundColor(Color.RED);
        }else{
            mFloatView.setBackgroundColor(Color.TRANSPARENT);
        }
        try {
            //try catch beacuse some bitch chinses System vendors
            mWindowManager.addView(mFloatView, getparams());
            getTouchViewParams();
            mWindowManager.addView(mTouchToSelectView, getparams());
        }catch (Exception e){
            Toast.makeText(this,R.string.window_permission_tip,Toast.LENGTH_SHORT).show();
        }
    }
    private void updateWindow(){
        if(showBound){
            mFloatView.setBackgroundColor(Color.RED);
        }else{
            mFloatView.setBackgroundColor(Color.TRANSPARENT);
        }
        try {
            //try catch beacuse some bitch chinses System vendors
            getFloatViewParams();
            mWindowManager.updateViewLayout(mFloatView,getparams());
            getTouchViewParams();
            mWindowManager.updateViewLayout(mTouchToSelectView,getparams());
        }catch (Exception e){
            Toast.makeText(this, R.string.window_permission_tip,Toast.LENGTH_SHORT).show();
        }
    }



    private void showFloatView(){
        mTouchToSelectView.setVisibility(View.GONE);
        mFloatView.setVisibility(View.VISIBLE);
    }

    private void showTouchView(){
        mTouchToSelectView.setVisibility(View.VISIBLE);
        mFloatView.setVisibility(View.GONE);

    }
    /**
     * addView前设置FloatView的宽高
     */
    private void getFloatViewParams() {
        mViewWidth= getDefaultViewWidht() ==-1 ? 20 : getDefaultViewWidht();
        mViewHeight=mScreenHeight/2;
    }

    /**
     * 第二次addView前修改FloatView的宽高参数为TouchView的宽高
     */
    private void getTouchViewParams() {
        mViewWidth=WindowManager.LayoutParams.MATCH_PARENT;
        mViewHeight=WindowManager.LayoutParams.MATCH_PARENT;
    }

    private void removeAllWindowView() {
        try{
            mWindowManager.removeView(mTouchToSelectView);
            mWindowManager.removeView(mFloatView);
            failStart=false;
        }catch (Exception e){
            failStart=true;
            Toast.makeText(getBaseContext(),getString(R.string.window_permission_tip),Toast.LENGTH_SHORT).show();
        }
    }

    private WindowManager.LayoutParams getparams() {
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        String slidePos = SpUtil.getStringSp(getApplicationContext(), Constant.SLIDEPOS);
        switch (slidePos){
            case "0":
                wmParams.gravity= Gravity.TOP | Gravity.START;
                break;
            case "1":
                wmParams.gravity= Gravity.TOP | Gravity.END;
                break;
            case "2":
                wmParams.gravity= Gravity.BOTTOM | Gravity.START;
                break;
            case "3":
                wmParams.gravity= Gravity.BOTTOM | Gravity.END;
                break;
            default:
                wmParams.gravity= Gravity.TOP | Gravity.END;
        }
        wmParams.format=PixelFormat.RGBA_8888;
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        //wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置可以显示在状态栏上
        wmParams.flags =  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR|
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        //这里设置的是默认的FloatView的宽高参数
        wmParams.width = mViewWidth;
        wmParams.height = mViewHeight;
        return wmParams;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean startTouchService=SpUtil.getBooleanSp(this,Constant.TOUCHSERVICE);
        if(!startTouchService){
            stopSelf();
        }
        showBound = SpUtil.getBooleanSp(this, Constant.SHOWBOUND);
        updateWindow();
        return START_REDELIVER_INTENT;
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action=motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartX = (int) motionEvent.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentX = (int) motionEvent.getRawX();
                mCurrentY = (int) motionEvent.getRawY();
                if (hasShowTouchView) {
                    mTouchToSelectView.setmCurrentX(mCurrentX);
                    mTouchToSelectView.setmCurrentY(mCurrentY);
                    //清除之前的状态
                    hasSelected=false;
                    dealCircleColor();
                }else if (mCurrentX + mMinDistant < mStartX
                        || mStartX + mMinDistant > mCurrentX) {
                    showTouchView();
                    vibrate(50);
                    mTouchToSelectView.notifyData();
                    hasShowTouchView = true;
                }

                break;
            case MotionEvent.ACTION_UP:
                hasShowTouchView=false;
                showFloatView();
                if(hasSelected){
                    DBManager dbManager=DBManager.getInstance();
                    String panel= SpUtil.getStringSp(this, Constant.PANEL);
                    List<LancherInfo> infos=dbManager.queryLancherInfoByPos(pos,panel);
                    if(infos.size()==0){
                        Toast.makeText(this, R.string.position_null,Toast.LENGTH_SHORT).show();
                    }else {
                        LancherInfo info=infos.get(0);
                        try {
                            Intent intent = Intent.parseUri(info.getIntent(), 0);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.lancher_fail, Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }

                }
                break;
        }
        return false;
    }

    public  void vibrate(long milliseconds) {
        boolean isVibrate=SpUtil.getBooleanSp(this,Constant.SHOCK);
        if(isVibrate) {
            Vibrator vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
            vib.vibrate(milliseconds);
        }
    }

    private void dealCircleColor() {
        for (int i = 0; i < PanelItemAdapter.EmptyListItemSize; i++) {
            View childView=gridView.getChildAt(i);
            childView.getLocationInWindow(location);
            int childX=location[0];
            int childY=location[1];
            if(mCurrentX>childX && mCurrentX-childX<150
                &&
                mCurrentY>childY && mCurrentY-childY<150){
                pos=i;
                mTouchToSelectView.changeCirlceColor(true,i);
                //有符合条件的坐标，通知重绘并直接跳出
                hasSelected=true;
                break;
            }
        }
        //没有符合条件的坐标
        if(!hasSelected){
            mTouchToSelectView.changeCirlceColor(false,0);
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        removeAllWindowView();
        if(!failStart) {
            startService(new Intent(this, TouchService.class));
        }
    }
}
