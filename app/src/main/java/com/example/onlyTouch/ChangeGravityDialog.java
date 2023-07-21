package com.example.onlyTouch;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

/*
 * 重力変更ダイアログ
 */
public class ChangeGravityDialog extends DialogFragment implements View.OnClickListener {

    // 選択中の重力
    private int mChoiceGravity;
    // クリックリスナー
    private PositiveClickListener mPositiveClickListener;

    //空のコンストラクタ（DialogFragmentのお約束）
    public ChangeGravityDialog() {
    }

    //インスタンス作成
    public static ChangeGravityDialog newInstance() {
        return new ChangeGravityDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_change_gravity);

        //アニメーションを設定
        setShowHideAnimation(dialog);
        // ユーザーOKイメージ押下設定
        setPositiveImage(dialog);
        // 重力選択肢の初期設定
        initGravity(dialog);
        // 重力選択肢リスナーの設定
        setChoiceListener(dialog);

        return dialog;
    }

    /*
     * ダイアログ表示／非表示アニメーション
     */
    private void setShowHideAnimation( Dialog dialog ) {
        // windowAnimationsのスタイルを上書き
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
    }

    /*
     * 重力選択肢の初期設定：
     */
    public void initGravity(Dialog dialog ) {

        // 設定中の重力を選択中状態にする
        switch(mChoiceGravity) {
            case FluidWorldRenderer.GRAVITY_FLOAT:
                ((RadioButton)dialog.findViewById( R.id.radio_float )).setChecked( true );
                break;
            case FluidWorldRenderer.GRAVITY_FLUFFY:
                ((RadioButton)dialog.findViewById( R.id.radio_fluffy )).setChecked( true );
                break;
            case FluidWorldRenderer.GRAVITY_NONE:
                ((RadioButton)dialog.findViewById( R.id.radio_no_gravity )).setChecked( true );
                break;
            case FluidWorldRenderer.GRAVITY_DEFAULT:
                ((RadioButton)dialog.findViewById( R.id.radio_gravity )).setChecked( true );
                break;
            case FluidWorldRenderer.GRAVITY_STRONG:
                ((RadioButton)dialog.findViewById( R.id.radio_strong_gravity )).setChecked( true );
                break;
            default:
                break;
        }
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
                mPositiveClickListener.onPositiveClick(mChoiceGravity);
                dismiss();
            }
        });
    }

    /*
     * 重力リスナー設定
     */
    private void setChoiceListener( Dialog dialog ) {
        dialog.findViewById( R.id.radio_float).setOnClickListener( this );
        dialog.findViewById( R.id.radio_fluffy).setOnClickListener( this );
        dialog.findViewById( R.id.radio_no_gravity ).setOnClickListener( this );
        dialog.findViewById( R.id.radio_gravity ).setOnClickListener( this );
        dialog.findViewById( R.id.radio_strong_gravity ).setOnClickListener( this );
    }

    /*
     * 選択中の重力の設定
     */
    public void selectedGravity(int gravity ) {
        mChoiceGravity = gravity;
    }

    /*
     * クリックリスナーの設定
     */
    public void setOnPositiveClickListener( PositiveClickListener listener ) {
        mPositiveClickListener = listener;
    }

    /*
     * 重力選択肢用としてリスナーを実装
     */
    @Override
    public void onClick(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.radio_float:
                if (checked) {
                    mChoiceGravity = FluidWorldRenderer.GRAVITY_FLOAT;
                }
                break;

            case R.id.radio_fluffy:
                if (checked) {
                    mChoiceGravity = FluidWorldRenderer.GRAVITY_FLUFFY;
                }
                break;

            case R.id.radio_no_gravity:
                if (checked) {
                    mChoiceGravity = FluidWorldRenderer.GRAVITY_NONE;
                }
                break;

            case R.id.radio_gravity:
                if (checked) {
                    mChoiceGravity = FluidWorldRenderer.GRAVITY_DEFAULT;
                }
                break;

            case R.id.radio_strong_gravity:
                if (checked) {
                    mChoiceGravity = FluidWorldRenderer.GRAVITY_STRONG;
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
