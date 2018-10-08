/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tools.computation;

import common.*;
import error.OTMException;
import keys.KeyCommPathOrLink;
import models.ctm.LaneGroup;
import runner.RunParameters;
import runner.Scenario;

import java.util.*;

public class MacroComputationCounter {

    public static void run(Scenario scenario){

        if(!scenario.is_initialized) {
            try {
                RunParameters run_params = new RunParameters(null,null,null,0f,1f);
                scenario.initialize(null, run_params);
            } catch (OTMException e) {
                e.printStackTrace();
            }
        }

        compute( scenario,
                 load_link_info(scenario),
                 load_node_info(scenario),
                 load_link_equations(),
                 load_node_init_equations(),
                 load_node_iter_equations() );
    }

    /** link model I ............................ **/
    private static List<Equation> load_link_equations() {

        List<Equation> X = new ArrayList<>();

        /** (1) **/
        X.add(new Equation<LinkInfoForComputation>("",
                l -> l.nSi - l.num_cells,    // additions
                l -> 0,    // multiplications
                l -> 0));   //comparisons

        /** (2) **/
        X.add(new Equation<LinkInfoForComputation>("",
                l -> 0,    // additions
                l -> l.num_cells,    // multiplications
                l -> 2 * l.num_cells));   //comparisons

        /** (3) **/
        X.add(new Equation<LinkInfoForComputation>("",
                l -> 0,    // additions
                l -> l.num_cells,    // multiplications
                l -> 0));   //comparisons

        /** (4) **/
        X.add(new Equation<LinkInfoForComputation>("",
                l -> 0,    // additions
                l -> l.nSi,    // multiplications
                l -> 0));   //comparisons

        /** (5) **/
        X.add(new Equation<LinkInfoForComputation>("",
                l -> l.num_cells,    // additions
                l -> l.num_cells,    // multiplications
                l -> l.num_cells));   //comparisons

        /** Link model II **/
        X.add( new Equation<LinkInfoForComputation>( "",
                l->2*l.nSi  ,    // additions
                l->0 ,    // multiplications
                l->0 ));  //comparisons

        return X;
    }

    /** Node model initialization **/
    private static List<Equation> load_node_init_equations() {

        List<Equation> X = new ArrayList<>();

        /** (9) **/
        X.add(new Equation<NodeInfoForComputation>("",
                n -> n.up_lgs.stream().mapToInt(i -> i.sum_Sir() - i.nDi).sum(),    // additions
                n -> 0,    // multiplications
                n -> 0));   //comparisons

        /** (10) **/
        X.add(new Equation<NodeInfoForComputation>("",
                n -> 0,    // additions
                n -> n.up_lgs.stream().mapToInt(i -> i.nSi).sum(),    // multiplications
                n -> 0));   //comparisons

        return X;
    }

    /** Node model iteration **/
    private static List<Equation> load_node_iter_equations() {

        List<Equation> X = new ArrayList<>();

        /** (11) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->0 ,    // multiplications
                n->n.dn_lgs.size() )); //comparisons

        /** (13) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.up_lgs.stream().mapToInt(u->u.nDi-1).sum() ,    // additions
                n->0 ,                                                 // multiplications
                n->n.up_lgs.size() ));                                 //comparisons

        /** (22) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.rcs.values().stream().mapToInt(r->r.nUr-1).sum() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        /** (23) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.rcs.values().stream().mapToInt(r->r.nDr-1).sum() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        /** (24) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->n.rcs.values().stream().mapToInt(r->r.nDr).sum() ,    // multiplications
                n->0 ));  //comparisons

        /** (25) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.dn_lgs.stream().mapToInt(j->j.nUj-1).sum() ,    // additions
                n->n.dn_lgs.stream().mapToInt(j->j.nUj+1).sum() ,    // multiplications
                n->n.dn_lgs.size() ));  //comparisons

        /** (26) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->0 ,    // multiplications
                n->n.rcs.values().stream().mapToInt(r->r.nDr-1).sum() ));  //comparisons

        /** (27) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->0 ,    // multiplications
                n->n.up_lgs.stream().mapToInt(i->i.nDi-1).sum() ));  //comparisons

        /** (28) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->n.up_lgs.stream().mapToInt(i->i.nDi).sum() ,    // multiplications
                n->0 ));  //comparisons

        /** (29) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->n.up_lgs.stream().mapToInt(i->i.nSi).sum() ,    // multiplications
                n->0 ));  //comparisons

        /** (30) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.up_lgs.stream().mapToInt(i->i.nSi).sum() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        /** (31) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.up_lgs.stream().mapToInt(i->i.nDi).sum() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        /** (32)
         X.add( new Equation<NodeInfoForComputation>("" ,
         n->0 ,    // additions
         n->0 ,    // multiplications
         n->0 ));  //comparisons
         **/

        /** (33) **/
//        X.add( new Equation<NodeInfoForComputation>("" ,
//                n->n.dn_lgs.stream().mapToInt(j->j.nSj*(j.nUj-1)).sum() ,    // additions
//                n->n.dn_lgs.stream().mapToInt(j->j.nSj*j.nUj).sum() ,    // multiplications
//                n->0 ));  //comparisons

        X.add( new Equation<NodeInfoForComputation>("" ,
                n->0 ,    // additions
                n->n.dn_lgs.stream().mapToInt(j->j.nSj).sum() ,    // multiplications
                n->0 ));  //comparisons

        /** (34) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.dn_lgs.stream().mapToInt(j->j.nSj-1).sum() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        /** (35) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.dn_lgs.size() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        /** (36) **/
        X.add( new Equation<NodeInfoForComputation>("" ,
                n->n.dn_lgs.stream().mapToInt(j->j.nSj).sum() ,    // additions
                n->0 ,    // multiplications
                n->0 ));  //comparisons

        return X;

    }

