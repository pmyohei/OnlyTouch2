package com.example.onlyTouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;

public class PaintView extends View {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    private Paint mPaint;
    private Path  mPath;
    private ArrayList<Vec2> mTouchPos = new ArrayList<Vec2>();

    private Bitmap mDrawBmp = null;
    private Canvas mCanvas;

    //■------old
    private PaintTouchManager mPaintTouchManager;
    //ライン情報リスト
    private ArrayList<PaintLineInfo> mPaintLineInfo = new ArrayList<PaintLineInfo>();
    //ペイントタッチ情報リスト(一時リスト)
    private ArrayList<PaintTouchInfo> mTouchList = new ArrayList<PaintTouchInfo>();
    //■------old

    //輪郭点設定マネージャ
    private TouchLineManager  mTouchLineManager;

    //タッチ点情報リスト
    private ArrayList<TouchPointInfo> mTouchPointList = new ArrayList<TouchPointInfo>();

    //ライン情報
    private LineInfo mLineInfo = new LineInfo();

    //ライン識別子
    private int mLineIdentifier = 0;
    private int mLineTopIdx;
    private int mLineLastIdx;

    private int pointIdx = 0;

    //1つ前にタッチされたX座標
    private float preTouchX;

    //1つ前に追加したタッチ座標
    private float preAddX;
    private float preAddY;

    //コンストラクタ
    public PaintView(Context context) {
        this(context, null);
    }

    //コンストラクタ
    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //描画点を保持するためのマネージャー
        mPaintTouchManager = new PaintTouchManager();      //削除予定

        //描画設定
        mPath  = new Path();
        mPaint = new Paint();
        mPaint.setColor(0xFF000000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);

        // 128*128ピクセルの Bitmap 作成
        mDrawBmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);

        // bmpを描画対象として保持するCanvasを作成
        mCanvas = new Canvas(mDrawBmp);

        //1つ前のタッチ座標
        preAddX = 0.0f;
        preAddY = 0.0f;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //bitmapのデータをViewに書き込む
        //(生成元に本クラスをレイアウトに割り当てているため、描画されていることを確認できる)
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //表示されるタイミングで呼び出され、viewの表示領域の値を取得できる。
        //ここで、Bitmapを適切な大きさにして再生成する
        super.onSizeChanged(w,h,oldw,oldh);
        mDrawBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas  = new Canvas(mDrawBmp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //座標取得
        float x = event.getX();
        float y = event.getY();

        //タッチアクションを取得
        int action = event.getAction();

        //-- X座標をずらす必要があるか判定
        //-- ！傾きの無限対策のため、X座標が連続して同じとなる点は、同じにならないようずらす

        //タッチ開始でなければ
        if( action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP ){

            //前回のX座標と同じ位置か
            if( preTouchX == x ){
                //直前の点と一致しないように調整を入れる
                x += 0.001f;
            }
        }

        //タッチ座標生成
        Vec2 pos = new Vec2(x, -y);

        //点追加確認用
        int prePointIdx = pointIdx;

        //-- アクション毎の処理
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //ライン先頭indexとして保持
                mLineTopIdx = pointIdx;
                //インデックスを進める
                pointIdx++;

                //タッチ開始位置に移動
                mPath.moveTo(x, y);
                invalidate();

                //タッチ点追加
                mTouchPointList.add( new TouchPointInfo(pos, MotionEvent.ACTION_DOWN, mLineIdentifier) );
                break;

            case MotionEvent.ACTION_MOVE:

                //タッチムーブした箇所まで、ラインを引く
                mPath.lineTo(x, y);
                invalidate();

                //前回の座標との距離
                //double distance = Math.sqrt((x - preX) * (x - preX) + (y - preY) * (y - preY));
                //if( distance > 50.0 ){

                //前回の座標とある程度距離があるものだけ追加
                if( (Math.abs(x - preAddX) > 30.0) || (Math.abs(y - preAddY) > 50.0) ){

                    //タッチ点追加
                    mTouchPointList.add( new TouchPointInfo(pos, MotionEvent.ACTION_MOVE, mLineIdentifier) );

                    //インデックスを進める
                    pointIdx++;

                    //前回の追加座標を保持
                    preAddX = x;
                    preAddY = y;
                }

                break;

            case MotionEvent.ACTION_UP:
                //ライン終端indexとして保持
                mLineLastIdx = pointIdx;
                //インデックスを進める
                pointIdx++;

                //Canvasを通して、Bitmapに描画する
                mPath.lineTo(x, y);
                mCanvas.drawPath(mPath, mPaint);
                invalidate();

                //タッチ情報追加
                mTouchPointList.add( new TouchPointInfo(pos, MotionEvent.ACTION_UP, mLineIdentifier) );

                //ライン情報追加
                mLineInfo.addLineData( new LineInfo.OneLineData(mLineIdentifier, mLineTopIdx, mLineLastIdx) );
                //ライン識別子更新
                mLineIdentifier++;

                break;
        }

        //点が登録されたら、直前の点のX座標として保持
        if( prePointIdx != pointIdx ){
            preTouchX = x;
        }

        return true;
    }

    //描画したBitmapを返す。
    public Bitmap getBitmap(){
        return mDrawBmp;
    }

    public ArrayList<Vec2> getTouchList(){
        return mTouchPos;
    }

    //タッチ情報を成形する
    public void reqMoldingPaintInfo(){
        //mPaintTouchManager.moldingPaintInfo( mPaintLineInfo );
        //mTouchLineManager.verifyOutLine( mPaintLineInfo );
        //mTouchLineManager.verifyOutLine( mTouchPointList, mLineInfo );

        //描画した点に対して、輪郭点を設定する
        TouchLineManager touchManager = new TouchLineManager( mLineInfo, mTouchPointList );
        touchManager.verifyOutLine();
    }
}
