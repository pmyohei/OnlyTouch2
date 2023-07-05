package com.example.onlyTouch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

/*
 * 画像トリミング画面
 */
public class TrimmingPictureActivity extends AppCompatActivity {

    private MenuActivity.PictureButton select;
    private static final int RESULT_FLUID_DESIGN = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //呼び出し元からデータを取得
        Intent intent = getIntent();
        select = (MenuActivity.PictureButton)intent.getSerializableExtra("PictureButton");
        Bundle b = intent.getExtras();

        //トリミング用ダイアログを生成
        DialogFragment dialog = new DialogPicture();
        dialog.setArguments(b);
        dialog.show(getSupportFragmentManager(), "Trimming");
    }

    //URIから画像を取得
    public Bitmap getBitmapFromUri(Uri uri) throws IOException {

        //ファイルの提供
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();

        Log.i("Image", "w：" + image.getWidth());
        Log.i("Image", "h：" + image.getHeight());

        //image.setWidth();
        //image.setWidth();

        return image;
    }

    /*
     * トリミング確定後処理
     */
    public void trimming_completed(Bitmap image){
        //共通クラスにBitmapを格納
        //(アクティビティ間の画像はサイズが大きいとエラーで落ちるため、共通クラスを介する)
        MyApplication app = (MyApplication)this.getApplication();
        app.setObj(image);
        app.setSelect(select);

        //Intent intent = new Intent(this, FluidPlayActivity.class);
        Intent intent = new Intent(this, CreateFluidWorldMenuActivity.class);
        startActivityForResult(intent, RESULT_FLUID_DESIGN);
    }

    /*
     * トリミング-戻るボタン押下時処理
     */
    public void trimming_return(){
        //再度、画像ギャラリーの表示を行うため、本アクティビティを終了する
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        intent.putExtra("Return", "Gallery");
        finish();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        switch (requestCode) {
            //画像ギャラリー選択結果
            case RESULT_FLUID_DESIGN:
                //戻り値取得成功
                if( (resultCode == RESULT_OK) && (resultData != null) ){

                    String code = resultData.getStringExtra("Return");

                    //Gallery指定なら、画像ギャラリー再表示
                    if (code.equals("Gallery")) {
                        //戻り値設定
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        intent.putExtra("Return", "Gallery");
                        finish();

                        break;
                    }
                }

                //該当なしなら、menuまで戻る
                finish();
                break;

            default:
                break;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        //Log.i("onResume", "onResume");
    }
    @Override
    protected void onPause(){
        super.onPause();
        //Log.i("onPause", "onPause");
    }
    @Override
    protected void onRestart(){
        super.onRestart();
        //Log.i("onRestart", "onRestart");
    }

}
