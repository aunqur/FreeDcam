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

package freed.cam.apis.sonyremote.parameters;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.parameters.AbstractParameterHandler;
import freed.cam.apis.basecamera.parameters.Parameters;
import freed.cam.apis.basecamera.parameters.modes.ModuleParameters;
import freed.cam.apis.sonyremote.CameraHolderSony;
import freed.cam.apis.sonyremote.FocusHandler;
import freed.cam.apis.sonyremote.SonyCameraRemoteFragment;
import freed.cam.apis.sonyremote.modules.I_CameraStatusChanged;
import freed.cam.apis.sonyremote.modules.PictureModuleSony;
import freed.cam.apis.sonyremote.parameters.manual.BaseManualParameterSony;
import freed.cam.apis.sonyremote.parameters.manual.ExposureCompManualParameterSony;
import freed.cam.apis.sonyremote.parameters.manual.PreviewZoomManual;
import freed.cam.apis.sonyremote.parameters.manual.ProgramShiftManualSony;
import freed.cam.apis.sonyremote.parameters.manual.WbCTManualSony;
import freed.cam.apis.sonyremote.parameters.manual.ZoomManualSony;
import freed.cam.apis.sonyremote.parameters.modes.BaseModeParameterSony;
import freed.cam.apis.sonyremote.parameters.modes.ContShootModeParameterSony;
import freed.cam.apis.sonyremote.parameters.modes.FocusModeSony;
import freed.cam.apis.sonyremote.parameters.modes.FocusPeakSony;
import freed.cam.apis.sonyremote.parameters.modes.I_SonyApi;
import freed.cam.apis.sonyremote.parameters.modes.NightModeSony;
import freed.cam.apis.sonyremote.parameters.modes.ObjectTrackingSony;
import freed.cam.apis.sonyremote.parameters.modes.PictureFormatSony;
import freed.cam.apis.sonyremote.parameters.modes.PictureSizeSony;
import freed.cam.apis.sonyremote.parameters.modes.ScalePreviewModeSony;
import freed.cam.apis.sonyremote.parameters.modes.WhiteBalanceModeSony;
import freed.cam.apis.sonyremote.parameters.modes.ZoomSettingSony;
import freed.cam.apis.sonyremote.sonystuff.SimpleCameraEventObserver;
import freed.cam.apis.sonyremote.sonystuff.SimpleRemoteApi;
import freed.cam.apis.sonyremote.sonystuff.SimpleStreamSurfaceView;
import freed.utils.FreeDPool;
import freed.utils.Log;

/**
 * Created by troop on 13.12.2014.
 */
public class ParameterHandler extends AbstractParameterHandler implements SimpleCameraEventObserver.ChangeListener
{
    private final String TAG = ParameterHandler.class.getSimpleName();
    public SimpleRemoteApi mRemoteApi;
    public Set<String> mAvailableCameraApiSet;
    private final List<I_SonyApi> parametersChangedList;
    private final SimpleStreamSurfaceView surfaceView;
    private final CameraWrapperInterface cameraUiWrapper;
    private String cameraStatus = "IDLE";

    public I_CameraStatusChanged CameraStatusListner;
    public CameraHolderSony.I_CameraShotMode cameraShotMode;


    public ParameterHandler(CameraWrapperInterface cameraUiWrapper, SimpleStreamSurfaceView surfaceView, Context context)
    {
        super(cameraUiWrapper);
        parametersChangedList = new ArrayList<>();
        this.surfaceView = surfaceView;
        this.cameraUiWrapper =cameraUiWrapper;
    }

    public void SetCameraApiSet(Set<String> mAvailableCameraApiSet)
    {
        this.mAvailableCameraApiSet = mAvailableCameraApiSet;

        Log.d(TAG, "Throw parametersChanged");
        throwSonyApiChanged(mAvailableCameraApiSet);

    }

    public String GetCameraStatus()
    { return cameraStatus;}

