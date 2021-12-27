package com.gbhong.powersupplycontrol;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

public class CustomProgressDialog extends Dialog {
    public CustomProgressDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 다이얼 로그 제목을 없앰

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);     // dialog 자체 배경 투명
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));    // dialog 배경 투명
        setContentView(R.layout.custom_progress); // 다이얼로그에 지정할 레이아웃
    }
}

