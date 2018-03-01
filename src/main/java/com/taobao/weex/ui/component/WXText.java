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
package com.taobao.weex.ui.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.Component;
import com.taobao.weex.common.Constants;
import com.taobao.weex.dom.WXAttr;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.dom.WXStyle;
import com.taobao.weex.ui.ComponentCreator;
import com.taobao.weex.ui.flat.FlatComponent;
import com.taobao.weex.ui.flat.widget.TextWidget;
import com.taobao.weex.ui.view.WXTextView;

import java.lang.reflect.InvocationTargetException;

/**
 * Text component
 */
@Component(lazyload = false)
public class WXText extends WXComponent<WXTextView> implements FlatComponent<TextWidget> {

    private TextWidget mTextWidget;

    /**
     * The default text size
     **/
    public static final int sDEFAULT_SIZE = 32;

    @Override
    public boolean promoteToView(boolean checkAncestor) {
        return getInstance().getFlatUIContext().promoteToView(this, checkAncestor, WXText.class);
    }

    @Override
    @NonNull
    public TextWidget getOrCreateFlatWidget() {
        if (mTextWidget == null) {
            mTextWidget = new TextWidget(getInstance().getFlatUIContext());
        }
        return mTextWidget;
    }

    @Override
    public boolean isVirtualComponent() {
        return !promoteToView(true);
    }

    public static class Creator implements ComponentCreator {

        public WXComponent createInstance(WXSDKInstance instance, WXDomObject node, WXVContainer
                parent) throws IllegalAccessException, InvocationTargetException,
                InstantiationException {
            return new WXText(instance, node, parent);
        }
    }

    @Deprecated
    public WXText(WXSDKInstance instance, WXDomObject dom, WXVContainer parent, String
            instanceId, boolean isLazy) {
        this(instance, dom, parent);
    }

    public WXText(WXSDKInstance instance, WXDomObject node,
                  WXVContainer parent) {
        super(instance, node, parent);
        //benmu.org
        initFontSize();
    }

    @Override
    protected WXTextView initComponentHostView(@NonNull Context context) {
        WXTextView textView = new WXTextView(context);
        textView.holdComponent(this);
        return textView;
    }

    @Override
    public void updateExtra(Object extra) {
        if (extra instanceof Layout) {
            final Layout layout = (Layout) extra;
            if (!promoteToView(true)) {
                getOrCreateFlatWidget().updateTextDrawable(layout);
            } else if (getHostView() != null && !extra.equals(getHostView().getTextLayout())) {
                getHostView().setTextLayout(layout);
                getHostView().invalidate();
            }
        }
        //benmu.org
        updateFontSize();
    }

    @Override
    protected void setAriaLabel(String label) {
        WXTextView text = getHostView();
        if (text != null) {
            text.setAriaLabel(label);
        }
    }

    @Override
    public void refreshData(WXComponent component) {
        super.refreshData(component);
        if (component instanceof WXText) {
            updateExtra(component.getDomObject().getExtra());
        }
    }

    @Override
    protected boolean setProperty(String key, Object param) {
        switch (key) {
            case Constants.Name.LINES:
            case Constants.Name.FONT_SIZE:
            case Constants.Name.FONT_WEIGHT:
            case Constants.Name.FONT_STYLE:
            case Constants.Name.COLOR:
            case Constants.Name.TEXT_DECORATION:
            case Constants.Name.TEXT_ALIGN:
            case Constants.Name.TEXT_OVERFLOW:
            case Constants.Name.LINE_HEIGHT:
            case Constants.Name.VALUE:
                return true;
            case Constants.Name.FONT_FAMILY:
                return true;
            default:
                return super.setProperty(key, param);
        }
    }

    @Override
    protected Object convertEmptyProperty(String propName, Object originalValue) {
        switch (propName) {
            case Constants.Name.FONT_SIZE:
                return WXText.sDEFAULT_SIZE;
            case Constants.Name.COLOR:
                return "black";
        }
        return super.convertEmptyProperty(propName, originalValue);
    }

