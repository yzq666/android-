package graffiti;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Created by Administrator on 2016/9/3.
 */
public class DrawUtil {

    public static void drawArrow(Canvas canvas, float sx, float sy, float ex,
                                 float ey, Paint paint) {
        float arrowSize = paint.getStrokeWidth();
        double H = arrowSize; // ��ͷ�߶�
        double L = arrowSize / 2; // �ױߵ�һ??

        double awrad = Math.atan(L / 2 / H); // ��ͷ�Ƕ�
        double arraow_len = Math.sqrt(L / 2 * L / 2 + H * H) - 5; // ��ͷ�ĳ�??
        double[] arrXY_1 = rotateVec(ex - sx, ey - sy, awrad, true,
                arraow_len);
        double[] arrXY_2 = rotateVec(ex - sx, ey - sy, -awrad, true,
                arraow_len);
        float x_3 = (float) (ex - arrXY_1[0]); // (x3,y3)�ǵ�??�˵�
        float y_3 = (float) (ey - arrXY_1[1]);
        float x_4 = (float) (ex - arrXY_2[0]); // (x4,y4)�ǵڶ���??
        float y_4 = (float) (ey - arrXY_2[1]);
        // ����
        Path linePath = new Path();
        linePath.moveTo(sx, sy);
        linePath.lineTo(x_3, y_3);
        linePath.lineTo(x_4, y_4);
        linePath.close();
        canvas.drawPath(linePath, paint);

        awrad = Math.atan(L / H); // ��ͷ�Ƕ�
        arraow_len = Math.sqrt(L * L + H * H); // ��ͷ�ĳ�??
        arrXY_1 = rotateVec(ex - sx, ey - sy, awrad, true, arraow_len);
        arrXY_2 = rotateVec(ex - sx, ey - sy, -awrad, true, arraow_len);
        x_3 = (float) (ex - arrXY_1[0]); // (x3,y3)�ǵ�??�˵�
        y_3 = (float) (ey - arrXY_1[1]);
        x_4 = (float) (ex - arrXY_2[0]); // (x4,y4)�ǵڶ���??
        y_4 = (float) (ey - arrXY_2[1]);
        Path triangle = new Path();
        triangle.moveTo(ex, ey);
        triangle.lineTo(x_3, y_3);
        triangle.lineTo(x_4, y_4);
        triangle.close();
        canvas.drawPath(triangle, paint);
    }

    // ���� ������px,py?? ��תang�ǶȺ���³�??
    public static double[] rotateVec(float px, float py, double ang,
                                     boolean isChLen, double newLen) {
        double mathstr[] = new double[2];
        // ʸ����ת��������������ֱ���x������y��������ת�ǡ��Ƿ�ı䳤��???�³���
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        if (isChLen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
        }
        mathstr[0] = vx;
        mathstr[1] = vy;
        return mathstr;
    }

    public static void drawLine(Canvas canvas, float sx, float sy, float dx, float dy, Paint paint) {
        canvas.drawLine(sx, sy, dx, dy, paint);
    }

    public static void drawCircle(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        canvas.drawCircle(cx, cy, radius, paint);
    }
    public static void drawOval(Canvas canvas, float sx, float sy, float dx, float dy, Paint paint) {
        RectF rectF = new RectF(sx, sy, dx, dy);
        if (sx < dx) {
            if (sy < dy) {
                canvas.drawOval(rectF, paint);
            } else {
                canvas.drawOval(rectF, paint);
            }
        } else {
            if (sy < dy) {
                canvas.drawOval(rectF, paint);
            } else {
                canvas.drawOval(rectF, paint);
            }
        }
    }
    public static void drawRect(Canvas canvas, float sx, float sy, float dx, float dy, Paint paint) {

        // ��֤�����Ͻǡ��롡���½ǡ��Ķ�Ӧ��ϵ
        if (sx < dx) {
            if (sy < dy) {
                canvas.drawRect(sx, sy, dx, dy, paint);
            } else {
                canvas.drawRect(sx, dy, dx, sy, paint);
            }
        } else {
            if (sy < dy) {
                canvas.drawRect(dx, sy, sx, dy, paint);
            } else {
                canvas.drawRect(dx, dy, sx, sy, paint);
            }
        }
    }
}
