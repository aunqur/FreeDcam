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

package freed.cam.apis.camera2.modules;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.VideoSource;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.troop.freedcam.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract;
import freed.cam.apis.basecamera.record.VideoRecorder;
import freed.cam.apis.camera2.Camera2Fragment;
import freed.cam.apis.camera2.CameraHolderApi2;
import freed.cam.apis.camera2.parameters.modes.VideoProfilesApi2;
import freed.cam.ui.themesample.handler.UserMessageHandler;
import freed.settings.SettingKeys;
import freed.settings.SettingsManager;
import freed.utils.Log;
import freed.utils.VideoMediaProfile;

/**
 * Created by troop on 26.11.2015.
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
public class VideoModuleApi2 extends AbstractModuleApi2
{
    private final String TAG = VideoModuleApi2.class.getSimpleName();
    private boolean isRecording;
    private VideoMediaProfile currentVideoProfile;
    private Surface previewsurface;
    private Surface recorderSurface;
    private File recordingFile;

    //private MediaRecorder mediaRecorder;
    private VideoRecorder videoRecorder;
    protected Camera2Fragment cameraUiWrapper;

    public VideoModuleApi2( CameraWrapperInterface cameraUiWrapper, Handler mBackgroundHandler, Handler mainHandler) {
        super(cameraUiWrapper,mBackgroundHandler,mainHandler);
        this.cameraUiWrapper = (Camera2Fragment)cameraUiWrapper;
        name = cameraUiWrapper.getResString(R.string.module_video);
        videoRecorder = new VideoRecorder(cameraUiWrapper);
    }

    @Override
    public String ModuleName() {
        return name;
    }

    @Override
    public void DoWork()
    {
        if (cameraUiWrapper.getActivityInterface().getPermissionManager().hasRecordAudioPermission(null))
            startStopRecording();
    }

    private void startStopRecording()
    {
        if (isRecording)
            stopRecording();
        else
            startRecording();
    }

    @Override
    public void InitModule()
    {
        Log.d(TAG, "InitModule");
        super.InitModule();
        changeCaptureState(ModuleHandlerAbstract.CaptureStates.video_recording_stop);
        VideoProfilesApi2 profilesApi2 = (VideoProfilesApi2) parameterHandler.get(SettingKeys.VideoProfiles);
        currentVideoProfile = profilesApi2.GetCameraProfile(SettingsManager.get(SettingKeys.VideoProfiles).get());
        if (currentVideoProfile == null)
        {
            currentVideoProfile = profilesApi2.GetCameraProfile(SettingsManager.get(SettingKeys.VideoProfiles).getValues()[0]);
        }
        parameterHandler.get(SettingKeys.VideoProfiles).fireStringValueChanged(currentVideoProfile.ProfileName);
        videoRecorder = new VideoRecorder(cameraUiWrapper);
        startPreview();
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    @Override
    public void DestroyModule()
    {
        if (isRecording)
            stopRecording();
        Log.d(TAG, "DestroyModule");
        videoRecorder.release();
        cameraUiWrapper.captureSessionHandler.CloseCaptureSession();
        videoRecorder = null;
        previewsurface = null;
    }

    @Override
    public String LongName() {
        return "Video";
    }

    @Override
    public String ShortName() {
        return "Vid";
    }

    private void startRecording()
    {
        changeCaptureState(ModuleHandlerAbstract.CaptureStates.video_recording_start);
        Log.d(TAG, "startRecording");
        startPreviewVideo();
    }

    private void stopRecording()
    {
        Log.d(TAG, "stopRecording");
        videoRecorder.stop();
        cameraUiWrapper.captureSessionHandler.StopRepeatingCaptureSession();
        cameraUiWrapper.captureSessionHandler.RemoveSurface(recorderSurface);
        recorderSurface = null;
        isRecording = false;

        changeCaptureState(ModuleHandlerAbstract.CaptureStates.video_recording_stop);
        cameraUiWrapper.captureSessionHandler.StartRepeatingCaptureSession();
        fireOnWorkFinish(recordingFile);
        cameraUiWrapper.getActivityInterface().ScanFile(recordingFile);
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    @Override
    public void startPreview()
    {
        Size previewSize = getSizeForPreviewDependingOnImageSize(cameraHolder.map.getOutputSizes(ImageFormat.YUV_420_888), cameraHolder.characteristics, currentVideoProfile.videoFrameWidth, currentVideoProfile.videoFrameHeight);
        int sensorOrientation = cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int orientation = 0;
        switch (sensorOrientation)
        {
            case 90:
                orientation = 270;
                break;
            case 180:
                orientation =0;
                break;
            case 270: orientation = 90;
                break;
            case 0: orientation = 180;
                break;
        }
        final int w,h, or;
        w = previewSize.getWidth();
        h = previewSize.getHeight();
        or = orientation;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                cameraUiWrapper.captureSessionHandler.SetTextureViewSize(w, h, or,or+180,false);
            }
        });

        SurfaceTexture texture = cameraUiWrapper.captureSessionHandler.getSurfaceTexture();
        texture.setDefaultBufferSize(currentVideoProfile.videoFrameWidth, currentVideoProfile.videoFrameHeight);
        previewsurface = new Surface(texture);
        cameraUiWrapper.captureSessionHandler.AddSurface(previewsurface,true);
        cameraUiWrapper.captureSessionHandler.CreateCaptureSession();
    }

    public Size getSizeForPreviewDependingOnImageSize(Size[] choices, CameraCharacteristics characteristics, int mImageWidth, int mImageHeight)
    {
        List<Size> sizes = new ArrayList<>();
        Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        double ratio = (double)mImageWidth/mImageHeight;
        for (Size s : choices)
        {
            if (s.getWidth() <= cameraUiWrapper.captureSessionHandler.displaySize.x && s.getHeight() <= cameraUiWrapper.captureSessionHandler.displaySize.y && (double)s.getWidth()/s.getHeight() == ratio)
                sizes.add(s);

        }
        if (sizes.size() > 0) {
            return Collections.max(sizes, new CameraHolderApi2.CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable previewSize size");
            return choices[0];
        }
    }

    @Override
    public void stopPreview() {
        DestroyModule();
    }


    @TargetApi(VERSION_CODES.LOLLIPOP)
    private void startPreviewVideo()
    {
        recordingFile = new File(cameraUiWrapper.getActivityInterface().getStorageHandler().getNewFilePath(SettingsManager.getInstance().GetWriteExternal(), ".mp4"));
        videoRecorder.setRecordingFile(recordingFile);
        videoRecorder.setErrorListener(new OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.d(TAG, "error MediaRecorder:" + what + "extra:" + extra);
                changeCaptureState(ModuleHandlerAbstract.CaptureStates.video_recording_stop);
            }
        });

        videoRecorder.setInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
                {
                    recordnextFile(mr);
                }
                else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                {
                    recordnextFile(mr);
                }
            }
        });

        if (SettingsManager.getInstance().getApiString(SettingsManager.SETTING_LOCATION).equals(cameraUiWrapper.getResString(R.string.on_))){
            Location location = cameraUiWrapper.getActivityInterface().getLocationManager().getCurrentLocation();
            if (location != null)
                videoRecorder.setLocation(location);
        }
        else
            videoRecorder.setLocation(null);

        videoRecorder.setCurrentVideoProfile(currentVideoProfile);
        videoRecorder.setVideoSource(VideoSource.SURFACE);
        videoRecorder.setOrientation(0);
        //videoRecorder.setPreviewSurface(previewsurface);

        videoRecorder.prepare();
        recorderSurface = videoRecorder.getSurface();
        cameraUiWrapper.captureSessionHandler.AddSurface(recorderSurface,true);

        if (currentVideoProfile.Mode != VideoMediaProfile.VideoMode.Highspeed)
            cameraUiWrapper.captureSessionHandler.CreateCaptureSession(previewrdy);
        else
            cameraUiWrapper.captureSessionHandler.CreateHighSpeedCaptureSession(previewrdy);
    }

    private void recordnextFile(MediaRecorder mr) {
        stopRecording();
        startRecording();
    }

    private final StateCallback previewrdy = new StateCallback()
    {

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession)
        {
            cameraUiWrapper.captureSessionHandler.SetCaptureSession(cameraCaptureSession);
            if (currentVideoProfile.Mode != VideoMediaProfile.VideoMode.Highspeed) {

                cameraUiWrapper.captureSessionHandler.StartRepeatingCaptureSession();
            }
            else
            {
                cameraUiWrapper.captureSessionHandler.StartHighspeedCaptureSession();
            }
            videoRecorder.start();
            isRecording = true;

        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession)
        {
            Log.d(TAG, "Failed to Config CaptureSession");
            UserMessageHandler.sendMSG("Failed to Config CaptureSession",false);
        }
    };

    @Override
    public void internalFireOnWorkDone(File file) {

    }
}
