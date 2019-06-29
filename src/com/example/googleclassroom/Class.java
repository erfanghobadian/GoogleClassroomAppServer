package com.example.googleclassroom ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class Class implements Serializable {

    private static final long serialVersionUID = 7829136421241571165L;

    String name ;
    String room ;
    String des ;
    ArrayList <User> teachers = new ArrayList<>() ;
    String code;
    ArrayList<User> students = new ArrayList<>() ;
    Class(String name , String room , String des) {
        this.name = name;
        this.room = room ;
        this.des = des  ;
    }


}
