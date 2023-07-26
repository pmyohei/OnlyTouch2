package com.example.onlyTouch;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.example.onlyTouch.opengl.ParticleWorldRenderer;
import com.example.onlyTouch.particle.ParticleData;

/*
 * 柔らかさ変更ダイアログ
 */
public class ChangeSoftDialog extends DialogFragment implements View.OnClickListener {

    // 選択中の柔らかさ
    private int mChoiceSoftness;
    // クリックリスナー
    private PositiveClickListener mPositiveClickListener;

    //空のコンストラクタ（DialogFragmentのお約束）
    public ChangeSoftDialog() {
    }

    //インスタンス作成
    public static ChangeSoftDialog newInstance() {
        return new ChangeSoftDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_change_soft);

        //アニメーションを設定
        setShowHideAnimation(dialog);
        // ユーザーOKイメージ押下設定
        setPositiveImage(dialog);
        // 柔らかさ選択肢の初期設定
        initSoftness(dialog);
        // 柔らかさ選択肢リスナーの設定
        setChoiceListener(dialog);

        return dialog;
    }

    /*
     * 柔らかさ選択肢の初期設定：
     */
    public void initSoftness(Dialog dialog) {

        // 設定中の柔らかさを選択中状態にする
        switch (mChoiceSoftness) {
            case ParticleData.SOFTNESS_SOFT:
                ((RadioButton) dialog.findViewById(R.id.radio_soft)).setChecked(true);
                break;
            case ParticleData.SOFTNESS_NORMAL:
                ((RadioButton) dialog.findViewById(R.id.radio_normal)).setChecked(true);
                break;
            case ParticleData.SOFTNESS_LITTEL_HARD:
                ((RadioButton) dialog.findViewById(R.id.radio_little_hard)).setChecked(true);
                break;
            default:
                break;
        }
    }

    /*
     * ダイアログ表示／非表示アニメーション
     */
    private void setShowHideAnimation( Dialog dialog ) {
        // windowAnimationsのスタイルを上書き
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
    }

    /*
     * OK押下設定
     */
    private void setPositiveImage( Dialog dialog ) {
        ImageView iv_positive = dialog.findViewById(R.id.iv_positive);
        iv_positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // リスナー処理
                mPositiveClickListener.onPositiveClick( mChoiceSoftness );
                dismiss();
            }
        });
    }

    /*
     * 柔らかさリスナー設定
     */
    private void setChoiceListener( Dialog dialog ) {
        dialog.findViewById( R.id.radio_soft ).setOnClickListener( this );
        dialog.findViewById( R.id.radio_normal ).setOnClickListener( this );
        dialog.findViewById( R.id.radio_little_hard ).setOnClickListener( this );
    }

    /*
     * 選択中の柔らかさの設定
     */
    public void selectedSoftness(int softness ) {
        mChoiceSoftness = softness;
    }

    /*
     * クリックリスナーの設定
     */
    public void setOnPositiveClickListener( PositiveClickListener listener ) {
        mPositiveClickListener = listener;
    }

    /*
     * 柔らかさ選択肢用としてリスナーを実装
     */
    @Override
    public void onClick(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.radio_soft:
                if (checked) {
                    mChoiceSoftness = ParticleData.SOFTNESS_SOFT;
                }
                break;

            case R.id.radio_normal:
                if (checked) {
                    mChoiceSoftness = ParticleData.SOFTNESS_NORMAL;
                }
                break;

            case R.id.radio_little_hard:
                if (checked) {
                    mChoiceSoftness = ParticleData.SOFTNESS_LITTEL_HARD;
                }
                break;

            default:
                break;
        }
    }

    /*
     * クリック検出用インターフェース
     */
    public interface PositiveClickListener {
        // クリックリスナー
        void onPositiveClick(int softness);
    }

}
