/*
	
	此為存放 serial port 資料的類別
	
*/

package com.example.luolab.measureppg;

import java.util.Stack;

public class DoubleTwoDimQueue {
    private Stack<double [][]> queue;
    private int myQsize = 0;

    public DoubleTwoDimQueue(){
        queue = new Stack<double [][]>();
    }

    public void Qpush(double [][]elem){
        //The local_elem variable is very important here.
        //If directly elem is used, Java links it to the original variable used in the function call
        //Thus changing that variable in the program elsewhere changes the contents in the queue.
        //I need to search web for more info about why this happens.
        double [][] local_elem = new double[1][2];

        local_elem[0][0] = elem[0][0];
        local_elem[0][1] = elem[0][1];

        queue.push(local_elem);
        myQsize = queue.size();
    }

    public double lastEle(){
        double [][] peek = new double[1][2];
        peek = queue.get(myQsize-1);
        return peek[0][0];
    }

    public double [][] Qtake(){
        double [][] pop = new double[1][2];
        pop = queue.get(0);
        queue.remove(0);
        myQsize = queue.size();

        return pop;
    }

    public double [][] Qpeek(int index){

        if(index > queue.size()){
            System.err.println("ERROR : Invalid index, Out of Bounds");
            return null;
        }

        double [][] pop = new double[1][2];
        pop = queue.get(index);

        return pop;
    }

    public double [][] toArray(){
        return  toArray(0,myQsize - 1);
    }

    public double [][] toArray(int start, int end){
        int size = end - start + 1;
        double [][] arr =new double[size][2];

        for(int i = start, j =0; i <= end; i++, j++){
            double [][] peek = new double[1][2];
            peek = queue.get(i);

            arr[j][0] = peek[0][0];
            arr[j][1] = peek[0][1];
        }

        return arr;
    }

    public double [] toArray(int start, int end, int index){
        int size = end - start + 1;
        double [] arr =new double[size];

        for(int i = start, j =0; i <= end ; i++, j++){
            double [][] peek = new double[1][2];
            peek = queue.get(i);

            arr[j] = peek[0][index];

        }

        return arr;
    }

    public int getQSize(){
        return myQsize;
    }
}
