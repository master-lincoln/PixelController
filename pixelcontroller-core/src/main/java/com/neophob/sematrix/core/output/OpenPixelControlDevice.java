/**
 * Copyright (C) 2011-2013 Michael Vogt <michu@neophob.com>
 *
 * This file is part of PixelController.
 *
 * PixelController is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PixelController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PixelController.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.neophob.sematrix.core.output;

import com.neophob.sematrix.core.properties.ApplicationConfigurationHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Send frames out via UDP
 *
 * @author michu
 * 
 */
public class OpenPixelControlDevice extends OnePanelResolutionAwareOutput {

	private static final Logger LOG = Logger.getLogger(OpenPixelControlDevice.class.getName());

	private Socket socket;

	private String targetHost;
	private int targetPort;
	private int errorCounter=0;

	/**
	 *
	 * @param ph ApplicationConfigurationHelper
	 */
	public OpenPixelControlDevice(ApplicationConfigurationHelper ph) {
		super(OutputDeviceEnum.OpenPixelControl, ph, 8);

		targetHost = ph.getOpcIp();
		targetPort = ph.getOpcPort();
		
		try {
            socket = new Socket(targetHost, targetPort);
            socket.setTcpNoDelay(true);
            
			this.initialized = true;
			LOG.log(Level.INFO, "Open Pixel Control device initialized, send data to {0}:{1}", 
					new String[] {this.targetHost, ""+this.targetPort});

		} catch (Exception e) {
			LOG.log(Level.WARNING, "failed to initialize Open Pixel Control device", e);
		}
	}


	/* (non-Javadoc)
	 * @see com.neophob.sematrix.core.output.Output#update()
	 */
	@Override
	public void update() {
		if (this.initialized) {			
			byte[] buffer = OutputHelper.convertBufferTo24bit(getTransformedBuffer(), colorFormat);
            
            // should be dispWidth * dispHeight * 3 (for rgb)
            int numBytes = buffer.length;
            byte[] opcHeader = new byte[4];
            // Channel: 0 (broadcast)
            opcHeader[0] = 0;
            // Command: 0 (set all pixel values)
            opcHeader[1] = 0;
            // numBytes high and low
            opcHeader[2] = (byte)((numBytes >> 8) & 0xFF);
            opcHeader[3] = (byte)(numBytes & 0xFF);
            
			try {
                OutputStream out = socket.getOutputStream();
                
				out.write(opcHeader);
				out.write(buffer);
                out.flush();
                
			} catch (IOException e) {
			    errorCounter++;
				LOG.log(Level.WARNING, "failed to send Open Pixel Control TCP data.", e);
			}
		}
	}

    @Override
    public boolean isSupportConnectionState() {
        return true;
    }

    @Override
    public boolean isConnected() {
        return initialized;
    }

    @Override
    public String getConnectionStatus(){
        if (initialized) {
            return "Target IP "+targetHost+":"+targetPort;            
        }
        return "Not connected!";
    }
    
	@Override
	public void close()	{
		if (initialized) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "failed to Open Pixel Control TCP socket.", e);
            }
		}	    
	}
	

	@Override
	public long getErrorCounter() {
	    return errorCounter;
	}

}

