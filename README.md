# ShimmerProjectECL is a java-based project to make a simple data streaming software for Shimmer biopotential signal sensor using LSL (Lab Streaming Layer) protocol.
## Designed for the Windows platform, Shimmer GSR+ sensor.

Based on the [ShimmerEngineering's repo][1]
[1]:https://github.com/ShimmerEngineering/ShimmerJavaExamples

##Original Description for the [repo][1]
There are a number of examples
- ECGToHRExample

  _Shows how to retieve data from the ObjectCluster (see ShimmerPC.MSG_IDENTIFIER_DATA_PACKET) as well how to use the ECG to Hear Rate Algorithm._
  
- PPGToHRExample

  _Shows how to retieve data from the ObjectCluster (see ShimmerPC.MSG_IDENTIFIER_DATA_PACKET) as well how to use the PPG to Hear Rate Algorithm._

- SensorMapsExample

  _Shows how to configure a Shimmer device via a User Interface while it is connected._

- ShimmerSetupExample

  _Shows how to configure a Shimmer device via the constructor._

For Shimmer2R uses please refer to the legacy example


#License
follow the original license

#how to use "ApplicationShimmer"
1. Install Java Development Kit 1.8
2. Clone this repo to local and open it with Eclipse Java IDE.
3. Select "ApplicationShimmer.java" under "ecl" directory and build it.
4. Pair your Shimmer GSR+ sensor to your PC by bluetooth connection.
5. Check the port number (i.e. COM6) for your Shimmer sensor on your PC.
6. Run the compiled "ApplicationShimmer.jar" in Eclipse IDE or by typing `java -jar ApplicationShimmer.jar` in powershell.
7. Type in the port number in the app and you'll see connection log and sensor data.

##TDL
- cleanup repo
- adding some function (perhaps GUI?)