    private static List<NodeInfoForComputation> load_node_info(Scenario scenario){
        List<NodeInfoForComputation> X = new ArrayList<>();
        for(Node node : scenario.network.nodes.values()) {
            if(node.out_links.size()>=2)
                X.add(new NodeInfoForComputation(node));
        }
        return X;
    }

    private static List<LinkInfoForComputation> load_link_info(Scenario scenario){
        List<LinkInfoForComputation> X = new ArrayList<>();
        for(Link link : scenario.network.links.values())
            X.add(new LinkInfoForComputation(link));
        return X;
    }

    private static void compute(Scenario scenario,List<LinkInfoForComputation> links,List<NodeInfoForComputation> nodes,List<Equation> link_model,List<Equation> node_model_init,List<Equation> node_model_iteration) {

        int total_adds = 0;
        int total_mult = 0;
        int total_comp = 0;

        // link model
        for(LinkInfoForComputation link : links){
            for(Equation eq : link_model ){
                total_adds += eq.adds.eval(link);
                total_mult += eq.mult.eval(link);
                total_comp += eq.comp.eval(link);
            }
        }

        // node model init
        for(NodeInfoForComputation node : nodes){
            for(Equation eq : node_model_init){
                total_adds += eq.adds.eval(node);
                total_mult += eq.mult.eval(node);
                total_comp += eq.comp.eval(node);
            }
        }

        int num_iterations = 2;
        for(NodeInfoForComputation node : nodes){
            for(Equation eq : node_model_iteration){
                total_adds += eq.adds.eval(node) * num_iterations;
                total_mult += eq.mult.eval(node) * num_iterations;
                total_comp += eq.comp.eval(node) * num_iterations;
            }
        }

        int num_nodes = scenario.network.nodes.size();
//        int num_lanegroups = scenario.network.lanegroups.size();
        int total_flops = total_adds + total_mult + total_comp;
        int num_cells = links.stream().mapToInt(link->link.num_cells).sum();
        int num_states = links.stream().mapToInt(link->link.nSi).sum();
        float avg_states_per_cell = ((float)num_states)/((float)num_cells);

        double edison_speed = (19.2)*1e3; // flops per micro second
        double flops_time = ((double)total_flops) / edison_speed;

        System.out.println("\nProblem size");
        System.out.println("total nodes\t"+num_nodes);
        System.out.println("total non-trivial nodes\t"+nodes.size());
        System.out.println("total graph2links\t"+scenario.network.links.size());
//        System.out.println("total lane groups\t"+num_lanegroups);
        System.out.println("total cells\t"+num_cells);
        System.out.println("avg_states_per_cell\t"+avg_states_per_cell);

        System.out.println("\nTotal computation");
        System.out.println("total_adds\t"+total_adds);
        System.out.println("total_mult\t"+total_mult);
        System.out.println("total_comp\t"+total_comp);
        System.out.println("total_flops\t"+total_flops);
        System.out.println("computation time\t"+flops_time);


        /** Estimate number of mesages for 2 nodes **/
        int num_bndry_nodes = (int) Math.sqrt(num_nodes);
        int total_demand_numbers = (int) avg_states_per_cell * num_bndry_nodes;
        int total_supply_numbers = num_bndry_nodes;
        int total_message_numbers = total_demand_numbers + total_supply_numbers;

        // message latency
        double alpha = 3;    // 3 microseconds
        double beta = 20000;     // 20 Kbytes/microsec  (20Gb/second)
        int message_bytes = total_message_numbers * 8;  // 8 bytes per double
        double message_time = alpha + ((double)message_bytes) / beta;  // in microseconds
        flops_time = ((double)total_flops/2) / edison_speed;

        System.out.println("\nMessage passing (split into two equal networks)");
        System.out.println("num_bndry_nodes\t"+num_bndry_nodes);
        System.out.println("message_bytes\t"+message_bytes);
        System.out.println("message_time\t"+message_time);
        System.out.println("flops_time\t"+flops_time);
        System.out.println("total time\t"+(message_time+flops_time));

    }

