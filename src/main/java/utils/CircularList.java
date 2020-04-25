/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CircularList<T extends Comparable<T>> {

    public final List<T> queue;
    public int index;

    public CircularList(){
        queue = new ArrayList<>();
        index = 0;
    }

    public CircularList(Collection items){
        queue = new ArrayList<>();
        queue.addAll(items);
        Collections.sort(queue);
        index = 0;
    }

    public void clear(){
        queue.clear();
        index = -1;
    }

    public void add(T x){
        queue.add(x);
        Collections.sort(queue); // pretty silly, but I see no other way.
    }

    public void remove(Collection<T> x){
        queue.removeAll(x);
        Collections.sort(queue);
    }

    public void remove(T x){
        queue.remove(x);
        Collections.sort(queue);
    }

    public void point_to(T x){
        if(queue.contains(x))
            index = queue.indexOf(x);
    }

    public void point_to_index(int i){
        index = Math.min(Math.max(i,0),queue.size()-1);
    }

    public T peek(){
        return queue.get(index);
    }

    public T peek_next(){
        return queue.get(next_index());
    }

    public T peek_previous(){
        return queue.get(previous_index());
    }

    public T get(int i){
        return queue.get(i);
    }

    public void step_forward(){
        index = next_index();
    }

    public void step_back(){
        index = previous_index();
    }

    private int next_index(){
        return (index+1)%queue.size();
    }

    private int previous_index(){
        return index==0 ? queue.size()-1 : index-1;
    }

    @Override
    public String toString() {
        String str = "";
        for(T q : queue)
            str += q + "\n";
        return str;
    }
}