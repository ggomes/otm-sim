OTM is a platform for simulating vehicular traffic scenarios. It offers multiple models that can be arbitrarily combined in a single simulation. You can use one of the native OTM models (CTM, 2Q, Newell), or you can create a plugin for your own model. You can similarly implement new controllers as plugins.

Learn more [here.](https://ggomes.github.io/otm-sim/)

# Python API

Most users will interact with OTM through its Python API, which can be installed with pip:

```
pip install pyotm
```
The `pyotm` package contains a single class called `OTMWrapper`. Below we see how to use `OTMWrapper` to run a step-by-step simulation.

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

Use `run` to run an entire simulation. 
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

Here `get_state_trajectory` has been used to retrieve the state trajectory. This is a dictionary containing various state profiles. 

A comprehensive API is available via the `otm` attribute of `OTMWrapper`. This API provides access to most of the internal state of OTM and should be used with care. The javadoc can be found [here](https://ggomes.github.io/otm-sim/apidocs/index.html).