    private void throwSonyApiChanged(Set<String> mAvailableCameraApiSet) {
        for (int i = 0; i < parametersChangedList.size(); i++)
        {
            if (parametersChangedList.get(i) == null)
            {
                parametersChangedList.remove(i);
                i--;
            }
            else
                parametersChangedList.get(i).SonyApiChanged(mAvailableCameraApiSet);
        }
    }

    private void createParameters()
    {
        add(Parameters.Module, new ModuleParameters(cameraUiWrapper));
        add(Parameters.PictureSize, new PictureSizeSony(mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.PictureSize));

        add(Parameters.PictureFormat, new PictureFormatSony(mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.PictureFormat));

        add(Parameters.FlashMode, new BaseModeParameterSony("getFlashMode", "setFlashMode", "getAvailableFlashMode", mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.FlashMode));

        add(Parameters.M_ExposureCompensation, new BaseModeParameterSony("getExposureMode", "setExposureMode", "getAvailableExposureMode", mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.M_ExposureCompensation));

        add(Parameters.ContShootMode, new ContShootModeParameterSony(mRemoteApi, cameraUiWrapper.getModuleHandler()));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.ContShootMode));

        add(Parameters.ContShootModeSpeed, new BaseModeParameterSony("getContShootingSpeed", "setContShootingSpeed", "getAvailableContShootingSpeed", mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.ContShootModeSpeed));

        add(Parameters.FocusMode, new FocusModeSony("getFocusMode", "setFocusMode", "getAvailableFocusMode", mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.FocusMode));

        add(Parameters.ObjectTracking, new ObjectTrackingSony(mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.ObjectTracking));

        add(Parameters.ZoomSetting, new ZoomSettingSony(mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.ZoomSetting));


        /*Zoom = new ZoomManualSony(cameraUiWrapper);
        parametersChangedList.add((ZoomManualSony) Zoom);*/
        add(Parameters.M_ExposureTime, new BaseManualParameterSony("getShutterSpeed", "getAvailableShutterSpeed","setShutterSpeed", cameraUiWrapper));
        parametersChangedList.add((BaseManualParameterSony) get(Parameters.M_ExposureTime));

        add(Parameters.M_Fnumber, new BaseManualParameterSony("getFNumber","getAvailableFNumber","setFNumber", cameraUiWrapper));
        parametersChangedList.add((BaseManualParameterSony) get(Parameters.M_Fnumber));

        add(Parameters.M_ManualIso, new BaseManualParameterSony("getIsoSpeedRate", "getAvailableIsoSpeedRate","setIsoSpeedRate", cameraUiWrapper));
        parametersChangedList.add((BaseManualParameterSony) get(Parameters.M_ManualIso));

        add(Parameters.M_ExposureCompensation, new ExposureCompManualParameterSony(cameraUiWrapper));
        parametersChangedList.add((BaseManualParameterSony) get(Parameters.M_ExposureCompensation));

        add(Parameters.M_ProgramShift, new ProgramShiftManualSony(cameraUiWrapper));
        parametersChangedList.add((BaseManualParameterSony) get(Parameters.M_ProgramShift));

        add(Parameters.M_Whitebalance, new WbCTManualSony(cameraUiWrapper));
        parametersChangedList.add((BaseManualParameterSony) get(Parameters.M_Whitebalance));

        add(Parameters.WhiteBalanceMode, new WhiteBalanceModeSony(mRemoteApi, (WbCTManualSony) get(Parameters.M_Whitebalance)));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.WhiteBalanceMode));

        add(Parameters.PostViewSize, new BaseModeParameterSony("getPostviewImageSize","setPostviewImageSize","getAvailablePostviewImageSize", mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.PostViewSize));

        add(Parameters.VideoSize, new BaseModeParameterSony("getMovieQuality", "setMovieQuality", "getAvailableMovieQuality", mRemoteApi));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.VideoSize));

        add(Parameters.Focuspeak, new FocusPeakSony(surfaceView));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.Focuspeak));

        add(Parameters.NightMode, new NightModeSony(surfaceView));
        parametersChangedList.add((BaseModeParameterSony) get(Parameters.NightMode));

        add(Parameters.M_PreviewZoom, new PreviewZoomManual(surfaceView, cameraUiWrapper));

        add(Parameters.scalePreview, new ScalePreviewModeSony(surfaceView));

    }

    public void SetRemoteApi(SimpleRemoteApi api)
    {
        mRemoteApi = api;
        createParameters();
    }

    @Override
    public void SetFocusAREA(Rect focusAreas) {

    }

    @Override
    public void SetPictureOrientation(int or) {

    }

    @Override
    public float[] getFocusDistances() {
        return new float[0];
    }

    @Override
    public float getCurrentExposuretime() {
        return 0;
    }

    @Override
    public int getCurrentIso() {
        return 0;
    }


    @Override
    public void onShootModeChanged(String shootMode) {
        if(cameraShotMode != null )
            cameraShotMode.onShootModeChanged(shootMode);
    }

    @Override
    public void onCameraStatusChanged(String status)
    {
        //if (cameraStatus.equals(status))
        //    return;
        cameraStatus = status;
        Log.d(TAG, "Camerastatus:" + cameraStatus);
        if (CameraStatusListner != null)
            CameraStatusListner.onCameraStatusChanged(status);
    }

    @Override
    public void onTimout() {
        cameraUiWrapper.onCameraError("Camera connection timed out");
        ((SonyCameraRemoteFragment)cameraUiWrapper).stopEventObserver();
    }

    @Override
    public void onApiListModified(List<String> apis) {

        synchronized (mAvailableCameraApiSet) {
            mAvailableCameraApiSet.clear();
            for (String api : apis) {
                mAvailableCameraApiSet.add(api);
            }
            SetCameraApiSet(mAvailableCameraApiSet);
        }
    }

    @Override
    public void onZoomPositionChanged(int zoomPosition)
    {
        //((ZoomManualSony)Zoom).setZoomsHasChanged(zoomPosition);
    }

    @Override
    public void onIsoChanged(String iso)
    {
        get(Parameters.M_ManualIso).fireStringValueChanged(iso);
    }

    @Override
    public void onIsoValuesChanged(String[] isovals) {
        get(Parameters.M_ManualIso).fireStringValuesChanged(isovals);
    }

    @Override
    public void onFnumberValuesChanged(String[] fnumbervals) {
        get(Parameters.M_Fnumber).fireStringValuesChanged(fnumbervals);
    }

    @Override
    public void onExposureCompensationMaxChanged(int epxosurecompmax) {
        //parameterHandler.ManualExposure.BackgroundMaxValueChanged(epxosurecompmax);
    }

    @Override
    public void onExposureCompensationMinChanged(int epxosurecompmin) {
        //parameterHandler.ManualExposure.BackgroundMinValueChanged(epxosurecompmin);
    }

    @Override
    public void onExposureCompensationChanged(int epxosurecomp) {
        get(Parameters.M_ExposureCompensation).fireIntValueChanged(epxosurecomp);
    }

    @Override
    public void onShutterSpeedChanged(String shutter) {
        get(Parameters.M_ExposureTime).fireStringValueChanged(shutter);
    }

    @Override
    public void onShutterSpeedValuesChanged(String[] shuttervals) {
        get(Parameters.M_ExposureTime).fireStringValuesChanged(shuttervals);
    }

    @Override
    public void onFlashChanged(String flash)
    {
        Log.d(TAG, "Fire ONFLashCHanged");
        get(Parameters.FlashMode).fireStringValueChanged(flash);
    }

    @Override
    public void onFocusLocked(boolean locked) {
        ((FocusHandler) cameraUiWrapper.getFocusHandler()).onFocusLock(locked);
    }

    @Override
    public void onWhiteBalanceValueChanged(String wb)
    {
        get(Parameters.WhiteBalanceMode).fireStringValueChanged(wb);
        if (get(Parameters.WhiteBalanceMode).GetStringValue().equals("Color Temperature") && get(Parameters.M_Whitebalance) != null)
            get(Parameters.M_Whitebalance).fireIsSupportedChanged(true);
        else
            get(Parameters.M_Whitebalance).fireIsSupportedChanged(false);
    }

    @Override
    public void onImagesRecieved(final String[] url)
    {
        FreeDPool.Execute(new Runnable() {
            @Override
            public void run() {
                for (String s : url)
                {
                    if (cameraUiWrapper.getModuleHandler().getCurrentModule() instanceof PictureModuleSony)
                    {
                        PictureModuleSony pictureModuleSony = (PictureModuleSony) cameraUiWrapper.getModuleHandler().getCurrentModule();
                        try {
                            pictureModuleSony.onPictureTaken(new URL(s));
                        }catch (MalformedURLException ex) {
                            Log.WriteEx(ex);
                        }
                    }
                }
            }});
    }

    @Override
    public void onFnumberChanged(String fnumber) {
        get(Parameters.M_Fnumber).fireStringValueChanged(fnumber);
    }

    @Override
    public void onLiveviewStatusChanged(boolean status) {

    }

    @Override
    public void onStorageIdChanged(String storageId) {

    }

    @Override
    public void onExposureModesChanged(String[] expomode)
    {
        get(Parameters.ExposureMode).fireStringValuesChanged(expomode);
    }

    @Override
    public void onImageFormatChanged(String imagesize) {
        if (imagesize!= null && !TextUtils.isEmpty(imagesize))
            get(Parameters.PictureFormat).fireStringValueChanged(imagesize);
    }

    @Override
    public void onImageFormatsChanged(String[] imagesize) {
        get(Parameters.PictureFormat).fireStringValuesChanged(imagesize);
    }

    @Override
    public void onImageSizeChanged(String imagesize) {
        if (imagesize!= null && !TextUtils.isEmpty(imagesize))
            get(Parameters.PictureSize).fireStringValueChanged(imagesize);
    }

    @Override
    public void onContshotModeChanged(String imagesize) {
        if (imagesize!= null && !TextUtils.isEmpty(imagesize))
            get(Parameters.ContShootMode).fireStringValueChanged(imagesize);
    }

    @Override
    public void onContshotModesChanged(String[] imagesize) {
        get(Parameters.ContShootMode).fireStringValuesChanged(imagesize);
    }

    @Override
    public void onFocusModeChanged(String imagesize) {
        if (imagesize!= null && !TextUtils.isEmpty(imagesize))
            get(Parameters.FocusMode).fireStringValueChanged(imagesize);
    }

    @Override
    public void onFocusModesChanged(String[] imagesize) {
        get(Parameters.FocusMode).fireStringValuesChanged(imagesize);
    }

    @Override
    public void onPostviewModeChanged(String imagesize) {
        if (imagesize!= null && !TextUtils.isEmpty(imagesize))
            get(Parameters.PostViewSize).fireStringValueChanged(imagesize);
    }

    @Override
    public void onPostviewModesChanged(String[] imagesize) {
        get(Parameters.PostViewSize).fireStringValuesChanged(imagesize);
    }

    @Override
    public void onTrackingFocusModeChanged(String imagesize) {
        get(Parameters.ObjectTracking).fireStringValueChanged(imagesize);
    }

    @Override
    public void onTrackingFocusModesChanged(String[] imagesize) {
        get(Parameters.ObjectTracking).fireStringValuesChanged(imagesize);
    }

    @Override
    public void onZoomSettingValueCHanged(String value) {
        get(Parameters.ZoomSetting).fireStringValueChanged(value);
    }

    @Override
    public void onZoomSettingsValuesCHanged(String[] values) {
        get(Parameters.ZoomSetting).fireStringValuesChanged(values);
    }

    @Override
    public void onExposureModeChanged(String expomode) {
        if (expomode == null && TextUtils.isEmpty(expomode))
            return;
        if (!get(Parameters.ExposureMode).GetStringValue().equals(expomode))
            get(Parameters.ExposureMode).fireStringValueChanged(expomode);
        if (expomode.equals("Intelligent Auto")|| expomode.equals("Superior Auto"))
            get(Parameters.WhiteBalanceMode).fireIsSupportedChanged(false);
        else
            get(Parameters.WhiteBalanceMode).fireIsSupportedChanged(true);
    }
}
