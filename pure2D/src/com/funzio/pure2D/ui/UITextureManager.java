/**
 * 
 */
package com.funzio.pure2D.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.res.Resources;
import android.util.Log;

import com.funzio.pure2D.Scene;
import com.funzio.pure2D.atlas.AtlasFrameSet;
import com.funzio.pure2D.atlas.JsonAtlas;
import com.funzio.pure2D.atlas.SingleFrameSet;
import com.funzio.pure2D.gl.gl10.textures.Texture;
import com.funzio.pure2D.gl.gl10.textures.TextureManager;
import com.funzio.pure2D.gl.gl10.textures.TextureOptions;
import com.funzio.pure2D.particles.nova.NovaConfig;
import com.funzio.pure2D.particles.nova.NovaDelegator;
import com.funzio.pure2D.particles.nova.NovaEmitter;
import com.funzio.pure2D.particles.nova.NovaParticle;
import com.funzio.pure2D.text.BitmapFont;
import com.funzio.pure2D.text.TextOptions;
import com.funzio.pure2D.ui.vo.FontVO;
import com.funzio.pure2D.ui.vo.UIConfigVO;

/**
 * @author long.ngo
 */
public class UITextureManager extends TextureManager {
    protected static final String TAG = UITextureManager.class.getSimpleName();

    protected HashMap<String, BitmapFont> mBitmapFonts = new HashMap<String, BitmapFont>();
    protected final HashMap<String, Texture> mGeneralTextures;
    protected final HashMap<String, AtlasFrameSet> mAtlasFrames;

    protected UIManager mUIManager;
    protected UIConfigVO mUIConfigVO;

    private NovaDelegator mNovaDelegator;

    /**
     * @param scene
     * @param res
     */
    public UITextureManager(final Scene scene, final Resources res) {
        super(scene, res);

        mGeneralTextures = new HashMap<String, Texture>();
        mAtlasFrames = new HashMap<String, AtlasFrameSet>();
    }

    public UIManager getUIManager() {
        return mUIManager;
    }

    public void setUIManager(final UIManager manager) {
        mUIManager = manager;

        if (manager != null) {
            mUIConfigVO = manager.getConfig();
            // texture expiration
            setExpirationCheckInterval(manager.getConfig().texture_manager.expiration_check_interval);
        }
    }

    public void loadBitmapFonts() {
        if (mUIManager == null) {
            Log.e(TAG, "UIManager not found!", new Exception());
            return;
        }

        // make bitmap fonts
        final List<FontVO> fonts = mUIConfigVO.fonts;
        final int size = fonts.size();
        for (int i = 0; i < size; i++) {
            final TextOptions options = fonts.get(i).createTextOptions(mAssets);
            final BitmapFont font = new BitmapFont(options.inCharacters, options);
            font.load(mGLState);
            // map it
            mBitmapFonts.put(options.id, font);
        }
    }

    public BitmapFont getBitmapFont(final String fontId) {
        return mBitmapFonts.get(fontId);
    }

    public Texture getUriTexture(String textureUri, final boolean async) {
        Log.v(TAG, "getUriTexture(): " + textureUri);

        if (mUIManager == null) {
            Log.e(TAG, "UIManager not found!", new Exception());
            return null;
        }

        final String actualPath = mUIManager.getPathFromUri(textureUri);
        // XXX HACK for nova backward compatibility
        if (!actualPath.startsWith(UIConfig.URI_HTTP) && textureUri.equals(actualPath)) {
            textureUri = UIConfig.URI_ASSET + actualPath;
        }

        if (mGeneralTextures.containsKey(actualPath)) {
            // use cache
            return mGeneralTextures.get(actualPath);
        } else {
            Texture texture = null;
            final TextureOptions textureOptions = mUIConfigVO.getTextureOptions();
            // create
            if (textureUri.startsWith(UIConfig.URI_DRAWABLE)) {
                // load from file / sdcard
                final int drawable = Integer.valueOf(actualPath);
                if (drawable > 0) {
                    texture = createDrawableTexture(drawable, textureOptions, async);
                }
            } else if (textureUri.startsWith(UIConfig.URI_FILE)) {
                // load from file / sdcard
                texture = createFileTexture(actualPath, textureOptions, async);
            } else if (textureUri.startsWith(UIConfig.URI_ASSET)) {
                // load from bundle assets
                texture = createAssetTexture(actualPath, textureOptions, async);
            } else if (textureUri.startsWith(UIConfig.URI_HTTP)) {
                // load from bundle assets
                texture = createURLTexture(actualPath, textureOptions, async);
            } else if (textureUri.startsWith(UIConfig.URI_CACHE)) {
                // load from url or cache file
                texture = createURLCacheTexture(mUIConfigVO.texture_manager.cdn_url + actualPath, mUIConfigVO.texture_manager.cache_dir + actualPath, textureOptions, async);
            }

            // and cache it if created
            if (texture != null) {
                // texture expiration
                texture.setExpirationTime(mUIConfigVO.texture_manager.texture_expiration_time);
                mGeneralTextures.put(actualPath, texture);
            }

            return texture;
        }
    }

