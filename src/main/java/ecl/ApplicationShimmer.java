package ecl;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;
import com.shimmerresearch.algorithms.Filter;
import com.shimmerresearch.biophysicalprocessing.*;
import com.shimmerresearch.driver.BasicProcessWithCallBack;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerMsg;
import com.shimmerresearch.driver.Configuration.Shimmer3;
import com.shimmerresearch.driverUtilities.ChannelDetails.CHANNEL_TYPE;
import com.shimmerresearch.pcDriver.ShimmerPC;
import com.shimmerresearch.sensors.SensorPPG;
import com.shimmerresearch.sensors.SensorGSR;
import com.shimmerresearch.sensors.lsm303.SensorLSM303;
import com.shimmerresearch.sensors.mpu9x50.SensorMPU9X50;
import com.shimmerresearch.tools.bluetooth.BasicShimmerBluetoothManagerPc;

import edu.ucsd.sccn.LSL;

public class ApplicationShimmer extends BasicProcessWithCallBack {

	/**** SHIMMER ****/
	final static String kDeviceName = "Shimmer-9595";
	String kDevicePort = "COM6";
	final static double kSamplingRate = 128;
	
	ShimmerPC device;
	static BasicShimmerBluetoothManagerPc btManager = new BasicShimmerBluetoothManagerPc();
	private PPGtoHRwithHRV ppgToHrv;
	private boolean bFirstConfig=true;
	Filter lpFilter = null, hpFilter = null;
	
	/**** LSL ****/
	static LSL.StreamInfo lslInfo;
	static LSL.XMLElement lslChns;
	static LSL.StreamOutlet lslOutlet;
	
	/**** UTILS ****/
	private Scanner scnr;
	private int counter = 0;
	
	public static void main(String args[]) throws IOException, InterruptedException {
		ApplicationShimmer main = new ApplicationShimmer();
		main.initialize();
		main.setWaitForData(btManager.callBackObject);
		
		lslInfo = new LSL.StreamInfo("ShimmerGSR", "Physiological", 12, kSamplingRate, LSL.ChannelFormat.float32, "shmrGSR");
		lslChns = lslInfo.desc().append_child("channels");
		lslChns.append_child("channel").append_child_value("label", "TIMESTAMP").append_child_value("unit", Configuration.CHANNEL_UNITS.MILLISECONDS).append_child_value("type", "TIME");
		lslChns.append_child("channel").append_child_value("label", "ACCEL_X").append_child_value("unit", Configuration.CHANNEL_UNITS.ACCEL_CAL_UNIT).append_child_value("type", "ACCEL_X");
		lslChns.append_child("channel").append_child_value("label", "ACCEL_Y").append_child_value("unit", Configuration.CHANNEL_UNITS.ACCEL_CAL_UNIT).append_child_value("type", "ACCEL_Y");
		lslChns.append_child("channel").append_child_value("label", "ACCEL_Z").append_child_value("unit", Configuration.CHANNEL_UNITS.ACCEL_CAL_UNIT).append_child_value("type", "ACCEL_Z");
		lslChns.append_child("channel").append_child_value("label", "GYRO_P").append_child_value("unit", Configuration.CHANNEL_UNITS.GYRO_CAL_UNIT).append_child_value("type", "GYRO_P");
		lslChns.append_child("channel").append_child_value("label", "GYRO_R").append_child_value("unit", Configuration.CHANNEL_UNITS.GYRO_CAL_UNIT).append_child_value("type", "GYRO_R");
		lslChns.append_child("channel").append_child_value("label", "GYRO_Y").append_child_value("unit", Configuration.CHANNEL_UNITS.GYRO_CAL_UNIT).append_child_value("type", "GYRO_Y");
		lslChns.append_child("channel").append_child_value("label", "GSR_RES").append_child_value("unit", SensorGSR.channelGsrKOhms.mDefaultCalUnits).append_child_value("type", "GSR_RESISTANCE");
		lslChns.append_child("channel").append_child_value("label", "GSR_CON").append_child_value("unit", SensorGSR.channelGsrMicroSiemens.mDefaultCalUnits).append_child_value("type", "GSR_CONDUCTANCE");
		lslChns.append_child("channel").append_child_value("label", "PPG_RAW").append_child_value("unit", SensorPPG.channelPPG_A13.mDefaultCalUnits).append_child_value("type", "PPG_RAW");
		lslChns.append_child("channel").append_child_value("label", "HEART_RATE").append_child_value("unit", "bpm").append_child_value("type", "HEART_RATE");
		lslChns.append_child("channel").append_child_value("label", "HRV").append_child_value("unit", "").append_child_value("type", "HEART_RATE_VARIANCE");
		lslOutlet = new LSL.StreamOutlet(lslInfo);
		
		System.out.println("Hi, warm up done");
		System.out.println("press [h] while data streaming to disconnect device and go to exiting option");
	}
	
