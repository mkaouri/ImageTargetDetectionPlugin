package com.cloudoki.imagedetectionplugin;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Camera;
import org.rajawali3d.Object3D;
import org.rajawali3d.lights.PointLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.materials.textures.VideoTexture;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.renderer.RajawaliRenderer;
import org.rajawali3d.scene.RajawaliScene;
import org.rajawali3d.util.ObjectColorPicker;
import org.rajawali3d.util.OnObjectPickedListener;

public class OGLRenderer extends RajawaliRenderer implements OnObjectPickedListener {

    private MediaPlayer mMediaPlayer;
    private VideoTexture mVideoTexture;
    private ObjectColorPicker mPicker;
    private Object3D object3d;
    private Plane screen;
    private RajawaliScene currentScene;
    private double x, y, z;
    private Camera currentCam;

    public OGLRenderer(Context context) {
        super(context);
        setFrameRate(60);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    @Override
    protected void onRender(long elapsedRealtime, double deltaTime) {
        super.onRender(elapsedRealtime, deltaTime);
        if(mVideoTexture != null) {
            mVideoTexture.update();
            move();
        } else if(object3d != null) {
            object3d.rotate(Vector3.Axis.Y, 1.0);
            move();
        } else {
            resetPosition();
        }
    }

    @Override
    protected void initScene() {
        currentScene = getCurrentScene();
        mPicker = new ObjectColorPicker(this);
        mPicker.setOnObjectPickedListener(this);

        PointLight pointLight = new PointLight();
        pointLight.setPower(8);
        pointLight.setPosition(0, 0, 0);
        pointLight.setLookAt(0, 0, 0);

        currentScene.addLight(pointLight);
        currentScene.setBackgroundColor(0x00000000);

        currentCam = getCurrentCamera();
        currentCam.enableLookAt();
        currentCam.getPosition();
        currentCam.setLookAt(0, 0, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaPlayer != null)
            mMediaPlayer.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaPlayer != null)
            mMediaPlayer.start();
    }

    @Override
    public void onRenderSurfaceDestroyed(SurfaceTexture surfaceTexture) {
        super.onRenderSurfaceDestroyed(surfaceTexture);
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
    }

    public void getObjectAt(float x, float y) {
        mPicker.getObjectAt(x, y);
    }

    @Override
    public void onObjectPicked(Object3D object) {
        if(mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
            else
                mMediaPlayer.start();
        }
    }

    public void addObject(String obj, String tex, Activity activity){
        try {
            int resID = getResourceId(obj, "raw", mContext.getPackageName(), activity);
            LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, resID);
            Material material = new Material();
            material.enableLighting(true);
            material.setDiffuseMethod(new DiffuseMethod.Lambert());
            material.setColor(0);
            if(tex != null)
                material.addTexture(new Texture("texture", getResourceId(tex, "raw", mContext.getPackageName(), activity)));
            objParser.parse();
            object3d = objParser.getParsedObject();
            object3d.setDoubleSided(true);
            object3d.setMaterial(material);
            object3d.setPosition(0, 0, 0);
            object3d.setScale(0.1);
            currentScene.addChild(object3d);
        } catch (ParsingException | ATexture.TextureException e) {
            Log.e("DEBUG", "Error " + e.toString());
        }
    }

    public void removeObject(){
        if(object3d != null){
            resetPosition();
            currentScene.removeChild(object3d);
            object3d = null;
        }
    }

    public void setVideo(String video, Activity activity) {
        Material material = new Material();
        material.setColorInfluence(0);
        try {
            int resID = getResourceId(video, "raw", mContext.getPackageName(), activity);
            mMediaPlayer = MediaPlayer.create(getContext(), resID);
            mMediaPlayer.setLooping(true);

            mVideoTexture = new VideoTexture("videoTexture", mMediaPlayer);
            material.addTexture(mVideoTexture);

            screen = new Plane(1, 1, 1, 1, Vector3.Axis.Z);
            screen.setMaterial(material);
            screen.setRotY(180);
            screen.setX(0.5f);
            screen.setY(-1f);
            screen.setZ(0f);
            screen.setScale(0.8);
            currentScene.addChild(screen);

            mPicker.registerObject(screen);
            mMediaPlayer.start();
        } catch (ATexture.TextureException e) {
            Log.e("DEBUG", "Error " + e.toString());
        }
    }

    public void removeVideo(){
        if(mMediaPlayer != null && screen != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            resetPosition();
            currentScene.removeChild(screen);
            screen = null;
        }
    }

    public void resetPosition(){
        currentCam.setPosition(0, 0, 4);
//        if(object3d != null) {
//            object3d.setPosition(0,0, 0);
//        } else if (screen != null) {
//            screen.setPosition(0, 0, 0);
//        }
    }

    public void move(){
        Vector3 oldpos = currentCam.getPosition();
        currentCam.setPosition(oldpos.x + x, oldpos.y + y, oldpos.z + z);
//        if(object3d != null) {
//            Vector3 oldpos = object3d.getPosition();
//            object3d.setPosition(oldpos.x + x, oldpos.y + y, oldpos.z + z);
//        } else if (screen != null) {
//            Vector3 oldpos = screen.getPosition();
//            screen.setPosition(oldpos.x + x, oldpos.y + y, oldpos.z + z);
//        }
    }



    public void set3DObjectSize(double d) {
        if (object3d != null)
            object3d.setScale(d);
    }

    public Vector3 get3DObjectSize() {
        return object3d.getScale();
    }

    public void setVideoSize(double d) {
        if (screen != null)
            screen.setScale(d);
    }

    public Vector3 getVideoSize() {
        return screen.getScale();
    }

    public void setCoordinates(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getResourceId(String pVariableName, String pResourcename, String pPackageName, Activity activity)
    {
        try {
            return activity.getResources().getIdentifier(pVariableName, pResourcename, pPackageName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
