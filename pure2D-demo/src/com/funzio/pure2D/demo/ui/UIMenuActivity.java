package com.funzio.pure2D.demo.ui;

import com.funzio.pure2D.demo.R;
import com.funzio.pure2D.demo.activities.MenuActivity;
import com.funzio.pure2D.demo.textures.Sprite9Activity;

public class UIMenuActivity extends MenuActivity {

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.demo.activities.MenuActivity#getLayout()
     */
    @Override
    protected int getLayout() {
        return R.layout.ui_menu;
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.demo.activities.MenuActivity#createMenus()
     */
    @Override
    protected void createMenus() {
        addMenu(R.id.btn_hello_text, HelloTextActivity.class);
        addMenu(R.id.btn_sprite_9, Sprite9Activity.class);
        addMenu(R.id.btn_button, ButtonActivity.class);
    }
}