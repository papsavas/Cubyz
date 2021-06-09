package io.cubyz.ui;

public class HealthBar extends Bar{

    public HealthBar(float maxFeature, float currentFeature) {
        super(maxFeature, currentFeature);
    }

    @Override
    void initBarImages(){
        this.barImages = new int[8];
        this.barImages[0] = NGraphics.loadImage("assets/cubyz/textures/health_bar_beg_empty.png");
        this.barImages[1] = NGraphics.loadImage("assets/cubyz/textures/health_bar_beg_full.png");
        this.barImages[2] = NGraphics.loadImage("assets/cubyz/textures/health_bar_end_empty.png");
        this.barImages[3] = NGraphics.loadImage("assets/cubyz/textures/health_bar_end_full.png");
        this.barImages[4] = NGraphics.loadImage("assets/cubyz/textures/health_bar_mid_empty.png");
        this.barImages[5] = NGraphics.loadImage("assets/cubyz/textures/health_bar_mid_half.png");
        this.barImages[6] = NGraphics.loadImage("assets/cubyz/textures/health_bar_mid_full.png");
        this.barImages[7] = NGraphics.loadImage("assets/cubyz/textures/health_bar_icon.png");
    }

}