package com.nexorha.subsidize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RainView extends View {
    private Paint paint;
    private List<Drop> drops;
    private final int numDrops = 90;
    private Random random = new Random();

    public RainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(0xFF8E2DE2); // Matching Nexorha Purple
        paint.setStrokeWidth(3);
        paint.setAlpha(95);
        drops = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (int i = 0; i < numDrops; i++) { drops.add(new Drop(w, h)); }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Drop drop : drops) {
            canvas.drawLine(drop.x, drop.y, drop.x, drop.y + drop.length, paint);
            drop.fall();
        }
        invalidate();
    }

    private class Drop {
        float x, y, speed, length;
        int height, width;

        Drop(int width, int height) {
            this.width = width;
            this.height = height;
            reset();
            y = random.nextInt(height);
        }

        void reset() {
            x = random.nextInt(width);
            y = -random.nextInt(200);
            speed = 8 + random.nextInt(18);
            length = 15 + random.nextInt(40);
        }

        void fall() {
            y += speed;
            if (y > height) reset();
        }
    }
}