package com.kandg.onlytouch;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.kandg.onlytouch.particle.ParticleManager;

/*
 * 柔らかさ変更ダイアログ
 */
public class HowToTouchDialog extends DialogFragment {

    //空のコンストラクタ（DialogFragmentのお約束）
    public HowToTouchDialog() {
    }

    //インスタンス作成
    public static HowToTouchDialog newInstance() {
        return new HowToTouchDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_how_to_help);

        //アニメーションを設定
        setShowHideAnimation(dialog);
        return dialog;
    }

    /*
     * ダイアログ表示／非表示アニメーション
     */
    private void setShowHideAnimation( Dialog dialog ) {
        // windowAnimationsのスタイルを上書き
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
    }
}