    /**
     *  CLASSES
     *  **/

    public static class LinkInfoForComputation {

        int nSi = 0;    // number of states in the link
        int num_cells = 0;  // number of cells in the link

        public LinkInfoForComputation(Link link){
            for(AbstractLaneGroup lg : link.long_lanegroups.values())
                if(lg instanceof LaneGroup){
                    LaneGroup ctm_lg = (LaneGroup) lg;
                    nSi += ctm_lg.cells.size() * ctm_lg.states.size();
                    num_cells += ctm_lg.cells.size();
                } else {
                    System.err.println("Bad lange group type in LinkInfoForComputation");
                }
        }
    }

    public static class NodeInfoForComputation {

        /** Integer id of the node. */
        public long id;
        public Set<UpLaneGroupInfo> up_lgs;
        public Map<Long,RoadConnectionInfo> rcs;
        public Set<DnLaneGroupInfo> dn_lgs;

        public NodeInfoForComputation(Node x){
            this.id = x.getId();

            // ulgs lane groups
            up_lgs = new HashSet<>();
            for(Link link : x.in_links.values())
                for(AbstractLaneGroup lg : link.long_lanegroups.values())
                    up_lgs.add(new UpLaneGroupInfo(lg));

            // road connections
            rcs = new HashMap<>();
            for(RoadConnection rc : x.road_connections)
                rcs.put(rc.getId(),new RoadConnectionInfo(rc));

            // dlgs lane groups
            dn_lgs = new HashSet<>();
            for(Link link : x.out_links.values())
                for(AbstractLaneGroup lg : link.long_lanegroups.values())
                    dn_lgs.add(new DnLaneGroupInfo(lg));

            // states for road connections
//            for(UpLaneGroupInfo uplg : up_lgs){
//                for(Map.Entry<Long,Integer> e : uplg.nSir.entrySet()){
//                    rcs.get(e.getKey()).add_states(e.getValue());
//                }
//            }
        }

        public long getId() {
            return id;
        }

    }

    public static class UpLaneGroupInfo {

        // number of commodity/path pairs using each road connection
        public Map<Long,Integer> nSir = new HashMap<>();

        // number of commodity/path pairs using this lanegroup
        public int nSi;

        // number of downstream road connections
        public int nDi;

        public UpLaneGroupInfo(AbstractLaneGroup lg) {
            if(lg instanceof LaneGroup){
                LaneGroup ctm_lg = (LaneGroup) lg;
                this.nSi = ctm_lg.states.size();
                if(nSi ==0) {
                    nDi = 0;
                    return;
                }
                nDi = ctm_lg.get_num_exiting_road_connections();
                for(Map.Entry<Long,Set<KeyCommPathOrLink>> e : ctm_lg.roadconnection2states.entrySet())
                    nSir.put(e.getKey(),e.getValue().size());
            }
        }

        public int sum_Sir(){
            return nSir.values().stream().reduce(0,(x, y)->x+y);
        }

    }

    public static class RoadConnectionInfo {

        // number of upstream lane groups
        public int nUr;

        // number of downstream lane groups
        public int nDr;
//        private int nSi;

        public RoadConnectionInfo(RoadConnection rc){
            this.nUr = rc.in_lanegroups.size();
            this.nDr = rc.out_lanegroups.size();
//            this.nSi = 0;
        }
//        public void add_states(int x){
//            nSi += x;
//        }
    }

    public static class DnLaneGroupInfo {

        // number of upstream road connections
        public int nUj;

        // number of states
        public int nSj;

        public DnLaneGroupInfo(AbstractLaneGroup lg){
            if(lg instanceof LaneGroup){
                LaneGroup ctm_lg = (LaneGroup) lg;
                this.nSj = ctm_lg.states.size();
                if(this.nSj ==0)
                    return;
                this.nUj = ctm_lg.get_num_exiting_road_connections();
            }
        }
    }

    public interface Adds <T> { int eval(T x); }
    public interface Mult <T> { int eval(T x); }
    public interface Comp <T> { int eval(T x); }

    public static class Equation <T> {

        public String name;
        public Adds adds;
        public Mult mult;
        public Comp comp;

        public Equation(String name,Adds<T> adds,Mult<T> mult, Comp<T> comp){
            this.name = name;
            this.adds = adds;
            this.mult = mult;
            this.comp = comp;
        }
    }

}
