package com.example.onlyTouch;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.isseiaoki.simplecropview.CropImageView;

import java.io.IOException;


public class DialogPicture extends DialogFragment {

    public DialogPicture() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new Dialog(getActivity());
        // タイトル非表示
        //dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        // フルスクリーン
        //dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        //タグで生成するダイアログを識別
        switch (getTag()) {

            case "Trimming":
                trimming(dialog);
                break;

            default:
                break;
        }

        return dialog;
    }

    /**
     * トリミング選択画面
     */
    private void trimming(final Dialog dialog) {
        //レイアウトファイル読み込み
        dialog.setContentView(R.layout.dialog_select_trimming);

        //ダイアログ位置／サイズ設定
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.width = metrics.widthPixels;         //画面サイズ
        lp.height = metrics.heightPixels;
        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;            //位置=画面下部
        dialog.getWindow().setAttributes(lp);

        //指定画像のURIを取得
        Bundle b = getArguments();
        Uri uri = (Uri)b.get("uri");

        Bitmap bmp = null;
        try {
            TrimmingPictureActivity activity = (TrimmingPictureActivity) getActivity();
            bmp = activity.getBitmapFromUri(uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //トリミングあり画像を設定
        final CropImageView crop = dialog.findViewById(R.id.cropImageView);
        crop.setImageBitmap(bmp);

        final ImageView trimming_image = dialog.findViewById(R.id.trimming_image);
        crop.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("test", "ACTION_DOWN 2");
                        break;
                    case MotionEvent.ACTION_UP:
                        //タッチが離れたとき、その時のトリミング結果を表示する。
                        Bitmap bitmap = crop.getCroppedBitmap();
                        trimming_image.setImageBitmap(bitmap);
                        Log.i("test", "ACTION_UP 2");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.i("test", "ACTION_MOVE 2");
                        break;
                }
                return false;
            }
        });

        //生成時点のトリミング結果を表示
        Bitmap bitmap = crop.getCroppedBitmap();
        trimming_image.setImageBitmap(bitmap);

        //アニメーションを設定
        //dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        //呼び出し元のアクティビティを取得
        final TrimmingPictureActivity activity = (TrimmingPictureActivity) getActivity();

        //OK
        dialog.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //トリミングした画像で流体生成へ
                activity.trimming_completed(((BitmapDrawable)trimming_image.getDrawable()).getBitmap());
                dismiss();
            }
        });
        //other
        dialog.findViewById(R.id.other).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.trimming_return();
                dismiss();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        //other押下時と同じ動作をさせる
        TrimmingPictureActivity activity = (TrimmingPictureActivity) getActivity();
        activity.trimming_return();
    }
}
