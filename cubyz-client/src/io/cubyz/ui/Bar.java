package io.cubyz.ui;
import io.cubyz.rendering.Window;

public abstract class Bar {
    protected float maxFeature;
    protected float currentFeature;
    protected int[] barImages;

    protected Bar(float maxFeature, float currentFeature){
        this.maxFeature = maxFeature;
        this.currentFeature = currentFeature;
        initBarImages();
    }

    abstract void initBarImages();

    public void drawBarLogoAndText(Window win, int yAxisImg, int yAxisText){
        String s = Math.round(currentFeature*10)/10.0f + "/" + Math.round(currentFeature) + " HP";
        float width = NGraphics.getTextWidth(s);
        NGraphics.drawImage(barImages[7], (int)(win.getWidth() - currentFeature*12 - 40 - width), yAxisImg, 24, 24);
        NGraphics.drawText(win.getWidth() - currentFeature*12 - 10 - width, yAxisText, s);
    }

    public void drawInsideBar(int yAxisImg, Window win){
        int i=0;
        int idx = isEmpty(i, currentFeature) ? 0 : 1; //begining
        drawImgInsideBar(i, idx, yAxisImg, win);
        i+=2;
        while(i < maxFeature - 2){
            if(isEmpty(i, currentFeature))
                idx = 4;
            else
                idx = isHalf(i, currentFeature) ? 5 : 6;//middle
            drawImgInsideBar(i, idx, yAxisImg, win);
            i += 2;
        }
        idx = i + 1 >= currentFeature ? 2 : 3; //end
        drawImgInsideBar(i, idx, yAxisImg, win);
    }

    public void drawImgInsideBar(int i, int index, int yAxisImg, Window win){
        NGraphics.drawImage(barImages[index], (int)(i*12 + win.getWidth() - this.maxFeature *12 - 4), yAxisImg, 24, 24);
    }

    public boolean isEmpty(int i, float feature){
        return i >= feature;
    }

    public boolean isHalf(int i, float feature){
        return i + 1 == feature;
    }
}