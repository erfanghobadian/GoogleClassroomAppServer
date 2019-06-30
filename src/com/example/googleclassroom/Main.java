package com.example.googleclassroom;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;


class Assignment implements Serializable {
    private static final long serialVersionUID = 7829136421241571165L;


}

class Topic implements Serializable {
    private static final long serialVersionUID = 7829136421241571165L;

    String name ;
    ArrayList <Assignment> assignments = new ArrayList<>();
    Topic(String name) {
        this.name = name ;
    }
}


class Class implements Serializable {
    private static final long serialVersionUID = 7829136421241571165L;
    String name ;
    String room ;
    String des ;
    ArrayList <User> teachers = new ArrayList<>() ;
    String code;
    ArrayList<User> students = new ArrayList<>() ;
    ArrayList<Topic> topics = new ArrayList<>() ;
    Class(String name , String room , String des) {
        this.name = name;
        this.room = room ;
        this.des = des  ;
    }


}


class DataBase {
    static ArrayList <User> users;

    // Read Users From File
    static {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("users.ser"))) {
            users = (ArrayList<User>) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            users = new ArrayList<>();
        }
    }

    // Save Users in File
    static  void save() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("users.ser"))) {
            outputStream.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}




class User implements Serializable {
    private static final long serialVersionUID = 7829136421241571165L;
    String username ;
    String password ;
    byte[] avatar;
    ArrayList<Class> classes = new ArrayList<>() ;
    User(String username , String password , byte[] avatar) {
        this.username = username;
        this.password = password;
        this.avatar =  avatar ;
    }



}


class ClientHandler extends Thread{
    Socket s ;
    ObjectOutputStream oos ;
    ObjectInputStream ois;
    DataOutputStream dos ;


    // Find The com.example.googleclassroom.User
    static private User findUser(String username, String password) {
        for (User user:DataBase.users) {
            if (user.username.equals(username) && user.password.equals(password))
                return user ;
        }
        return null ;
    }

    static private Class findClass(String code) {
        for (User user:DataBase.users ) {
            for (Class cls:user.classes) {
                if (cls.code.equals(code))
                    return cls ;
            }
        }
        return null ;

    }

    // Check if Username Exists
    static private Boolean UserNameCheck(String username) {
        for(User user:DataBase.users) {
            if (user.username.equals(username))
                return false;
        }
        return true;
    }

    ClientHandler(Socket cs) {
        this.s = cs ;
    }
    @Override
    public void run() {
        try {
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            String[] a = (String[])ois.readObject();
            System.out.println(a[0]);

            if(a[0].equals("Login")) {
                System.out.println("Check For Login");
                String username = a[1];
                String password = a[2];
                User user = findUser(username ,password) ;
                if (user !=null) {
                    System.out.println("Found");
                    oos.writeBoolean(true);
                    oos.flush();
                    oos.writeObject(user);
                    oos.flush();
                }
                else {
                    System.out.println("Wrong U & P");
                    oos.writeBoolean(false);
                    oos.flush();

                }

            }
            else if (a[0].equals("Register")) {
                System.out.println("Request For Register");
                String username = a[1];
                String password = a[2];
                byte[] imgByte = (byte[])ois.readObject();
                ByteArrayInputStream bis = new ByteArrayInputStream(imgByte);
                BufferedImage bImage2 = ImageIO.read(bis);
                if (bImage2 ==null) {
                    bImage2 = ImageIO.read(new File("avatar.jpg"));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(bImage2, "jpg", bos );
                    imgByte= bos.toByteArray();
                    System.out.println("null image");
                }
                System.out.println(Arrays.toString(imgByte));
                User newUser = new User(username,password , imgByte);
                DataBase.users.add(newUser) ;
                DataBase.save();

                oos.writeBoolean(true);
                oos.flush();
                oos.writeObject(newUser);
                oos.flush();

            }
            else if (a[0].equals("UserNameCheck")) {
                System.out.println("Check For Username");
                String username = a[1];
                oos.writeBoolean(UserNameCheck(username));
                oos.flush();
            }
            else if (a[0].equals("AddClass")) {
                String username = a[1];
                String password = a[2];
                Class clas = new Class(a[3],a[4],a[5]) ;
                User user = findUser(username , password);
                if (user !=null) {
                    clas.teachers.add(user) ;
                    clas.code =  UUID.randomUUID().toString() ;
                    System.out.println(clas.code);
                    user.classes.add(clas);
                    DataBase.save();
                    System.out.println(clas.name);
                    oos.writeBoolean(true);
                    oos.flush();
                    oos.writeObject(clas);
                    oos.flush();

                }

            }
            else if (a[0].equals("Refresh")) {
                String username = a[1];
                String password = a[2];
                User user = findUser(username , password);
                if (user !=null) {
                    oos.writeObject(user);
                    oos.flush();
                }
            }
            else if (a[0].equals("JoinClass")) {
                String username = a[1];
                String password = a[2];
                User user = findUser(username, password);
                if (user!=null) {
                    String code = (String)ois.readObject();
                    Class cls = findClass(code);
                    if (cls !=null) {
                        boolean check= false ;
                        for (User usr:cls.teachers)
                            if (usr.username.equals(user.username))
                                check = true ;
                        if (!check) {
                            if (!cls.students.contains(user))
                                cls.students.add(user);
                            if (!user.classes.contains(cls))
                                user.classes.add(cls);
                        }
                        oos.writeBoolean(true);
                        oos.flush();
                        oos.writeObject(cls);
                        oos.flush();
                    }
                    else {
                        oos.writeBoolean(false);
                        oos.flush();
                    }
                }
            }
            else if (a[0].equals("RemoveFromClass")) {
                String username = a[1];
                String password = a[2];
                String code = a[3] ;
                User user = findUser(username,password);
                if (user!=null) {
                    Class cls = findClass(code);
                    cls.students.remove(user);
                    user.classes.remove(cls);

                }
            }
            else if (a[0].equals("RefreshCLW" )) {
                String username = a[1];
                String password = a[2];
                String code = a[3];
                User user = findUser(username,password);
                Class cls = findClass(code);
                if (user!=null && cls!=null) {
                    oos.writeObject(user);
                    oos.flush();
                    oos.writeObject(cls);
                    oos.flush();
                }
            }
            else if (a[0].equals("CreateTopic")) {
                String code = a[1];
                String name = a[2];
                Topic topic = new Topic(name);
                Class cls = findClass(code);
                if (cls!=null){
                    cls.topics.add(topic);
                }

            }



            oos.close();
            ois.close();
            s.close();

        }catch (Exception e){
            e.printStackTrace();
            DataBase.save();
        }

        System.out.println("Client Exited");
    }
}

public class Main {

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(8080) ;

        while (true) {
            Socket cs =  ss.accept();
            System.out.println("Client Connected");
            ClientHandler newClient =  new ClientHandler(cs);
            newClient.start();

        }
    }
}
