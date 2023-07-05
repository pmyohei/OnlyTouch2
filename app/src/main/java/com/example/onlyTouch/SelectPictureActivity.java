package com.example.onlyTouch;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

/*
 * 画像選択画面
 */
public class SelectPictureActivity extends AppCompatActivity {

    private MenuActivity.PictureButton select;
    private static final int RESULT_PICK_IMAGEFILE = 1000;
    private static final int RESULT_TRIMMING = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //押下されたボタンを取得
        Intent intent = getIntent();
        select = (MenuActivity.PictureButton)intent.getSerializableExtra("PictureButton");

        //端末の画像ギャラリーを表示
        this.displayGallery();
    }

    /**
     * 画像ギャラリーを表示する。
     */
    private void displayGallery(){
        Intent doc_intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        doc_intent.addCategory(Intent.CATEGORY_OPENABLE);              //画像ファイルなどの開くことができるドキュメントのみを表示
        doc_intent.setType("image/*");                                  //画像の内、MIMEデータタイプのドキュメントのみを表示

        startActivityForResult(doc_intent, RESULT_PICK_IMAGEFILE);
    }

    /**
     * 画像選択完了時にコールされる
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        Log.i("onActivityResult", "onActivityResult check");

        switch (requestCode){
            //画像ギャラリー選択結果
            case RESULT_PICK_IMAGEFILE:
                if( (resultCode != RESULT_OK) || (resultData == null) ){
                    //未選択なら、本アクティビティ終了(menuへ戻る)
                    finish();
                    break;
                }

                //画像パス取得
                Uri uri = resultData.getData();
                Log.i("URI", uri.toString());

                //ここで、トリミング選択ダイアログをコールできないため、
                //ダイアログコール用のアクティビティを介する。
                Intent intent = new Intent(this, TrimmingPictureActivity.class);
                Bundle b = new Bundle();
                b.putParcelable("uri", uri);
                intent.putExtras(b);
                intent.putExtra("PictureButton", select);
                startActivityForResult(intent, RESULT_TRIMMING);

                break;

            //トリミング結果
            case RESULT_TRIMMING:
                //戻り値取得成功
                if( (resultCode == RESULT_OK) && (resultData != null) ){

                    String code = resultData.getStringExtra("Return");
                    //Gallery指定なら、画像ギャラリー再表示
                    if (code.equals("Gallery")) {
                        //トリミング選択時にotherが選択された場合、再度、画像ギャラリーを表示
                        displayGallery();
                        break;
                    }
                }

                //OK未選択なら、本アクティビティ終了(menuへ戻る)
                finish();

                break;

            default:
                break;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }
    @Override
    protected void onPause(){
        super.onPause();
    }
}
