package nchc.crackcamera;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class DrawImageView extends ImageView {
	private final Paint paint;
	private final Context context; 
	public ArrayList<PointF> mArrayList;
	public int camImgWidth = 0;
	public int camImgHeight = 0;

    public DrawImageView(Context context,AttributeSet paramAttributeSet){
        super(context,paramAttributeSet);
		// TODO Auto-generated constructor stub
		  this.context = context;
		  this.paint = new Paint();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		if (mArrayList==null)
			return;

		//設置畫筆
		Paint paint = new Paint();
		paint.setStrokeWidth(3);//筆寬5圖元
		paint.setStyle(Paint.Style.STROKE);//空心  
		paint.setColor(Color.BLUE);//設置為紅筆
		paint.setAntiAlias(true);//鋸齒不顯示
		
		//設置字
		Paint paintWord = new Paint();
		paintWord.setTextSize(54);//設定字體大小
		paintWord.setStrokeWidth(3);//筆寬5圖元
		paintWord.setStrokeWidth(24);//筆寬24圖元
		paintWord.setColor(Color.RED);//設置為紅筆

		//座標轉換
        float wscale = (float)canvas.getWidth()/camImgWidth;
        float hscale = (float)canvas.getHeight()/camImgHeight;
		
        float pts[] = new float[2];
		for (int i=0;i<mArrayList.size();i++)
		{
			//取值
			pts[0] = mArrayList.get(i).x * wscale;
			pts[1] = mArrayList.get(i).y * hscale;
			//畫圖
			canvas.drawLine(pts[0]-8, pts[1]-8, pts[0]+8, pts[1]+8, paint);
			canvas.drawLine(pts[0]+8, pts[1]-8, pts[0]-8, pts[1]+8, paint);
			//如果有四個 畫上ABCD
			if (mArrayList.size() == 4)
			{
				if (i==0)
				    canvas.drawText("A", pts[0]-20, pts[1]-20, paintWord); 	
				if (i==1)
				    canvas.drawText("B", pts[0]-20, pts[1]-20, paintWord); 
				if (i==2)
				    canvas.drawText("C", pts[0]-20, pts[1]-20, paintWord); 
				if (i==3)
				    canvas.drawText("D", pts[0]-20, pts[1]-20, paintWord); 
				
			}
		}  
		super.onDraw(canvas);
	}
	public void setCamImgSize(int w,int h){
		camImgWidth = w;
		camImgHeight = h;
	}
	public static int dip2px(Context context, float dpValue) {  
		final float scale = context.getResources().getDisplayMetrics().density;  
		return (int) (dpValue * scale + 0.5f);  
	}  
}
