from subprocess import call
import os
import subprocess
import signal
import platform
import time
import sys
import inspect
from py4j.java_gateway import JavaGateway, GatewayParameters


class JavaConnect():

    def __init__(self, port_num = 25333):

        self.process = None
        self.pid = None
        self.port_number = str(port_num)

        this_folder = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
        # root_folder = os.path.dirname(this_folder)
        # jar_file_name = os.path.join(root_folder,'javacnct','target','otm-py4j-1.0-SNAPSHOT-jar-with-dependencies.jar')
        # jar_file_name = os.path.join(this_folder,'otm-py4j-1.0-SNAPSHOT-jar-with-dependencies.jar')
        jar_file_name = os.path.join(this_folder,'otm-sim-1.0-SNAPSHOT-jar-with-dependencies.jar')
        #First check if the file exists indeed:

        print(jar_file_name)

        if os.path.isfile(jar_file_name):

            if platform.system() == "Windows":
                self.openWindows(jar_file_name, self.port_number)
            elif platform.system() in ["Linux", "Darwin"]:
                self.openLinux(jar_file_name, self.port_number)
            else:
                raise Exception('Unknown platform')

            self.gateway = JavaGateway(
                gateway_parameters=GatewayParameters(auto_convert=True,port=int(self.port_number)))
            # self.gateway = JavaGateway(gateway_parameters=GatewayParameters(port=int(self.port_number)))
            # self.gateway = JavaGateway()

        else:
            print("Jar file missing")

    def openWindows(self, jar_file_name, port_number):
        try:
            self.process = subprocess.Popen(['java', '-jar', jar_file_name, '-gateway',port_number],
                                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.pid = self.process.pid
            time.sleep(1)
        except subprocess.CalledProcessError:
            print("caught exception")
            sys.exit()

    def openLinux(self, jar_file_name, port_number):
        self.pid = os.fork()

        if self.pid == 0:
            self.pid = os.getpid()
            retcode = call(['java', '-jar', jar_file_name, '-gateway',port_number])
            sys.exit()

        # Here we wait for 4 sec to allow the java server to start
        time.sleep(4.0)

    # def to_int_set(self,pset):
    #     # int_class = self.gateway.jvm.int
    #     # int_array = self.gateway.new_array(int_class,2len(pset))
    #
    #     int_set = self.gateway.jvm.java.util.HashSet()
    #
    #     int_array[0] = 1
    #     int_array[1] = 2
    #     return int_array

    def close(self):
        if platform.system() == "Windows":
            #self.process.terminate()
            os.kill(self.pid, signal.CTRL_C_EVENT)
        elif platform.system() in ["Linux", "Darwin"]:
            self.gateway.shutdown()
            os.kill(self.pid, signal.SIGTERM)
        else:
            raise Exception('Unknown platform')

    def is_valid(self):
        return self.pid is not None
