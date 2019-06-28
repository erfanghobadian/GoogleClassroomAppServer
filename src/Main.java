import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;


class UserSend implements Serializable {
    String username ;
    String password ;
    byte[] avatar;
    UserSend(String username , String password , byte[] avatar) {
        this.username = username;
        this.password = password;
        this.avatar =  avatar ;
    }
}

class User implements Serializable {
    static ArrayList <User> users;
    String username ;
    String password ;
    byte[] avatar;
    User(String username , String password , byte[] avatar) {
        this.username = username;
        this.password = password;
        this.avatar =  avatar ;
        users.add(this) ;
        save();
    }



    // Read Users From File
    static {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("users.ser"))) {
            users = (ArrayList<User>) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            users = new ArrayList<>();
        }
    }

    // Save Users in File
    static private void save() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("users.ser"))) {
            outputStream.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}


class ClientHandler extends Thread{
    Socket s ;
    ObjectOutputStream oos ;
    ObjectInputStream ois;
    DataOutputStream dos ;


    // Find The User
    static private User findUser(String username, String password) {
        for (User user:User.users) {
            if (user.username.equals(username) && user.password.equals(password))
                return user ;
        }
        return null ;
    }

    // Check if Username Exists
    static private Boolean UserNameCheck(String username) {
        for(User user:User.users) {
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
                    oos.writeObject(user.username);
                    oos.flush();
                    oos.writeObject(user.password);
                    oos.flush();
                    oos.writeObject(user.avatar);
//                    System.out.println(Arrays.toString(user.avatar));
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
                oos.writeBoolean(true);
                oos.flush();
                oos.writeObject(newUser.username);
                oos.flush();
                oos.writeObject(newUser.password);
                oos.flush();
                oos.writeObject(newUser.avatar);
                oos.flush();

            }
            else if (a[0].equals("UserNameCheck")) {
                System.out.println("Check For Username");
                String username = a[1];
                oos.writeBoolean(UserNameCheck(username));
                oos.flush();
            }



            oos.close();
            ois.close();
            s.close();

        }catch (Exception e){
            e.printStackTrace();
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
