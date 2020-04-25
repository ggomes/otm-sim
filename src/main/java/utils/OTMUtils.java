/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class OTMUtils {

    public static double epsilon = 1e-6;
    public static long lane_group_counter;
    public static long vehicle_id_count;

    private static Random random;

    static{
        lane_group_counter = 0;
        vehicle_id_count = 0;
        random = new Random();
    }

    public static void reset_counters(){
        lane_group_counter = 0;
        vehicle_id_count = 0;
    }

    public static void set_random_seed(long seed){
        random = new Random(seed);
    }

    ///////////////////////////////////////////////////
    // type conversion
    ///////////////////////////////////////////////////

    public static double[] toDoubleArray(List list) {
        double retValue[] = new double[list.size()];
        ListIterator iterator = list.listIterator();
        for (int idx = 0; idx < retValue.length; ++idx )
            retValue[idx] = (double) iterator.next();
        return retValue;
    }

    public static float[] toFloatArray(List list) {
        float retValue[] = new float[list.size()];
        ListIterator iterator = list.listIterator();
        for (int idx = 0; idx < retValue.length; ++idx )
            retValue[idx] = (float) iterator.next();
        return retValue;
    }

    public static long[] toLongArray(List list) {
        long retValue[] = new long[list.size()];
        ListIterator iterator = list.listIterator();
        for (int idx = 0; idx < retValue.length; ++idx )
            retValue[idx] = (long) iterator.next();
        return retValue;
    }

    ///////////////////////////////////////////////////
    // math
    ///////////////////////////////////////////////////

    public static <T> List<T> init_list(int n,T val){
        List<T> x = new ArrayList<>(n);
        for(int i=0;i<n;i++)
            x.set(i,val);
        return x;
    }

    public static int min(int a,int b){
        return a<b ? a : b;
    }

    public static boolean approximately_equals(double x,double y){
        return Math.abs(x-y)<epsilon;
    }

    public static boolean greater_than(double x,double y){
        return x-y>epsilon;
    }

    public static double sum(Collection<Double> X){
        double s = 0d;
        for(Double x:X)
            s += x;
        return s;
    }

    public static <T> double sum(Map<T,Double> X){
        return X.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public static List<Double> times(List<Double> X,Double a){
        if(X==null)
            return null;
        List<Double> Y = new ArrayList<>();
        for(Double x:X)
            Y.add(x*a);
        return Y;
    }

    public static List<Float> times(List<Float> X,Float a){
        if(X==null)
            return null;
        List<Float> Y = new ArrayList<>();
        for(Float x:X)
            Y.add(x*a);
        return Y;
    }

    public static <T> Map<T,Double> times(Map<T,Double> X,Double a){
        if(X==null)
            return null;
        Map<T,Double> Y = new HashMap<>();
        for(Map.Entry<T,Double> entry : X.entrySet())
            Y.put(entry.getKey(), entry.getValue() * a);
        return Y;
    }

    public static Float get_waiting_time(double rate,StochasticProcess process){

        if(rate<=0d)
            return null;

        double wait = 0d;
        switch(process){
            case poisson:
                wait = -Math.log(1.0-random.nextDouble())/rate;
                break;
            case deterministic:
                wait = 1.0/rate;
        }
        return (float) wait;
    }

    public static int random_int(int min, int max){
        return min + random.nextInt(max);
    }

    public static double random_double(double min,double max){
        return min + random_zero_to_one()*(max-min);
    }

    public static double random_zero_to_one(){
        return random.nextDouble();
    }

    ///////////////////////////////////////////////////
    // sets
    ///////////////////////////////////////////////////

    public static <T> T sample_from_set(Set<T> set){
        int size = set.size();
        if(size==1)
            return (T) set.toArray()[0];
        int item = random.nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(T obj : set)
        {
            if (i == item)
                return obj;
            i = i + 1;
        }
        return null;
    }

    public static <T> Set<T> intersect(Collection<T> A, Collection<T> B){
        if(A.isEmpty() || B.isEmpty())
            return new HashSet<>();
        Set<T> C = new HashSet<>(A);
        C.retainAll(B);
        return C;
    }

    public static <T> Set<T> setminus(Collection<T> A,Collection<T> B){
        if(A.isEmpty())
            return new HashSet<>();
        Set<T> C = new HashSet<>(A);
        C.removeAll(B);
        return C;
    }

    public static <T> boolean equals(Collection<T> A, Collection<T> B){
        return A.containsAll(B) && B.containsAll(A);
    }

    ///////////////////////////////////////////////////
    // strings
    ///////////////////////////////////////////////////

    public static List<Double> csv2list(String csstr){
        List<Double> vals = new ArrayList<>();
        if(csstr!=null)
            for(String str : Arrays.asList(csstr.split("\\s*,\\s*")))
                vals.add(Double.parseDouble(str));
        return vals;
    }

    public static List<Long> csv2longlist(String csstr){
        List<Long> vals = new ArrayList<>();
        if(csstr!=null)
            for(String str : Arrays.asList(csstr.split("\\s*,\\s*")))
                vals.add(Long.parseLong(str));
        return vals;
    }

    public static int[] int_hash_int(String str){
        int [] res = new int[2];
        String [] strsplt = str.split("#");
        if(strsplt.length<1 || strsplt.length>2)
            return null;
        res[0] = Integer.parseInt(strsplt[0]);
        res[1] = strsplt.length==1 ? res[0] : Integer.parseInt(strsplt[1]);
        return res;
    }

    public static <T> String format_delim(T [] array,String delim){
        String str = "";
        if(array==null || array.length==0)
            return str;
        for(int i=0;i<array.length-1;i++)
            str += array[i] + delim;
        str += array[array.length-1];
        return str;
    }

    public static <T> String comma_format(Collection<T> x){
        return format_delim(x.toArray(),",");
    }

    public static int[] read_lanes(String str,int full_lanes){
        int [] x = new int[2];
        if(str==null) {
            x[0] = 1;
            x[1] = full_lanes;
        }
        else {
            String[] strsplit = str.split("#");
            if (strsplit.length == 2) {
                x[0] = Integer.parseInt(strsplit[0]);
                x[1] = Integer.parseInt(strsplit[1]);
            } else {
                x[0] = -1;
                x[1] = -1;
            }
        }
        return x;
    }

    ///////////////////////////////////////////////////
    // counter
    ///////////////////////////////////////////////////

    public static long get_lanegroup_id(){
        return lane_group_counter++;
    }

    public static long get_vehicle_id(){
        return vehicle_id_count++;
    }

    ///////////////////////////////////////////////////
    // git
    ///////////////////////////////////////////////////

    public static String getBaseGitHash(){
        InputStream inputStream = OTMUtils.class.getResourceAsStream("/otm-base.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read properties file", e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return properties.getProperty("base.git");
    }

    ///////////////////////////////////////////////////
    // file
    ///////////////////////////////////////////////////

    public static ArrayList<Double> read_double_csv_file(File file) {
        if(file==null)
            return null;
        ArrayList<Double> x = new ArrayList<>();
        try {
            String line = (new Scanner(file)).nextLine();
            for (String str : line.split(","))
                x.add(Double.parseDouble(str));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return x;
    }

}
