/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.taobao.weex.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.taobao.weex.ui.component.WXImage;
import com.taobao.weex.ui.view.gesture.WXGesture;
import com.taobao.weex.ui.view.gesture.WXGestureObservable;
import com.taobao.weex.utils.ImageDrawable;
import com.taobao.weex.utils.WXLogUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class WXImageView extends ImageView implements WXGestureObservable,
                                                      IRenderStatus<WXImage>,
                                                      IRenderResult<WXImage>, WXImage.Measurable {

  private WeakReference<WXImage> mWeakReference;
  private WXGesture wxGesture;
  private float[] borderRadius;
  private boolean gif;
  private boolean isBitmapReleased = false;
  private boolean enableBitmapAutoManage = true;


  public WXImageView(Context context) {
    super(context);
    initPaint();
  }

  @Override
  public void setImageResource(int resId) {
    Drawable drawable = getResources().getDrawable(resId);
    setImageDrawable(drawable);
  }

  public void setImageDrawable(@Nullable Drawable drawable, boolean isGif) {
    this.gif = isGif;
    ViewGroup.LayoutParams layoutParams;
    if ((layoutParams = getLayoutParams()) != null) {
      Drawable wrapDrawable = ImageDrawable.createImageDrawable(drawable,
                                                                getScaleType(), borderRadius,
                                                                layoutParams.width - getPaddingLeft() - getPaddingRight(),
                                                                layoutParams.height - getPaddingTop() - getPaddingBottom(),
                                                                isGif);
      if (wrapDrawable instanceof ImageDrawable) {
        ImageDrawable imageDrawable = (ImageDrawable) wrapDrawable;
        if (!Arrays.equals(imageDrawable.getCornerRadii(), borderRadius)) {
          imageDrawable.setCornerRadii(borderRadius);
        }
      }
      super.setImageDrawable(wrapDrawable);
      if (mWeakReference != null) {
        WXImage component = mWeakReference.get();
        if (component != null) {
          component.readyToRender();
        }
      }
    }
  }

  @Override
  public void setImageDrawable(@Nullable Drawable drawable) {
    setImageDrawable(drawable, false);
  }

  @Override
  public void setImageBitmap(@Nullable Bitmap bm) {
    setImageDrawable(bm == null ? null : new BitmapDrawable(getResources(), bm));
  }

  @Override
  public void registerGestureListener(WXGesture wxGesture) {
    this.wxGesture = wxGesture;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = super.onTouchEvent(event);
    if (wxGesture != null) {
      result |= wxGesture.onTouch(this, event);
    }
    return result;
  }

  public void setBorderRadius(@NonNull float[] borderRadius) {
    this.borderRadius = borderRadius;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (changed) {
      setImageDrawable(getDrawable(), gif);
    }
  }

  @Override
  public void holdComponent(WXImage component) {
    mWeakReference = new WeakReference<>(component);
  }

  @Nullable
  @Override
  public WXImage getComponent() {
    return null != mWeakReference ? mWeakReference.get() : null;
  }

  @Override
  public int getNaturalWidth() {
    Drawable drawable = getDrawable();
    if (drawable != null) {
      if (drawable instanceof ImageDrawable) {
        return ((ImageDrawable) drawable).getBitmapWidth();
      } else if (drawable instanceof BitmapDrawable) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap != null) {
          return bitmap.getWidth();
        } else {
          WXLogUtils.w("WXImageView", "Bitmap on " + drawable.toString() + " is null");
        }
      } else {
        WXLogUtils.w("WXImageView", "Not supported drawable type: " + drawable.getClass().getSimpleName());
      }
    }
    return -1;
  }

  @Override
  public int getNaturalHeight() {
    Drawable drawable = getDrawable();
    if (drawable != null) {
      if (drawable instanceof ImageDrawable) {
        return ((ImageDrawable) drawable).getBitmapHeight();
      } else if (drawable instanceof BitmapDrawable) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap != null) {
          return bitmap.getHeight();
        } else {
          WXLogUtils.w("WXImageView", "Bitmap on " + drawable.toString() + " is null");
        }
      } else {
        WXLogUtils.w("WXImageView", "Not supported drawable type: " + drawable.getClass().getSimpleName());
      }
    }
    return -1;
  }

  private boolean mOutWindowVisibilityChangedReally;
  @Override
  public void dispatchWindowVisibilityChanged(int visibility) {
    mOutWindowVisibilityChangedReally = true;
    super.dispatchWindowVisibilityChanged(visibility);
    mOutWindowVisibilityChangedReally = false;
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if(mOutWindowVisibilityChangedReally){
      if(visibility == View.VISIBLE){
         autoRecoverImage();
      }else{
         autoReleaseImage();
      }
    }
  }


  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    autoRecoverImage();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    autoReleaseImage();

  }


  @Override
  public void onStartTemporaryDetach () {
    super.onStartTemporaryDetach();
    autoReleaseImage();

  }


  @Override
  public void onFinishTemporaryDetach () {
    super.onFinishTemporaryDetach();
    autoRecoverImage();
  }


  public void setEnableBitmapAutoManage(boolean enableBitmapAutoManage) {
     this.enableBitmapAutoManage = enableBitmapAutoManage;
  }

  public void autoReleaseImage(){
      if(enableBitmapAutoManage) {
        if (!isBitmapReleased) {
          isBitmapReleased = true;
          WXImage image = getComponent();
          if (image != null) {
            image.autoReleaseImage();
          }
        }
      }
  }

  public void autoRecoverImage(){
    if(enableBitmapAutoManage){
      if(isBitmapReleased){
        WXImage image = getComponent();
        if(image != null){
          image.autoRecoverImage();
        }
        isBitmapReleased = false;
      }
    }
  }


  /**
   * ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝添加方法＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
   **/

  public void setCurrentUrl(String url) {
      this.mCurrentUrl = url;
  }

  public String getCurrentUrl() {
      return mCurrentUrl;
  }

  public Drawable getRoundDrawable(Bitmap bitmap) {
      if (bitmap == null) return null;
      ViewGroup.LayoutParams layoutParams = getLayoutParams();
      if (layoutParams == null) return null;
      Drawable wrapDrawable = ImageDrawable.createImageDrawable(new BitmapDrawable
                      (getResources(), bitmap),
              getScaleType(), borderRadius,
              layoutParams.width - getPaddingLeft() - getPaddingRight(),
              layoutParams.height - getPaddingTop() - getPaddingBottom(),
              false);
      if (wrapDrawable instanceof ImageDrawable) {
          ImageDrawable imageDrawable = (ImageDrawable) wrapDrawable;
          if (!Arrays.equals(imageDrawable.getCornerRadii(), borderRadius)) {
              imageDrawable.setCornerRadii(borderRadius);
          }
          return imageDrawable;
      }
      return null;
  }

  @Override
  protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      if (mShowing) {
          drawLoading(canvas);
          mHandler.postDelayed(new Runnable() {
              @Override
              public void run() {
                  invalidate();
              }
          }, 100);
      }
      if (mDrawError && mErrorBitmap != null) {
          canvas.drawBitmap(mErrorBitmap, getMeasuredWidth() / 2 - mErrorBitmap.getWidth() / 2,
                  getMeasuredHeight() / 2 - mErrorBitmap.getHeight() / 2, mPaint);
      }

  }

  private int mWidth;
  private int mHeight;
  private Paint mPaint;
  private int mWidthRect;
  private int mHeightRect;
  private int r = 30;
  private boolean mShowing = false;
  private boolean mDrawError = false;
  private Rect mRect;
  private int pos = 0;
  private String[] color = {"#bbbbbb", "#aaaaaa", "#999999", "#888888", "#777777", "#666666"};
  private Handler mHandler = new Handler();
  private Bitmap mErrorBitmap;
  private int max = 300;
  private int mEnsrueLength;
  private int mLoadingPadding;
  private String mCurrentUrl;
  /**
   * 展示菊花的方法
   */
  public void showLoading(int width, int height) {
      mWidth = width;
      mHeight = height;
      mEnsrueLength = Math.min(mWidth, mHeight);
      if (mEnsrueLength > max) {
          mEnsrueLength = max;
      }
      mHeightRect = mEnsrueLength / 5;
      mWidthRect = mHeightRect / 6;
      mLoadingPadding = mEnsrueLength / 5;
      mShowing = true;
      invalidate();
  }

  /**
   * 隐藏菊花
   */

  public void hideLoading() {
      mShowing = false;
      invalidate();
      mHandler.removeCallbacksAndMessages(null);
  }


  public void drawLoading(Canvas canvas) {
      if (mRect == null) {
          mRect = new Rect((mWidth - mWidthRect) / 2, mHeight / 2 - mEnsrueLength / 2 +
                  mLoadingPadding, (mWidth +
                  mWidthRect) /
                  2, mHeight / 2 - mEnsrueLength / 6);
      }
      for (int i = 0; i < 12; i++) {
          if (i - pos >= 5) {
              mPaint.setColor(Color.parseColor(color[5]));
          } else if (i - pos >= 0 && i - pos < 5) {
              mPaint.setColor(Color.parseColor(color[i - pos]));
          } else if (i - pos >= -7 && i - pos < 0) {
              mPaint.setColor(Color.parseColor(color[5]));
          } else if (i - pos >= -11 && i - pos < -7) {
              mPaint.setColor(Color.parseColor(color[12 + i - pos]));
          }

          canvas.drawRect(mRect, mPaint);  //绘制
          canvas.rotate(30, mWidth / 2, mHeight / 2);    //旋转
      }

      pos++;
      if (pos > 11) {
          pos = 0;
      }
  }


  private void initPaint() {
      mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPaint.setStrokeWidth(5);
      mPaint.setStrokeJoin(Paint.Join.ROUND);
      mPaint.setStrokeCap(Paint.Cap.ROUND);
  }


  public void drawErrorBitmap(Bitmap bitmap) {
      this.mErrorBitmap = bitmap;
      mDrawError = true;
      invalidate();
  }

  public void hideErrorBitmap() {
      this.mErrorBitmap = null;
      mDrawError = false;
      invalidate();
  }


  /**＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝结束＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝**/


}
