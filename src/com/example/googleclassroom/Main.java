package com.example.googleclassroom;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


class HomeWork implements Serializable {
    private static final long serialVersionUID = 7829136421241571165L;
    String text ;
    User student ;
    byte [] attach ;
    int score ;
}

class Assignment implements Serializable {
    private static final long serialVersionUID = 7829136421241571165L;
    String title ;
    String des ;
    int points ;
    byte[] attach ;
    Calendar due ;
    Topic topic ;

    HashMap<String,HomeWork> works = new HashMap<>();


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
                    clas.topics.add(new Topic("No Topic"));
                    user.classes.add(clas);
                    DataBase.save();
                    System.out.println(clas.name);
                    oos.writeBoolean(true);
                    oos.flush();
                    oos.writeObject(clas);
                    oos.flush();

                }
                DataBase.save();

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
                DataBase.save();

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
                DataBase.save();

            }
            else if (a[0].equals("RefreshCLW" )) {
                String username = a[1];
                String password = a[2];
                String code = a[3];
                System.out.println(username);
                System.out.println(password);
                System.out.println(code);
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
                DataBase.save();


            }
            else if (a[0].equals("EditTopic")) {
                String code = a[1];
                String tname = a[2];
                String name = a[3] ;
                Class cls = findClass(code);
                if (cls!=null){
                    for (Topic t:cls.topics){
                        if (t.name.equals(tname)) {
                            t.name = name ;
                            break;
                        }
                    }
                }
                DataBase.save();


            }
            else if (a[0].equals("RemoveTopicFromClass")) {
                String code = a[1];
                String name = a[2];
                Class myClass = findClass(code);
                for(Topic t :myClass.topics) {
                    if (t.name.equals(name)) {
                        myClass.topics.remove(t);
                        break;
                    }

                }
                DataBase.save();

            }
            else if (a[0].equals("AddStudentToClass")) {
                String username = a[1] ;
                String code = a[2];
                System.out.println(username);

                User user = null ;
                for (User us:DataBase.users) {
                    if (us.username.equals(username)) {
                        user = us;
                        break;
                    }

                }
                if (user == null){
                    oos.writeBoolean(false);
                    oos.flush();
                }
                else {
                    oos.writeBoolean(true);
                    oos.flush();
                    Class cls = findClass(code);
                    if (cls!=null) {
                        cls.students.add(user);
                        user.classes.add(cls);
                    }
                }
                DataBase.save();

            }
            else if (a[0].equals("AddTeacherToClass")) {
                String username = a[1];
                String code = a[2];
                System.out.println(code);
                Class myclass = findClass(code);
                if (myclass!=null) {
                    User user = null ;
                    for (User us:DataBase.users) {
                        if(us.username.equals(username)) {
                            user = us ;
                            break;
                        }
                    }
                    if(user!=null) {
                        myclass.teachers.add(user);
                        user.classes.add(myclass);
                        oos.writeBoolean(true);
                        oos.flush();

                    }
                    else {
                        oos.writeBoolean(false);
                        oos.flush();
                    }
                }
                DataBase.save();

            }
            else if (a[0].equals("EditClass")) {
                String code = a[1];
                String clsname = a[2];
                String clsdes = a[3];
                String clsroom = a[4];
                Class myclass = findClass(code) ;
                if (myclass!=null) {
                    myclass.name = clsname ;
                    myclass.des  = clsdes ;
                    myclass.room = clsroom ;
                    oos.writeObject(myclass);
                    oos.flush();
                }
                DataBase.save();
            }
            else if (a[0].equals("CreateAssignment")) {
                String code  = a[1];
                String topicname = a[2];
                String asstitle = a[3] ;
                Class cls = findClass(code);
                int year = Integer.parseInt(a[4]);
                int month = Integer.parseInt(a[5]);
                int day = Integer.parseInt(a[6]);
                int hour = Integer.parseInt(a[7]);
                int min = Integer.parseInt(a[8]);
                String des = a[9] ;
                byte[] attach = (byte[])ois.readObject() ;
                int points = Integer.parseInt(a[10]) ;
                Calendar due = new GregorianCalendar();
                due.set(year,month,day,hour,min);
                Assignment ass = new Assignment() ;
                ass.title = asstitle ;
                ass.due = due ;
                ass.des = des ;
                ass.points = points ;
                ass.attach = attach ;
                System.out.println(ass.des);
                System.out.println(ass.points);
                if  (ass.attach !=null)
                    System.out.println(Arrays.toString(ass.attach));

                for (Topic t :cls.topics) {
                    if (t.name.equals(topicname)) {
                        System.out.println(t.name);
                        ass.topic = t;
                        t.assignments.add(ass);

                        break;
                    }
                }
                System.out.println(ass.title);
                DataBase.save();

            }
            else if (a[0].equals("EditAssignment")) {
                String code  = a[1];
                String topicname = a[2];
                String asstitle = a[3] ;
                Class cls = findClass(code);
                int year = Integer.parseInt(a[4]);
                int month = Integer.parseInt(a[5]);
                int day = Integer.parseInt(a[6]);
                int hour = Integer.parseInt(a[7]);
                int min = Integer.parseInt(a[8]);
                String des = a[9] ;
                byte[] attach = (byte[])ois.readObject() ;
                int points = Integer.parseInt(a[10]) ;
                String ctname = a[11];
                String cassname = a[12] ;
                Calendar due = new GregorianCalendar();
                due.set(year,month,day,hour,min);

                Topic t2 = null ;
                for (Topic t :cls.topics) {
                    if (t.name.equals(ctname)) {
                        System.out.println(t.name);
                        t2 = t ;
                        break;
                    }
                }
                Assignment ass = null ;
                for (Assignment ass2 :t2.assignments) {
                    if(ass2.title.equals(cassname)) {
                        ass = ass2 ;
                        break;
                    }
                }

                ass.title = asstitle ;
                ass.due = due ;
                ass.des = des ;
                ass.points = points ;
                ass.attach = attach ;
                System.out.println(ass.des);
                System.out.println(ass.points);
                if  (ass.attach !=null)
                    System.out.println(Arrays.toString(ass.attach));

                for (Topic t :cls.topics) {
                    if (t.name.equals(topicname)) {
                        System.out.println(t.name);
                        ass.topic = t;
                        t.assignments.add(ass);

                        break;
                    }
                }
                System.out.println(ass.title);
                t2.assignments.remove(ass)  ;
                DataBase.save();

            }
            else if (a[0].equals("RemoveAss")) {
                String code = a[1];
                String tname = a[2] ;
                String assname = a[3];
                Topic t2  = null;
                Class myClass = findClass(code) ;
                for (Topic t :myClass.topics) {
                    if (t.name.equals(tname)) {
                        t2 = t ;
                        System.out.println("hey");
                        break;
                    }
                }
                for (Assignment ass : t2.assignments) {
                    if (ass.title.equals(assname)) {
                        t2.assignments.remove(ass) ;
                        System.out.println("rem");
                        break;
                    }
                }
                DataBase.save();
            }
            else if (a[0].equals("AddHomeWork")) {
                String code = a[1] ;
                String username = a[2];
                String password = a[3];
                String tname = a[4];
                String assname = a[5];
                String text  = a[6] ;
                byte[] attach = (byte[])ois.readObject() ;
                Class cls = findClass(code) ;
                User user = findUser(username,password) ;
                Topic t2 =null ;
                for (Topic t:cls.topics ) {
                    if (t.name.equals(tname)) {
                        t2 = t ;
                        break;
                    }
                }
                Assignment ass = null ;
                for (Assignment ass2 : t2.assignments) {
                    if (ass2.title.equals(assname)) {
                        ass = ass2 ;
                        break;
                    }
                }

                HomeWork hm = new HomeWork();
                hm.text = text ;
                hm.attach = attach ;
                hm.student = user ;
                ass.works.put(user.username , hm) ;
                DataBase.save();
            }
            else if (a[0].equals("RefreshSTASS")) {
                String username = a[1];
                String password = a[2];
                String code = a[3] ;
                String tname = a[4] ;
                String asst = a[5] ;
                User  user = findUser(username, password) ;
                Class cls = findClass(code) ;
                Topic t = null ;
                Assignment ass = null ;
                for (Topic t2: cls.topics) {
                    if (t2.name.equals(tname)) {
                        t = t2 ;
                        break;
                    }
                }
                for (Assignment ass2 :t.assignments) {
                    if (ass2.title.equals(asst)) {
                        ass = ass2 ;
                        break;
                    }
                }

                oos.writeObject(user);
                oos.flush();
                oos.writeObject(cls);
                oos.flush();
                oos.writeObject(ass);
                oos.flush();
                DataBase.save();
            }
            else if (a[0].equals("MarkStudent")) {
                String code = a[1];
                String tname = a[2];
                String asst = a[3] ;
                String username = a[4] ;
                int point = Integer.parseInt(a[5]) ;
                Class cls = findClass(code) ;
                Topic t = null ;
                Assignment ass = null ;
                for (Topic t2: cls.topics) {
                    if (t2.name.equals(tname)) {
                        t = t2 ;
                        break;
                    }
                }

                for (Assignment ass2 : t.assignments) {
                    if (ass2.title.equals(asst)) {
                        ass = ass2;
                        break;
                    }
                }
                ass.works.get(username).score = point ;
                DataBase.save();
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