	public void initialize(){
		System.out.println("Which COM port are you gonna use? [like COM6]");
		scnr = new Scanner(System.in);
		kDevicePort = scnr.nextLine();
		
		btManager.connectShimmerThroughCommPort(kDevicePort);
	}
	
	public void whatDoYouWant() {
		System.out.println("What do you want to do now?");
		System.out.println("[e]Exit program [r]retry to connect");
		try {
			String s = scnr.nextLine();
			if (s == "e") {
				System.out.println("bye");
				lslOutlet.close();
				lslInfo.destroy();
				System.exit(0);
			}
			else if (s == "r") {
				btManager.connectShimmerThroughCommPort(kDevicePort);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void processMsgFromCallback(ShimmerMsg shimmerMSG) {
		
		// TODO Auto-generated method stub
		  int ind = shimmerMSG.mIdentifier;
		  Object object = (Object) shimmerMSG.mB;

		  switch (ind) {
		  case ShimmerPC.MSG_IDENTIFIER_STATE_CHANGE:
		  {
			  CallbackObject cb = (CallbackObject)object;
			  switch (cb.mState) {
			  case CONNECTING:
				  System.out.println("Connecting to: " + kDeviceName);
				  break;
			  case CONNECTED:
				  System.out.println("Connected!");
				  device = (ShimmerPC)btManager.getShimmerDeviceBtConnected(kDevicePort);
				  if (bFirstConfig) {
					  ShimmerPC clone = device.deepClone();
					  clone.setSensorEnabledState(Configuration.Shimmer3.SENSOR_ID.SHIMMER_LSM303_ACCEL, true);
					  clone.setAccelRange(0);
					  clone.setSensorEnabledState(Configuration.Shimmer3.SENSOR_ID.SHIMMER_MPU9X50_GYRO, true);
					  clone.setGyroRange(SensorMPU9X50.ListofMPU9X50GyroRangeConfigValues[0]);
					  clone.setSensorEnabledState(Configuration.Shimmer3.SENSOR_ID.SHIMMER_GSR, true);
					  clone.setGSRRange(SensorGSR.ListofGSRRangeConfigValues[4]);
					  clone.setSensorEnabledState(Configuration.Shimmer3.SENSOR_ID.HOST_PPG_A13, true);
					  btManager.configureShimmer(clone);
					  device.writeShimmerAndSensorsSamplingRate(kSamplingRate);
					  ppgToHrv = new PPGtoHRwithHRV(kSamplingRate);
					  
					  try {
						  double [] cutoff = {5.0};
						  lpFilter = new Filter(Filter.LOW_PASS, device.getSamplingRateShimmer(), cutoff);
						  cutoff[0] = 0.5;
						  hpFilter = new Filter(Filter.HIGH_PASS, device.getSamplingRateShimmer(), cutoff);
					  } catch (Exception e) {
						  e.printStackTrace();
					  }
					  bFirstConfig = false;
				  }
				  //device.startStreaming();
				  break;
			  case DISCONNECTED:
				  System.out.println("Disconnected...");
				  whatDoYouWant();
				  break;
			  case CONNECTION_FAILED:
				  System.out.println("Connection Failed...");
				  whatDoYouWant();
				  break;
			  case CONNECTION_LOST:
				  System.out.println("Connection Lost...");
				  whatDoYouWant();
				  break;
			  default:
				  break;
			  }
		  }
			  break;
		  case ShimmerPC.MSG_IDENTIFIER_NOTIFICATION_MESSAGE:
		  {
			  CallbackObject cb = (CallbackObject)object;
			  int msg = cb.mIndicator;
			  if (msg == ShimmerPC.NOTIFICATION_SHIMMER_FULLY_INITIALIZED) {
				  device.startStreaming();
			  }
			  if (msg == ShimmerPC.NOTIFICATION_SHIMMER_STOP_STREAMING) {
				  System.out.println("Device stopped streaming...");
			  } else if (msg == ShimmerPC.NOTIFICATION_SHIMMER_START_STREAMING) {
				  System.out.println("Device started streaming!");
			  } else {
				  // something
			  }
		  }
		  	  break;
		  case ShimmerPC.MSG_IDENTIFIER_DATA_PACKET:
		  {
			  double[] acc = {Double.NaN, Double.NaN, Double.NaN};	//x,y,z
			  double[] gyr = {Double.NaN, Double.NaN, Double.NaN};	//p,r,y
			  double[] gsr = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};	//resCAL,conCAL,resRAW,conRAW
			  double ppg = 0;
			  double ppgRaw = 0;
			  double ppgBuf = 0;
			  double hr = Double.NaN;
			  double hrv = Double.NaN;
			  double sysTime = Double.NaN;
			  int INVALID_RESULT = -1;
			  
			  ObjectCluster oc = (ObjectCluster)shimmerMSG.mB;
			  try {
				  //Acc X
				  Collection<FormatCluster> accXFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.ACCEL_WR_X);
				  FormatCluster axFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(accXFmt, CHANNEL_TYPE.CAL.toString()));	//retrieve calibrated data
				  if (axFmt != null) acc[0] = axFmt.mData;
				  //Acc Y
				  Collection<FormatCluster> accYFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y);
				  FormatCluster ayFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(accYFmt, CHANNEL_TYPE.CAL.toString()));
				  if (ayFmt != null) acc[1] = ayFmt.mData;
				  //Acc Z
				  Collection<FormatCluster> accZFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z);
				  FormatCluster azFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(accZFmt, CHANNEL_TYPE.CAL.toString()));
				  if (azFmt != null) acc[2] = azFmt.mData;
				  //Gyro pitch
				  Collection<FormatCluster> gyrPFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.GYRO_X);
				  FormatCluster gpFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gyrPFmt, CHANNEL_TYPE.CAL.toString()));	//retrieve calibrated data
				  if (gpFmt != null) gyr[0] = gpFmt.mData;
				  //Gyro roll
				  Collection<FormatCluster> gyrRFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.GYRO_Y);
				  FormatCluster grFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gyrRFmt, CHANNEL_TYPE.CAL.toString()));
				  if (grFmt != null) gyr[1] = grFmt.mData;
				  //Gyro yaw
				  Collection<FormatCluster> gyrYFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.GYRO_Z);
				  FormatCluster gyFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gyrYFmt, CHANNEL_TYPE.CAL.toString()));
				  if (gyFmt != null) gyr[2] = gyFmt.mData;
				  
				  //GSR res
				  Collection<FormatCluster> gsrResCalFmt = oc.getCollectionOfFormatClusters(SensorGSR.ObjectClusterSensorName.GSR_RESISTANCE);
				  FormatCluster gRCFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gsrResCalFmt, CHANNEL_TYPE.CAL.toString()));	//retrieve calibrated data
				  if (gRCFmt != null) gsr[0] = gRCFmt.mData;
				  //GSR con
				  Collection<FormatCluster> gsrConCalFmt = oc.getCollectionOfFormatClusters(SensorGSR.ObjectClusterSensorName.GSR_CONDUCTANCE);
				  FormatCluster gCCFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gsrConCalFmt, CHANNEL_TYPE.CAL.toString()));
				  if (gCCFmt != null) gsr[1] = gCCFmt.mData;
				  //GSR res RAW
				  Collection<FormatCluster> gsrResFmt = oc.getCollectionOfFormatClusters(SensorGSR.ObjectClusterSensorName.GSR_RESISTANCE);
				  FormatCluster gRRFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gsrResRawFmt, CHANNEL_TYPE.UNCAL.toString()));	//retrieve calibrated data
				  if (gRRFmt != null) gsr[2] = gRRFmt.mData;
				  //GSR con RAW
				  Collection<FormatCluster> gsrConRawFmt = oc.getCollectionOfFormatClusters(SensorGSR.ObjectClusterSensorName.GSR_CONDUCTANCE);
				  FormatCluster gCRFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(gsrConRawFmt, CHANNEL_TYPE.UNCAL.toString()));
				  if (gCRFmt != null) gsr[3] = gCRFmt.mData;
				  
				  //PPG
				  Collection<FormatCluster> ppgFmt = oc.getCollectionOfFormatClusters(SensorPPG.ObjectClusterSensorName.PPG_A13);
				  FormatCluster pFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(ppgFmt, CHANNEL_TYPE.CAL.toString()));	//retrieve calibrated data
				  if (pFmt != null) ppg = pFmt.mData;
				  //PPG RAW
				  Collection<FormatCluster> ppgRawFmt = oc.getCollectionOfFormatClusters(SensorPPG.ObjectClusterSensorName.PPG_A13);
				  FormatCluster pRFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(ppgRawFmt, CHANNEL_TYPE.UNCAL.toString()));	//retrieve calibrated data
				  if (pRFmt != null) ppgRaw = pRFmt.mData;
				  //Timestamp
				  Collection<FormatCluster> tsFmt = oc.getCollectionOfFormatClusters(Shimmer3.ObjectClusterSensorName.TIMESTAMP);
				  FormatCluster tFmt = ((FormatCluster)ObjectCluster.returnFormatCluster(tsFmt, CHANNEL_TYPE.CAL.toString()));
				  if (tFmt != null) sysTime = tFmt.mData;
				  
				  ppgBuf = ppg;
				  try {
					 ppgBuf = lpFilter.filterData(ppgBuf);
					 ppgBuf = hpFilter.filterData(ppgBuf);
				  } catch (Exception e) {
					  e.printStackTrace();
				  }
				  
				  hr = ppgToHrv.ppgToHrConversion(ppgBuf, sysTime);
				  hrv = ppgToHrv.getRRInterval();
				  
				  float[] samples = new float[15];
				  samples[0] = (float)sysTime;
				  samples[1] = (float)acc[0];
				  samples[2] = (float)acc[1];
				  samples[3] = (float)acc[2];
				  samples[4] = (float)gyr[0];
				  samples[5] = (float)gyr[1];
				  samples[6] = (float)gyr[2];
				  samples[7] = (float)gsr[0];
				  samples[8] = (float)gsr[1];
				  samples[9] = (float)gsr[2];
				  samples[10] = (float)gsr[3];
				  samples[11] = (float)ppg;
				  samples[12] = (float)ppgRaw;
				  samples[13] = (float)hr;
				  samples[14] = (float)hrv;
				  
				  lslOutlet.push_sample(samples);
				  
				  counter++;
				  if (counter == 128) {
					  System.out.println("TS: " + samples[0] + ", ACCX: " + samples[1] + ", ACCY: " + samples[2] + ", ACCZ: " + samples[3]);
					  System.out.println("GYROX: " + samples[4] + ", GYROY: " + samples[5] + ", GYROZ: " + samples[6]);
					  System.out.println("GSR-RES: " + samples[7] + ", GSR-CON: " + samples[8] + ", PPG: " + samples[11] + ", HR: " + samples[13] + ", HRV: " + samples[14]);
					  counter = 0;
				  }
				  
			  } catch (Exception e) {
				  e.printStackTrace();
			  }
		  }
		  	  break;
		  case ShimmerPC.MSG_IDENTIFIER_PACKET_RECEPTION_RATE_OVERALL:
			  //something
			  break;
		  }
	}
}