    @Override
    protected void createViewImpl() {
        if (promoteToView(true)) {
            super.createViewImpl();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    //benmu.org
    private String mCurrentFontSize = "NORM";
    private String mChangeFontSize;
    private float mCurrentEnlarge;
    private float mCurrentScale = 1;
    private DefaultBroadcastReceiver mReceiver;

    private void initFontSize() {
        registerBroadCast();
        SharedPreferences sp = getContext().getSharedPreferences("JYT_NATIVE_SP", Context
                .MODE_PRIVATE);
        mChangeFontSize = sp.getString("SP_FONTSIZE", null);
    }


    private void updateFontSize() {
        if (getDomObject() != null && getDomObject().getStyles().get(Constants.Name.FONT_SIZE) ==
                null) {
            WXStyle s = getDomObject().getStyles();
            s.put(Constants.Name.FONT_SIZE, 30);
            updateStyle(s);
            return;
        }
        if (mChangeFontSize == null) {
            return;
        }
        WXStyle styles = null;
        WXAttr attrs = null;
        if (getDomObject() != null) {
            styles = getDomObject().getStyles();
            attrs = getDomObject().getAttrs();
            if ((styles != null && "iconfont".equals(styles.get("fontFamily"))) || (attrs != null
                    && attrs.get("changeFont") != null && !Boolean.valueOf((String) attrs.get
                    ("changeFont")))) {
                return;
            }
        }

        float scale = 0;
        //获取fontScale字段
        if (attrs != null && attrs.get("fontScale") != null) {
            float fontScale = Float.valueOf((String) attrs.get("fontScale"));
            mCurrentScale = fontScale / mCurrentScale;
        }
        if (mChangeFontSize.equals(mCurrentFontSize) && mCurrentScale == 1) {
            return;
        }
        //获取scale字段 在标准字体下不产生变化
        if (attrs != null && attrs.get("scale") != null && !(scale > 0)) {
            scale = Float.valueOf((String) attrs.get("scale"));
            float change = getFixedEnlarge(mChangeFontSize, scale);
            float current = getFixedEnlarge(mCurrentFontSize, scale);
            scale = change / current;
        }
        //根据全局字体配置设置字体大小
        if (!(scale > 0)) {
            float current = getEnlarge(mCurrentFontSize);
            float change = getEnlarge(mChangeFontSize);
            scale = change / current * mCurrentScale;
        }
        if (getDomObject() != null && getDomObject().getStyles() != null) {
            WXStyle wxStyle = getDomObject().getStyles();
            Object object = wxStyle.get("fontSize");
            if (object instanceof Integer) {
                int fontSize = (int) object;
                int changeFontSize = Math.round(fontSize * (scale));
                wxStyle.put("fontSize", changeFontSize);

            }
            //设置lineHeight
            Object lineHeight = wxStyle.get("lineHeight");
            if (lineHeight instanceof Integer) {
                int target = (int) lineHeight;
                wxStyle.put("lineHeight", Math.round(target * scale));
            }


            updateStyle(wxStyle);

        }
        mCurrentFontSize = mChangeFontSize;

    }

    private float getEnlarge(String fontsize) {
        if ("NORM".equals(fontsize)) {
            return 1;
        } else if ("BIG".equals(fontsize)) {
            return 1.15f;
        } else if ("EXTRALARGE".equals(fontsize)) {
            return 1.3f;
        } else {
            throw new RuntimeException("未知的字体大小" + fontsize);
        }
    }

    private float getFixedEnlarge(String fontsize, float scale) {
        if ("NORM".equals(fontsize)) {
            return 1;
        } else if ("BIG".equals(fontsize)) {
            return scale;
        } else if ("EXTRALARGE".equals(fontsize)) {
            return scale;
        } else {
            throw new RuntimeException("未知的字体大小" + fontsize);
        }
    }


    private void registerBroadCast() {
        mReceiver = new DefaultBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.benmu.jyt.ACTION_GOBALFONTSIZE_CHANGE");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
    }


    public class DefaultBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String size = intent.getStringExtra("currentFontSize");
            if (size == null) {
                return;
            }
            mChangeFontSize = size;
            updateFontSize();
        }
    }


    //benmu.org

}