    /**
     * Load a Json atlas file
     * 
     * @param assets
     * @param jsonUri
     * @return
     */
    public AtlasFrameSet getUriAtlas(String jsonUri, final boolean async) {
        Log.v(TAG, "getUriAtlas(): " + jsonUri);

        final String actualPath = mUIManager.getPathFromUri(jsonUri);
        // XXX HACK for nova backward compatibility
        if (!actualPath.startsWith(UIConfig.URI_HTTP) && jsonUri.equals(actualPath)) {
            jsonUri = UIConfig.URI_ASSET + actualPath;
        }

        if (mAtlasFrames.containsKey(actualPath)) {
            // reuse cache
            return mAtlasFrames.get(actualPath);
        } else if (actualPath.endsWith(UIConfig.FILE_JSON)) {
            try {
                // create new
                final JsonAtlas atlas = new JsonAtlas(mScene.getAxisSystem());

                // load from sdcard / assets
                if (jsonUri.startsWith(UIConfig.URI_ASSET)) {
                    if (async) {
                        atlas.loadAsync(mAssets, actualPath, mUIConfigVO.scale);
                    } else {
                        atlas.load(mAssets, actualPath, mUIConfigVO.scale);
                    }
                } else if (jsonUri.startsWith(UIConfig.URI_FILE)) {
                    if (async) {
                        atlas.loadAsync(null, actualPath, mUIConfigVO.scale);
                    } else {
                        atlas.load(actualPath, mUIConfigVO.scale);
                    }
                } else if (jsonUri.startsWith(UIConfig.URI_HTTP)) {
                    if (async) {
                        atlas.loadURLAsync(actualPath, null, mUIConfigVO.scale);
                    } else {
                        atlas.loadURL(actualPath, null, mUIConfigVO.scale);
                    }
                } else if (jsonUri.startsWith(UIConfig.URI_CACHE)) {
                    if (async) {
                        atlas.loadURLAsync(mUIConfigVO.texture_manager.cdn_url + actualPath, mUIConfigVO.texture_manager.cache_dir + actualPath, mUIConfigVO.scale);
                    } else {
                        atlas.loadURL(mUIConfigVO.texture_manager.cdn_url + actualPath, mUIConfigVO.texture_manager.cache_dir + actualPath, mUIConfigVO.scale);
                    }
                }

                // now load texture
                final AtlasFrameSet multiFrames = atlas.getMasterFrameSet();
                multiFrames.setTexture(getUriTexture(jsonUri.replace(UIConfig.FILE_JSON, UIConfig.FILE_PNG), async));

                // cache it
                mAtlasFrames.put(actualPath, multiFrames);
                return multiFrames;

            } catch (Exception e) {
                Log.e(TAG, "Atlas Loading Error! " + actualPath, e);
                return null;
            }
        } else {
            final SingleFrameSet singleFrame = new SingleFrameSet(actualPath, getUriTexture(jsonUri, async));
            // cache it
            mAtlasFrames.put(actualPath, singleFrame);
            return singleFrame;
        }
    }

    public NovaDelegator getNovaDelegator() {
        if (mNovaDelegator != null) {
            return mNovaDelegator;
        }

        // this is how you assign texture to the sprite to a particle
        mNovaDelegator = new NovaDelegator() {
            @Override
            public void delegateEmitter(final NovaEmitter emitter, final Object... params) {

            }

            @Override
            public void delegateParticle(final NovaParticle particle, final Object... params) {
                final String sprite = NovaConfig.getString(particle.getParticleVO().sprite, -1);

                if (sprite == null) {
                    return;
                }

                final String formattedSprite = sprite.replace(NovaConfig.$SD, mUIConfigVO.texture_manager.cache_dir);
                // get the loaded frames
                AtlasFrameSet frames = null;

                // apply the texture
                if (formattedSprite.startsWith(NovaConfig.$TEXT)) {
                    // set floatie text texture
                    // frames.setTexture(getFloatieTextTexture((String) NovaConfig.getParamValue($TEXT, formattedSprite, params)));
                } else if (formattedSprite.startsWith(NovaConfig.$SPRITE)) {
                    final String decodedSprite = (String) NovaConfig.getParamValue(NovaConfig.$SPRITE, formattedSprite, params);
                    // load the frames
                    frames = getUriAtlas(decodedSprite, true);
                    // frames.setTexture(getUriTexture(decodedSprite.replaceFirst(UIConfig.FILE_JSON, UIConfig.FILE_PNG), true));
                } else {
                    frames = getUriAtlas(formattedSprite, true);
                    // frames.setTexture(getUriTexture(formattedSprite.replaceFirst(UIConfig.FILE_JSON, UIConfig.FILE_PNG), true));
                }

                // apply the frames
                particle.setAtlasFrameSet(frames);
            }
        };

        return mNovaDelegator;
    }

    /**
     * Clear and reset everything for memory saving
     */
    public void reset() {
        Log.w(TAG, "reset()");

        synchronized (mAtlasFrames) {
            // also release the textures
            Set<String> keys = mAtlasFrames.keySet();
            for (String key : keys) {
                mAtlasFrames.get(key).setTexture(null);
            }

            mAtlasFrames.clear();
        }
    }

}