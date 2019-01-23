/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import utils.OTMUtils;

import java.util.*;

/**
 * Each link holds a map from each commodity (that uses the link)
 * to one of these. When the commodity enters the link it uses this object
 * to split itself by next link (according to the split ratio). The next link
 * determines a set of "target lanegroups". The (now subdivided) commodity
 * navigates the link to its target lanegroups. How it does this depends on whether
 * its model. When it reaches the end of the link, it proceeds
 * to the next link according to its next link tag.
 */
public class SplitInfo {

    // use this is there is only one possible downstream link
    public Long sole_downstream_link;

    public Map<Long,Double> outlink2split;        // output link id -> split
    public List<LinkCumSplit> link_cumsplit;        // output link id -> cummulative split

    public SplitInfo(Long trivial_answer){
        this.sole_downstream_link = trivial_answer;
    }

    public void set_splits(Map<Long,Double> outlink2split) {

        this.outlink2split = outlink2split;

        if(outlink2split.size()==1)
            sole_downstream_link = outlink2split.keySet().iterator().next();


        if(outlink2split.size()<=1)
            return;

        double s = 0d;
        link_cumsplit = new ArrayList<>();
        for(Map.Entry<Long,Double> e : outlink2split.entrySet()){
            link_cumsplit.add(new LinkCumSplit(e.getKey(),s));
            s += e.getValue();
        }
    }

    public Double get_split_for_link(Long linkid){
        return outlink2split.containsKey(linkid) ? outlink2split.get(linkid) : 0d;
    }

    /** return an output link id according to split ratios
     * for this commodity and link
     */
    public Long sample_output_link(){

        // no splits have been specified
        // packet_splitter validation should check that this is a one-to-one link
        if(link_cumsplit==null || link_cumsplit.size()<=1)
            return sole_downstream_link;

        double r = OTMUtils.random_zero_to_one();

        Optional<LinkCumSplit> z = link_cumsplit.stream()
                                    .filter(x->x.cumsplit<r)  // get all cumsplit < out
                                    .reduce((a,b)->b);        // get last such vauue

        return z.isPresent() ? z.get().link_id : null;
    }

    class LinkCumSplit{
        public Long link_id;
        public Double cumsplit;
        public LinkCumSplit(Long link_id, Double cumsplit) {
            this.link_id = link_id;
            this.cumsplit = cumsplit;
        }
    }

}
