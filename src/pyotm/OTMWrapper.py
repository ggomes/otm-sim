from pyotm.JavaConnect import JavaConnect
import numpy as np
# import matplotlib.pyplot as plt
# from matplotlib.collections import LineCollection
# import matplotlib.colors as pltc
# from random import sample
# import networkx as nx
from os import path

class OTMWrapper:
    """Provides a connection to the OTM API via py4j. Also includes convenience methods for running simulations and
    processing the output.
    """

    def __init__(self, configfile, port_num = 25333):

        self.configfile = configfile
        self.sim_output = None
        self.start_time = None
        self.duration = None

        self.conn = JavaConnect(port_num = port_num)
        if self.conn.pid is not None:
            self.otm = self.conn.gateway.get(configfile, True)

        # if self.otm==None:
        #     error("Error loading configuration file.")

    def __del__(self):
        if hasattr(self, 'conn') and self.conn is not None:
            self.conn.close()

    def describe(self):
        """ High level description of the scenario."""

        print("# nodes: {}".format(self.otm.scenario().num_nodes()))
        print("# links: {}".format(self.otm.scenario().num_links()))
        print("# commodities: {}".format(self.otm.scenario().num_commodities()))
        print("# subnetworks: {}".format(self.otm.scenario().num_subnetworks()))
        print("# sensors: {}".format(self.otm.scenario().num_sensors()))
        print("# actuators: {}".format(self.otm.scenario().num_actuators()))
        print("# controllers: {}".format(self.otm.scenario().num_controllers()))

    # def show_network(self, linewidth=1):
    #     """ Plot the network using node locations."""

    #     fig, ax = plt.subplots()

    #     nodes = {}
    #     for node_id in self.otm.scenario().node_ids():
    #         node = self.otm.scenario().get_node(node_id)
    #         nodes[node_id] = {'x': node.get_x(), 'y': node.get_y()}

    #     lines = []
    #     minX = float('Inf')
    #     maxX = -float('Inf')
    #     minY = float('Inf')
    #     maxY = -float('Inf')
    #     for link_id in self.otm.scenario().network().link_ids():

    #         link = self.otm.scenario().get_link(link_id)

    #         start_point = nodes[link.get_start_node_id()]
    #         end_point = nodes[link.get_end_node_id()]

    #         p0 = (start_point['x'], start_point['y'])
    #         p1 = (end_point['x'], end_point['y'])
    #         lines.append([p0, p1])

    #         minX = min([minX, p0[0], p1[0]])
    #         maxX = max([maxX, p0[0], p1[0]])
    #         minY = min([minY, p0[1], p1[1]])
    #         maxY = max([maxY, p0[1], p1[1]])

    #     all_colors = [k for k, v in pltc.cnames.items()]
    #     colors = sample(all_colors, len(lines))
    #     lc = LineCollection(lines, colors=colors)
    #     lc.set_linewidths(linewidth)
    #     ax.add_collection(lc)

    #     dY = maxY - minY
    #     dX = maxX - minX

    #     if (dY > dX):
    #         ax.set_ylim((minY, maxY))
    #         c = (maxX + minX) / 2
    #         ax.set_xlim((c - dY / 2, c + dY / 2))
    #     else:
    #         ax.set_xlim((minX, maxX))
    #         c = (maxY + minY) / 2
    #         ax.set_ylim((c - dX / 2, c + dX / 2))

    #     plt.draw()

    # run a simulation
    def run_simple(self, start_time=0., duration=3600., output_dt=30.):
        """ Easy running method. Requests vehicle and flow outputs, and runs the simulation for a given duration. """

        self.start_time = float(start_time)
        self.duration = float(duration)

        self.otm.output().clear()
        link_ids = self.otm.scenario().network().link_ids()
        self.otm.output().request_links_flow(None, None, None, link_ids, float(output_dt))
        self.otm.output().request_links_veh(None, None, None, link_ids, float(output_dt))

        # run the simulation
        self.otm.run(self.start_time, self.duration)

    def get_links_table(self):
        """Creates a pandas dataframe with network link information."""

        link_ids = []
        link_lengths = []
        link_lanes = []
        link_start = []
        link_end = []
        link_is_source = []
        link_is_sink = []
        # link_capacity = []
        # link_ffspeed = []
        # link_jamdensity = []
        # link_travel_time = []
        for link_id in self.otm.scenario().link_ids():
            link = self.otm.scenario().get_link(link_id)
            link_ids.append(link_id)
            link_lengths.append(link.get_full_length())
            link_lanes.append(link.get_full_lanes())
            link_start.append(link.get_start_node_id())
            link_end.append(link.get_end_node_id())
            link_is_source.append(link.get_is_source())
            link_is_sink.append(link.get_is_sink())
            # link_capacity.append(link.get_capacity_vphpl())
            # link_ffspeed.append(link.get_ffspeed_kph())
            # link_jamdensity.append(link.get_jam_density_vpkpl())
            # link_travel_time.append(link.get_full_length() * 3.6 / link.get_ffspeed_kph())

        return pd.DataFrame(data={'id': link_ids,'length_meter': link_lengths,'lanes': link_lanes,'start_node': link_start,'end_node': link_end,'is_source': link_is_source,'is_sink': link_is_sink}) #,'capacity_vphpl': link_capacity,'speed_kph': link_ffspeed,'max_vpl': link_jamdensity,'travel_time_sec': link_travel_time})

    # def to_networkx(self):
    #     """ Creates a networkx graph."""
    #     G = nx.MultiDiGraph()
    #     for node_id in self.otm.scenario().node_ids():
    #         node = self.otm.scenario().get_node(node_id)
    #         G.add_node(node_id, pos=(node.get_x(), node.get_y))
    #     for link_id in self.otm.scenario().link_ids():
    #         link = self.otm.scenario().get_link(link_id)
    #         G.add_edge(link.get_start_node_id(),link.get_end_node_id(), id=link_id)
    #     return G

    def get_state_trajectory(self):
        """Extracts the vehicles, flows, and speeds for each link in the network, following a run, and returns them
        in a dictionary."""
        X = {'time': None, 'link_ids': None, 'vehs': None, 'flows_vph': None, 'speed_kph': None}
        output_data = self.otm.output().get_data()
        it = output_data.iterator()
        while (it.hasNext()):

            output = it.next()

            # collect common link ids
            if X['link_ids'] is None:
                link_list = list(output.get_link_ids())
                X['link_ids'] = np.array(link_list)
            else:
                if not np.array_equal(X['link_ids'], np.array(list(output.get_link_ids()))):
                    raise ValueError('incompatible output requests')

            # collect common time vector
            if X['time'] is None:
                X['time'] = np.array(list(output.get_time()))
            else:
                if not np.array_equal(X['time'], np.array(list(output.get_time()))):
                    raise ValueError('incompatible output requests')

        # initialize outputs
        num_time = len(X['time'])
        num_links = len(X['link_ids'])

        X['vehs'] = np.empty([num_links, num_time])
        X['flows_vph'] = np.empty([num_links, num_time])

        it = output_data.iterator()
        while (it.hasNext()):
            output = it.next()

            for i in range(len(link_list)):
                z = output.get_profile_for_linkid(link_list[i])
                classname = output.getClass().getSimpleName()
                if (classname == "OutputLinkFlow"):
                    X['flows_vph'][i, 0:-1] = np.diff(np.array(list(z.get_values()))) * 3600.0 / z.get_dt()
                if (classname == "OutputLinkVehicles"):
                    X['vehs'][i, :] = np.array(list(z.get_values()))

        # X['speed_kph'] = np.empty([num_links, num_time])
        # for i in range(len(link_list)):
        #     link = self.otm.scenario().get_link(link_list[i])
        #     if link.get_is_source():
        #         X['speed_kph'][i, :] = np.nan;
        #     else:
        #         ffspeed_kph = link.get_ffspeed_kph()
        #         link_length_km = link.get_full_length() / 1000.0;

        #         with np.errstate(divide='ignore', invalid='ignore'):
        #             speed_kph = np.nan_to_num(link_length_km * np.divide(X['flows_vph'][i], X['vehs'][i]));
        #         speed_kph[speed_kph > ffspeed_kph] = ffspeed_kph;
        #         X['speed_kph'][i] = speed_kph;

        return X

    def read_lg_file(self,filename):
        # read lane group output file
        x = []
        with open(filename) as fp:
            while True:
                line = fp.readline()
                if not line:
                    break
                lgid, linkid, startlane, endlane = line.strip().split(",")
                x.append({'lgid': lgid,
                          'linkid': linkid,
                          'startlane': startlane,
                          'endlane': endlane,
                          'as_str': "{0} ({1}-{2})".format(linkid, startlane, endlane)})
        return x

    def read_cell_file(self,filename):
        # read cell output file
        x = []
        with open(filename) as fp:
            while True:
                line = fp.readline()
                if not line:
                    break
                cind, lgid, linkid, startlane, endlane = line.strip().split(",")
                x.append({'cellind': cind,
                          'lgid': lgid,
                          'linkid': linkid,
                          'startlane': startlane,
                          'endlane': endlane,
                          'as_str': "link {0}, cell {1} ({2}-{3})".format(linkid, cind, startlane, endlane)})
        return x

    def load_data(self,prefix, output_folder, comm, granularity, quantity):

        if granularity not in ('link', 'lg', 'cell'):
            print('Error, wrong granularity')
            return (None, None, None)

        if quantity not in ('flw', 'veh', 'lcin', 'lcout'):
            print('Error, wrong quantity')
            return (None, None, None)

        if comm == None:
            comm = 'allcomms'

        datafile = "{0}/{1}_{2}_{3}_{4}.txt".format(output_folder, prefix, comm, granularity, quantity)
        colsfile = "{0}/{1}_{2}_{3}_{4}_cols.txt".format(output_folder, prefix, comm, granularity, quantity)
        timefile = "{0}/{1}_{2}_{3}_{4}_time.txt".format(output_folder, prefix, comm, granularity, quantity)

        if not path.exists(datafile):
            print("Error: File not found: " + datafile)
            return (None, None, None)

        data = np.loadtxt(datafile, delimiter=',')

        cols = []

        if granularity == 'link':
            for linkid in np.loadtxt(colsfile, delimiter=',', dtype=int, ndmin=1):
                cols.append({'linkid': linkid, 'as_str': "link {0}".format(linkid)})
        elif granularity == 'lg':
            cols = self.read_lg_file(colsfile)
        elif granularity == 'cell':
            cols = self.read_cell_file(colsfile)

        time = np.loadtxt(timefile, delimiter=',')
        return (data, cols, time)

    # def lineplot(self,time, data, cols, title=""):
    #     plt.figure(figsize=(10, 4))
    #     plt.plot(time, data)
    #     plt.legend([col['as_str'] for col in cols])
    #     plt.grid()
    #     plt.title(title)