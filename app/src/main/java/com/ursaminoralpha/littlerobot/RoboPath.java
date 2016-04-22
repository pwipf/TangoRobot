package com.ursaminoralpha.littlerobot;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/**
 * Created by Magpie on 4/22/2016.
 */
public class RoboPath{

    ArrayList<PointF> path=new ArrayList<>();

    void add(float x,float y){path.add(new PointF(x,y));}
    void add(PointF p){path.add(p);}







    class Graph{
        ArrayList<Node> nodes=new ArrayList<>();

        void add(Node n){
            nodes.add(n);
        }
        void add(Node n, Node connectTo){
            add(n);
            float d=n.distFrom(connectTo);
            n.connections.add(new Edge(connectTo,d));
            connectTo.connections.add(new Edge(connectTo,d));
        }
        class Node{
            PointF coord=new PointF();
            ArrayList<Edge> connections = new ArrayList<>();
            Node(float x,float y){coord.x=x; coord.y=y;}
            Node(PointF point){coord=point;}
            float distFrom(Node n){
                float x=coord.x-n.coord.x, y=coord.y-n.coord.y;
                return (float)Math.sqrt(x*x+y*y);
            }
        }
        class Edge{
            Node to;
            float dist;
            Edge(Node to,float dist){this.to=to;this.dist=dist;}
        }

        ArrayList<Node> bfs(Node a, Node b){
            ArrayList<Node> list=new ArrayList<>();
            Queue<Node> q=new LinkedList<>();
            Map<Node,Node> prev=new HashMap<>();
            Map<Node,Boolean> marks=new HashMap<>();
            Node c=a;
            while(c!=b){
                for(Edge e:c.connections){
                    if(!marks.get(e.to)){
                        marks.put(e.to,true);
                        q.add(e.to);
                        prev.put(e.to,c);
                    }
                }
                if(q.isEmpty())
                    break;
                else{
                    c=q.remove();
                }
            }
            if(c!=b){
                return null;
            }
            while(c!=a){
                list.add(c);
                c=prev.get(c);
            }
            return list;
        }
    }





}
