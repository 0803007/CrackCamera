package nchc.crackcamera;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class SquareView extends View {
    
    private static final String TAG = "SquareView";


    public SquareView(Context coNtext) {
        super(coNtext);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint p = new Paint();
        p.setColor(Color.YELLOW);

        float w = canvas.getWidth()/2;
        float h = canvas.getHeight()/2;

        float size = 50;
        canvas.drawLine(w-size,h-size,w+size,h-size,p);
        canvas.drawLine(w-size,h+size,w+size,h+size,p);
        canvas.drawLine(w-size,h-size,w-size,h+size,p);
        canvas.drawLine(w+size,h-size,w+size,h+size,p);

    }
}