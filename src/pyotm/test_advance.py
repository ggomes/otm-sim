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