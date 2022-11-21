OTM is a platform for simulating vehicular traffic scenarios. It offers multiple models that can be arbitrarily combined in a single simulation. You can use one of the native OTM models (CTM, 2Q, Newell), or you can create a plugin for your own model. You can similarly implement new controllers as plugins.

Learn more [here.](https://ggomes.github.io/otm-sim/)

# Python API

Most users will interact with OTM through its Python API, which can be installed with pip:

```
pip install pyotm
```
This package provides a wrapper class called `OTMWrapper`, which  creates a gateway to the OTM jar file using `py4j`. `OTMWrapper` also includes some convenience methods for working with OTM. These are illustrated in the following example. 
```python 
from pyotm.OTMWrapper import OTMWrapper

# in case there is a lingering open gateway
if "otm" in locals():
	del otm
	
# load the configuration file
otm = OTMWrapper("intersection.xml")

# initialize (prepare/rewind the simulation)
otm.initialize(start_time=0.0)

# run step-by-step using the 'advance' method
time = 0.0  # in seconds
advance_time = 10.0
while(time<3600.0 ):
	otm.advance(advance_time)   # seconds, should be a multiple of sim_dt
	print(otm.get_current_time())
	time += advance_time;

# deleting the wrapper to shut down the gateway
del otm
```

Use `run` to execute a full simulation. 
``` python 
from pyotm.OTMWrapper import OTMWrapper
import numpy as np

if "otm" in locals():
	del otm

otm = OTMWrapper("intersection.xml")
otm.run(start_time=0,duration=2500,output_dt=10)
Y = otm.get_state_trajectory()
print(Y.keys())
del otm
```

Here `get_state_trajectory` has been used to retrieve the state trajectory. These are the flow and density profiles in each of the links in the network. You can extract more fine-grained data using ``output requests'' or through the Java API. An output requests prompts the simulator to dump specific outputs to text files. You can see which outputs are available [here](https://ggomes.github.io/otm-sim/apidocs/core/Output.html). 

The `OTMWrapper` exposes OTM's Java API through its `otm` attribute, which is an instance of [`core.OTM`](https://ggomes.github.io/otm-sim/apidocs/core/OTM.html). From here you can place requests to the [Output](https://ggomes.github.io/otm-sim/apidocs/core/Output.html) object, or gain access to all of the scenario elements (links, nodes, lane groups, controllers, sensors, actuators, etc.) via the [Scenario](https://ggomes.github.io/otm-sim/apidocs/core/Scenario.html) object. Both of these are otained with their getters. 

The following example demonstrates the max pressure algorithm for controlling a signalized intersection.

```python
TBD
```
