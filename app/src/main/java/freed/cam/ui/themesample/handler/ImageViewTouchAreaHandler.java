/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package freed.cam.ui.themesample.handler;

/**
 * Created by troop on 09.06.2015.
 */

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.FocusRect;

/**
 * This class handles touch events that happens to the attached imageview and moves them
 * and return the values when its moving or a click happen
 */
public class ImageViewTouchAreaHandler implements OnTouchListener
{
    private final CameraWrapperInterface cameraUiWrapper;
    private final I_TouchListnerEvent touchListnerEvent;
    private final ImageView imageView;
    private float x;
    private float y;
    private float difx;
    private float dify;
    private final Handler longClickHandler;

    public interface I_TouchListnerEvent
    {
        void onAreaCHanged(FocusRect imageRect, int previewWidth, int previewHeight);
        void OnAreaClick(int x, int y);
        void OnAreaLongClick(int x, int y);
        void IsMoving(boolean moving);
    }

    /**
     *
     * @param imageView the view that should get moved
     */
    public ImageViewTouchAreaHandler(ImageView imageView, CameraWrapperInterface cameraUiWrapper, I_TouchListnerEvent touchListnerEvent)
    {
        this.imageView = imageView;
        this.cameraUiWrapper = cameraUiWrapper;
        recthalf = imageView.getWidth()/2;
        this.touchListnerEvent = touchListnerEvent;
        allowDrag = true;
        longClickHandler = new Handler();
    }


    /**
     * if set to true the imageview is dragable
     */
    private final boolean allowDrag;
    /**
     * distance in pixel? to move bevor it gets detected as move
     */
    private final int distance = 20;
    /**
     * the start values that gets set on action down
     */
    private int startX;
    private int startY;
    /**
     * holdes the time when last action down happend
     */
    private long start;
    private static final int MAX_DURATION = 1000;
    /*
    the area where the imageview is on screen
     */
    private FocusRect imageRect;
    /**
     * true if a move was detected
     */
    private boolean moving;
    /**
     * half size from the imageview to calculate the center postion
     */
    private int recthalf;

    private int getDistance(int startvalue, int currentvalue)
    {
        int dis = startvalue - currentvalue;
        if (dis < 0)
            dis = dis *-1;
        return dis;
    }

    private boolean longClickHappen;

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        boolean ret = true;
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                startX = (int)event.getX() - (int) imageView.getX();
                startY =(int) event.getY() - (int) imageView.getY();
                start = System.currentTimeMillis();
                longClickHandler.postDelayed(longClickRunnable, MAX_DURATION);
                longClickHappen = false;
                break;
            case MotionEvent.ACTION_MOVE:

                difx = x - imageView.getX();
                dify = y - imageView.getY();
                int xd = getDistance(startX, (int) difx);
                int yd = getDistance(startY, (int) dify);

                if (allowDrag) {
                    if (event.getX() - difx > cameraUiWrapper.getMargineLeft() && event.getX() - difx + imageView.getWidth() < cameraUiWrapper.getMargineLeft() + cameraUiWrapper.getPreviewWidth())
                        imageView.setX(event.getX() - difx);
                    if (event.getY() - dify > cameraUiWrapper.getMargineTop() && event.getY() - dify + imageView.getHeight() < cameraUiWrapper.getMargineTop() + cameraUiWrapper.getPreviewHeight())
                        imageView.setY(event.getY() - dify);
                    if ((xd >= distance || yd >= distance) && !moving) {
                        moving = true;
                        longClickHandler.removeCallbacks(longClickRunnable);
                        ret = false;
                        if (touchListnerEvent != null)
                            touchListnerEvent.IsMoving(true);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:


                if (moving)
                {
                    longClickHandler.removeCallbacks(longClickRunnable);
                    moving = false;
                    x = 0;
                    y = 0;
                    difx = 0;
                    dify = 0;
                    recthalf = imageView.getWidth() /2;
                    imageRect = new FocusRect((int) imageView.getX() , (int) imageView.getX() + imageView.getWidth(), (int) imageView.getY(), (int) imageView.getY() + imageView.getWidth(),(int) imageView.getX(),(int) imageView.getY());
                    if (touchListnerEvent != null) {
                        touchListnerEvent.onAreaCHanged(imageRect, cameraUiWrapper.getPreviewWidth(), cameraUiWrapper.getPreviewHeight());
                        touchListnerEvent.IsMoving(false);
                    }
                }
                else
                {
                    if (!longClickHappen)
                    {
                        longClickHandler.removeCallbacks(longClickRunnable);
                        touchListnerEvent.OnAreaClick((int) imageView.getX() + (int) event.getX(), (int) imageView.getY() + (int) event.getY());
                    }
                }
                ret = false;

                break;
        }
        return ret;
    }

    private final Runnable longClickRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            longClickHappen = true;
            touchListnerEvent.OnAreaLongClick((int) imageView.getX(), (int) imageView.getY());
        }
    };
}
