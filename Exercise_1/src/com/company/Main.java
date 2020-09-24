package com.company;

public class Main {
    private boolean dirProcess(String dir) {
        String[] originDir = dir.split("/");
        System.out.print("d");
        return false;
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.dirProcess("../../../ddd");
    }
}
