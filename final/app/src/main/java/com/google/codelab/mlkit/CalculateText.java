package com.google.codelab.mlkit;

import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.google.android.gms.vision.text.TextBlock;
import com.google.mlkit.vision.text.Text;

import java.util.List;

public class CalculateText {
    int sourceL, sourceR, sourceT, sourceB, destL, destR, destT, destB;
    private float startX, startY;

    public CalculateText(Text.TextBlock source, int destL, int destR, int destT, int destB) {
        sourceL = (int)source.getBoundingBox().left;
        sourceR = (int)source.getBoundingBox().right;
        sourceT = (int)source.getBoundingBox().top;
        sourceB = (int)source.getBoundingBox().bottom;
        this.destL = (int)destL;
        this.destR = (int)destR;
        this.destT = (int)destT;
        this.destB = (int)destB;
    }

    public boolean doRectOverlap(){
        // If one rectangle is left of other
        if (sourceL >= destR || destL >= sourceR) {
            return false;
        }
        // If one rectangle is above other
        if (sourceT >= destB || destT >= sourceB) {
            return false;
        }
        return true;
    }
    // if destination is inside source from left
    private boolean isDestLeftIn(){
        if (destL >= sourceL && destL <= sourceR)
            return true;
        return false;
    }
    // if destination is inside source from Right
    private boolean isDestRightIn(){
        if (destR >= sourceL && destR <= sourceR)
            return true;
        return false;
    }
    // if destination is inside source from Top
    private boolean isDestTopIn(){
        if (destT >= sourceT  && destT <= sourceB)
            return true;
        return false;
    }
    // if destination is inside source from Bottom
    private boolean isDestBottomIn(){
        if (destB >= sourceT  && destB <= sourceB)
            return true;
        return false;
    }

    // calculate the percentage of horizontal overlapping and starting point of intersection
    public float calHorizontalOverlap(){
        int interX;
        int lenX = sourceR - sourceL;
        float ratio, startX;
        if (isDestLeftIn() && isDestRightIn()){
            interX = destR - destL;
            ratio = Math.round((interX*100)/lenX);
            startX = Math.round((destL - sourceL)*100/lenX);
        }else if (isDestLeftIn()){
            interX = sourceR - destL;
            ratio = Math.round((interX*100)/lenX);
            startX = Math.round((destL - sourceL)*100/lenX);
        }else if (isDestRightIn()){
            interX = destR - sourceL;
            ratio = Math.round((interX*100)/lenX);
            startX = 0;
        } else{
            ratio = 100;
            startX = 0;
        }
        setStartX(startX);
        return ratio;
    }

    private void setStartX(float startX) {
        this.startX = startX;
    }

    // calculate the percentage of vertical overlapping & starting point of intersection
    public float calVerticalOverlap(){
        int lenY = sourceB - sourceT;
        int interY;
        float ratio, startY;
        if (isDestTopIn() && isDestBottomIn()){
            interY = destB - destT;
            ratio = Math.round((interY*100)/lenY);
            startY = Math.round((destT - sourceT)*100/lenY);
        }else if (isDestTopIn()){
            interY = sourceB - destT;
            ratio = Math.round((interY*100)/lenY);
            startY = Math.round((destT - sourceT)*100/lenY);
        }else if (isDestBottomIn()){
            interY = destB - sourceT;
            ratio = Math.round((interY*100)/lenY);
            startY = 0;
        }else{
            ratio = 100;
            startY = 0;
        }
        setStartY(startY);
        return ratio;
    }

    private void setStartY(float startY) {
        this.startY = startY;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }
}
