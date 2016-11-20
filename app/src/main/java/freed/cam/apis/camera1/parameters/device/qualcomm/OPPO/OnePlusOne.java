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

package freed.cam.apis.camera1.parameters.device.qualcomm.OPPO;

import android.hardware.Camera.Parameters;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.parameters.modes.AbstractModeParameter;
import freed.cam.apis.basecamera.parameters.modes.MatrixChooserParameter;
import freed.cam.apis.camera1.parameters.device.BaseQcomDevice;
import freed.cam.apis.camera1.parameters.modes.OpCodeParameter;
import freed.dng.DngProfile;

/**
 * Created by troop on 01.06.2016.
 */
public class OnePlusOne extends BaseQcomDevice {


    public OnePlusOne(Parameters parameters, CameraWrapperInterface cameraUiWrapper) {
        super(parameters, cameraUiWrapper);
    }

    @Override
    public boolean IsDngSupported() {
        return true;
    }
    @Override
    public DngProfile getDngProfile(int filesize)
    {
        switch (filesize)
        {
            case 16424960:
                return new DngProfile(64, 4212, 3120, DngProfile.Mipi, DngProfile.RGGB, DngProfile.ROWSIZE,matrixChooserParameter.GetCustomMatrix(MatrixChooserParameter.NEXUS6));
            case 16473600: //oneplus
                return new DngProfile(16,4224,3120,DngProfile.Mipi,DngProfile.BGGR,5280, matrixChooserParameter.GetCustomMatrix(MatrixChooserParameter.NEXUS6));
            case 17522688:
                return new DngProfile(64, 4212, 3082, DngProfile.Qcom, DngProfile.RGGB, DngProfile.ROWSIZE, matrixChooserParameter.GetCustomMatrix(MatrixChooserParameter.NEXUS6));
        }
        return null;
    }

    @Override
    public AbstractModeParameter getOpCodeParameter() {
        return new OpCodeParameter(cameraUiWrapper.GetAppSettingsManager());
    }
/*
    @Override
    public ManualParameterInterface getCCTParameter() {
        return new BaseCCTManual(parameters, KEYS.WB_CURRENT_CCT,KEYS.MAX_WB_CCT, KEYS.MIN_WB_CCT, cameraUiWrapper,100f,KEYS.WB_MODE_MANUAL_CCT);
    }*/
}