package core;

public class Point {
    public float x;
    public float y;
    public Point(float x,float y){
        this.x = x;
        this.y = y;
    }
    public Point(core.Node node){
        this.x = node.xcoord;
        this.y = node.ycoord;
    }
}