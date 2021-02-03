# Demos

OTM is written in Java, and includes connections to Matlab and Python. This is done via [OTM's Java API](docs/apidocs/index.html). The Matlab and Python interfaces are included with [otm-tools](https://github.com/ggomes/otm-tools).
All of the methods in the Java API are accessible from Matlab and Python, so the Java documentation is valid for all three
languages. The Matlab and Python interfaces provide additional useful methods in the `OTMWrapper` class.

A few simple examples are provided below.

## Load a scenario

### Java ([source](https://github.com/ggomes/otm-tools/blob/master/java/DemoLoad.java))

```java
import api.OTM;
public class DemoLoad {
  public static void main(String[] args)  {
    try {
      api.OTM otm = new api.OTM("../configs/line.xml");
      System.out.println(otm==null ? "Failure" : "Success");
    } catch (Exception e) {
      System.err.print(e.getMessage());
    }
  }
}
```

### Python ([source](https://github.com/ggomes/otm-tools/blob/master/python/demo_load.py))

```python
from OTMWrapper import OTMWrapper
otm = OTMWrapper("../configs/line.xml")
del otm
```

### Matlab ([source](https://github.com/ggomes/otm-tools/blob/master/matlab/demo_load.m))

```octave
import api.OTM
otm = OTMWrapper("../configs/line.xml");
```

## Run a complete simulation

### Java ([source](https://github.com/ggomes/otm-tools/blob/master/java/DemoRun.java))

```java
import error.OTMException;
import output.*;
import java.util.Set;

public class DemoRun {
  public static void main(String[] args)  {
    try {
      // load the scenario
      api.OTM otm = new api.OTM("../configs/line.xml");

      // request 10-second sampling of all link flows and densities
      float outdt = 10f;  // sampling time in seconds
      Set<Long> link_ids = otm.scenario.get_link_ids();  // request all link ids
      otm.output.request_links_flow(null, link_ids, outdt);
      otm.output.request_links_veh(null, link_ids, outdt);

      // run the simulation for 200 seconds
      otm.run(0,200f);

      // plot the output by iterating through the requested output data and
      // calling the 'plot_for_links' method.
      for(AbstractOutput output :  otm.output.get_data()){
        if (output instanceof LinkFlow)
          ((LinkFlow) output).plot_for_links(null, String.format("%sflow.png", ""));
        if (output instanceof LinkVehicles)
          ((LinkVehicles) output).plot_for_links(null, String.format("%sveh.png", ""));
      }

    } catch (OTMException e) {
      System.err.print(e);
    }
  }
}
```

### Python ([source](https://github.com/ggomes/otm-tools/blob/master/python/demo_run.py))

```python
from OTMWrapper import OTMWrapper
from matplotlib import pyplot as plt 

# open the api
otm = OTMWrapper('../configs/line.xml')

# Plot the network
otm.show_network()

# run a simulation
otm.run_simple(start_time=0,duration=1500,output_dt=10)

# extract the state trajectory
Y = otm.get_state_trajectory()

# plot the state trajectory
fig = plt.figure()
plt.subplot(311)
plt.plot(Y['time'],Y['vehs'].T) 
plt.ylabel("vehicles") 
plt.title("OTM simulation result") 
plt.subplot(312)
plt.plot(Y['time'],Y['flows_vph'].T)
plt.ylabel("flow [vph]") 
plt.subplot(313)
plt.plot(Y['time'],Y['speed_kph'].T)
plt.ylabel("speed [kph]") 
plt.legend(['link 1','link 2','link 3'])
plt.xlabel("time [sec]") 
plt.draw()

plt.show()

# always end by deleting the wrapper
del otm
```

### Matlab ([source](https://github.com/ggomes/otm-tools/blob/master/matlab/demo_run.m))

```octave
import api.OTM

% load the configuration file into an OTMWrapper object
otm = OTMWrapper("../configs/line.xml");

% run a simulation
start_time = 0;
duration = 1000;
request_links = [1 2 3];
outdt = 10;
otm.run_simple(start_time,duration,request_links,request_dt)

% extract the state trajectory
Y = otm.get_state_trajectory;

% plot the state trajectory
figure
subplot(311)
plot(Y.time,Y.vehs)
subplot(312)
plot(Y.time(2:end),Y.flows_vph)
subplot(313)
plot(Y.time(2:end),Y.speed_kph)
```

## Run a scenario step-by-step

### Java ([source](https://github.com/ggomes/otm-tools/blob/master/java/DemoRunStep.java))

```java
import error.OTMException;

public class DemoRunStep {
  public static void main(String[] args) {
    try {
      float start_time = 0f;
      float duration = 3600f;
      float advance_time = 300f;

      // load the scenario
      api.OTM otm = new api.OTM("../configs/line.xml");

      // initialize (prepare/rewind the simulation)
      otm.initialize(start_time);

      // run step-by-step using the 'advance' method
      float time = start_time;
      float end_time = start_time+duration;

      while(time<end_time){
        System.out.println(time);
        otm.advance(advance_time);

        // Insert your code here -----

        time += advance_time;
      }
    } 
    catch (OTMException e) {
      System.err.print(e);
    }
  }
}
```

### Python ([source](https://github.com/ggomes/otm-tools/blob/master/python/demo_run_step.py))

```python
from OTMWrapper import OTMWrapper

start_time = 0.
duration = 3600.
advance_time = 300.

# load the configuration file
otm = OTMWrapper('../configs/line.xml')

# initialize (prepare/rewind the simulation)
otm.initialize(start_time)

# run step-by-step using the 'advance' method
time = start_time
end_time = start_time + duration

while(time<end_time):
  otm.advance(advance_time)

  # Insert your code here -----
  print(otm.otm.get_current_time())

  time += advance_time;

# always end by deleting the wrapper
del otm
```

### Matlab ([source](https://github.com/ggomes/otm-tools/blob/master/matlab/demo_run_step.m))

```octave
import api.OTM

start_time = 0;
duration = 3600;
advance_time = 300;

% load the configuration file into an OTMWrapper object
otm = OTMWrapper('../configs/line.xml');

% initialize (prepare/rewind the simulation)
otm.initialize(start_time);

% run step-by-step using the 'advance' method
time = start_time;
end_time = start_time+duration;

while(time<end_time)
  otm.advance(advance_time);

  % Insert your code here -----
  disp(otm.api.get_current_time())

  time = time + advance_time;
end
```

## Write a controller plugin
Coming soon...

## Write a model plugin
Coming soon...

## Import a network from OSM
[OSM](https://github.com/ggomes/otm-simcenter)

## Import a network from SUMO
